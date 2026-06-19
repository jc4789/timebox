package com.example.timeboxvibe.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timeboxvibe.theme.LocalPc98Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: MainScreenViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val colors = LocalPc98Colors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // --- HEADER ---
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Pc98Text(
                text = "SYSTEM DIAGNOSTICS / 設定",
                color = colors.primary,
                fontSize = 22.sp,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(40.dp)
                    .background(colors.primary)
                    .padding(top = 4.dp)
            )
        }

        // --- 01. LOCALE (Language) ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsLabelText("01. LOCALE / 言語")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("en" to "ENG", "zh" to "中文", "ja" to "日本語").forEach { (code, name) ->
                    SettingsToggle(
                        text = name,
                        isSelected = state.language == code,
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                            viewModel.updateLanguage(code)
                        }
                    )
                }
            }
        }

        SettingsDividerLine()

        // --- 02. AUDIO & HAPTICS ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsLabelText("02. AUDIO & HAPTICS / 音響")

            val sounds = listOf(
                "synth-chime" to "ZEN CHIME (SYNTH)",
                "synth-victory" to "VICTORY ARPEGGIO",
                "oriental" to "ORIENTAL WHIMSICAL",
                "synth-bad-apple" to "BAD APPLE!! (CHIPTUNE)",
                "synth-senbonzakura" to "SENBONZAKURA (CHIPTUNE)"
            )

            // Focus End Alarm Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pc98Text(
                    text = "FOCUS END / RELAX START ALARM",
                    color = colors.secondary,
                    fontSize = 10.sp
                )
                sounds.forEach { (key, name) ->
                    SettingsToggle(
                        text = name,
                        isSelected = state.selectedFocusSound == key,
                        fullWidth = true,
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                            viewModel.updateFocusSound(key)
                            viewModel.previewSound(key)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Relax End Alarm Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pc98Text(
                    text = "RELAX END / FOCUS START ALARM",
                    color = colors.secondary,
                    fontSize = 10.sp
                )
                sounds.forEach { (key, name) ->
                    SettingsToggle(
                        text = name,
                        isSelected = state.selectedRelaxSound == key,
                        fullWidth = true,
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                            viewModel.updateRelaxSound(key)
                            viewModel.previewSound(key)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volume Slider
            SettingsFloatSlider(
                label = "MASTER VOLUME",
                value = state.volume,
                onValueChange = { viewModel.updateVolume(it) }
            )

            // Vibe Slider
            SettingsFloatSlider(
                label = "HAPTIC INTENSITY",
                value = state.vibeIntensity,
                onValueChange = { viewModel.updateSettings(state.strictMode, state.tickEnabled, state.selectedSound, it) }
            )
        }

        SettingsDividerLine()

        // --- 03. PROTOCOLS (Strict Mode) ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsLabelText("03. PROTOCOLS / 規律")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Pc98Text(
                        text = "ADHD BLOCKADE (STRICT)",
                        color = colors.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Pc98Text(
                        text = "LOCKS DEVICE DURING ACTIVE SESSION.\nCANNOT BE BYPASSED.",
                        color = colors.secondary,
                        fontSize = 10.sp
                    )
                }

                SettingsSwitch(
                    isChecked = state.strictMode,
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.updateSettings(!state.strictMode, state.tickEnabled, state.selectedSound, state.vibeIntensity)
                    }
                )

            }
        }

        SettingsDividerLine()

        // --- 04. DISPLAY (Theme) ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsLabelText("04. DISPLAY / 画面")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    "reimu" to "REIMU RED",
                    "marisa" to "MARISA AMBER",
                    "alice" to "ALICE BLUE",
                    "kaguya" to "KAGUYA PURPLE"
                ).forEach { (key, label) ->
                    SettingsToggle(
                        text = label,
                        isSelected = state.appTheme == key,
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                            viewModel.updateTheme(key)
                        }
                    )
                }
            }
        }

        SettingsDividerLine()

        // --- 05. DIAGNOSTICS / 診断 ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsLabelText("05. DIAGNOSTICS / 診断")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Pc98Text(
                        text = "ALARM PRECISION",
                        color = colors.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (state.isExactAlarmPermitted) {
                        Pc98Text(
                            text = "STATUS: ACTIVE // PRECISION LOCK ENGAGED",
                            color = colors.primary,
                            fontSize = 10.sp
                        )
                    } else {
                        Pc98Text(
                            text = "STATUS: DEGRADED // INEXACT FALLBACK\nBACKGROUND TIMING MAY JITTER.",
                            color = colors.error,
                            fontSize = 10.sp
                        )
                    }
                }

                if (!state.isExactAlarmPermitted) {
                    var isPressed by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    Box(
                        modifier = Modifier
                            .offset(x = if (isPressed) 2.dp else 0.dp, y = if (isPressed) 2.dp else 0.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isPressed = true
                                scope.launch {
                                    delay(100)
                                    isPressed = false
                                }
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                                viewModel.requestExactAlarmPermission()
                            }
                            .touhouHudFrame(
                                backgroundColor = Color(0xFF1A1A1A),
                                lineColor = colors.primary,
                                isActive = isPressed
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Pc98Text(
                            text = "AUTHORIZE",
                            color = colors.primary,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .touhouHudFrame(
                                backgroundColor = Color(0xFF1A1A1A),
                                lineColor = colors.primary.copy(alpha = 0.5f),
                                isActive = false
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Pc98Text(
                            text = "SECURE",
                            color = colors.primary.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- PRIVATE COMPONENTS FOR SETTINGS ---

@Composable
private fun SettingsLabelText(text: String) {
    val colors = LocalPc98Colors.current
    Pc98Text(
        text = text,
        fontSize = 12.sp,
        color = colors.secondary,
        letterSpacing = 2.sp
    )
}

@Composable
private fun SettingsDividerLine() {
    val colors = LocalPc98Colors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(colors.primary.copy(alpha = 0.3f))
    )
}

@Composable
private fun SettingsToggle(text: String, isSelected: Boolean, fullWidth: Boolean = false, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = LocalPc98Colors.current
    Box(
        modifier = Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .offset(x = if (isPressed) 2.dp else 0.dp, y = if (isPressed) 2.dp else 0.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
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
                lineColor = if (isSelected || isPressed) colors.primary else Color.White,
                isActive = isSelected || isPressed
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Pc98Text(
            text = text,
            color = if (isSelected || isPressed) Color.Black else colors.primary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsSwitch(isChecked: Boolean, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = LocalPc98Colors.current
    Box(
        modifier = Modifier
            .offset(x = if (isPressed) 2.dp else 0.dp, y = if (isPressed) 2.dp else 0.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
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
                lineColor = if (isChecked || isPressed) colors.error else Color.White,
                isActive = isChecked || isPressed
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Pc98Text(
            text = if (isChecked) "ACTIVE" else "OFF",
            color = if (isChecked || isPressed) Color.Black else colors.error,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun SettingsFloatSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    val colors = LocalPc98Colors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pc98Text(label, color = colors.secondary, fontSize = 10.sp)
            Pc98Text(
                text = "[${(value * 100).toInt()}%]",
                color = colors.primary,
                fontSize = 12.sp
            )
        }
        Pc98Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}