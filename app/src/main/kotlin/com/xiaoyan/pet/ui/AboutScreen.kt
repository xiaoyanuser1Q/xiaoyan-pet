package com.xiaoyan.pet.ui

import android.content.Intent
import android.net.Uri
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
import com.xiaoyan.pet.PetApp

/** 关于页 (v1.3.0: 版本 1.3.0 + 新权限说明 + 隐私政策链接) */
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
        PetApp.audio.playSfx("sfx_01_tap")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Text("🌸", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text("小桃养成记", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("v${PetApp.CURRENT_VERSION_NAME} (build ${PetApp.CURRENT_VERSION_CODE}) · 《四季的小屋》", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(14.dp))

        Text(
            "一个完全免费的小养成游戏。\n" +
                "四季的小屋，和一个小桃。没有广告，没有内购，永远免费。",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(14.dp))

        Text(
            "本版本说明：\n" +
                "· 应用内下载并安装更新包 —— 需要系统「安装未知应用」权限（仅首次安装更新包时引导）\n" +
                "· 本地通知（升级 / 跨幕 / 新版本）—— Android 13+ 需系统通知权限（设置页内请求一次）\n" +
                "两项权限详情见隐私政策。",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        Column(Modifier.fillMaxWidth()) {
            Text("链接", fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(bottom = 4.dp))
            LinkRow("官网 · 小桃养成记", "https://xiaoyan-pet.xiaoyan-pan.eu.cc", openUrl)
            LinkRow("隐私政策", "https://xiaoyan-pet.xiaoyan-pan.eu.cc/privacy.html", openUrl)
            LinkRow("源码 · Gitee（主备份）", "https://gitee.com/xiaoyanuser/xiaoyan-pet", openUrl)
            LinkRow("源码 · GitHub（第二备份）", "https://github.com/xiaoyanuser1Q/xiaoyan-pet", openUrl)
        }
        Spacer(Modifier.height(14.dp))
        Text("立绘/背景由 AI 生成（Agnes 图像模型）· 音乐音效程序生成 · 素材许可见 CREDITS（MIT 项目内使用）", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(Modifier.height(10.dp))
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
