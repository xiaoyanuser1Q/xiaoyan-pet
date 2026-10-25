package com.xiaoyan.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.fadeIn
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.slideIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toPx
import com.xiaoyan.pet.R
import kotlinx.coroutines.delay
import kotlin.random.Random

/** 立绘表情 (6 张静态图 + Compose 程序化动画) */
enum class PortraitMood { NEUTRAL, HAPPY, SAD, ANGRY, SHY, SURPRISED }

/**
 * 主保险 (审计①): 静态 when 映射, AAPT 可静态追踪引用, R8 shrink 不会删。
 * 禁止 getIdentifier。兜底 res/raw/keep.xml 见工程内该文件。
 */
fun portraitResFor(mood: PortraitMood): Int = when (mood) {
    PortraitMood.NEUTRAL -> R.drawable.pet_neutral
    PortraitMood.HAPPY -> R.drawable.pet_happy
    PortraitMood.SAD -> R.drawable.pet_sad
    PortraitMood.ANGRY -> R.drawable.pet_angry
    PortraitMood.SHY -> R.drawable.pet_shy
    PortraitMood.SURPRISED -> R.drawable.pet_surprised
}

/**
 * 立绘精灵 (v1.2.0 动画定案):
 * - 呼吸: 整图 scaleY 1.0→1.012 / 3.2s (1.6s 上 1.6s 下) + translateY 0→-2px
 * - 微摆: 整图以底边为轴 ±1.5° / 4.1s; 低配模式关闭
 * - 眨眼: 整图 scaleY 0.97 / 120ms 回弹, 每 3-6s 随机
 * - 情绪切换: 交叉淡化 + 上滑 200ms (Crossfade + 入场 slideIn)
 * 内容: ContentScale.Fit + 底边中点对齐 (审计② 零裁切)
 */
@Composable
fun PortraitSprite(mood: PortraitMood, lowSpec: Boolean) {
    val res = portraitResFor(mood)
    Crossfade(targetState = res, animationSpec = tween(200)) { id ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedSprite(id, lowSpec)
        }
    }
}

@Composable
private fun AnimatedSprite(resId: Int, lowSpec: Boolean) {
    // 入场: 上滑 + 淡入 (每次情绪切换由 Crossfade 重建)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    // 呼吸 (3.2s 全周期) + 微摆 (4.1s 全周期), 两条独立无限动画
    val infinite = rememberInfiniteTransition()
    val breathA = infinite.animate(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
    )
    val swayA = infinite.animate(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2050), RepeatMode.Reverse),
    )
    val breath01 = breathA.value            // 0..1
    val swayWave = swayA.value * 2f - 1f    // -1..1

    // 眨眼: 3-6s 随机, scaleY 0.97 / 120ms 回弹
    var blinkOn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((3000 + Random.nextInt(3000)).toLong())
            blinkOn = true
            delay(60)
            blinkOn = false
            delay(60)
        }
    }
    val blinkScale by animateFloatAsState(
        targetValue = if (blinkOn) 0.97f else 1f,
        animationSpec = tween(120),
        finishedValue = 1f,
    )

    AnimatedVisibility(
        visible = shown,
        enter = slideIn(animationSpec = tween(200)) { IntOffset(0, (it.height * 0.05f).toInt()) } + fadeIn(tween(200)),
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 以底边为轴 (呼吸 scaleY 与微摆 rotationZ 共用)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleY = (1f + 0.012f * breath01) * blinkScale
                    rotationZ = if (lowSpec) 0f else swayWave * 1.5f
                    translationY = (-2f * breath01).toPx()
                },
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )
    }
}
