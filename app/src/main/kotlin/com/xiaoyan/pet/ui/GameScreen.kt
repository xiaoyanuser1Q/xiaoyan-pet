package com.xiaoyan.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.data.PetState
import com.xiaoyan.pet.game.ActionResult
import com.xiaoyan.pet.game.GameLogic
import com.xiaoyan.pet.game.Story
import com.xiaoyan.pet.game.StoryLine
import kotlinx.coroutines.delay

/**
 * 主屏 (v1.2.0 横屏 VN 版):
 * - 左 45%: 小桃立绘 (ContentScale.Fit + BottomCenter, 零裁切, 审计②) + 顶部紧凑状态
 * - 右 55%: 对话框 (打字机 25-30ms/字 + 点击加速) / 抉择 (stagger 入场) / 动作栏
 * - 台词带 mood → 立绘自动切表情 (Crossfade + 上滑 200ms)
 */
@Composable
fun GameScreen() {
    val pet = PetApp.pet
    if (pet == null || !pet.started) return
    val p = pet

    if (p.ended.isNotEmpty()) {
        if (p.endingSeen) EndedScene(p) else EndingWait()
        return
    }

    val lowSpec = PetApp.repo.lowSpecOn()

    // VN 台词状态: 当前台词 (打字机), null = 无
    var line by remember { mutableStateOf<StoryLine?>(null) }
    var talkOpen by remember { mutableStateOf(false) }

    val actNeed = GameLogic.pendingChoice(p)
    val showChoice = actNeed != null && p.level >= Story.choiceLevel(actNeed!!)

    Row(Modifier.fillMaxSize()) {
        // ---------- 左 45%: 立绘 + 状态 (点立绘 = 抚摸) ----------
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .clickable {
                    var r: ActionResult? = null
                    PetApp.doAction { s -> GameLogic.pet(s).also { r = it } }
                    r?.line?.let { ln -> line = ln }
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val mood = line?.mood ?: when {
                p.isUnwell() -> PortraitMood.SAD
                p.mood >= 80 -> PortraitMood.HAPPY
                p.mood <= 40 -> PortraitMood.SAD
                else -> PortraitMood.NEUTRAL
            }
            PortraitSprite(mood = mood, lowSpec = lowSpec)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .width(150.dp)
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
            ) {
                Text("${p.name} · Lv.${p.level}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                MiniStat("饥饿", p.hunger, Color(0xFFFFB7C8))
                MiniStat("心情", p.mood, Color(0xFFFFD76A))
                MiniStat("健康", p.health, Color(0xFF9AE6B4))
                MiniStat("亲密", p.intimacy, Color(0xFFB7A8FF))
                Text("经验 ${p.xp}/${p.xpNeed()}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        // ---------- 右 55%: 对话框 + 选项 + 动作栏 ----------
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .imePadding()
                .padding(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(actLabel(p.act), fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.width(10.dp))
                if (p.missingThing.isNotBlank()) {
                    Text("找回：${p.missingThing}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(8.dp))

            if (showChoice && actNeed != null) {
                ChoiceBlock(act = actNeed, onPick = { idx ->
                    var r: ActionResult? = null
                    PetApp.doAction { s -> GameLogic.applyChoice(s, actNeed, idx).also { r = it } }
                    r?.line?.let { ln -> line = ln }
                })
            } else {
                DialogueBox(line = line)
            }
            Spacer(Modifier.weight(1f))

            TalkRow(open = talkOpen, onToggle = { talkOpen = it }, onLine = { ln -> line = ln }, p = p)
            Spacer(Modifier.height(8.dp))
            ActionRow(
                onPick = { key ->
                    if (key == "talk") {
                        talkOpen = true
                    } else {
                        var r: ActionResult? = null
                        when (key) {
                            "feed" -> PetApp.doAction { s -> GameLogic.feed(s).also { r = it } }
                            "play" -> PetApp.doAction { s -> GameLogic.play(s).also { r = it } }
                            "rest" -> PetApp.doAction { s -> GameLogic.rest(s).also { r = it } }
                            "pet" -> PetApp.doAction { s -> GameLogic.pet(s).also { r = it } }
                        }
                        r?.line?.let { ln -> line = ln }
                        // 随机小事件 (15%, 每条只触发一次)
                        val ev = GameLogic.rollRandomEvent(p)
                        if (ev != null) PetApp.pushEvent(ev)
                    }
                },
            )
        }
    }
}

private fun actLabel(act: Int): String = when (act) {
    3 -> "第三幕 · 抉择"
    2 -> "第二幕 · 异变"
    else -> "第一幕 · 相遇"
}

/** 紧凑状态条 (立绘左上角) */
@Composable
private fun MiniStat(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.width(30.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value / 100f)
                    .background(color, RoundedCornerShape(3.dp)),
            )
        }
        Text("${value}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp))
    }
}

/** 对话框: 打字机 27ms/字, 点击立即完成 */
@Composable
private fun DialogueBox(line: StoryLine?) {
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
            .height(120.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .clickable {
                if (shown < text.length) shown = text.length // 点击加速
            }
            .padding(14.dp),
    ) {
        if (text.isEmpty()) {
            Text("点一点小桃，或从下面的动作里挑一个。", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
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

/** 抉择: 二选一, 选项 stagger 逐条入场 */
@Composable
private fun ChoiceBlock(act: Int, onPick: (Int) -> Unit) {
    val choice = Story.choiceFor(act)
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
            .height(96.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            choice.prompt.text.take(promptShown),
            fontSize = 14.sp,
            color = Color.White,
            lineHeight = 20.sp,
        )
    }
    Spacer(Modifier.height(10.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        choice.options.forEachIndexed { i, opt ->
            StaggeredOption(i) {
                ChoiceOptionButton(opt.label) { onPick(i) }
            }
            if (i < choice.options.size - 1) Spacer(Modifier.height(8.dp))
        }
    }
}

/** 带 stagger 入场的选项 */
@Composable
private fun StaggeredOption(index: Int, content: @Composable () -> Unit) {
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(180L * (index + 1))
        placed = true
    }
    AnimatedVisibility(
        visible = placed,
        enter = slideIn(animationSpec = tween(200)) { IntOffset(0, it.height * 0.2f) } + fadeIn(tween(200)),
        exit = fadeOut(tween(150)),
    ) {
        content()
    }
}

@Composable
private fun ChoiceOptionButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 14.sp,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

/** 谈心一行 (横屏压缩版, 键盘只顶右栏) */
@Composable
private fun TalkRow(open: Boolean, onToggle: (Boolean) -> Unit, onLine: (StoryLine) -> Unit, p: PetState) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!open) {
            Text(
                "💬 点这儿和小桃说心里话",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable { onToggle(true) }
                    .padding(vertical = 8.dp),
            )
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(60) },
                placeholder = { Text("想说给小桃听的话…", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = fieldColors(),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PrimaryButton(
                text = "说",
                onClick = {
                    if (text.isNotBlank()) {
                        var r: ActionResult? = null
                        PetApp.doAction { s -> GameLogic.talk(s, text.trim()).also { r = it } }
                        onLine(r?.line ?: StoryLine(r?.message ?: "", PortraitMood.NEUTRAL))
                    }
                    text = ""
                    onToggle(false)
                },
                modifier = Modifier.width(70.dp),
            )
        }
    }
}

/** 动作栏 (横屏紧凑 5 键) */
@Composable
private fun ActionRow(onPick: (String) -> Unit) {
    val actions = listOf(
        Triple("🍎", "喂食", "feed"),
        Triple("🎮", "玩耍", "play"),
        Triple("😴", "休息", "rest"),
        Triple("✋", "抚摸", "pet"),
        Triple("💬", "谈心", "talk"),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        actions.forEach { (icon, label, key) ->
            Column(
                modifier = Modifier
                    .clickable { onPick(key) }
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(icon, fontSize = 20.sp)
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

/** 结局尚未读完时的占位画面 (EndingOverlay 会覆盖在上面) */
@Composable
private fun EndingWait() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦ ✧ ✦", fontSize = 36.sp, color = Color(0xFFFFF3C4))
            Spacer(Modifier.height(10.dp))
            Text("……", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

/** 结局后的主屏 (横屏): 左立绘 + 右文案 + 重新开始 */
@Composable
private fun EndedScene(p: PetState) {
    Row(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            PortraitSprite(mood = PortraitMood.NEUTRAL, lowSpec = PetApp.repo.lowSpecOn())
        }
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("✦ ✧ ✦", fontSize = 32.sp, color = Color(0xFFFFF3C4))
            Spacer(Modifier.height(14.dp))
            Text(
                when (p.ended) {
                    "B" -> "她说：「${p.endingFirstWord.ifBlank { "……" }}」"
                    else -> "她散进了星光里"
                },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when (p.ended) {
                    "A" -> "「归途」——星光替你照亮回家的路。"
                    "B" -> "「永恒」——你们都不再孤单。"
                    else -> "「消散」——掌心还留着一点温度。"
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = "开启新的相伴",
                onClick = {
                    PetApp.audio.playSfx("sfx_levelup")
                    PetApp.repo.clearState()
                    PetApp.pet = null
                    PetApp.bump()
                },
                modifier = Modifier.width(220.dp),
            )
        }
    }
}
