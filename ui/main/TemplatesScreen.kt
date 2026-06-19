package com.example.timeboxvibe.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timeboxvibe.theme.LocalPc98Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CalendarBlockState(
    val id: String,
    val durationMinutes: Int,
    val isRelax: Boolean,
    val label: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplatesScreen(viewModel: MainScreenViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val t = getStrings(state.language)
    val view = LocalView.current
    val colors = LocalPc98Colors.current

    var showForm by remember { mutableStateOf(false) }

    val calendarBlocks = remember {
        mutableStateListOf(
            CalendarBlockState("1", 25, false, "Focus"),
            CalendarBlockState("2", 5, true, "Break")
        )
    }

    var presetName by remember { mutableStateOf("") }
    var engineStyle by remember { mutableStateOf("classic") }
    var completionBehavior by remember { mutableStateOf("alarm") }

    var classicDurationMinutes by remember { mutableStateOf(25) }
    var dualBigMinutes by remember { mutableStateOf(60) }
    var dualSmallSeconds by remember { mutableStateOf(90) }
    var dual5BigMinutes by remember { mutableStateOf(60) }
    var dual5MidMinutes by remember { mutableStateOf(15) }
    var dual5SmallSeconds by remember { mutableStateOf(300) }
    var sequenceInput by remember { mutableStateOf("") }
    var sequenceUnit by remember { mutableStateOf("minutes") }
    var sequenceDualSmallSeconds by remember { mutableStateOf(60) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).padding(16.dp)) {
        // --- HEADER ROW ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Pc98Text(
                    text = if (showForm) "DATA ENTRY / 入力" else "SPELL CARDS / 呪符",
                    color = colors.primary,
                    fontSize = 20.sp, letterSpacing = 1.sp
                )
                Box(modifier = Modifier.height(1.dp).width(40.dp).background(colors.primary).padding(top = 4.dp))
            }
            TouhouButton(text = if (showForm) "CANCEL" else "FORGE NEW") {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                showForm = !showForm
                if (!showForm) presetName = ""
            }
        }

        if (showForm) {
            // --- DATA ENTRY FORM ---
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pc98Text("01. IDENTIFIER", colors.secondary, 12.sp)
                    Pc98TextField(value = presetName, onValueChange = { presetName = it }, placeholder = t.presetNamePlaceholder.uppercase())
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pc98Text("02. ENGINE ARCHITECTURE", colors.secondary, 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("classic" to "CLASSIC", "dual" to "DUAL", "dual.5" to "DUAL.5", "sequence" to "SPIRAL SEQ", "dual-sequence" to "SPIRAL DUAL", "calendar" to "CALENDAR").forEach { (key, name) ->
                            TouhouToggle(text = name, isSelected = engineStyle == key) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); engineStyle = key
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pc98Text("03. COMPLETION DIRECTIVE", colors.secondary, 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("alarm" to "REQUIRE BOMBS", "auto" to "AUTO PROGRESSION").forEach { (key, label) ->
                            TouhouToggle(text = label, isSelected = completionBehavior == key) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); completionBehavior = key
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.primary.copy(alpha = 0.3f)))

                Pc98Text("04. PARAMETERS", colors.secondary, 12.sp)
                when (engineStyle) {
                    "classic" -> Pc98SliderBlock("DURATION", classicDurationMinutes, t.minutes, 1f..120f) { classicDurationMinutes = it }
                    "dual" -> {
                        Pc98SliderBlock("MACRO BLOCK (BIG)", dualBigMinutes, t.minutes, 5f..180f) { dualBigMinutes = it }
                        Pc98SliderBlock("MICRO LOOP (SMALL)", dualSmallSeconds, t.seconds, 10f..300f) { dualSmallSeconds = it }
                    }
                    "dual.5" -> {
                        Pc98SliderBlock("MACRO BLOCK", dual5BigMinutes, t.minutes, 15f..180f) { dual5BigMinutes = it }
                        Pc98SliderBlock("MEDIUM LOOP", dual5MidMinutes, t.minutes, 1f..60f) { dual5MidMinutes = it }
                        Pc98SliderBlock("MICRO LOOP", dual5SmallSeconds, t.seconds, 10f..300f) { dual5SmallSeconds = it }
                    }
                    "sequence", "dual-sequence" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Pc98TextField(value = sequenceInput, onValueChange = { sequenceInput = it }, placeholder = "e.g., 25, 5, 25, 15")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("minutes" to "MINUTES", "seconds" to "SECONDS").forEach { (key, label) ->
                                    TouhouToggle(text = label, isSelected = sequenceUnit == key) { sequenceUnit = key }
                                }
                            }
                            if (engineStyle == "dual-sequence") {
                                Pc98SliderBlock("MICRO LOOP", sequenceDualSmallSeconds, t.seconds, 10f..300f) { sequenceDualSmallSeconds = it }
                            }
                        }
                    }
                    "calendar" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            calendarBlocks.forEachIndexed { index, block ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .touhouHudFrame(
                                            backgroundColor = Color(0xFF1A1A1A),
                                            lineColor = if (block.isRelax) colors.secondary else colors.primary,
                                            isActive = false
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Pc98Text(text = "BLOCK #${index + 1}", color = if (block.isRelax) colors.secondary else colors.primary, fontSize = 12.sp)
                                            if (calendarBlocks.size > 1) {
                                                Pc98Text(
                                                    text = "[DELETE]",
                                                    color = colors.error,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.clickable {
                                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                                                        calendarBlocks.remove(block)
                                                    }
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Pc98Text("TYPE:", color = colors.secondary, fontSize = 10.sp)
                                            TouhouToggle(
                                                text = "FOCUS",
                                                isSelected = !block.isRelax,
                                                onClick = {
                                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                                    calendarBlocks[index] = block.copy(isRelax = false)
                                                }
                                            )
                                            TouhouToggle(
                                                text = "RELAX",
                                                isSelected = block.isRelax,
                                                onClick = {
                                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                                    calendarBlocks[index] = block.copy(isRelax = true)
                                                }
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Pc98Text("BLOCK LABEL", color = colors.secondary, fontSize = 10.sp)
                                            Pc98TextField(
                                                value = block.label,
                                                onValueChange = { calendarBlocks[index] = block.copy(label = it) },
                                                placeholder = if (block.isRelax) "RELAX THEME" else "FOCUS THEME"
                                            )
                                        }
                                        Pc98SliderBlock(
                                            label = "DURATION",
                                            value = block.durationMinutes,
                                            unit = t.minutes,
                                            range = 1f..120f
                                        ) { newDur ->
                                            calendarBlocks[index] = block.copy(durationMinutes = newDur)
                                        }
                                    }
                                }
                            }
                            TouhouButton(text = "ADD TIME BLOCK", isLarge = true) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                                calendarBlocks.add(
                                    CalendarBlockState(
                                        id = java.util.UUID.randomUUID().toString(),
                                        durationMinutes = 25,
                                        isRelax = false,
                                        label = "Focus"
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isValid = presetName.isNotBlank() && (
                    (engineStyle != "sequence" && engineStyle != "dual-sequence" && engineStyle != "calendar") || 
                    (engineStyle == "calendar" && calendarBlocks.isNotEmpty()) ||
                    (engineStyle in listOf("sequence", "dual-sequence") && sequenceInput.split(",").mapNotNull { it.trim().toIntOrNull() }.isNotEmpty())
                )
                if (isValid) {
                    TouhouButton(text = "COMPILE SPELL CARD", isLarge = true) {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                        val rawSeq = if (engineStyle == "calendar") {
                            calendarBlocks.map { it.durationMinutes * 60 }
                        } else {
                            sequenceInput.split(",").mapNotNull { it.trim().toIntOrNull() }.map { if (sequenceUnit == "minutes") it * 60 else it }
                        }
                        val newPreset = TimerPreset(
                            id = "custom_${System.currentTimeMillis()}", name = presetName.trim(), mode = engineStyle,
                            sequence = if (engineStyle in listOf("sequence", "dual-sequence", "calendar")) rawSeq else if (engineStyle == "classic") listOf(classicDurationMinutes * 60) else emptyList(),
                            dualBigDuration = if (engineStyle == "dual") dualBigMinutes * 60 else if (engineStyle == "dual.5") dual5BigMinutes * 60 else 0,
                            dualMidDuration = if (engineStyle == "dual.5") dual5MidMinutes * 60 else 0,
                            dualSmallDuration = if (engineStyle == "dual") dualSmallSeconds else if (engineStyle == "dual.5") dual5SmallSeconds else if (engineStyle == "dual-sequence") sequenceDualSmallSeconds else 0,
                            alarmBehavior = if (engineStyle == "calendar") "alarm" else completionBehavior, 
                            description = if (engineStyle == "calendar") "SYS.CALENDAR // CUSTOM TIMELINE" else "SYS.$engineStyle // CUSTOM PROTOCOL",
                            sequenceTypes = if (engineStyle == "calendar") calendarBlocks.map { if (it.isRelax) "relax" else "focus" } else emptyList(),
                            sequenceLabels = if (engineStyle == "calendar") calendarBlocks.map { it.label } else emptyList()
                        )
                        viewModel.addCustomPreset(newPreset)
                        showForm = false; presetName = ""
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            // --- SPELL CARD LIST ---
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                items(state.presets.filter { it.id != "emergency" }) { preset ->
                    SpellCardItem(
                        preset = preset, isActive = preset.id == state.activePresetId,
                        onDelete = if (preset.id.startsWith("custom_")) { { viewModel.deletePreset(preset.id) } } else null,
                        onClick = { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS); viewModel.selectPreset(preset.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// --- LOCAL PC-98 COMPONENTS ---

@Composable
fun Pc98TextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val colors = LocalPc98Colors.current
    BasicTextField(
        value = value, onValueChange = onValueChange,
        textStyle = TextStyle(color = colors.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
        cursorBrush = SolidColor(colors.primary), modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth()
                    .touhouHudFrame(
                        backgroundColor = Color(0xFF1A1A1A),
                        lineColor = Color.White,
                        isActive = false
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) Pc98Text(text = "> $placeholder _", color = colors.primary.copy(alpha = 0.4f), fontSize = 14.sp)
                else Row { Pc98Text("> ", color = colors.primary, fontSize = 16.sp); innerTextField() }
            }
        }
    )
}

@Composable
fun TouhouToggle(text: String, isSelected: Boolean, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = LocalPc98Colors.current
    Box(
        modifier = Modifier
            .offset(x = if (isPressed) 2.dp else 0.dp, y = if (isPressed) 2.dp else 0.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Pc98Text(
            text = text,
            color = if (isSelected || isPressed) Color.Black else colors.primary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun Pc98SliderBlock(label: String, value: Int, unit: String, range: ClosedFloatingPointRange<Float>, onValueChange: (Int) -> Unit) {
    val colors = LocalPc98Colors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Pc98Text(label, colors.secondary, 10.sp)
            Pc98Text("[$value ${unit.uppercase()}]", colors.primary, 12.sp)
        }
        Pc98Slider(
            value = value.toFloat(), onValueChange = { onValueChange(it.toInt()) }, valueRange = range, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SpellCardItem(preset: TimerPreset, isActive: Boolean, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val activeState = isActive || isPressed
    val scope = rememberCoroutineScope()
    val colors = LocalPc98Colors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = if (isPressed) 2.dp else 0.dp, y = if (isPressed) 2.dp else 0.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                isPressed = true
                onClick()
                scope.launch {
                    delay(100)
                    isPressed = false
                }
            }
            .touhouHudFrame(
                backgroundColor = Color(0xFF1A1A1A),
                lineColor = if (activeState) colors.primary else Color.White,
                isActive = activeState
            )
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Pc98Text(
                    text = preset.name.uppercase(),
                    fontSize = 16.sp,
                    color = if (activeState) Color.Black else colors.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pc98Text(
                        text = preset.mode.uppercase(),
                        fontSize = 10.sp,
                        color = if (activeState) colors.primary else Color.Black,
                        modifier = Modifier
                            .background(if (activeState) Color.Black else colors.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Pc98Text(
                        text = "SYS_ID: ${preset.id.takeLast(6)}",
                        fontSize = 10.sp,
                        color = if (activeState) Color.Black.copy(alpha = 0.8f) else colors.secondary
                    )
                }
            }
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .clickable { onDelete() }
                        .touhouHudFrame(
                            backgroundColor = Color(0xFF1A1A1A),
                            lineColor = colors.error,
                            isActive = false
                        )
                        .padding(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = colors.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}