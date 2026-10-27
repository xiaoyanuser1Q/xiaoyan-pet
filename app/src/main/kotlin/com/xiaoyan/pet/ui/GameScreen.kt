package com.xiaoyan.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.data.PetState
import com.xiaoyan.pet.game.DayPhase
import com.xiaoyan.pet.game.GameLogic
import com.xiaoyan.pet.game.Rules
import com.xiaoyan.pet.game.Scenes
import com.xiaoyan.pet.game.StoryData
import kotlinx.coroutines.delay

/**
 * 主养成屏 (v1.3.0 场景制横屏 VN):
 * - 左 45%: 立绘 (换装优先级 + fallback 直切帧) + 紧凑状态 + 星光粒子
 * - 右 55%: 幕/日时 + 场景 tab + 对话框 (打字机) / 抉择 + 当前场景互动锚点
 * - 离线回归: 先温情台词, warn 延后 30s 每回归周期 1 次 (v3 裁定⑤)
 */
@Composable
fun GameScreen() {
    val p = PetApp.pet
    if (p == null || !p.started) return
    val pet = p

    if (pet.ended.isNotEmpty()) {
        if (pet.endingSeen) EndedScene() else EndingWait()
        return
    }

    val repo = PetApp.repo
    val lowSpec = repo.lowSpecOn()
    val act = Rules.actOf(pet.level)
    val phase = DayPhase.current(repo, pet.asleep, System.currentTimeMillis())

    // ---------- 当前场景 (会话内记忆; 解锁变化时回落客厅) ----------
    var scene by remember { mutableStateOf(Scenes.LIVINGROOM) }
    if (scene !in Rules.unlockedScenes(act)) scene = Scenes.LIVINGROOM
    val costume = Scenes.costumeFor(act, phase, scene)
    val hotspots = Scenes.hotspots(scene)

    // ---------- VN 台词 ----------
    var line by remember { mutableStateOf<StoryData.Line?>(null) }
    var warmPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 离线回归: 温情台词优先 (粒子小特效已随立绘层开启)
        if (PetApp.warmReturnPending.value && !warmPlayed) {
            line = StoryData.WARM_RETURN
            PetApp.consumeWarmReturn()
            warmPlayed = true
            // warn (饱食<30) 延后 30s, 每回归周期仅 1 次, 不与温情台词同屏
            delay(30_000L)
            val petNow = PetApp.pet
            if (petNow != null && petNow.satiety < 30 && petNow.ended.isEmpty()) {
                line = StoryData.WARN_SATIE
                PetApp.audio.playSfx("sfx_13_warn")
            }
        }
    }

    Row(Modifier.fillMaxSize()) {
        // ---------- 左 45%: 立绘 + 状态 ----------
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
        ) {
            PortraitSprite(costume = costume, mood = currentMoodOf(pet, line?.mood), lowSpec = lowSpec)
            StarParticles(on = !lowSpec)

            // 紧凑状态面板
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .width(172.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
            ) {
                Text(
                    "${pet.name} · Lv.${pet.level} · ${actLabel(act)}",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                MiniStat("饱食", pet.satiety, Color(0xFFFFB7C8))
                MiniStat("心情", pet.mood, Color(0xFFFFD76A))
                MiniStat("体力", pet.energy, Color(0xFF9AE6B4), maxV = pet.energyCap())
                MiniStat("亲密", pet.intimacy, Color(0xFFB7A8FF))
                Text("经验 ${pet.xp}/${pet.xpNeed()} · 今日 ${pet.xpToday}/45", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                ThreeStars(pet)
            }

            // 场景指示
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Scenes.icon(scene) + " " + Scenes.label(scene), fontSize = 12.sp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(DayPhase.icon(phase) + " " + DayPhase.label(phase), fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }

        // ---------- 右 55%: 场景 + 台词 + 抉择 + 互动 ----------
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .padding(14.dp),
        ) {
            // 场景 tab 行
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // for 循环而非 forEach: 循环体内直接调 @Composable SceneTab (非 composable lambda 里调会编译错)
                for (sc in Scenes.ALL) {
                    val unlocked = sc in Rules.unlockedScenes(act)
                    SceneTab(sc, unlocked = unlocked, active = sc == scene, modifier = Modifier.weight(1f)) {
                        if (unlocked) {
                            scene = it
                            PetApp.audio.playSfx("sfx_01_tap")
                        }
                    }
                }
            }

            // 抉择 (Lv20/45/70/95; 100 前未选 = 结局前强制)
            val pendingKey = StoryData.pendingChoiceLevel(pet.level, pet.choices)
            if (pendingKey != null) {
                ChoiceBlock(pendingKey)
            } else {
                DialogueBox(line = line)
            }
            Spacer(Modifier.weight(1f))

            // 互动锚点 (当前场景; 体力 0 锁耗体力按钮, S13)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                hotspots.forEach { h ->
                    val id = if (scene == Scenes.BEDROOM && h.id == "sleep" && pet.asleep) "wake" else h.id
                    val label = if (id == "wake") "起床" else h.label
                    val disabled = id != "wake" && Rules.consumesEnergy(id) && pet.energy <= 0
                    HotspotButton(
                        text = label,
                        enabled = !disabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        PetApp.doInteraction(id)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "体力 ${pet.energy}/${pet.energyCap()} · 书架 ${pet.dailyBookshelf}/3 · 电视 ${pet.dailyTv}/2" +
                    if (pet.energy <= 0) " · 力竭了（睡觉回满）" else "",
                fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

private fun actLabel(act: Int): String = when (act) {
    1 -> "一幕 · 春"
    2 -> "二幕 · 夏"
    3 -> "三幕 · 秋"
    else -> "四幕 · 冬"
}

/** 紧凑状态条 (立绘左上角; maxV 用于体力等非常规 100 上限) */
@Composable
private fun MiniStat(label: String, value: Int, color: Color, maxV: Int = 100) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.width(32.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / maxV.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(3.dp)),
            )
        }
        Text("${value}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp))
    }
}

/** 对话框: 打字机 27ms/字, 点击立即完成 */
@Composable
private fun DialogueBox(line: StoryData.Line?) {
    val text = line?.text ?: ""
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        while (shown < text.length) {
            delay(27)
            shown = shown + 1
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .clickable {
                if (shown < text.length) shown = text.length   // 点击加速
            }
            .padding(14.dp),
    ) {
        if (text.isEmpty()) {
            Text("点场景里的小物件，和小桃互动。", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
        } else {
            Text(
                text.take(shown) + if (shown < text.length) "▌" else "",
                fontSize = 15.sp,
                color = Color.White,
                lineHeight = 22.sp,
            )
        }
        if (line != null && shown >= text.length) {
            Text("▼", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun SceneTab(sc: String, unlocked: Boolean, active: Boolean, modifier: Modifier = Modifier, onClick: (String) -> Unit) {
    Box(
        modifier = modifier
            .background(
                when {
                    active -> Color(0xFFFF8FB8).copy(alpha = 0.5f)
                    unlocked -> Color.White.copy(alpha = 0.12f)
                    else -> Color.White.copy(alpha = 0.04f)
                },
                RoundedCornerShape(10.dp),
            )
            .clickable { onClick(sc) }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (if (unlocked) Scenes.icon(sc) else "🔒") + " " + Scenes.label(sc),
            fontSize = 11.sp,
            color = if (unlocked) Color.White else Color.White.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun HotspotButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (enabled) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(14.dp),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (enabled) text else "💤 $text",
            fontSize = 14.sp,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
        )
    }
}

/** 心意三枚星 (G2 三态: 微光/半亮/全亮, 不剧透数值) */
@Composable
private fun ThreeStars(p: PetState) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        StarDot("温", Rules.starState(p.warmthLine))
        StarDot("好", Rules.starState(p.curiosityLine))
        StarDot("诺", Rules.starState(p.promiseLine))
        Spacer(Modifier.weight(1f))
        Text("心意", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
private fun StarDot(label: String, state: Int) {
    val color = when (state) {
        2 -> Color(0xFFFFE08A)
        1 -> Color(0xFFFFE08A).copy(alpha = 0.6f)
        else -> Color(0xFFFFE08A).copy(alpha = 0.25f)
    }
    Row(
        modifier = Modifier.padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✦", fontSize = 11.sp, color = color)
        Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.55f))
    }
}

@Composable
private fun ChoiceBlock(key: String) {
    val pet = PetApp.pet ?: return
    val choice = StoryData.choiceByKey(key)
    // Lv95: 「真相」选项仅三线 ≥80 时出现 (判定在 Lv95 加分前, R1)
    val opts = if (key == "95") choice.options.filter { it.id != "truth" || GameLogic.canTruth(pet) }
        else choice.options
    var promptShown by remember { mutableIntStateOf(0) }
    LaunchedEffect(choice) {
        while (promptShown < choice.prompt.text.length) {
            delay(27)
            promptShown = promptShown + 1
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(
            choice.prompt.text.take(promptShown),
            fontSize = 14.sp, color = Color.White, lineHeight = 20.sp,
        )
    }
    Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxWidth()) {
        // for 循环而非 forEachIndexed: 循环体内直接调 @Composable (非 composable lambda 里调会编译错)
        for (i in opts.indices) {
            StaggeredOption(i) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        .clickable {
                            PetApp.audio.playSfx("sfx_15_choice")
                            PetApp.doChoice(key, opts[i].id)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(opts[i].label, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
                }
            }
            if (i < opts.size - 1) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun StaggeredOption(index: Int, content: @Composable () -> Unit) {
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(180L * (index + 1))
        placed = true
    }
    AnimatedVisibility(
        visible = placed,
        enter = slideIn(animationSpec = tween(200)) { IntOffset(0, (it.height * 0.2f).toInt()) } + fadeIn(tween(200)),
    ) {
        content()
    }
}

/** 结局尚未读完时的占位画面 (EndingOverlay 覆盖) */
@Composable
private fun EndingWait() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦ ✧ ✦", fontSize = 36.sp, color = Color(0xFFFFF3C4))
            Spacer(Modifier.height(10.dp))
            Text("……", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

/** 结局后的主屏: 左立绘 (C = 星光裙 + 粒子) + 右文案 + 重新开始 */
@Composable
private fun EndedScene() {
    val p = PetApp.pet ?: return
    val costume = if (p.ended == "C") Scenes.COSTUME_STAR else Scenes.COSTUME_DAILY
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(0.45f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            PortraitSprite(costume = costume, mood = if (p.ended == "A") "sad" else "happy", lowSpec = PetApp.repo.lowSpecOn())
            if (p.ended == "C") StarParticles(on = !PetApp.repo.lowSpecOn())
        }
        Column(
            modifier = Modifier.weight(0.55f).fillMaxHeight().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("✦ ✧ ✦", fontSize = 30.sp, color = Color(0xFFFFF3C4))
            Spacer(Modifier.height(12.dp))
            Text(StoryData.endingTitle(p.ended), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                when (p.ended) {
                    "A" -> "「春风归途」——她带着记忆去了信里的小镇。"
                    "B" -> "「小镇永远」——小屋的灯，总有人留着。"
                    else -> "「永恒的约定」——烟花散进冬天，她看着这个小屋。"
                },
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .background(Color(0xFFFF8FB8), RoundedCornerShape(14.dp))
                    .clickable {
                        PetApp.audio.playSfx("sfx_12_levelup")
                        PetApp.audio.stopFinale()
                        PetApp.repo.clearState()
                        PetApp.pet = null
                        PetApp.bump()
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("开启新的相伴", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C1230))
            }
        }
    }
}
