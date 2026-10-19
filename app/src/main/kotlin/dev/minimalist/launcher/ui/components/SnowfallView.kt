package dev.minimalist.launcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class Snowflake(
    val xRatio: Float,
    val speed: Float,
    val radius: Float,
    val alpha: Float,
    val initialYRatio: Float
)

/**
 * Minimalist snowfall animation easter egg.
 * Renders small drifting translucent dots on a Canvas.
 */
@Composable
fun SnowfallEffect(
    modifier: Modifier = Modifier,
    flakeCount: Int = 40,
    color: Color = Color.White
) {
    val snowflakes = remember(flakeCount) {
        val rand = Random(42)
        List(flakeCount) {
            Snowflake(
                xRatio = rand.nextFloat(),
                speed = rand.nextFloat() * 0.5f + 0.5f,
                radius = rand.nextFloat() * 2f + 1.5f,
                alpha = rand.nextFloat() * 0.4f + 0.2f,
                initialYRatio = rand.nextFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "snowfall")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        snowflakes.forEach { flake ->
            val y = ((flake.initialYRatio + progress * flake.speed) % 1f) * height
            val x = (flake.xRatio * width) + kotlin.math.sin(progress * 6.28f + flake.initialYRatio * 10f) * 15f
            drawCircle(
                color = color.copy(alpha = flake.alpha),
                radius = flake.radius,
                center = Offset(x.toFloat(), y)
            )
        }
    }
}
