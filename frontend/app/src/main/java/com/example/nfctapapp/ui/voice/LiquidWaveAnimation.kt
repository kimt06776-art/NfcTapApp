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
        Canvas(modifier = modifier) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseRadius = size.minDimension * 0.32f

            // 여러 개의 원을 회전시켜 유기적인 blob 효과 만들기
            for (i in 0..7) {
                val angle1 = Math.toRadians((blob1 + i * 45).toDouble())
                val angle2 = Math.toRadians((blob2 + i * 45).toDouble())
                val offset1 = size.minDimension * 0.1f
                val offset2 = size.minDimension * 0.06f

                val x1 = centerX + (offset1 * kotlin.math.cos(angle1)).toFloat()
                val y1 = centerY + (offset1 * kotlin.math.sin(angle1)).toFloat()

                val x2 = centerX + (offset2 * kotlin.math.cos(angle2)).toFloat()
                val y2 = centerY + (offset2 * kotlin.math.sin(angle2)).toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.4f),
                            color.copy(alpha = 0.05f)
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
                                color.copy(alpha = 0.3f),
                                color.copy(alpha = 0.02f)
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
                        color.copy(alpha = 0.6f),
                        color.copy(alpha = 0.1f)
                    ),
                    center = Offset(centerX, centerY)
                ),
                radius = baseRadius,
                center = Offset(centerX, centerY)
            )
        }
    }
}
