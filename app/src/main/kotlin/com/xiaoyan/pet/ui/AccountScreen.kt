package com.xiaoyan.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.xiaoyan.pet.data.SupabaseApi
import kotlinx.coroutines.launch

/** 账号页: Supabase 邮箱登录/注册 + 云存档入口提示 */
@Composable
fun AccountScreen() {
    val email = PetApp.repo.cloudEmail()
    if (email != null) {
        SessionCard(email)
    } else {
        LoginForm()
    }
}

@Composable
private fun SessionCard(email: String) {
    val uid = PetApp.repo.cloudUid().orEmpty()
    val lastSync = PetApp.repo.lastCloudSyncMs()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("👤", fontSize = 40.sp)
        Spacer(Modifier.height(10.dp))
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
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "退出登录",
            onClick = {
                PetApp.repo.setCloudSession(null, null, null)
                PetApp.audio.playSfx("sfx_tap")
                PetApp.bump()
            },
        )
        Spacer(Modifier.height(6.dp))
        Text("提示: 云存档在「设置 · 云存档」里同步", fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
    }
}

@Composable
private fun LoginForm() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var needConfirm by remember { mutableStateOf(false) }

    val cscope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        Text("☁️", fontSize = 40.sp)
        Spacer(Modifier.height(10.dp))
        Text("账号与云存档", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text("登录后可把存档备份到云，换手机也不丢。", fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
        Spacer(Modifier.height(24.dp))

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
        Spacer(Modifier.height(16.dp))

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
                enabled = email.isNotBlank() && password.length() >= 6 && !busy,
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
                                PetApp.audio.playSfx("sfx_levelup")
                                PetApp.bump()
                            }
                            is AuthResult.Err -> {
                                status = r.message
                                PetApp.audio.playSfx("sfx_warn")
                            }
                        }
                        busy = false
                    }
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "注册",
                enabled = email.isNotBlank() && password.length() >= 6 && !busy,
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
                                    PetApp.audio.playSfx("sfx_levelup")
                                }
                                PetApp.bump()
                            }
                            is AuthResult.Err -> {
                                status = r.message
                                PetApp.audio.playSfx("sfx_warn")
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
