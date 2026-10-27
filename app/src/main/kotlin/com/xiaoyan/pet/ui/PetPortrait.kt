package com.xiaoyan.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.game.Scenes
import com.xiaoyan.pet.data.SaveRepository
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 立绘精灵 (v1.3.0):
 * - 换装优先级: 四幕+夜间→星光裙 / 夜间+卧室→睡衣 / 其余→日常 (Scenes.costumeFor)
 * - fallback 直接切帧 (不交叉淡化, 避免五官瞬移被看见); 同套表情切换 200ms Crossfade
 * - 呼吸/微摆加 in-out 缓动 + 相位错开 (去机械感); 眨眼随机 3-6s, 30% 连双眨
 * - 动画风格三档 (温柔/活泼/关闭) 参数集中; 低配模式全关; 主页/待机星光粒子层
 */
data class AnimParams(
    val breathMs: Int, val swayMs: Int,
    val breathScale: Float, val swayDeg: Float,
    val doubleBlink: Int,   // 连双眨概率 %
    val particle: Boolean,
)

/** 动画风格三档 (设置页可切, 免重编译) */
fun animParams(style: String): AnimParams = when (style) {
    "lively" -> AnimParams(2200, 2800, 0.016f, 2.2f, 45, true)
    "off" -> AnimParams(0, 0, 0f, 0f, 0, false)
    else -> AnimParams(3200, 4100, 0.012f, 1.5f, 30, true)   // gentle 默认
}

/** 当前心情 (台词未覆盖时): 难过/开心/中性 */
fun currentMoodOf(s: com.xiaoyan.pet.data.PetState, lineMood: String? = null): String =
    lineMood ?: when {
        s.satiety < 30 -> "sad"
        s.mood >= 80 -> "happy"
        s.mood <= 40 -> "sad"
        else -> "neutral"
    }

@Composable
fun PortraitSprite(costume: String, mood: String, lowSpec: Boolean) {
    val repo = PetApp.repo
    val params = if (lowSpec) animParams("off") else animParams(repo.animStyle())
    val target = Scenes.portraitResFor(costume, Scenes.moodFallback(costume, mood))

    // 同套表情切换 Crossfade; 换套/换表情 fallback = 直接切帧 (外层不做跨套淡化)
    Crossfade(targetState = target, animationSpec = tween(200)) { id ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedSprite(id, params)
        }
    }
}

@Composable
private fun AnimatedSprite(resId: Int, p: AnimParams) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val infinite = rememberInfiniteTransition()
    val breath01 by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(p.breathMs.coerceAtLeast(1), easing = androidx.compose.animation.core.EaseInOut), RepeatMode.Reverse))
    val sway01 by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(p.swayMs.coerceAtLeast(1), easing = androidx.compose.animation.core.EaseInOut), RepeatMode.Reverse))
    val swayWave = sway01 * 2f - 1f

    var blinkOn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((3000 + Random.nextInt(3000)).toLong())
            if (p.doubleBlink == 0 || Random.nextInt(100) >= p.doubleBlink) {
                blinkOn = true; delay(60); blinkOn = false; delay(60)
            } else {
                blinkOn = true; delay(60); blinkOn = false; delay(90)
                blinkOn = true; delay(60); blinkOn = false
            }
        }
    }
    val blinkScale = if (blinkOn) 0.97f else 1f

    val density = LocalDensity.current.density
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
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleY = (1f + p.breathScale * breath01) * blinkScale
                    rotationZ = p.swayDeg * swayWave
                    translationY = (-2f * breath01) * density
                },
            contentScale = ContentScale.Fit,     // 横屏防裁切 (Fit + 底边中点, 10-24 审计② 红线)
            alignment = Alignment.BottomCenter,
        )
    }
}

/** 轻量星光粒子层 (主页/待机; 低配全关) */
@Composable
fun StarParticles(on: Boolean, count: Int = 14) {
    if (!on) return
    val seeds = remember { (0 until count).map { Random.nextInt(10000) } }
    val infinite = rememberInfiniteTransition()
    val t by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(5200), RepeatMode.Reverse))
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        seeds.forEachIndexed { i, s ->
            val x = (s * 0.618033f).toFloat() % 1f * size.width
            val y = ((s * 0.381966f).toFloat() % 1f) * size.height
            val tw = (0.4f + 0.6f * ((t * 3f + i * 0.13f) % 1f).let { if (it < 0.5f) it * 2f else (1f - it) * 2f }).coerceIn(0f, 1f)
            drawCircle(
                color = androidx.compose.ui.graphics.Color(0xFFFFF3C4).copy(alpha = 0.5f * tw),
                radius = 1.6f + (i % 3),
                center = androidx.compose.ui.geometry.Offset(x, y),
            )
        }
    }
}
