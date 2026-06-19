package com.example.timeboxvibe.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import com.example.timeboxvibe.theme.LocalPc98Colors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EntropyScreen(viewModel: MainScreenViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val t = getStrings(uiState.language)
    val view = LocalView.current
    val colors = LocalPc98Colors.current

    var tasks by remember(uiState.language) { mutableStateOf(t.defaultTasks) }
    var newTask by remember { mutableStateOf("") }
    var isSpinning by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<String?>(null) }
    var animationIndex by remember { mutableStateOf(-1) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // --- HEADER ---
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Pc98Text(
                text = "ENTROPY BOMB / 八卦炉",
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
            Spacer(modifier = Modifier.height(8.dp))
            Pc98Text(
                text = t.entropyDesc.uppercase(),
                color = colors.secondary,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        // --- INPUT TERMINAL ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = newTask,
                onValueChange = { newTask = it },
                textStyle = TextStyle(
                    color = colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ArcadeFont, // Using your custom font!
                    letterSpacing = 1.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(2f, 2f),
                        blurRadius = 0f
                    )
                ),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .touhouHudFrame(backgroundColor = Color(0xFF1A1A1A), lineColor = Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (newTask.isEmpty()) {
                            Pc98Text(
                                text = "> " + t.addTaskPlaceholder.uppercase() + " _",
                                color = colors.primary.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        } else {
                            Row {
                                Pc98Text("> ", color = colors.primary, fontSize = 16.sp)
                                innerTextField()
                            }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Replaced the blocky button with our TouhouButton
            TouhouButton(text = "LOAD", isLarge = false) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                if (newTask.isNotBlank()) {
                    tasks = tasks + newTask.trim()
                    newTask = ""
                    selectedTask = null
                }
            }
        }

        // --- THE TARGETING LIST ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .touhouHudFrame(backgroundColor = Color(0xFF1A1A1A), lineColor = Color.White)
                .padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(tasks) { index, task ->
                    val isHighlighted = (animationIndex == index)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isHighlighted) colors.primary else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pc98Text(
                            text = "[${String.format("%02d", index)}] ${task.uppercase()}",
                            color = if (isHighlighted) Color.Black else colors.primary,
                            fontSize = 14.sp
                        )

                        if (!isSpinning) {
                            Pc98Text(
                                text = "[X]",
                                color = if (isHighlighted) Color.Black else colors.secondary,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                    tasks = tasks.filterIndexed { i, _ -> i != index }
                                    selectedTask = null
                                    animationIndex = -1
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- DETONATOR BUTTON ---
        val canSpin = tasks.isNotEmpty() && !isSpinning
        var isDetonatorPressed by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = if (isDetonatorPressed) 2.dp else 0.dp, y = if (isDetonatorPressed) 2.dp else 0.dp)
                .clickable(
                    enabled = canSpin,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isDetonatorPressed = true
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)

                    coroutineScope.launch {
                        isSpinning = true
                        selectedTask = null
                        var delayMs = 40L

                        // Spin Roulette with haptics
                        for (i in 0..25) {
                            animationIndex = (animationIndex + 1) % tasks.size
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            delay(delayMs)
                            delayMs = (delayMs * 1.15).toLong()
                        }

                        // Lock on target
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                        val finalIdx = tasks.indices.random()
                        animationIndex = finalIdx
                        selectedTask = tasks[finalIdx]
                        isSpinning = false

                        coroutineScope.launch {
                            delay(100)
                            isDetonatorPressed = false
                        }
                    }
                }
                .touhouHudFrame(
                    backgroundColor = Color(0xFF1A1A1A),
                    lineColor = if (canSpin) colors.primary else Color.White,
                    isActive = isDetonatorPressed || isSpinning // Inverts color when spinning!
                )
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Pc98Text(
                text = if (isSpinning) "CALCULATING TRAJECTORY..." else "IGNITE ENTROPY BOMB",
                color = if (isDetonatorPressed || isSpinning) {
                    Color.Black
                } else if (canSpin) {
                    colors.primary
                } else {
                    colors.secondary
                },
                fontSize = 16.sp,
                letterSpacing = 2.sp
            )
        }

        // --- EMERGENCY DIRECTIVE RESULT BOX ---
        AnimatedVisibility(
            visible = selectedTask != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(300))
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .touhouHudFrame(
                            backgroundColor = Color(0xFF1A1A1A),
                            lineColor = colors.error,
                            isActive = true // Makes the frame solid Error Red
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Pc98Text(
                            text = "DIRECTIVE OVERRIDE",
                            color = Color.Black,
                            fontSize = 12.sp,
                            letterSpacing = 4.sp
                        )

                        Pc98Text(
                            text = "> ${selectedTask?.uppercase()} <",
                            color = Color.Black,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(vertical = 24.dp),
                            textAlign = TextAlign.Center
                        )

                        // Launch Button
                        TouhouButton(text = "COMMENCE EMERGENCY SESSION", isLarge = false) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                            viewModel.updateTask(selectedTask!!)
                            viewModel.applyPreset(25 * 60, false)
                            viewModel.navigateTo("timer")
                        }
                    }
                }
            }
        }
    }
}