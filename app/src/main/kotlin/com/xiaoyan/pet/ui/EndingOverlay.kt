package com.xiaoyan.pet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.xiaoyan.pet.game.Scenes
import com.xiaoyan.pet.game.StoryData

/**
 * 结局浮层 (v1.3.0 三结局: A 春风归途 / B 小镇永远 / C 永恒的约定·烟花)。
 * C = 星光山丘夜景 + 星光裙 + 粒子 + 终曲 (BGM 已在 PetApp 切); 逐行点击推进 (VN 式)。
 */
@Composable
fun EndingOverlay() {
    val p = PetApp.pet ?: return
    if (p.ended.isEmpty() || p.endingSeen) return

    val lines = StoryData.endingLines(p.ended, p.promiseAnswer)
    var idx by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }

    val isFinale = p.ended == "C"
    Box(
        Modifier.fillMaxSize()
            .background(Color(0xFF100E1E))
            .clickable {
                if (!done) idx = (idx + 1).coerceAtMost(lines.size)
            },
    ) {
        // 背景: C = 星空山丘夜景 (烟花场景); A/B = 对应幕夜景
        val bgRes = when (p.ended) {
            "C" -> Scenes.bgResFor(Scenes.HILL, "night")
            "A" -> Scenes.bgResFor(Scenes.HILL, "morning")
            else -> Scenes.bgResFor(Scenes.LIVINGROOM, "night")
        }
        Image(
            painter = painterResource(bgRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
            alpha = 0.9f,
        )
        if (isFinale) StarParticles(on = !PetApp.repo.lowSpecOn())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                StoryData.endingTitle(p.ended),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFF3C4),
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    lines.take(idx).forEach { ln ->
                        Text(
                            ln.text,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.92f),
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (idx < lines.size) {
                        Text("▼", fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f))
                    }
                }
            }
            if (done || idx >= lines.size) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.16f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .clickable {
                                done = true
                                p.endingSeen = true
                                PetApp.repo.saveState(p)
                                PetApp.audio.playSfx("sfx_01_tap")
                                PetApp.bump()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("继续相伴", fontSize = 14.sp, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFF8FB8), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .clickable {
                                done = true
                                PetApp.audio.playSfx("sfx_12_levelup")
                                PetApp.audio.stopFinale()
                                PetApp.repo.clearState()
                                PetApp.pet = null
                                PetApp.bump()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("新的相伴", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C1230))
                    }
                }
            }
        }
    }
}
