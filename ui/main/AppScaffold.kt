package com.example.timeboxvibe.ui.main


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import com.example.timeboxvibe.theme.LocalPc98Colors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timeboxvibe.R

@Composable
fun AppScaffold(viewModel: MainScreenViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalPc98Colors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Main Screen Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            when (state.activeTab) {
                "timer" -> TimerScreen(viewModel = viewModel)
                "templates" -> TemplatesScreen(viewModel = viewModel)
                "entropy" -> EntropyScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel)
                else -> TimerScreen(viewModel = viewModel)
            }
        }

        // Bottom Navigation Bar
        val t = getStrings(state.language)
        val view = LocalView.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // PC-98 Beveled Groove Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.surfaceVariant))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "timer" to t.timer, "templates" to t.templates,
                    "entropy" to t.entropy, "settings" to t.showSettings
                ).forEach { (tab, label) ->
                    val isActive = state.activeTab == tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            viewModel.navigateTo(tab)
                        }
                    ) {
                        val iconName = when (tab) {
                            "timer" -> "watch"
                            "templates" -> "ofuda"
                            "entropy" -> "hakkero"
                            else -> "gohei"
                        }
                        ProceduralPixelIcon(
                            iconName = iconName,
                            modifier = Modifier.size(32.dp),
                            alpha = if (isActive) 1f else 0.3f,
                            customColor = if (isActive) colors.primary else colors.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Pc98Text(
                            text = label.uppercase(),
                            color = if (isActive) colors.primary else colors.primary.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Sharp, sleek cursor indicator
            val activeIndex = when (state.activeTab) { "settings" -> 3; "entropy" -> 2; "templates" -> 1; else -> 0 }
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val weight = activeIndex.toFloat()
                if (weight > 0f) Spacer(modifier = Modifier.weight(weight))
                Box(modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally)) {
                    Box(modifier = Modifier.width(16.dp).height(2.dp).background(colors.primary))
                }
                if (3f - weight > 0f) Spacer(modifier = Modifier.weight(3f - weight))
            }
        }
    }
}