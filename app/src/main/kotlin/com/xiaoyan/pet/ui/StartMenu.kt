package com.xiaoyan.pet.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.R
import com.xiaoyan.pet.data.SaveRepository
import com.xiaoyan.pet.game.DayPhase
import com.xiaoyan.pet.game.Scenes
import kotlinx.coroutines.delay

/**
 * 开始菜单主页 (v1.3.0 新增, 方案 §4-1):
 * - 主图 = 小桃大立绘 (呼吸动画, Fit 底对齐零裁切) + 星光粒子
 * - 表情条 = 当前套装表情缩略图横排 (当前心情 accent 高亮), 点击放大预览
 * - 入口按钮行: 开始 / 继续 (有存档才显示) / 设置 / 检查更新
 * - 首次启动 → 主页 → 开始 → 开场; 有存档 → 主页为常驻入口
 */
@Composable
fun StartMenu() {
    val repo = PetApp.repo
    val pet = PetApp.pet
    var preview by remember { mutableStateOf<Int?>(null) }

    val phase = DayPhase.current(repo, pet?.asleep ?: false, System.currentTimeMillis())
    val scene = Scenes.LIVINGROOM
    val costume = Scenes.COSTUME_DAILY   // 主页固定日常套 (睡前氛围)
    val mood = currentMoodOf(pet ?: com.xiaoyan.pet.data.PetState(), null)
    val lowSpec = repo.lowSpecOn()

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF141024)),
    ) {
        // 背景 = 客厅·当前时段 (轻微暗化)
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Scenes.bgResFor(scene, phase)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }
        StarParticles(on = !lowSpec)

        Column(Modifier.fillMaxSize().padding(24.dp)) {
            // ---------- 标题 ----------
            Row(Modifier.fillMaxWidth()) {
                Text("小桃养成记", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(12.dp))
                Text(
                    "${DayPhase.icon(phase)} ${DayPhase.label(phase)} · v${PetApp.CURRENT_VERSION_NAME}",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f),
                )
            }
            Spacer(Modifier.height(14.dp))

            // ---------- 主图 + 表情条 (Column 作用域内, 可 weight) ----------
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        PortraitSprite(costume = costume, mood = mood, lowSpec = lowSpec)
                    }
                    // 表情条
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Scenes.expressionStrip(costume).forEach { (m, res) ->
                            val active = m == mood
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (active) Color(0xFFFF8FB8).copy(alpha = 0.45f)
                                        else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable { preview = res },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
                // 点击放大预览
                if (preview != null) {
                    Box(
                        Modifier.fillMaxSize().clickable { preview = null },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(preview!!),
                            contentDescription = null,
                            modifier = Modifier.height(180.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------- 入口按钮行 ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                // 无档 = 开始 (→ 序幕开场); 有档 = 继续 (AppRoot: Game 屏 + 未开始 → Onboarding)
                val fresh = pet?.started == false || pet == null
                if (fresh) {
                    MenuButton("✦ 开始", true) {
                        PetApp.audio.playSfx("sfx_01_tap")
                        PetApp.screen = com.xiaoyan.pet.Screen.Game
                    }
                } else {
                    MenuButton("▶ 继续", true) {
                        PetApp.audio.playSfx("sfx_01_tap")
                        PetApp.screen = com.xiaoyan.pet.Screen.Game
                        // 首次进入补播序幕
                        val p = PetApp.pet
                        PetApp.pushLines(PetApp.prologueLinesIfFresh(p) ?: emptyList())
                    }
                }
                Spacer(Modifier.width(10.dp))
                MenuButton("⚙ 设置", true) {
                    PetApp.audio.playSfx("sfx_01_tap")
                    PetApp.screen = com.xiaoyan.pet.Screen.Settings
                }
                Spacer(Modifier.width(10.dp))
                MenuButton("⇪ 检查更新", true) {
                    PetApp.audio.playSfx("sfx_01_tap")
                    PetApp.screen = com.xiaoyan.pet.Screen.Settings
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MenuButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(118.dp)
            .background(
                if (active) Color(0xFFFF8FB8) else Color.White.copy(alpha = 0.14f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) Color(0xFF5C1230) else Color.White.copy(alpha = 0.9f),
        )
    }
}
