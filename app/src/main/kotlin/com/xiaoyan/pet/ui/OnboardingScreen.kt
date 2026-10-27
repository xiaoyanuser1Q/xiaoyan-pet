package com.xiaoyan.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.game.StoryData
import kotlinx.coroutines.delay

/**
 * 序幕 · 春夜雨 (v1.3.0 开场重写):
 * 左 = 序幕旁白 (逐条显现); 右 = 起名 + 「你想许一个什么约？」(答案记入存档, 结局 C 用到)。
 * 键盘只顶右栏 (横屏防裁切规则)。
 */
@Composable
fun OnboardingScreen() {
    var name by remember { mutableStateOf("") }
    var promise by remember { mutableStateOf("") }
    var shownBeats by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (shownBeats < StoryData.PROLOGUE_BEFORE.size) {
            delay(1600L)
            shownBeats = shownBeats + 1
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        // 左栏: 序幕旁白
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("序幕 · 春夜雨", fontSize = 15.sp, color = Color(0xFFFFB7D5))
            Spacer(Modifier.height(10.dp))
            StoryData.PROLOGUE_BEFORE.take(shownBeats).forEach { ln ->
                Text(ln.text, fontSize = 14.sp, color = Color.White.copy(alpha = 0.88f), lineHeight = 22.sp)
                Spacer(Modifier.height(8.dp))
            }
        }
        // 右栏: 表单 (键盘只顶这一栏)
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .imePadding(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("给她起个名字", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(12) },
                placeholder = { Text("她的名字（12 字内）", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))

            Text("你想许一个什么约？", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("她会一直记着。", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = promise,
                onValueChange = { promise = it.take(30) },
                placeholder = { Text("你的约（30 字内，可留空）", fontSize = 12.sp) },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = "开始相伴",
                enabled = name.isNotBlank(),
                onClick = {
                    PetApp.startGame(name.trim(), promise.trim())
                },
            )
        }
    }
}
