package com.example.timeboxvibe.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeboxvibe.R
import com.example.timeboxvibe.theme.LocalPc98Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val ArcadeFont = FontFamily(Font(R.font.dotgothic))

enum class DitherPattern {
    SOLID,
    DITHER_12,
    DITHER_25,
    DITHER_50,
    HORIZONTAL_LINE,
    VERTICAL_LINE
}

object Pc98DitherCache {
    private val bitmapCache = mutableMapOf<String, Bitmap>()
    private val brushCache = mutableMapOf<String, ShaderBrush>()

    fun getDitherBitmap(color1: Color, color2: Color, pattern: DitherPattern): Bitmap {
        val key = "${color1.toArgb()}_${color2.toArgb()}_$pattern"
        return bitmapCache.getOrPut(key) {
            val size = 8
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c1 = color1.toArgb()
            val c2 = color2.toArgb()
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val useColor1 = when (pattern) {
                        DitherPattern.SOLID -> true
                        DitherPattern.DITHER_50 -> (x + y) % 2 == 0
                        DitherPattern.DITHER_25 -> (x % 2 == 0 && y % 2 == 0) && (x + y) % 4 == 0
                        DitherPattern.DITHER_12 -> (x % 2 == 0 && y % 2 == 0) && (x + y) % 8 == 0
                        DitherPattern.HORIZONTAL_LINE -> y % 2 == 0
                        DitherPattern.VERTICAL_LINE -> x % 2 == 0
                    }
                    bmp.setPixel(x, y, if (useColor1) c1 else c2)
                }
            }
            bmp
        }
    }

    fun getDitherBrush(color1: Color, color2: Color, pattern: DitherPattern): ShaderBrush {
        val key = "${color1.toArgb()}_${color2.toArgb()}_$pattern"
        return brushCache.getOrPut(key) {
            ShaderBrush(BitmapShader(getDitherBitmap(color1, color2, pattern), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT))
        }
    }
}

@Composable
fun Pc98Text(
    text: String,
    color: Color,
    fontSize: TextUnit,
    letterSpacing: TextUnit = 0.sp,
    textAlign: TextAlign? = null,
    shadowColor: Color = Color.Black.copy(alpha = 0.8f),
    modifier: Modifier = Modifier
) {
    val shadowOffset = if (fontSize.value > 20f) 3f else 1.5f
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        fontFamily = ArcadeFont,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        style = TextStyle(
            shadow = Shadow(
                color = shadowColor,
                offset = Offset(shadowOffset, shadowOffset),
                blurRadius = 0f
            )
        ),
        modifier = modifier
    )
}

/**
 * THE TRUE TOUHOU HUD MODIFIER
 * Draws a flat, high-tech/magical HUD frame with heavy corner brackets.
 * Aligns perfectly to the pixel grid!
 */
fun Modifier.touhouHudFrame(
    backgroundColor: Color = Color(0xFF1A1A1A),
    lineColor: Color = Color.White,
    isActive: Boolean = false,
    ditherPattern: DitherPattern = DitherPattern.SOLID,
    ditherColor: Color = Color.Transparent
) = this.then(
    Modifier.drawBehind {
        if (!isActive && ditherPattern != DitherPattern.SOLID && ditherColor != Color.Transparent) {
            // Draw with strict zero-filtering, zero-anti-aliasing tiled bitmap shader
            drawIntoCanvas { canvas ->
                val paint = androidx.compose.ui.graphics.Paint()
                val nativePaint = paint.asFrameworkPaint()
                nativePaint.isAntiAlias = false
                nativePaint.isFilterBitmap = false
                
                val bitmap = Pc98DitherCache.getDitherBitmap(backgroundColor, ditherColor, ditherPattern)
                nativePaint.shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, nativePaint)
            }
        } else {
            // Draw the base background
            drawRect(if (isActive) lineColor else backgroundColor)
        }

        val thinStroke = 1.dp.toPx()
        val thickStroke = 2.dp.toPx()
        val w = size.width
        val h = size.height

        val actualLineColor = if (isActive) backgroundColor else lineColor

        // 1. Concentric Bounding Frames
        // Outer thin frame
        drawRect(actualLineColor, style = Stroke(thinStroke))

        // Inner thin frame (faded, offset by 3.dp)
        val gap = 3.dp.toPx()
        if (w > gap * 2 && h > gap * 2) {
            drawRect(
                color = actualLineColor.copy(alpha = 0.4f),
                topLeft = Offset(gap, gap),
                size = Size(w - gap * 2, h - gap * 2),
                style = Stroke(thinStroke)
            )
        }

        // 2. Ornate Traditional Shrine / Sigil Corners
        val oLength = 8.dp.toPx()  // Outer length
        val iLength = 4.dp.toPx()  // Inner length
        val iOffset = 3.dp.toPx()  // Inner offset
        val dotOffset = 4.5.dp.toPx()
        val dotSize = 2.dp.toPx()

        // Top Left
        // Outer L-bracket
        drawLine(actualLineColor, Offset(0f, 0f), Offset(oLength, 0f), thickStroke)
        drawLine(actualLineColor, Offset(0f, 0f), Offset(0f, oLength), thickStroke)
        // Inner L-bracket
        drawLine(actualLineColor, Offset(iOffset, iOffset), Offset(iOffset + iLength, iOffset), thinStroke)
        drawLine(actualLineColor, Offset(iOffset, iOffset), Offset(iOffset, iOffset + iLength), thinStroke)
        // Accent Dot (Orb)
        drawRect(
            color = actualLineColor,
            topLeft = Offset(dotOffset, dotOffset),
            size = Size(dotSize, dotSize)
        )

        // Top Right
        // Outer L-bracket
        drawLine(actualLineColor, Offset(w, 0f), Offset(w - oLength, 0f), thickStroke)
        drawLine(actualLineColor, Offset(w, 0f), Offset(w, oLength), thickStroke)
        // Inner L-bracket
        drawLine(actualLineColor, Offset(w - iOffset, iOffset), Offset(w - iOffset - iLength, iOffset), thinStroke)
        drawLine(actualLineColor, Offset(w - iOffset, iOffset), Offset(w - iOffset, iOffset + iLength), thinStroke)
        // Accent Dot
        drawRect(
            color = actualLineColor,
            topLeft = Offset(w - dotOffset - dotSize, dotOffset),
            size = Size(dotSize, dotSize)
        )

        // Bottom Left
        // Outer L-bracket
        drawLine(actualLineColor, Offset(0f, h), Offset(oLength, h), thickStroke)
        drawLine(actualLineColor, Offset(0f, h), Offset(0f, h - oLength), thickStroke)
        // Inner L-bracket
        drawLine(actualLineColor, Offset(iOffset, h - iOffset), Offset(iOffset + iLength, h - iOffset), thinStroke)
        drawLine(actualLineColor, Offset(iOffset, h - iOffset), Offset(iOffset, h - iOffset - iLength), thinStroke)
        // Accent Dot
        drawRect(
            color = actualLineColor,
            topLeft = Offset(dotOffset, h - dotOffset - dotSize),
            size = Size(dotSize, dotSize)
        )

        // Bottom Right
        // Outer L-bracket
        drawLine(actualLineColor, Offset(w, h), Offset(w - oLength, h), thickStroke)
        drawLine(actualLineColor, Offset(w, h), Offset(w, h - oLength), thickStroke)
        // Inner L-bracket
        drawLine(actualLineColor, Offset(w - iOffset, h - iOffset), Offset(w - iOffset - iLength, h - iOffset), thinStroke)
        drawLine(actualLineColor, Offset(w - iOffset, h - iOffset), Offset(w - iOffset, h - iOffset - iLength), thinStroke)
        // Accent Dot
        drawRect(
            color = actualLineColor,
            topLeft = Offset(w - dotOffset - dotSize, h - dotOffset - dotSize),
            size = Size(dotSize, dotSize)
        )
    }
)

@Composable
fun TouhouButton(text: String, isLarge: Boolean = false, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = LocalPc98Colors.current
    Box(
        modifier = Modifier
            .offset(x = if (isPressed) 2.dp else 0.dp, y = if (isPressed) 2.dp else 0.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) {
                isPressed = true
                onClick()
                scope.launch {
                    delay(100)
                    isPressed = false
                }
            }
            .touhouHudFrame(
                backgroundColor = Color(0xFF1A1A1A),
                lineColor = if (isPressed) colors.primary else Color.White,
                isActive = isPressed
            )
            .padding(horizontal = if (isLarge) 24.dp else 16.dp, vertical = if (isLarge) 16.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Pc98Text(
            text = text,
            color = if (isPressed) Color.Black else colors.primary,
            fontSize = if (isLarge) 16.sp else 12.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun Pc98Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val colors = LocalPc98Colors.current
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val fraction = (offset.x / width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val fraction = (change.position.x / width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
            }
    ) {
        val width = constraints.maxWidth
        val density = LocalDensity.current
        val widthDp = with(density) { width.toDp() }
        
        // Track: PC-98 groove line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .background(colors.surfaceVariant)
        )
        
        // Handle: A retro seek block [ ] or a filled rectangle
        val handleWidth = 12.dp
        val handleHeight = 20.dp
        val maxOffset = widthDp - handleWidth
        val handleOffset = (normalizedValue * maxOffset.value).dp

        Box(
            modifier = Modifier
                .offset(x = handleOffset)
                .size(handleWidth, handleHeight)
                .align(Alignment.CenterStart)
                .touhouHudFrame(
                    backgroundColor = Color(0xFF1A1A1A),
                    lineColor = Color.White,
                    isActive = false
                )
        )
    }
}