package com.xiaoyan.pet.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.data.PetState
import com.xiaoyan.pet.game.GameLogic

/** 主页: 小桃 + 数值 + 互动按钮 */
@Composable
fun GameScreen() {
    val pet = PetApp.pet
    if (pet == null || !pet.started) return
    val p = pet

    if (p.ended.isNotEmpty()) {
        EndedScene(p)
        return
    }

    var talkOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // 头部: 名字 + 等级 + 幕
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${p.name} · Lv.${p.level}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                actLabel(p.act),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(4.dp))

        // 小桃 (点击该区域 = 抚摸)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clickable {
                    PetApp.doAction { s -> GameLogic.pet(s) }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PetCanvas(stage = p.stage)
                if (p.isUnwell()) {
                    Spacer(Modifier.height(6.dp))
                    Text("小桃不太舒服，快去照顾它吧…", fontSize = 13.sp, color = Color(0xFFFFD7D7))
                }
            }
        }
        Text("点一点小桃，它心情会变好", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(Modifier.height(8.dp))

        // 数值
        StatBar("饥饿", p.hunger, Color(0xFFFFB7C8))
        StatBar("心情", p.mood, Color(0xFFFFD76A))
        StatBar("健康", p.health, Color(0xFF9AE6B4))
        StatBar("亲密", p.intimacy, Color(0xFFB7A8FF))
        // 经验
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("经验", fontSize = 13.sp, color = Color.White.copy(alpha = 0.92f), modifier = Modifier.width(52.dp))
            Text(
                "${p.xp}/${p.xpNeed()}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(" · 升到 Lv.${(p.level + 1).coerceAtMost(PetState.MAX_LEVEL)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
        }

        Spacer(Modifier.height(10.dp))

        // 互动按钮
        ActionGrid(onTalk = { talkOpen = true })

        Spacer(Modifier.weight(1f))
    }

    if (talkOpen) {
        TalkDialog(onDismiss = { talkOpen = false })
    }
}

private fun actLabel(act: Int): String = when (act) {
    3 -> "第三幕 · 抉择"
    2 -> "第二幕 · 异变"
    else -> "第一幕 · 相遇"
}

/** 五个互动按钮 */
@Composable
private fun ActionGrid(onTalk: () -> Unit) {
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
                    .weight(1f)
                    .padding(4.dp)
                    .clickable {
                        when (key) {
                            "feed" -> PetApp.doAction { s -> GameLogic.feed(s) }
                            "play" -> PetApp.doAction { s -> GameLogic.play(s) }
                            "rest" -> PetApp.doAction { s -> GameLogic.rest(s) }
                            "pet" -> PetApp.doAction { s -> GameLogic.pet(s) }
                            "talk" -> onTalk()
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(icon, fontSize = 22.sp)
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

/** 谈心: 输入心里话 */
@Composable
private fun TalkDialog(onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("和小桃说说心里话", fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(60) },
                    label = { Text("想说给小桃听的话", fontSize = 12.sp) },
                    singleLine = false,
                    maxLines = 3,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("越真诚，它越懂你。（理解 +3）", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
                    PetApp.doAction { s -> GameLogic.talk(s, text.trim()) }
                }
                onDismiss()
            }) { Text("说") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 结局后的主屏: 星光 + 文案 + 重新开始 */
@Composable
private fun EndedScene(p: PetState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("✦ ✧ ✦", fontSize = 40.sp, color = Color(0xFFFFF3C4))
        Spacer(Modifier.height(16.dp))
        Text(
            when (p.ended) {
                "B" -> "她说：「${p.endingFirstWord.ifBlank { "……" }}」"
                else -> "它散进了星光里"
            },
            fontSize = 20.sp,
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
        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            text = "开启新的相伴",
            onClick = {
                PetApp.audio.playSfx("sfx_levelup")
                PetApp.repo.clearState()
                PetApp.pet = null
                PetApp.bump()
            },
            modifier = Modifier.width(240.dp),
        )
    }
}
