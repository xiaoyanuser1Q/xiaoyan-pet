package com.xiaoyan.pet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.Screen
import com.xiaoyan.pet.data.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 设置页: 声音 / 云存档 / 存档数据 / 检查更新 / 通知 */
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var syncMsg by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }

    val sfx = PetApp.repo.sfxOn()
    val bgm = PetApp.repo.bgmOn()
    val cloudEmail = PetApp.repo.cloudEmail()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 10.dp))

        // ---------- 声音与音乐 ----------
        SectionCard("声音与音乐") {
            SettingRow("音效", "点击、喂食、升级等提示音") {
                Switch(
                    checked = sfx,
                    onCheckedChange = { on ->
                        PetApp.repo.setSfxOn(on)
                        PetApp.audio.setSfxEnabled(on)
                        PetApp.bump()
                    },
                )
            }
            SettingRow("背景音乐", "轻柔的八音盒旋律，循环播放") {
                Switch(
                    checked = bgm,
                    onCheckedChange = { on ->
                        PetApp.repo.setBgmOn(on)
                        PetApp.audio.setBgmEnabled(on)
                        PetApp.bump()
                    },
                )
            }
        }

        // ---------- 云存档 ----------
        SectionCard("云存档") {
            if (cloudEmail == null) {
                SettingRow("登录账号", "登录后才能同步云存档") {
                    TextButton(onClick = { PetApp.screen = Screen.Account }) { Text("去登录") }
                }
            } else {
                SettingRow("账号", cloudEmail) {
                    TextButton(onClick = {
                        val uid = PetApp.repo.cloudUid()
                        val token = PetApp.repo.cloudToken()
                        val p = PetApp.pet
                        if (uid != null && token != null && p != null) {
                            scope.launch {
                                try {
                                    val ok = PetApp.api.upsertSave(uid, token, p.toJson().toString())
                                    if (ok) {
                                        PetApp.repo.setLastCloudSync(System.currentTimeMillis())
                                        syncMsg = "已同步到云 ✓"
                                        PetApp.audio.playSfx("sfx_tap")
                                    } else {
                                        syncMsg = "同步失败，请稍后再试"
                                        PetApp.audio.playSfx("sfx_warn")
                                    }
                                    PetApp.bump()
                                } catch (e: SupabaseApi.SessionExpired) {
                                    PetApp.repo.setCloudSession(null, null, null)
                                    updateMsg = "登录已过期，请重新登录"
                                    PetApp.bump()
                                }
                            }
                        }
                    }) {
                        Text("同步")
                    }
                }
                Text(
                    cloudStatusText(ctx),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                )
                syncMsg?.let {
                    Text(it, fontSize = 12.sp, color = Color(0xFFFFD76A))
                }
            }
        }

        // ---------- 存档数据 ----------
        SectionCard("存档数据") {
            SettingRow("复制存档", "把存档文本复制到剪贴板") {
                TextButton(onClick = {
                    val text = PetApp.repo.exportStateText() ?: "（还没有存档）"
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("xiaoyan-pet-save", text))
                    updateMsg = "存档已复制到剪贴板"
                    PetApp.audio.playSfx("sfx_tap")
                }) {
                    Text("复制")
                }
            }
            SettingRow("重置存档", "名字、数值、剧情全部清空，从头开始") {
                TextButton(onClick = { resetOpen = true }) { Text("重置") }
            }
            if (resetOpen) {
                AlertDialog(
                    onDismissRequest = { resetOpen = false },
                    title = { Text("确认重置存档？") },
                    text = { Text("小桃会回到最初，名字和所有数值都会清空。") },
                    confirmButton = {
                        TextButton(onClick = {
                            PetApp.repo.clearState()
                            PetApp.pet = null
                            resetOpen = false
                            PetApp.audio.playSfx("sfx_warn")
                            PetApp.bump()
                        }) { Text("重置") }
                    },
                    dismissButton = {
                        TextButton(onClick = { resetOpen = false }) { Text("取消") }
                    },
                )
            }
        }

        // ---------- 检查更新 ----------
        SectionCard("检查更新") {
            SettingRow("当前版本", "v${PetApp.CURRENT_VERSION_NAME} (${PetApp.CURRENT_VERSION_CODE})") {
                TextButton(
                    onClick = {
                        if (!checking) {
                            checking = true
                            updateMsg = null
                            scope.launch {
                                val r = checkUpdate()
                                updateMsg = when {
                                    r == null -> "检查失败，请稍后再试"
                                    r.first -> "已是最新版 v${PetApp.CURRENT_VERSION_NAME}"
                                    else -> "发现新版本 v${r.second}，请到官网下载新 APK"
                                }
                                checking = false
                                PetApp.bump()
                            }
                        }
                    },
                    enabled = !checking,
                ) {
                    Text(if (checking) "检查中…" else "检查")
                }
            }
            updateMsg?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, fontSize = 12.sp, color = Color(0xFFFFD76A))
            }
        }

        // ---------- 通知 ----------
        SectionCard("通知") {
            Text("通知功能（小桃的提醒）即将加入下一版。", fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Supabase 401 会话过期由 SupabaseApi.SessionExpired 抛出 */
private fun cloudStatusText(ctx: Context): String {
    val last = PetApp.repo.lastCloudSyncMs()
    return if (last > 0L) {
        "最近同步: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(last))
    } else {
        "尚未同步过云存档"
    }
}

/**
 * 检查更新: 拉官网 version.json。
 * @return null = 失败; (true, "") = 最新; (false, 新版名) = 有更新
 */
private suspend fun checkUpdate(): Pair<Boolean, String>? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val conn = java.net.URL("https://xiaoyan-pet-site.pages.dev/version.json").openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return@withContext null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val j = JSONObject(text)
            val latestCode = j.optInt("versionCode", 0)
            if (latestCode > PetApp.CURRENT_VERSION_CODE) {
                false to j.optString("version", "")
            } else {
                true to ""
            }
        } catch (e: Exception) {
            null
        }
    }
