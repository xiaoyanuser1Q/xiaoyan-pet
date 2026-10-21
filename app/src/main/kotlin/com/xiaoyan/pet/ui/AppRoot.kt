package com.xiaoyan.pet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
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
        bottomBar = {
            NavigationBar(
                colors = NavigationBarDefaults.colors(
                    containerColor = Color(0xFF2E1B2A),
                    activeIndicatorColor = Color(0xFFFF8FB8).copy(alpha = 0.30f),
                ),
            ) {
                NavItem(selected = screen == Screen.Game, icon = "🏠", label = "小桃", onClick = { PetApp.screen = Screen.Game })
                NavItem(selected = screen == Screen.Account, icon = "👤", label = "账号", onClick = { PetApp.screen = Screen.Account })
                NavItem(selected = screen == Screen.Settings, icon = "⚙️", label = "设置", onClick = { PetApp.screen = Screen.Settings })
                NavItem(selected = screen == Screen.About, icon = "ℹ️", label = "关于", onClick = { PetApp.screen = Screen.About })
            }
        },
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

@Composable
private fun NavItem(selected: Boolean, icon: String, label: String, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Text(icon, fontSize = 20.sp) },
        label = { Text(label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedTextColor = Color(0xFFFFD7E5),
            unselectedTextColor = Color.White.copy(alpha = 0.62f),
        ),
    )
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
