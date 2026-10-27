package com.xiaoyan.pet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.xiaoyan.pet.BuildConfig
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.Screen
import com.xiaoyan.pet.data.SaveRepository
import com.xiaoyan.pet.data.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 设置页 (v1.3.0 重排: 声音/显示/动画/日时/通知/云存档/存档/更新/调试) */
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var syncMsg by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var probing by remember { mutableStateOf(false) }
    var probeMsg by remember { mutableStateOf<String?>(null) }
    var resetOpen by remember { mutableStateOf(false) }

    val sfx = PetApp.repo.sfxOn()
    val bgm = PetApp.repo.bgmOn()
    val cloudEmail = PetApp.repo.cloudEmail()

    // POST_NOTIFICATIONS 运行时权限 (33+; API 29 无此弹窗, 分支兼容)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        PetApp.repo.setNotificationsOn(true)
        if (!granted) updateMsg = "通知权限未授予（Android 13+），开关已开但收不到通知"
        PetApp.bump()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 10.dp))

        // ---------- 声音与音乐 ----------
        SectionCard("声音与音乐") {
            SettingRow("音效", "15 条场景音效 (S01-S15)") {
                Switch(checked = sfx, onCheckedChange = { on ->
                    PetApp.repo.setSfxOn(on)
                    PetApp.audio.sfxEnabled = on
                    PetApp.bump()
                })
            }
            SettingRow("背景音乐", "四季 BGM + 终曲 (季节交叉淡化)") {
                Switch(checked = bgm, onCheckedChange = { on ->
                    PetApp.repo.setBgmOn(on)
                    PetApp.audio.applyBgm(on)
                    PetApp.bump()
                })
            }
        }

        // ---------- 显示 ----------
        SectionCard("显示与动画") {
            SettingRow("低配模式", "微摆/粒子/星光全关, 旧手机更流畅") {
                Switch(
                    checked = PetApp.repo.lowSpecOn(),
                    onCheckedChange = { on -> PetApp.repo.setLowSpecOn(on); PetApp.bump() },
                )
            }
            SettingRow("动画风格", "温柔 / 活泼 / 关闭 (参数表集中, 免重编译)") {
                Row(Modifier.fillMaxWidth()) {
                    listOf("gentle" to "温柔", "lively" to "活泼", "off" to "关闭").forEach { (k, label) ->
                        val active = PetApp.repo.animStyle() == k
                        Box(
                            modifier = Modifier
                                .background(
                                    if (active) Color(0xFFFF8FB8).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                )
                                .clickable { PetApp.repo.setAnimStyle(k); PetApp.bump() }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(" $label ", fontSize = 12.sp,
                                color = if (active) Color(0xFFFF8FB8) else Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }

        // ---------- 日时 (4 态兜底开关) ----------
        SectionCard("日时 (早/午/下午/晚)") {
            SettingRow("强制夜间", "随时切到夜间 (观星/睡衣套)") {
                Switch(
                    checked = PetApp.repo.forceNightOn(),
                    onCheckedChange = { on ->
                        PetApp.repo.setForceNightOn(on)
                        if (on) PetApp.repo.setForceDayOn(false)
                        PetApp.bump()
                    },
                )
            }
            SettingRow("强制白天", "随时切回白天 (与强制夜间互斥)") {
                Switch(
                    checked = PetApp.repo.forceDayOn(),
                    onCheckedChange = { on ->
                        PetApp.repo.setForceDayOn(on)
                        if (on) PetApp.repo.setForceNightOn(false)
                        PetApp.bump()
                    },
                )
            }
        }

        // ---------- 通知 (v1.3.0 新增: 3 事件双渠道) ----------
        SectionCard("通知") {
            SettingRow(
                "小桃的提醒",
                "新版本 / 升级 / 跨幕 (本地通知, 不联网推送)" +
                    (if (Build.VERSION.SDK_INT >= 33) " · 需系统通知权限" else ""),
            ) {
                Switch(
                    checked = PetApp.repo.notificationsOn(),
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33) {
                            val granted = ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                                PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                PetApp.repo.setNotificationsOn(true)
                                PetApp.bump()
                            } else {
                                notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            PetApp.repo.setNotificationsOn(on)
                            PetApp.bump()
                        }
                    },
                )
            }
        }

        // ---------- 云存档 ----------
        SectionCard("云存档") {
            if (cloudEmail == null) {
                SettingRow("登录账号", "登录后同步云存档 (失败不阻断玩法)") {
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
                                        syncMsg = "已同步到云 ✓ (v1.3.0 新格式, 旧云行按设计被覆盖)"
                                        PetApp.audio.playSfx("sfx_01_tap")
                                    } else {
                                        syncMsg = "同步失败：网络不可用（可开启 VPN 后重试）"
                                        PetApp.audio.playSfx("sfx_13_warn")
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
                    if (PetApp.repo.lastCloudSyncMs() > 0L)
                        "最近同步: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(PetApp.repo.lastCloudSyncMs()))
                    else "尚未同步过云存档",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f),
                )
                syncMsg?.let { Text(it, fontSize = 12.sp, color = Color(0xFFFFD76A)) }
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
                    PetApp.audio.playSfx("sfx_01_tap")
                }) {
                    Text("复制")
                }
            }
            SettingRow("重置存档", "名字、数值、剧情全部清空，从头开始") {
                TextButton(onClick = { resetOpen = true }) { Text("重置") }
            }
            if (PetApp.repo.oldSaveVersion() == 2) {
                SettingRow("旧版本存档", "v1.2.0 及更早 (save_version=2)") {
                    Text("不可用于 v1.3.0", fontSize = 12.sp, color = Color(0xFFFF8FB8))
                }
                Text("v1.3.0 是全新故事，旧进度无法继续使用。旧档文件保留，不会自动加载。",
                    fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Text("本地数据不参与 Android 系统备份；云存档只存在你的账号里。",
                fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
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
                            PetApp.audio.playSfx("sfx_13_warn")
                            PetApp.bump()
                        }) { Text("重置") }
                    },
                    dismissButton = { TextButton(onClick = { resetOpen = false }) { Text("取消") } },
                )
            }
        }

        // ---------- 检查更新 (v1.3.0 多源 + APP 内下载) ----------
        SectionCard("检查更新") {
            SettingRow("当前版本", "v${PetApp.CURRENT_VERSION_NAME} (${PetApp.CURRENT_VERSION_CODE})") {
                TextButton(
                    onClick = {
                        if (!checking) {
                            checking = true
                            updateMsg = null
                            scope.launch {
                                PetApp.checkForUpdate()
                                val info = PetApp.updateInfo.value
                                updateMsg = when {
                                    info != null -> "发现 v${info.version}（${info.source} 源）· 已挂顶栏"
                                    PetApp.updateMsg.value != null -> PetApp.updateMsg.value
                                    else -> "已是最新版 v${PetApp.CURRENT_VERSION_NAME}"
                                }
                                checking = false
                            }
                        }
                    },
                    enabled = !checking,
                ) {
                    Text(if (checking) "检查中…" else "检查")
                }
            }
            SettingRow("测试连接", "多源状态: 当前生效源 " + (PetApp.repo.lastGoodSource().ifEmpty { "未命中" })) {
                TextButton(onClick = {
                    if (!probing) {
                        probing = true
                        probeMsg = null
                        scope.launch {
                            val g = com.xiaoyan.pet.data.Updater.probeSource(com.xiaoyan.pet.data.Updater.SRC_GITEE)
                            val e = com.xiaoyan.pet.data.Updater.probeSource(com.xiaoyan.pet.data.Updater.SRC_EUCC)
                            val p = com.xiaoyan.pet.data.Updater.probeSource(com.xiaoyan.pet.data.Updater.SRC_PAGES)
                            probeMsg = "Gitee ${if (g) "✓" else "✗"} · eu.cc ${if (e) "✓" else "✗"} · pages.dev ${if (p) "✓" else "✗"}"
                            if (g) PetApp.repo.setLastGoodSource(com.xiaoyan.pet.data.Updater.SRC_GITEE)
                            else if (e) PetApp.repo.setLastGoodSource(com.xiaoyan.pet.data.Updater.SRC_EUCC)
                            else if (p) PetApp.repo.setLastGoodSource(com.xiaoyan.pet.data.Updater.SRC_PAGES)
                            probing = false
                            PetApp.bump()
                        }
                    }
                }) {
                    Text(if (probing) "探测中…" else "探测")
                }
            }
            probeMsg?.let { Text(it, fontSize = 12.sp, color = Color(0xFF9AE6B4)) }
            updateMsg?.let { Text(it, fontSize = 12.sp, color = Color(0xFFFFD76A)) }
        }

        // ---------- 调试 (仅 debug 构建入口; release 无入口无代码路径, BuildConfig 门控) ----------
        if (BuildConfig.DEBUG) {
            SectionCard("调试 (debug 构建)") {
                SettingRow("Debug 面板", "直设三线/等级/表面值 (上限 104/103/110), 验收走真实 Lv95 抉择流") {
                    TextButton(onClick = { PetApp.debugOpen.value = true; PetApp.bump() }) { Text("打开") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
