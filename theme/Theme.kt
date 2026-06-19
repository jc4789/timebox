package com.example.timeboxvibe.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class Pc98ColorScheme(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val surfaceVariant: Color,
    val scrim: Color,
    val error: Color
)

val LocalPc98Colors = staticCompositionLocalOf<Pc98ColorScheme> {
    error("No Pc98ColorScheme provided")
}

// 1. REIMU RED (Hakurei Shrine Theme)
private val ReimuFocusColors = Pc98ColorScheme(
    background = ReimuRedFocusBackground,
    primary = ReimuRedFocusPrimary,
    secondary = ReimuRedFocusSecondary,
    surfaceVariant = ReimuRedFocusSurface,
    scrim = ReimuRedFocusShadow,
    error = ReimuRedFocusPrimary
)

private val ReimuBreakColors = Pc98ColorScheme(
    background = ReimuRedBreakBackground,
    primary = ReimuRedBreakPrimary,
    secondary = ReimuRedBreakSecondary,
    surfaceVariant = ReimuRedBreakSurface,
    scrim = ReimuRedBreakShadow,
    error = ReimuRedFocusPrimary
)

// 2. MARISA AMBER (Ordinary Magician Theme)
private val MarisaFocusColors = Pc98ColorScheme(
    background = MarisaAmberFocusBackground,
    primary = MarisaAmberFocusPrimary,
    secondary = MarisaAmberFocusSecondary,
    surfaceVariant = MarisaAmberFocusSurface,
    scrim = MarisaAmberFocusShadow,
    error = ReimuRedFocusPrimary
)

private val MarisaBreakColors = Pc98ColorScheme(
    background = MarisaAmberBreakBackground,
    primary = MarisaAmberBreakPrimary,
    secondary = MarisaAmberBreakSecondary,
    surfaceVariant = MarisaAmberBreakSurface,
    scrim = MarisaAmberBreakShadow,
    error = ReimuRedFocusPrimary
)

// 3. ALICE BLUE (Seven-Colored Puppeteer Theme)
private val AliceFocusColors = Pc98ColorScheme(
    background = AliceBlueFocusBackground,
    primary = AliceBlueFocusPrimary,
    secondary = AliceBlueFocusSecondary,
    surfaceVariant = AliceBlueFocusSurface,
    scrim = AliceBlueFocusShadow,
    error = ReimuRedFocusPrimary
)

private val AliceBreakColors = Pc98ColorScheme(
    background = AliceBlueBreakBackground,
    primary = AliceBlueBreakPrimary,
    secondary = AliceBlueBreakSecondary,
    surfaceVariant = AliceBlueBreakSurface,
    scrim = AliceBlueBreakShadow,
    error = ReimuRedFocusPrimary
)

// 4. KAGUYA PURPLE (Eternal Princess Theme)
private val KaguyaFocusColors = Pc98ColorScheme(
    background = KaguyaPurpleFocusBackground,
    primary = KaguyaPurpleFocusPrimary,
    secondary = KaguyaPurpleFocusSecondary,
    surfaceVariant = KaguyaPurpleFocusSurface,
    scrim = KaguyaPurpleFocusShadow,
    error = ReimuRedFocusPrimary
)

private val KaguyaBreakColors = Pc98ColorScheme(
    background = KaguyaPurpleBreakBackground,
    primary = KaguyaPurpleBreakPrimary,
    secondary = KaguyaPurpleBreakSecondary,
    surfaceVariant = KaguyaPurpleBreakSurface,
    scrim = KaguyaPurpleBreakShadow,
    error = ReimuRedFocusPrimary
)

@Composable
fun TimeBoxVibeTheme(
    appTheme: String = "reimu",
    isBreak: Boolean = false,
    content: @Composable () -> Unit
) {
    val targetScheme = when (appTheme) {
        "reimu" -> if (isBreak) ReimuBreakColors else ReimuFocusColors
        "marisa" -> if (isBreak) MarisaBreakColors else MarisaFocusColors
        "alice" -> if (isBreak) AliceBreakColors else AliceFocusColors
        "kaguya" -> if (isBreak) KaguyaBreakColors else KaguyaFocusColors
        // Fallbacks for legacy/backward compatibility
        "dark", "oled" -> if (isBreak) ReimuBreakColors else ReimuFocusColors
        "dark-pink" -> if (isBreak) KaguyaBreakColors else KaguyaFocusColors
        "pastel" -> if (isBreak) AliceBreakColors else AliceFocusColors
        else -> if (isBreak) ReimuBreakColors else ReimuFocusColors
    }

    CompositionLocalProvider(LocalPc98Colors provides targetScheme) {
        content()
    }
}