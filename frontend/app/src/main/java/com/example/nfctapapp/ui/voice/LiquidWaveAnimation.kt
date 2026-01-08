package com.example.nfctapapp.ui.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Liquid Wave Animation for voice recognition
 *
 * 음성 인식 중 액체 물결 애니메이션 효과
 */
@Composable
fun LiquidWaveAnimation(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true,
    color: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_wave")

    // 여러 파동 애니메이션
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    if (isAnimating) {
        Canvas(modifier = modifier.size(300.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // 3개의 물결 효과를 겹쳐서 그리기
            for (i in 0..2) {
                val phase = when (i) {
                    0 -> wave1
                    1 -> wave2
                    else -> wave3
                }
                val amplitude = 30f * (i + 1)
                val frequency = 0.02f / (i + 1)

                val path = androidx.compose.ui.graphics.Path()
                path.moveTo(0f, centerY)

                // 부드러운 물결 그리기
                for (x in 0..width.toInt()) {
                    val y = centerY + amplitude * sin(frequency * x + phase)
                    path.lineTo(x.toFloat(), y)
                }

                // 그라데이션 효과
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.2f * (3 - i) / 3f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
            }
        }
    }
}

/**
 * Pulsing Circles Animation
 *
 * 맥동하는 원 애니메이션 (액체 방울 효과)
 */
@Composable
fun PulsingCirclesAnimation(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true,
    color: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_circles")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )

    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale3"
    )

    if (isAnimating) {
        Canvas(modifier = modifier.size(200.dp)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseRadius = 60f

            // 3개의 원을 겹쳐 그리기 (바깥쪽부터)
            listOf(
                Triple(scale1, 0.1f, baseRadius),
                Triple(scale2, 0.15f, baseRadius * 0.8f),
                Triple(scale3, 0.2f, baseRadius * 0.6f)
            ).forEach { (scale, alpha, radius) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            color.copy(alpha = 0f)
                        ),
                        center = Offset(centerX, centerY)
                    ),
                    radius = radius * scale,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}

/**
 * Sound Wave Bars Animation
 *
 * 사운드 웨이브 바 애니메이션
 */
@Composable
fun SoundWaveBarsAnimation(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true,
    color: Color = Color.White,
    barCount: Int = 5
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sound_wave")

    // 애니메이션 값들을 미리 생성
    val heights = remember { List(barCount) { 0f }.toMutableList() }

    for (i in 0 until barCount) {
        val animatedHeight by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + (i * 100),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$i"
        )
        heights[i] = animatedHeight
    }

    if (isAnimating) {
        Canvas(modifier = modifier.size(150.dp, 80.dp)) {
            val barWidth = size.width / (barCount * 2f)
            val spacing = barWidth
            val maxHeight = size.height * 0.8f

            for (i in 0 until barCount) {
                val height = maxHeight * heights[i]
                val x = spacing + (i * (barWidth + spacing))
                val y = (size.height - height) / 2f

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                )
            }
        }
    }
}

/**
 * Liquid Blob Animation
 *
 * 액체 방울 애니메이션 (유기적인 움직임)
 */
@Composable
fun LiquidBlobAnimation(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true,
    color: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_blob")

    val blob1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob1"
    )

    val blob2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2"
    )

    if (isAnimating) {
        Canvas(modifier = modifier.size(250.dp)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseRadius = 80f

            // 여러 개의 원을 회전시켜 유기적인 blob 효과 만들기
            for (i in 0..7) {
                val angle1 = Math.toRadians((blob1 + i * 45).toDouble())
                val angle2 = Math.toRadians((blob2 + i * 45).toDouble())
                val offset1 = 25f
                val offset2 = 15f

                val x1 = centerX + (offset1 * kotlin.math.cos(angle1)).toFloat()
                val y1 = centerY + (offset1 * kotlin.math.sin(angle1)).toFloat()

                val x2 = centerX + (offset2 * kotlin.math.cos(angle2)).toFloat()
                val y2 = centerY + (offset2 * kotlin.math.sin(angle2)).toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            color.copy(alpha = 0f)
                        ),
                        center = Offset(x1, y1)
                    ),
                    radius = baseRadius * 0.6f,
                    center = Offset(x1, y1)
                )

                if (i % 2 == 0) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.1f),
                                color.copy(alpha = 0f)
                            ),
                            center = Offset(x2, y2)
                        ),
                        radius = baseRadius * 0.4f,
                        center = Offset(x2, y2)
                    )
                }
            }

            // 중앙 원
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0f)
                    ),
                    center = Offset(centerX, centerY)
                ),
                radius = baseRadius,
                center = Offset(centerX, centerY)
            )
        }
    }
}
