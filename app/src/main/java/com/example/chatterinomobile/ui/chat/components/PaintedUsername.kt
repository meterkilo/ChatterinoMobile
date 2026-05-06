package com.example.chatterinomobile.ui.chat.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.chatterinomobile.data.model.ColorStop
import com.example.chatterinomobile.data.model.GradientFunction
import com.example.chatterinomobile.data.model.Paint as PaintModel
import com.example.chatterinomobile.data.model.Shadow as ShadowModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PaintedUsername(
    name: String,
    fallbackColor: Color,
    paint: PaintModel?,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val brush = remember(paint, fallbackColor) { paint.toBrush(fallbackColor) }
    val shadow = remember(paint) { paint?.shadows?.firstOrNull()?.toComposeShadow() }
    val resolved = style.copy(
        brush = brush,
        fontWeight = FontWeight.Bold,
        shadow = shadow
    )
    BasicText(text = name, style = resolved, modifier = modifier)
}

private fun PaintModel?.toBrush(fallback: Color): Brush = when (this) {
    null -> SolidColor(fallback)
    is PaintModel.Solid -> SolidColor(Color(color))
    is PaintModel.Gradient -> {
        val composeStops: Array<Pair<Float, Color>> = (stops
            .sortedBy { it.at }
            .map { it.toPair() }
            .takeIf { it.isNotEmpty() }
            ?: listOf(0f to fallback, 1f to fallback))
            .toTypedArray()
        when (function) {
            GradientFunction.LINEAR -> linearAt(angle.toFloat(), composeStops)
            GradientFunction.RADIAL -> Brush.radialGradient(colorStops = composeStops)
            GradientFunction.CONIC -> Brush.sweepGradient(colorStops = composeStops)
        }
    }
    is PaintModel.Image -> SolidColor(fallback)
}

private fun ColorStop.toPair(): Pair<Float, Color> = at to Color(color)

private fun linearAt(angleDeg: Float, stops: Array<Pair<Float, Color>>): Brush {
    val rad = angleDeg * PI.toFloat() / 180f
    val dx = cos(rad)
    val dy = sin(rad)
    val end = Offset(dx * 200f, dy * 200f)
    return Brush.linearGradient(
        colorStops = stops,
        start = Offset.Zero,
        end = end
    )
}

private fun ShadowModel.toComposeShadow(): Shadow = Shadow(
    color = Color(color),
    offset = Offset(xOffset, yOffset),
    blurRadius = radius
)
