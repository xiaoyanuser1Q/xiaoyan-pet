package com.xiaoyan.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.BuildConfig
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.R
import com.xiaoyan.pet.Screen
import com.xiaoyan.pet.game.PetEvent
import kotlinx.coroutines.delay

/** 根节点 (v1.3.0): 开始菜单为常驻入口; 事件弹窗挂根上; 更新横幅; 旧档弹窗 */
@Composable
fun AppRoot() {
    PetApp.tick.value

    val pet = PetApp.pet
    var oldSaveDialog by remember { mutableStateOf(false) }

    // 首次启动 + 本地有 v2 旧档 → 弹窗「v1.3.0 是全新故事，旧进度无法继续使用」
    LaunchedEffect(Unit) {
        if (PetApp.repo.oldSaveVersion() == 2 && (pet == null || !pet.started)) {
            oldSaveDialog = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            PetApp.screen == Screen.Start -> StartMenu()
            PetApp.screen == Screen.Game && (pet == null || !pet.started) -> {
                Box(Modifier.fillMaxSize()) {
                    BackgroundImage(R.drawable.bg_road_night)
                    OnboardingScreen()
                }
            }
            PetApp.screen == Screen.Account -> PageFrame { AccountScreen() }
            PetApp.screen == Screen.Settings -> PageFrame { SettingsScreen() }
            PetApp.screen == Screen.About -> PageFrame { AboutScreen() }
            else -> PageFrame { GameScreen() }
        }

        // ---------- 剧情事件弹窗 (挂根上, 切页不丢) ----------
        val ev = PetApp.pendingEvent.value
        if (ev != null) {
            EventDialog(ev)
        }

        // ---------- 结局浮层 ----------
        if (pet != null && pet.ended.isNotEmpty() && !pet.endingSeen && PetApp.screen == Screen.Game) {
            EndingOverlay()
        }

        // ---------- 更新横幅 (T0: 发现新版本 → APP 内下载 → 安装) ----------
        val info = PetApp.updateInfo.value
        if (info != null) {
            UpdateBanner(info, Modifier.align(Alignment.TopEnd))
        }

        // ---------- debug 面板 (仅 debug 构建; release 整行被常量折叠剔除) ----------
        if (BuildConfig.DEBUG && PetApp.debugOpen.value) {
            val host = PetApp.debugPanel
            host?.invoke()
        }

        // ---------- 旧档弹窗 ----------
        if (oldSaveDialog) {
            AlertDialog(
                onDismissRequest = { oldSaveDialog = false },
                title = { Text("v1.3.0 是全新故事") },
                text = { Text("旧进度无法继续使用（存档已全新开始）。\n旧版本存档（不可用于 v1.3.0）仍保留在本地，可在设置页查看。") },
                confirmButton = {
                    TextButton(onClick = {
                        oldSaveDialog = false
                        PetApp.audio.playSfx("sfx_01_tap")
                        PetApp.bump()
                    }) { Text("开始新故事") }
                },
            )
        }
    }
}

/** 跨幕光柱级过场 (600ms 场景 + BGM 交叉淡化 + 服装自动切换, 过场后出事件文案) */
@Composable
private fun EventDialog(ev: PetEvent) {
    var flashing by remember { mutableStateOf(false) }
    LaunchedEffect(ev) {
        flashing = true
        delay(600L)
        flashing = false
    }
    val content = when (ev) {
        is PetEvent.Story -> ev.beat
        is PetEvent.Random -> com.xiaoyan.pet.game.StoryData.Beat(ev.ev.id, ev.ev.title,
            listOf(com.xiaoyan.pet.game.StoryData.Line(ev.ev.text, ev.ev.mood)))
    }
    Box(Modifier.fillMaxSize()) {
        if (flashing) {
            // 光柱: 中央白色纵向渐变闪烁 (600ms)
            Box(
                Modifier.fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent)
                        ),
                    ),
            )
        }
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(content.title.ifEmpty { "小桃" }, fontSize = 17.sp, color = Color(0xFF5C1230))
            },
            text = {
                Column {
                    content.lines.forEach { ln ->
                        Text(ln.text, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { PetApp.dismissEvent() }) {
                    Text("继续", color = Color(0xFFFF5C8A))
                }
            },
        )
    }
}

/** 更新横幅 (顶栏): 发现新版本 / 下载进度 / 安装 */
@Composable
private fun UpdateBanner(info: com.xiaoyan.pet.data.Updater.UpdateInfo, modifier: Modifier = Modifier) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity ?: return
    Column(
        modifier = modifier
            .padding(10.dp)
            .fillMaxWidth()
            .background(Color(0xFF5C1230).copy(alpha = 0.92f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "发现新版本 v${info.version}（${info.source} 源 · 多源回退）",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                )
                if (info.note.isNotBlank()) {
                    Text(info.note, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                }
                if (PetApp.downloading.value) {
                    Text(
                        "下载中 ${"%.0f".format(PetApp.downloadProgress.floatValue * 100)}%（断点续传 · SHA256 校验）",
                        fontSize = 11.sp, color = Color(0xFFFFD76A),
                    )
                }
                PetApp.downloadedApk.value?.let {
                    Text("下载完成 · 点「安装」开始（离线也可装）", fontSize = 11.sp, color = Color(0xFF9AE6B4))
                }
                PetApp.updateMsg.value?.let { Text(it, fontSize = 11.sp, color = Color(0xFFFFD76A)) }
            }
            if (PetApp.downloadedApk.value != null) {
                BannerBtn("安装") { PetApp.installUpdate(activity) }
            } else if (!PetApp.downloading.value) {
                BannerBtn("立即下载") { PetApp.downloadUpdate() }
            } else {
                BannerBtn("下载中…") {}
            }
            BannerBtn("✕") { PetApp.dismissUpdate() }
        }
    }
}

@Composable
private fun BannerBtn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .background(Color(0xFFFF8FB8), RoundedCornerShape(10.dp))
            .clickable(enabled = text != "下载中…", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C1230))
    }
}

/** 页面容器: 背景 + 底部导航 (小桃/账号/设置/关于) */
@Composable
fun PageFrame(content: @Composable () -> Unit) {
    val pet = PetApp.pet
    val bgRes = when {
        pet == null || !pet.started -> R.drawable.bg_road_night
        pet.ended.isNotEmpty() -> endingBgRes(pet.ended)
        else -> com.xiaoyan.pet.game.Scenes.bgResFor(
            com.xiaoyan.pet.game.Scenes.LIVINGROOM,
            com.xiaoyan.pet.game.DayPhase.current(PetApp.repo, pet.asleep, System.currentTimeMillis()),
        )
    }
    Box(Modifier.fillMaxSize()) {
        BackgroundImage(bgRes)
        Surface(color = Color.Transparent) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().weight(1f)) {
                    content()
                }
                if (pet != null && pet.started && PetApp.screen != Screen.Start) {
                    BottomBar()
                }
            }
        }
    }
}

/** 全屏背景图 + 轻微暗化 (横屏 Fit 不裁切内容区; 背景允许 Cover) */
@Composable
fun BackgroundImage(resId: Int) {
    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(resId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.16f)))
    }
}

/** 结局场景背景 (A=山丘清晨 / B=客厅夜 / C=山丘夜) */
fun endingBgRes(ending: String): Int = when (ending) {
    "A" -> com.xiaoyan.pet.game.Scenes.bgResFor(com.xiaoyan.pet.game.Scenes.HILL, com.xiaoyan.pet.game.DayPhase.MORNING)
    "B" -> com.xiaoyan.pet.game.Scenes.bgResFor(com.xiaoyan.pet.game.Scenes.LIVINGROOM, com.xiaoyan.pet.game.DayPhase.NIGHT)
    else -> com.xiaoyan.pet.game.Scenes.bgResFor(com.xiaoyan.pet.game.Scenes.HILL, com.xiaoyan.pet.game.DayPhase.NIGHT)
}

@Composable
private fun BottomBar() {
    Surface(color = Color(0xFF2E1B2A), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(56.dp)) {
            BarItem("🏠", "小桃", PetApp.screen == Screen.Game, { PetApp.screen = Screen.Game }, Modifier.weight(1f))
            BarItem("👤", "账号", PetApp.screen == Screen.Account, { PetApp.screen = Screen.Account }, Modifier.weight(1f))
            BarItem("⚙️", "设置", PetApp.screen == Screen.Settings, { PetApp.screen = Screen.Settings }, Modifier.weight(1f))
            BarItem("ℹ️", "关于", PetApp.screen == Screen.About, { PetApp.screen = Screen.About }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BarItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) Color(0xFFFF8FB8).copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, fontSize = 17.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = if (selected) Color(0xFFFFD7E5) else Color.White.copy(alpha = 0.62f))
    }
}
