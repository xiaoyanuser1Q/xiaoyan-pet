package com.xiaoyan.pet.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 数值条: 标签 + 进度 + 数值 (半透明底, 适配彩色背景图) */
@Composable
fun StatBar(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.92f), modifier = Modifier.width(52.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(5.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(5.dp)),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${value}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
    }
}

/** 设置区卡片 */
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.12f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(Modifier.padding(12.dp)) { content() }
        }
    }
}

/** 一行设置项: 标题 + 副标题 + 右侧控件 */
@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = Color.White)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
        trailing()
    }
}

/** 主按钮 (粉色圆角) */
@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (enabled) Color(0xFFFF8FB8) else Color.White.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp),
            )
            .padding(vertical = 10.dp),
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color(0xFF5C1230) else Color.White.copy(alpha = 0.5f),
        )
    }
}

/** 次级按钮 (半透明白) */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Text(text, fontSize = 14.sp, color = Color.White)
    }
}
