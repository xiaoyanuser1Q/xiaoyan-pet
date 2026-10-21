package com.xiaoyan.pet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.xiaoyan.pet.PetApp

/** 关于页 */
@Composable
fun AboutScreen() {
    val ctx = LocalContext.current

    val openUrl: (String) -> Unit = { url ->
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addCategory(Intent.CATEGORY_BROWSABLE)
            )
        }
        PetApp.audio.playSfx("sfx_tap")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🌸", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text("小桃养成记", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("v${PetApp.CURRENT_VERSION_NAME} (build ${PetApp.CURRENT_VERSION_CODE})", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(18.dp))

        Text(
            "一个完全免费的小养成游戏。\n" +
                "它陪你一点点把那些你曾经弄丢的东西，再养回来。\n" +
                "没有广告，没有内购，永远免费。",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(22.dp))

        Column(Modifier.fillMaxWidth()) {
            Text("开发者", fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(bottom = 4.dp))
            Text("宵檐（xiaoyan）· 用手机一点点做出来的", fontSize = 14.sp, color = Color.White)
            Spacer(Modifier.height(14.dp))

            Text("链接", fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(bottom = 4.dp))
            LinkRow("官网 · 小桃养成记", "https://xiaoyan-pet.xiaoyan-pan.eu.cc", openUrl)
            LinkRow("源码 · Gitee（主备份）", "https://gitee.com/xiaoyanuser/xiaoyan-pet", openUrl)
            LinkRow("源码 · GitHub（第二备份）", "https://github.com/xiaoyanuser1Q/xiaoyan-pet", openUrl)
            LinkRow("旧版 APK（v1.0.0）", "https://xiaoyan-pet-site.pages.dev/download/xiaoyan-pet-v1.0.0-release.apk", openUrl)
        }
        Spacer(Modifier.height(24.dp))
        Text("谢谢你来陪小桃。", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LinkRow(label: String, url: String, open: (String) -> Unit) {
    TextButton(onClick = { open(url) }) {
        Text(label, fontSize = 14.sp, color = Color(0xFFFFB7D5), modifier = Modifier.fillMaxWidth())
    }
}
