package com.xiaoyan.pet.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.data.AuthResult
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 账号页 (v1.2.0 横屏重构, 审计体验项): 左右两栏 —— 左品牌, 右表单。
 * 键盘只顶右栏 (imePadding 只作用在右栏), 左侧品牌不动。
 */
@Composable
fun AccountScreen() {
    val email = PetApp.repo.cloudEmail()
    Row(Modifier.fillMaxSize()) {
        BrandColumn(Modifier.weight(0.45f))
        if (email != null) {
            SessionCard(email, Modifier.weight(0.55f))
        } else {
            LoginForm(Modifier.weight(0.55f))
        }
    }
}

/** 左栏: 品牌 (登录页横屏, 键盘不顶它) */
@Composable
private fun BrandColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("☁️", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text("账号与云存档", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            "登录后可把存档备份到云，换手机也不丢。\n数据只存在你的 Supabase 账号里。",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun SessionCard(email: String, modifier: Modifier = Modifier) {
    val uid = PetApp.repo.cloudUid().orEmpty()
    val lastSync = PetApp.repo.lastCloudSyncMs()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(email, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
            "云账号 ID: " + (uid.take(8) + "…" + uid.takeLast(4)),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (lastSync > 0L) "最近云同步: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastSync))
            else "尚未同步过云存档",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TestConnectionRow()
            PrimaryButton(
                text = "退出登录",
                onClick = {
                    PetApp.repo.setCloudSession(null, null, null)
                    PetApp.audio.playSfx("sfx_01_tap")
                    PetApp.bump()
                },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("提示: 云存档在「设置 · 云存档」里同步", fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
    }
}

/** v1.2.0 「🔌 测试连接」: 代理 → Supabase 探活, 显示 HTTP code + 原文 */
@Composable
private fun TestConnectionRow() {
    val cscope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.width(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PrimaryButton(
            text = if (busy) "检测中…" else "🔌 测试连接",
            enabled = !busy,
            onClick = {
                busy = true
                cscope.launch {
                    val (code, desc, ok) = PetApp.api.testConnection()
                    result = if (ok) "✅ 连接正常 (HTTP $code)" else "❌ $code · $desc"
                    busy = false
                    PetApp.bump()
                }
            },
        )
        result?.let {
            Text(
                it,
                fontSize = 12.sp,
                color = if (it.startsWith("✅")) Color(0xFF9AE6B4) else Color(0xFFFFB0B0),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun LoginForm(modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var needConfirm by remember { mutableStateOf(false) }

    val cscope = rememberCoroutineScope()

    // 右栏表单: 键盘只顶这一栏 (imePadding 只加在右栏); weight 由调用方在 Row 内传
    Column(
        modifier = modifier
            .fillMaxHeight()
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("邮箱", fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码（至少 6 位）", fontSize = 12.sp) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))

        if (needConfirm) {
            Text("注册成功！请先到邮箱点「确认」，再回来登录。", fontSize = 13.sp, color = Color(0xFFFFD76A))
            Spacer(Modifier.height(8.dp))
        }
        status?.let {
            Text(it, fontSize = 13.sp, color = Color(0xFFFFB0B0))
            Spacer(Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = "登录",
                enabled = email.isNotBlank() && password.length >= 6 && !busy,
                onClick = {
                    busy = true
                    status = null
                    needConfirm = false
                    cscope.launch {
                        val r = PetApp.api.signIn(email.trim(), password)
                        when (r) {
                            is AuthResult.Ok -> {
                                PetApp.repo.setCloudSession(r.email, r.token, r.uid)
                                runCatching { PetApp.api.upsertPlayer(r.uid, r.token, PetApp.pet?.name ?: "") }
                                // v1.2.0: 登录时写 event_logs (表已存在, 不建表)
                                runCatching {
                                    PetApp.api.logEvent(
                                        r.uid, r.token, "login",
                                        JSONObject().put("version", "1.2.0").put("ts", System.currentTimeMillis()),
                                    )
                                }
                                PetApp.audio.playSfx("sfx_12_levelup")
                                PetApp.bump()
                            }
                            is AuthResult.Err -> {
                                status = r.message
                                PetApp.audio.playSfx("sfx_13_warn")
                            }
                        }
                        busy = false
                    }
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "注册",
                enabled = email.isNotBlank() && password.length >= 6 && !busy,
                onClick = {
                    busy = true
                    status = null
                    cscope.launch {
                        val r = PetApp.api.signUp(email.trim(), password)
                        when (r) {
                            is AuthResult.Ok -> {
                                if (r.needsEmailConfirm) {
                                    needConfirm = true
                                } else {
                                    PetApp.repo.setCloudSession(r.email, r.token, r.uid)
                                    runCatching { PetApp.api.upsertPlayer(r.uid, r.token, PetApp.pet?.name ?: "") }
                                    PetApp.audio.playSfx("sfx_12_levelup")
                                }
                                PetApp.bump()
                            }
                            is AuthResult.Err -> {
                                status = r.message
                                PetApp.audio.playSfx("sfx_13_warn")
                            }
                        }
                        busy = false
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
