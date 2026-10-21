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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.R
import com.xiaoyan.pet.Screen

/** 根节点: 根据存档状态与当前页面渲染 */
@Composable
fun AppRoot() {
    // 观察 tick: 任何数据变化后整棵树刷新
    PetApp.tick.value

    val pet = PetApp.pet
    if (pet == null || !pet.started) {
        Box(Modifier.fillMaxSize()) {
            BackgroundImage(R.drawable.bg_login)
            OnboardingScreen()
        }
        return
    }

    val screen = PetApp.screen

    Scaffold(
        bottomBar = { BottomBar(screen) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            BackgroundImage(PetApp.backgroundRes(pet))
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.Game -> GameScreen()
                    Screen.Account -> AccountScreen()
                    Screen.Settings -> SettingsScreen()
                    Screen.About -> AboutScreen()
                }
            }
            // 结局浮层
            EndingOverlay()
        }
        // 剧情事件弹窗 (挂在根上, 切页也不丢)
        val ev = PetApp.pendingEvent.value
        if (ev != null) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(ev.title, fontSize = 17.sp, color = Color(0xFF5C1230))
                },
                text = { Text(ev.text, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(onClick = { PetApp.dismissEvent() }) {
                        Text("继续", color = Color(0xFFFF5C8A))
                    }
                },
            )
        }
    }
}

/**
 * 自定义底部栏: 不依赖 material3 的 NavigationBar/NavigationBarItem
 * (CI 解析到的 material3 版本可能不含该组件, 用基础布局手搓, 任何版本都能编译)
 */
@Composable
private fun BottomBar(screen: Screen) {
    Surface(color = Color(0xFF2E1B2A), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(62.dp)) {
            BarItem("🏠", "小桃", screen == Screen.Game, { PetApp.screen = Screen.Game }, Modifier.weight(1f))
            BarItem("👤", "账号", screen == Screen.Account, { PetApp.screen = Screen.Account }, Modifier.weight(1f))
            BarItem("⚙️", "设置", screen == Screen.Settings, { PetApp.screen = Screen.Settings }, Modifier.weight(1f))
            BarItem("ℹ️", "关于", screen == Screen.About, { PetApp.screen = Screen.About }, Modifier.weight(1f))
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
        Text(icon, fontSize = 19.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) Color(0xFFFFD7E5) else Color.White.copy(alpha = 0.62f),
        )
    }
}

/** 全屏背景图 + 轻微暗化, 保证文字可读 */
@Composable
fun BackgroundImage(resId: Int) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.16f)))
    }
}
