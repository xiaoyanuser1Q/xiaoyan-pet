package com.xiaoyan.pet.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * debug 面板注册器 (仅 debug 构建: release 不编译此类, manifest 合并也不用它 → 面板零残留进包)。
 */
class DebugBootstrap : Application() {
    override fun onCreate() {
        super.onCreate()
        PetApp.debugPanel = { DebugPanel() }
    }
}

/**
 * Debug 面板 (v3 裁定② / 指令 §4.4, 仅 debug 构建):
 * - 三条隐藏线直接设值 (上限 104/103/110, 与 §1 表 C 路径有效上限一致)
 * - 等级直接设值 (1-100); 表面值设值
 * - 验收: 三线 85 + 等级 95 → 走真实 Lv95 抉择流 (不跳过节点事件与抉择点)
 */
@Composable
fun DebugPanel() {
    val p = PetApp.pet
    if (p == null) return

    var promise by remember { mutableStateOf(p.promiseLine.toString()) }
    var warmth by remember { mutableStateOf(p.warmthLine.toString()) }
    var curiosity by remember { mutableStateOf(p.curiosityLine.toString()) }
    var level by remember { mutableStateOf(p.level.toString()) }
    var satiety by remember { mutableStateOf(p.satiety.toString()) }
    var mood by remember { mutableStateOf(p.mood.toString()) }
    var energy by remember { mutableStateOf(p.energy.toString()) }
    var intimacy by remember { mutableStateOf(p.intimacy.toString()) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text("Debug 面板 (仅 debug 构建)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE08A))
            Spacer(Modifier.height(8.dp))
            Text("三线上限 104/103/110 · 验收: 三线 85 + Lv95 → 真实抉择流", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    NumField("承诺线 (0..104)", promise) { promise = it }
                    NumField("等级 (1..100)", level) { level = it }
                    NumField("饱食 (0..100)", satiety) { satiety = it }
                }
                Column(Modifier.weight(1f)) {
                    NumField("温度线 (0..103)", warmth) { warmth = it }
                    NumField("心情 (0..100)", mood) { mood = it }
                    NumField("体力 (0..10)", energy) { energy = it }
                }
                Column(Modifier.weight(1f)) {
                    NumField("好奇线 (0..110)", curiosity) { curiosity = it }
                    NumField("亲密 (0..100)", intimacy) { intimacy = it }
                }
            }
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                QuickBtn("三线 85 + Lv95 (测 C)") {
                    promise = "85"; warmth = "85"; curiosity = "85"; level = "95"
                }
                QuickBtn("承诺 60 + Lv95 (测 B)") {
                    promise = "60"; warmth = "0"; curiosity = "0"; level = "95"
                }
                QuickBtn("三线 79/79/80 + Lv95 (判定前不出现真相)") {
                    promise = "79"; warmth = "79"; curiosity = "80"; level = "95"
                }
            }
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                QuickBtn("应用") {
                    p.promiseLine = promise.toIntOrNull()?.coerceIn(0, 104) ?: p.promiseLine
                    p.warmthLine = warmth.toIntOrNull()?.coerceIn(0, 103) ?: p.warmthLine
                    p.curiosityLine = curiosity.toIntOrNull()?.coerceIn(0, 110) ?: p.curiosityLine
                    p.level = level.toIntOrNull()?.coerceIn(1, 100) ?: p.level
                    p.satiety = satiety.toIntOrNull()?.coerceIn(0, 100) ?: p.satiety
                    p.mood = mood.toIntOrNull()?.coerceIn(0, 100) ?: p.mood
                    p.energy = energy.toIntOrNull()?.coerceIn(0, p.energyCap()) ?: p.energy
                    p.intimacy = intimacy.toIntOrNull()?.coerceIn(0, 100) ?: p.intimacy
                    p.lastOpenMs = System.currentTimeMillis()
                    PetApp.repo.saveState(p)
                    PetApp.debugOpen.value = false
                    PetApp.bump()
                }
                QuickBtn("关闭") {
                    PetApp.debugOpen.value = false
                    PetApp.bump()
                }
            }
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onValue: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(end = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { v -> onValue(v.filter { ch -> ch.isDigit() }.take(4)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = fieldColors(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickBtn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFF8FB8).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, fontSize = 12.sp, color = Color(0xFF5C1230), fontWeight = FontWeight.Bold)
    }
}
