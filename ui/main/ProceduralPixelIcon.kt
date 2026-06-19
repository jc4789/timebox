package com.example.timeboxvibe.ui.main

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import com.example.timeboxvibe.theme.LocalPc98Colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun ProceduralPixelIcon(
    iconName: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    customColor: Color? = null
) {
    val colors = LocalPc98Colors.current
    val primaryColor = customColor ?: colors.primary
    val onBackgroundColor = colors.primary
    val surfaceColor = colors.surfaceVariant

    // THE ARCHITECT UPGRADE: Memoization (Texture Caching)
    // We only run the 1,024-pixel math loop ONCE.
    // If the theme color changes, it rebuilds the texture instantly.
    val pixelTexture = remember(iconName, primaryColor, onBackgroundColor, surfaceColor) {
        val gridWidth = 32
        val gridHeight = 32

        // Create a raw 32x32 byte array in memory
        val bitmap = Bitmap.createBitmap(gridWidth, gridHeight, Bitmap.Config.ARGB_8888)

        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                val color = getPixelColor(
                    iconName = iconName, x = x, y = y,
                    primaryColor = primaryColor,
                    onBackgroundColor = onBackgroundColor,
                    surfaceColor = surfaceColor
                )
                // Inject the raw hex color directly into the memory grid
                bitmap.setPixel(x, y, color?.toArgb() ?: android.graphics.Color.TRANSPARENT)
            }
        }

        // Convert to a Compose-friendly hardware texture
        bitmap.asImageBitmap()
    }

    // Draw the cached texture with zero-blur pixel scaling!
    Image(
        bitmap = pixelTexture,
        contentDescription = iconName,
        modifier = modifier,
        filterQuality = FilterQuality.None, // Keeps it perfectly crunchy
        alpha = alpha
    )
}

// Keep your exact math down here!
private fun getPixelColor(
    iconName: String,
    x: Int,
    y: Int,
    primaryColor: Color,
    onBackgroundColor: Color,
    surfaceColor: Color
): Color? {
    val dx = x - 15.5f
    val dy = y - 15.5f
    val rSq = dx * dx + dy * dy

    return when (iconName) {
        "yinyang", "reset_yinyang" -> {
            if (rSq > 225f) return null
            if (rSq > 196f) return Color.Black
            val dxSub = dx
            val dyTop = y - 8.5f
            val dyBottom = y - 22.5f
            val rTopSq = dxSub * dxSub + dyTop * dyTop
            val rBottomSq = dxSub * dxSub + dyBottom * dyBottom
            when {
                rTopSq <= 20.25f -> if (rTopSq <= 4f) Color.White else Color.Black
                rBottomSq <= 20.25f -> if (rBottomSq <= 4f) Color.Black else Color.White
                dx < 0f -> Color.White
                else -> Color.Black
            }
        }
        "play_danmaku" -> {
            val cx = 12f
            val cy = 15.5f
            val dxPlay = x - cx
            val dyPlay = y - cy
            fun inFlameShape(r: Float, tail: Float): Boolean {
                if (dxPlay <= 0) {
                    return dxPlay * dxPlay + dyPlay * dyPlay <= r * r
                } else {
                    if (dxPlay > tail) return false
                    val thickness = r * (1f - dxPlay / tail)
                    return abs(dyPlay) <= thickness
                }
            }
            if (inFlameShape(11f, 17f)) {
                when {
                    inFlameShape(5f, 7f) -> Color.White
                    inFlameShape(8f, 12f) -> Color(0xFFFFCC00) // yellow
                    inFlameShape(10f, 15f) -> Color(0xFFFF2200) // fire-red
                    else -> Color.Black // high-contrast outline
                }
            } else null
        }
        "pause_ofuda" -> {
            val isTal1 = x in 7..13 && y in 4..27
            val isTal2 = x in 18..24 && y in 4..27
            if (isTal1 || isTal2) {
                val tx = if (isTal1) x - 7 else x - 18
                val ty = y - 4
                if (tx == 0 || tx == 6 || ty == 0 || ty == 23) {
                    return Color(0xFFCC0000)
                }
                val isRune = when (ty) {
                    3, 4 -> tx == 3
                    6 -> tx in 2..4
                    8, 9 -> tx == 3
                    11 -> tx in 2..4
                    13, 14 -> tx == 3
                    16 -> tx in 2..4
                    18, 19 -> tx == 3
                    21 -> tx in 1..5
                    else -> false
                }
                return if (isRune) Color(0xFFCC0000) else Color.White
            }
            // 1-pixel high-contrast black outline surrounding the talismans
            val isOutline1 = x in 6..14 && y in 3..28
            val isOutline2 = x in 17..25 && y in 3..28
            if (isOutline1 || isOutline2) {
                return Color.Black
            }
            null
        }
        "skip_double_danmaku" -> {
            fun getChevronColor(xTip: Float): Color? {
                val dxVal = xTip - x
                if (dxVal < 0 || dxVal > 10f) return null
                val dyVal = abs(y - 15.5f)
                val isOuter = dyVal <= dxVal && !(dxVal >= 4f && dyVal <= (dxVal - 4f))
                if (!isOuter) return null
                val isCore = dyVal <= (dxVal - 1f) / 2f && dxVal >= 1f && !(dxVal >= 4f && dyVal <= (dxVal - 4f))
                if (isCore) return Color.White
                if (dyVal <= dxVal - 1f) {
                    return Color(0xFF00AAFF)
                }
                return Color.Black // high-contrast outline
            }
            val col2 = getChevronColor(26f)
            if (col2 != null) return col2
            val col1 = getChevronColor(15f)
            if (col1 != null) return col1
            null
        }
        "ribbon" -> {
            if (abs(dx) <= 2.2f && abs(dy) <= 2.2f) return primaryColor
            val isLeftLoop = dx >= -12f && dx <= -2f && dy >= -7f && dy <= 3f &&
                    !(dx >= -9f && dx <= -4f && dy >= -5f && dy <= 1f) &&
                    (dy <= -0.5f * dx + 1f && dy >= 0.5f * dx - 3f)
            val isRightLoop = dx >= 2f && dx <= 12f && dy >= -7f && dy <= 3f &&
                    !(dx >= 4f && dx <= 9f && dy >= -5f && dy <= 1f) &&
                    (dy <= 0.5f * dx + 1f && dy >= -0.5f * dx - 3f)
            val isLeftTail = dy >= 2f && dy <= 13f && dx >= -9f && dx <= -1f && abs(dy - (-dx * 1.2f)) <= 2.5f
            val isRightTail = dy >= 2f && dy <= 13f && dx >= 1f && dx <= 9f && abs(dy - (dx * 1.2f)) <= 2.5f
            if (isLeftLoop || isRightLoop || isLeftTail || isRightTail) return primaryColor
            null
        }
        "gohei" -> {
            val stickDist = abs(x + y - 31f)
            if (stickDist <= 0.8f && x in 5..26 && y in 5..26) return Color(0xFF8B4513)
            val isRibbon1 = (x in 15..17 && y in 5..9) || (x in 17..20 && y in 9..13) || (x in 15..18 && y in 13..17)
            val isRibbon2 = (x in 10..12 && y in 10..14) || (x in 12..15 && y in 14..18) || (x in 10..13 && y in 18..22)
            if (isRibbon1 || isRibbon2) return Color.White
            null
        }
        "ofuda" -> {
            // --- FRONT TALISMAN LAYOUT (Offset down-right: x in 14..25, y in 8..29) ---
            val isFrontOutline = (x == 14 || x == 25) && y in 8..29 || (y == 8 || y == 29) && x in 14..25
            val isFrontPin = x in 19..20 && y in 9..10
            val isFrontPinOutline = (x == 18 && y in 9..10) || (x == 21 && y in 9..10) || (y == 11 && x in 19..20)
            val isFrontRune = (x == 19 || x == 20) && y in 12..27 ||
                    y == 14 && x in 16..23 ||
                    y == 18 && x in 16..23 ||
                    y == 22 && x in 16..23 ||
                    y == 25 && x in 17..22 ||
                    (x == 16 && y == 16) || (x == 23 && y == 16) ||
                    (x == 16 && y == 20) || (x == 23 && y == 20)

            if (isFrontOutline) return Color.Black
            if (isFrontPinOutline) return Color.Black
            if (isFrontPin) return Color(0xFFFFEE55) // Gold header pin
            if (isFrontRune) return Color(0xFFCC0000) // Crimson rune seal
            
            // Dithered shading along bottom/right edge of front paper
            val isFrontDither = (x == 24 && y in 15..28) || (y == 28 && x in 15..24)
            if (isFrontDither && (x + y) % 2 == 0) return Color(0xFFFFAAA6)
            
            if (x in 15..24 && y in 9..28) return Color.White // Front paper body

            // --- BACK TALISMAN LAYOUT (Offset up-left: x in 6..15, y in 3..24) ---
            val isBackOutline = (x == 6 || x == 15) && y in 3..24 || (y == 3 || y == 24) && x in 6..15
            val isBackRune = x == 10 && y in 6..22 ||
                    y == 8 && x in 8..13 ||
                    y == 13 && x in 8..13 ||
                    y == 18 && x in 7..14

            if (isBackOutline) return Color.Black
            if (isBackRune) return Color(0xFFCC0000) // Crimson rune seal
            
            // Dithered shading along bottom/right edge of back paper
            val isBackDither = (x == 14 && y in 10..23) || (y == 23 && x in 7..14)
            if (isBackDither && (x + y) % 2 == 0) return Color(0xFFFFAAA6)
            
            if (x in 7..14 && y in 4..23) return Color.White // Back paper body

            null
        }
        "hakkero" -> {
            val octVal = maxOf(abs(dx), abs(dy)) + 0.5f * (abs(dx) + abs(dy))
            if (octVal > 16f) return null
            if (octVal > 12f) return Color(0xFFD4AF37)
            if (octVal > 10f) return Color.Black
            if (rSq <= 36f) return if (rSq <= 9f) Color(0xFFFFCC00) else primaryColor
            val isTrigram = (y == 7 && x in 13..18) || (y == 24 && x in 13..18) || (x == 7 && y in 13..18) || (x == 24 && y in 13..18)
            if (isTrigram) return Color.White
            Color(0xFF333333)
        }
        "watch" -> {
            val cx = 15.5f
            val cy = 16.5f
            val dxWatch = x - cx
            val dyWatch = y - cy
            val rWatchSq = dxWatch * dxWatch + dyWatch * dyWatch

            // Hanger loop at top (y in 1..4, x in 12..19)
            val isHangerOutline = (y == 1 && x in 13..18) || (y == 2 && (x == 12 || x == 19)) ||
                    (y == 3 && (x == 12 || x == 19)) || (y == 4 && (x == 13 || x == 18))
            val isHangerFill = (y == 2 && x in 13..18) || (y == 3 && x in 13..18) || (y == 4 && x in 14..17)
            val isHangerHole = (y == 2 || y == 3) && x in 15..16
            
            if (isHangerOutline) return Color.Black
            if (isHangerFill) {
                if (isHangerHole) return null
                return Color(0xFFCCCCCC) // Silver hanger
            }

            // Pocket watch casing circle
            if (rWatchSq <= 144f) {
                if (rWatchSq > 121f) return Color.Black // Casing outer outline
                if (rWatchSq > 100f) return Color(0xFFD4AF37) // Gold outer bezel
                if (rWatchSq > 81f) return Color.Black // Casing inner bevel

                // Inside the watch face (radius <= 9, rWatchSq <= 81f)
                // Center pivot
                val isPivot = x in 15..16 && y in 16..17
                if (isPivot) return Color(0xFFD4AF37) // Gold pivot

                // Hands (Black)
                val isHourHand = dxWatch > 0f && dyWatch < 0f && abs(dxWatch - (-dyWatch)) <= 0.8f && rWatchSq <= 16f
                val isMinHand = dxWatch < 0f && dyWatch < 0f && abs(dxWatch - dyWatch) <= 0.8f && rWatchSq <= 49f
                if (isHourHand || isMinHand) return Color.Black

                // Dial Ticks
                val is12Tick = (x == 15 || x == 16) && y == 8
                val is6Tick = (x == 15 || x == 16) && y == 24
                val is3Tick = x == 23 && (y == 16 || y == 17)
                val is9Tick = x == 7 && (y == 16 || y == 17)
                val isDiagTick = (x == 11 && y == 10) || (x == 19 && y == 10) ||
                        (x == 11 && y == 22) || (x == 19 && y == 22) ||
                        (x == 8 && y == 12) || (x == 22 && y == 12) ||
                        (x == 8 && y == 20) || (x == 22 && y == 20)

                if (is12Tick || is6Tick || is3Tick || is9Tick || isDiagTick) return Color.Black

                return Color.White
            }
            null
        }
        else -> null
    }
}