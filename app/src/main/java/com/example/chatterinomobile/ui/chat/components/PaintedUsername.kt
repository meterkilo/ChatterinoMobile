package com.example.chatterinomobile.ui.chat.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import coil.compose.rememberAsyncImagePainter
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
    val shadow = remember(paint) { paint?.shadows?.firstOrNull()?.toComposeShadow() }

    if (paint is PaintModel.Image) {
        ImagePaintedUsername(
            name = name,
            fallbackColor = fallbackColor,
            paint = paint,
            style = style,
            shadow = shadow,
            modifier = modifier
        )
        return
    }

    val brush = remember(paint, fallbackColor) { paint.toBrush(fallbackColor) }

    val resolved = style.copy(
        brush = brush,
        fontWeight = FontWeight.Bold,
        shadow = shadow
    )

    Box(modifier = modifier) {
        BasicText(text = name, style = resolved)
    }
}

@Composable
private fun ImagePaintedUsername(
    name: String,
    fallbackColor: Color,
    paint: PaintModel.Image,
    style: TextStyle,
    shadow: Shadow?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = style.copy(
        color = Color.Black,
        fontWeight = FontWeight.Bold,
        shadow = shadow
    )
    val textLayout = remember(name, textStyle) {
        textMeasurer.measure(text = name, style = textStyle)
    }
    val density = LocalDensity.current
    val width = with(density) { textLayout.size.width.toDp() }
    val height = with(density) { textLayout.size.height.toDp() }
    val imagePainter = rememberAsyncImagePainter(model = paint.url)

    Box(modifier = modifier.size(width, height)) {
        BasicText(
            text = name,
            style = style.copy(
                color = fallbackColor,
                fontWeight = FontWeight.Bold,
                shadow = shadow
            )
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(
                    bounds = Rect(Offset.Zero, size),
                    paint = Paint()
                )
                with(imagePainter) {
                    draw(size = size)
                }
                drawText(
                    textLayoutResult = textLayout,
                    blendMode = BlendMode.DstIn
                )
                canvas.restore()
            }
        }
    }
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
