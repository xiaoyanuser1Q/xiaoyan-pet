package com.xiaoyan.pet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * 小桃造型: 粉团子 + 嫩叶 + 横排双眼 + 腮红。
 * stage >= 2 (觉醒形态): 头顶后侧加星辉纹路。
 */
@Composable
fun PetCanvas(stage: Int, sizeDp: Int = 170) {
    val leaf = Color(0xFF7BC67E)
    val body = Color(0xFFFFB7D5)
    val cheek = Color(0xFFFF6994)
    val star = Color(0xFFFFD76A)

    Box(
        modifier = Modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val cx = w / 2f
            val bodyR = w * 0.40f
            val bodyC = Offset(cx, w * 0.56f)

            // 星辉纹路 (觉醒形态, 头顶弧线上三颗小星)
            if (stage >= 2) {
                drawSparkle(cx - w * 0.20f, w * 0.14f, w * 0.045f, star)
                drawSparkle(cx, w * 0.07f, w * 0.04f, star)
                drawSparkle(cx + w * 0.20f, w * 0.14f, w * 0.045f, star)
            }

            // 两片嫩叶
            drawOval(
                color = leaf,
                topLeft = Offset(cx - w * 0.27f, w * 0.12f),
                size = Size(w * 0.16f, w * 0.10f),
            )
            drawOval(
                color = leaf,
                topLeft = Offset(cx + w * 0.11f, w * 0.12f),
                size = Size(w * 0.16f, w * 0.10f),
            )

            // 身体
            drawCircle(color = body, center = bodyC, radius = bodyR)

            // 眼睛 (横排)
            val eyeY = bodyC.y - bodyR * 0.18f
            val eyeR = bodyR * 0.085f
            val eyeGap = bodyR * 0.46f
            drawCircle(color = Color.Black, center = Offset(bodyC.x - eyeGap, eyeY), radius = eyeR)
            drawCircle(color = Color.Black, center = Offset(bodyC.x + eyeGap, eyeY), radius = eyeR)

            // 腮红
            val cheekR = bodyR * 0.11f
            drawCircle(
                color = cheek.copy(alpha = 0.75f),
                center = Offset(bodyC.x - eyeGap * 1.7f, eyeY + bodyR * 0.42f),
                radius = cheekR,
            )
            drawCircle(
                color = cheek.copy(alpha = 0.75f),
                center = Offset(bodyC.x + eyeGap * 1.7f, eyeY + bodyR * 0.42f),
                radius = cheekR,
            )
        }
    }
}

/** 小星星: 圆点 + 十字微光 */
private fun DrawScope.drawSparkle(x: Float, y: Float, r: Float, color: Color) {
    drawCircle(color = color.copy(alpha = 0.95f), center = Offset(x, y), radius = r)
    drawLine(
        color = color.copy(alpha = 0.55f),
        start = Offset(x - r * 2.4f, y),
        end = Offset(x + r * 2.4f, y),
        strokeWidth = 2f,
    )
    drawLine(
        color = color.copy(alpha = 0.55f),
        start = Offset(x, y - r * 2.4f),
        end = Offset(x, y + r * 2.4f),
        strokeWidth = 2f,
    )
}
