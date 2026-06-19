package com.example.timeboxvibe.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timeboxvibe.R
import com.example.timeboxvibe.theme.LocalPc98Colors
import kotlin.math.cos
import kotlin.math.sin

fun formatTime(secs: Int): String {
    if (secs < 0) return "00:00"
    val m = secs / 60
    val s = secs % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val t = getStrings(state.language)
    val view = LocalView.current
    val colors = LocalPc98Colors.current

    val activePresetName = state.presets.firstOrNull { it.id == state.activePresetId }?.name ?: ""

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {

        // Subtle scanline overlay effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanlineColor = Color.Black.copy(alpha = 0.05f)
            for (y in 0..size.height.toInt() step 4) {
                drawLine(scanlineColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- HEADER ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val logoTransition = rememberInfiniteTransition(label = "logoPulse")
                        val logoAlpha by logoTransition.animateFloat(
                            initialValue = 0.4f, targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(animation = snap(delayMillis = 800), repeatMode = RepeatMode.Reverse), label = "logoAlpha"
                        )

                        ProceduralPixelIcon(
                            iconName = "ribbon",
                            modifier = Modifier.size(24.dp),
                            alpha = logoAlpha
                        )

                        Pc98Text(text = t.title.uppercase(), color = colors.primary, fontSize = 20.sp, letterSpacing = 2.sp)
                    }
                    Pc98Text(text = activePresetName.uppercase(), color = colors.secondary, fontSize = 12.sp, letterSpacing = 1.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TouhouToggle(
                        text = if (state.isBreak) t.breakMode.uppercase() else t.focusMode.uppercase(),
                        isActive = state.isBreak,
                        color = colors.primary,
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                            viewModel.toggleBreak()
                        }
                    )
                }
            }

            // --- TASK INPUT ---
            BasicTextField(
                value = state.currentTask,
                onValueChange = { viewModel.updateTask(it) },
                textStyle = TextStyle(
                    color = colors.primary,
                    fontSize = 16.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                    fontFamily = ArcadeFont, letterSpacing = 1.sp,
                    shadow = Shadow(color = colors.scrim, offset = Offset(2f, 2f), blurRadius = 0f)
                ),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .touhouHudFrame(
                                backgroundColor = Color(0xFF1A1A1A),
                                lineColor = Color.White,
                                isActive = false
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.currentTask.isEmpty()) {
                            Pc98Text(text = "> " + t.focusThemePlaceholder.uppercase() + " _", color = colors.primary.copy(alpha = 0.5f), fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                }
            )

            // --- CIRCULAR TIMER (MAHOUJIN) ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp).padding(16.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "yinYangRotate")
                val rotationAngle by if (state.isRunning && !state.isRinging) {
                    infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(12000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "rotate"
                    )
                } else { remember { mutableStateOf(0f) } }

                val outerProgress = if (state.totalDuration > 0) state.timeRemaining.toFloat() / state.totalDuration.toFloat() else 0f
                val innerProgress = if (state.isDual) {
                    if (state.activeMode == "dual.5") {
                        if (state.midTotalDuration > 0) state.midTimeRemaining.toFloat() / state.midTotalDuration.toFloat() else 0f
                    } else {
                        if (state.bigTotalDuration > 0) state.bigTimeRemaining.toFloat() / state.bigTotalDuration.toFloat() else 0f
                    }
                } else 0f

                val primaryColor = colors.primary
                val secondaryColor = colors.secondary
                val trackColor = colors.surfaceVariant
                val onBackground = colors.primary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val thinLine = 1.dp.toPx()
                    val medLine = 2.dp.toPx()

                    val outerRadius = (size.minDimension - 16.dp.toPx()) / 2
                    val innerRadius = outerRadius - 28.dp.toPx()

                    val outerSize = Size(outerRadius * 2, outerRadius * 2)
                    val outerOffset = Offset((size.width - outerRadius * 2) / 2, (size.height - outerRadius * 2) / 2)

                    val innerSize = Size(innerRadius * 2, innerRadius * 2)
                    val innerOffset = Offset((size.width - innerRadius * 2) / 2, (size.height - innerRadius * 2) / 2)

                    val magicCircleColor = primaryColor.copy(alpha = 0.4f)

                    // COMPLEX MAHOUJIN
                    rotate(degrees = -rotationAngle, pivot = center) {
                        drawCircle(magicCircleColor, radius = outerRadius - 8.dp.toPx(), center = center, style = Stroke(thinLine))
                        drawCircle(magicCircleColor, radius = outerRadius - 12.dp.toPx(), center = center, style = Stroke(thinLine))

                        val starRadius = outerRadius - 12.dp.toPx()
                        val points = 8
                        val vertices = List(points) { idx ->
                            val angle = Math.toRadians(idx * (360.0 / points))
                            Offset(center.x + starRadius * cos(angle).toFloat(), center.y + starRadius * sin(angle).toFloat())
                        }

                        for (idx in 0 until points) {
                            drawLine(magicCircleColor, vertices[idx], vertices[(idx + 1) % points], thinLine)
                            drawLine(magicCircleColor, vertices[idx], vertices[(idx + 3) % points], thinLine)
                        }

                        val tickRadiusOuter = starRadius * 0.55f
                        val tickRadiusInner = starRadius * 0.45f
                        for (i in 0 until 24) {
                            val angle = Math.toRadians(i * (360.0 / 24))
                            val p1 = Offset(center.x + tickRadiusOuter * cos(angle).toFloat(), center.y + tickRadiusOuter * sin(angle).toFloat())
                            val p2 = Offset(center.x + tickRadiusInner * cos(angle).toFloat(), center.y + tickRadiusInner * sin(angle).toFloat())
                            drawLine(magicCircleColor, p1, p2, thinLine)
                        }
                        drawCircle(magicCircleColor, radius = tickRadiusInner, center = center, style = Stroke(medLine))
                    }

                    // COUNTER-ROTATING INNER GEAR
                    rotate(degrees = rotationAngle * 1.5f, pivot = center) {
                        val gearRadius = innerRadius - 10.dp.toPx()
                        drawCircle(magicCircleColor.copy(alpha = 0.2f), radius = gearRadius, center = center, style = Stroke(thinLine))
                        val points = 4
                        val vertices = List(points) { idx ->
                            val angle = Math.toRadians(idx * (360.0 / points))
                            Offset(center.x + gearRadius * cos(angle).toFloat(), center.y + gearRadius * sin(angle).toFloat())
                        }
                        for (idx in 0 until points) {
                            drawLine(magicCircleColor.copy(alpha = 0.2f), vertices[idx], vertices[(idx + 1) % points], thinLine)
                        }
                    }

                    // OUTER TIMER TRACK
                    val trackSegments = 90
                    val outerActive = (outerProgress * trackSegments).toInt()
                    for (i in 0 until trackSegments) {
                        val isActive = i < outerActive
                        val startAngle = -90f + i * (360f / trackSegments) + 0.5f
                        val sweepAngle = (360f / trackSegments) - 1.5f
                        drawArc(
                            color = if (isActive) primaryColor else trackColor.copy(alpha = 0.3f),
                            startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false,
                            topLeft = outerOffset, size = outerSize, style = Stroke(width = if (isActive) 4.dp.toPx() else 2.dp.toPx())
                        )
                    }

                    // DANMAKU BULLET
                    if (outerProgress > 0f) {
                        val angleDegrees = -90f + 360f * outerProgress
                        val angleRadians = Math.toRadians(angleDegrees.toDouble())
                        val cx = center.x + outerRadius * cos(angleRadians).toFloat()
                        val cy = center.y + outerRadius * sin(angleRadians).toFloat()

                        drawCircle(Color.Black, radius = 6.dp.toPx(), center = Offset(cx, cy))
                        drawCircle(primaryColor, radius = 5.dp.toPx(), center = Offset(cx, cy))
                        drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(cx, cy))

                        val sparkAngle = Math.toRadians(angleDegrees.toDouble() - 5.0)
                        val sx = center.x + outerRadius * cos(sparkAngle).toFloat()
                        val sy = center.y + outerRadius * sin(sparkAngle).toFloat()
                        drawCircle(primaryColor.copy(alpha = 0.5f), radius = 2.dp.toPx(), center = Offset(sx, sy))
                    }

                    // INNER TIMER TRACK
                    if (state.isDual) {
                        val innerSegments = 60
                        val innerActive = (innerProgress * innerSegments).toInt()
                        for (i in 0 until innerSegments) {
                            val isActive = i < innerActive
                            val startAngle = -90f + i * (360f / innerSegments) + 1f
                            val sweepAngle = (360f / innerSegments) - 2f
                            drawArc(
                                color = if (isActive) onBackground else trackColor.copy(alpha = 0.2f),
                                startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false,
                                topLeft = innerOffset, size = innerSize, style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // --- CENTER TEXT READOUT ---
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Pc98Text(
                        text = formatTime(state.timeRemaining),
                        color = colors.primary,
                        fontSize = 48.sp,
                        letterSpacing = 2.sp
                    )

                    if ((state.activeMode == "sequence" || state.activeMode == "dual-sequence" || state.activeMode == "calendar") && state.sequenceLength > 1) {
                        Pc98Text(
                            text = "STEP ${state.currentIndex + 1}/${state.sequenceLength}",
                            color = colors.secondary,
                            fontSize = 12.sp
                        )
                    }

                    if (state.isDual) {
                        Spacer(modifier = Modifier.height(6.dp))

                        if (state.activeMode == "dual.5") {
                            Pc98Text(
                                text = "[ ALARM: ${formatTime(state.midTimeRemaining)} ]",
                                color = colors.primary,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Pc98Text(
                            text = formatTime(state.bigTimeRemaining),
                            color = colors.primary,
                            fontSize = 18.sp
                        )
                        Pc98Text(
                            text = if (state.activeMode == "dual-sequence") "BLOCK LIMIT" else "SESSION LIMIT",
                            color = colors.secondary,
                            fontSize = 10.sp
                        )
                    } else if (state.activeMode != "sequence") {
                        Pc98Text(
                            text = if (state.isBreak) "UNWINDING" else "FOCUSING",
                            color = colors.secondary,
                            fontSize = 14.sp,
                            letterSpacing = 4.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // --- CONTROLS ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TouhouIconButton(
                    iconName = "reset_yinyang",
                    color = colors.secondary,
                    onClick = { view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); viewModel.resetTimer() }
                )

                Spacer(modifier = Modifier.width(32.dp))

                TouhouIconButton(
                    iconName = if (state.isRunning) "pause_ofuda" else "play_danmaku",
                    color = colors.primary,
                    isLarge = true,
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                        if (state.isRunning) viewModel.stopTimer() else viewModel.startTimer()
                    }
                )

                Spacer(modifier = Modifier.width(32.dp))

                if (state.activeMode == "sequence" || state.activeMode == "dual-sequence" || state.activeMode == "calendar") {
                    TouhouIconButton(
                        iconName = "skip_double_danmaku",
                        color = colors.secondary,
                        onClick = { view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); viewModel.skipTimer() }
                    )
                } else {
                    Box(modifier = Modifier.size(48.dp))
                }
            }

            // --- TAG CHIPS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TouhouToggle(
                    text = "${t.ticks}: " + if (state.tickEnabled) "ON" else "OFF",
                    isActive = state.tickEnabled,
                    color = colors.primary,
                    onClick = { viewModel.updateSettings(state.strictMode, !state.tickEnabled, state.selectedSound, state.vibeIntensity) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                TouhouToggle(
                    text = "${t.vibe}: " + if (state.vibeIntensity > 0f) "ON" else "OFF",
                    isActive = state.vibeIntensity > 0f,
                    color = colors.primary,
                    onClick = { viewModel.updateSettings(state.strictMode, state.tickEnabled, state.selectedSound, if (state.vibeIntensity > 0f) 0f else 0.8f) }
                )
            }

            // --- PEEKING TIMELINE CALENDAR PANEL ---
            val preset = state.presets.firstOrNull { it.id == state.activePresetId }
            if (preset != null && preset.mode == "calendar") {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .touhouHudFrame(
                            backgroundColor = Color(0xFF1A1A1A),
                            lineColor = Color.White,
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
                            Pc98Text(text = "TIMELINE / 行程", color = colors.primary, fontSize = 14.sp)
                            Pc98Text(text = "${state.currentIndex + 1} / ${preset.sequence.size} BLOCKS", color = colors.secondary, fontSize = 11.sp)
                        }

                        Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(colors.primary.copy(alpha = 0.3f)))

                        preset.sequence.forEachIndexed { idx, duration ->
                            val blockType = preset.sequenceTypes.getOrNull(idx) ?: "focus"
                            val blockLabel = preset.sequenceLabels.getOrNull(idx) ?: (if (blockType == "relax") "Relax" else "Focus")
                            val isActive = idx == state.currentIndex
                            val isCompleted = idx < state.currentIndex

                            val cardBorderColor = when {
                                isActive -> colors.primary
                                blockType == "relax" -> colors.secondary.copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.5f)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .touhouHudFrame(
                                        backgroundColor = if (isActive) Color(0xFF2A2A35) else Color(0xFF1E1E24),
                                        lineColor = cardBorderColor,
                                        isActive = isActive
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Pc98Text(
                                                text = "%02d.".format(idx + 1),
                                                color = if (isCompleted) colors.secondary.copy(alpha = 0.5f) else colors.primary,
                                                fontSize = 12.sp
                                            )
                                            Pc98Text(
                                                text = blockLabel.uppercase(),
                                                color = when {
                                                    isCompleted -> colors.secondary.copy(alpha = 0.5f)
                                                    isActive -> Color.White
                                                    else -> colors.primary
                                                },
                                                fontSize = 12.sp
                                            )
                                            if (isCompleted) {
                                                Pc98Text(
                                                    text = "✓",
                                                    color = colors.secondary.copy(alpha = 0.5f),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val badgeBg = if (blockType == "relax") colors.secondary else colors.primary
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isCompleted) colors.secondary.copy(alpha = 0.3f) else badgeBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Pc98Text(
                                                    text = blockType.uppercase(),
                                                    color = Color.Black,
                                                    fontSize = 9.sp
                                                )
                                            }
                                            Pc98Text(
                                                text = "DURATION: ${duration / 60}m",
                                                color = colors.secondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    if (isActive) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Pc98Text(
                                                text = formatTime(state.timeRemaining),
                                                color = colors.primary,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val progressPercent = if (state.totalDuration > 0) state.timeRemaining.toFloat() / state.totalDuration.toFloat() else 0f
                                            val barWidth = 60.dp
                                            Box(
                                                modifier = Modifier
                                                    .size(barWidth, 6.dp)
                                                    .background(Color.Black)
                                                    .touhouHudFrame(backgroundColor = Color.Black, lineColor = colors.primary, isActive = false)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(1f - progressPercent)
                                                        .background(colors.primary)
                                                )
                                            }
                                        }
                                    } else if (isCompleted) {
                                        Pc98Text(
                                            text = "00:00",
                                            color = colors.secondary.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Pc98Text(
                                            text = formatTime(duration),
                                            color = colors.secondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- ALARM OVERLAY ---
        AnimatedVisibility(visible = state.isRinging, enter = fadeIn(), exit = fadeOut()) {
            val warningTransition = rememberInfiniteTransition(label = "warningFlash")
            val flashColor by warningTransition.animateColor(
                initialValue = colors.surfaceVariant, targetValue = colors.primary.copy(alpha = 0.4f),
                animationSpec = infiniteRepeatable(animation = snap(delayMillis = 150), repeatMode = RepeatMode.Reverse), label = "flashColor"
            )

            val rotateTransition = rememberInfiniteTransition(label = "orbRotate")
            val orbRotation by rotateTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "orbRotation"
            )

            Box(
                modifier = Modifier.fillMaxSize().background(flashColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.dismissAlarm() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)
                ) {
                    WarningMarquee()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        val pulseScale by warningTransition.animateFloat(initialValue = 1f, targetValue = 1.1f, animationSpec = infiniteRepeatable(snap(150), RepeatMode.Reverse), label = "pulse")

                        Pc98Text(
                            text = "SPELL CARD ACTIVE", color = colors.error, fontSize = 28.sp, letterSpacing = 2.sp,
                            modifier = Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                        )

                        ProceduralPixelIcon(
                            iconName = "yinyang",
                            modifier = Modifier.size(160.dp).graphicsLayer { rotationZ = orbRotation }
                        )

                        Pc98Text(text = "ADHD BLOCKADE INITIATED\nCONCENTRATION FIELD ACTIVE", color = colors.primary, fontSize = 12.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.dismissAlarm() }
                                .touhouHudFrame(
                                    backgroundColor = colors.surfaceVariant,
                                    lineColor = colors.error,
                                    isActive = true,
                                    ditherPattern = DitherPattern.DITHER_50,
                                    ditherColor = colors.error
                                )
                                .padding(horizontal = 32.dp, vertical = 16.dp)
                        ) {
                            Pc98Text(text = "BOMB / 霊撃", color = colors.background, fontSize = 20.sp, letterSpacing = 2.sp)
                        }
                        WarningMarquee()
                    }
                }
            }
        }
    }
}

// --- LOCAL SCREEN COMPONENTS (Placed correctly outside the main function!) ---

@Composable
private fun TouhouIconButton(
    iconName: String,
    color: Color,
    isLarge: Boolean = false,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val size = if (isLarge) 68.dp else 48.dp
    val iconSize = if (isLarge) 36.dp else 28.dp
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(size)
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
                lineColor = if (isPressed) color else Color.White,
                isActive = isPressed
            )
            .padding(if (isLarge) 16.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        ProceduralPixelIcon(
            iconName = iconName,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun TouhouToggle(text: String, isActive: Boolean, color: Color, onClick: () -> Unit) {
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
                lineColor = if (isActive || isPressed) color else Color.White,
                isActive = isActive || isPressed
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Pc98Text(
            text = text,
            color = if (isActive || isPressed) Color.Black else color,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun WarningMarquee() {
    val text = "★ WARNING ★ SPELL CARD ACTIVE ★ 警報 ★ 呪符発動 ★ "
    val repeatedText = remember { text.repeat(10) }
    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -500f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "xOffset"
    )
    val colors = LocalPc98Colors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .touhouHudFrame(
                backgroundColor = colors.surfaceVariant,
                lineColor = colors.error,
                isActive = true,
                ditherPattern = DitherPattern.DITHER_25,
                ditherColor = colors.error
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Pc98Text(
            text = repeatedText,
            color = colors.background,
            fontSize = 14.sp,
            modifier = Modifier.offset(x = xOffset.dp)
        )
    }
}