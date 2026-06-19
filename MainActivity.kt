package com.example.timeboxvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.timeboxvibe.theme.LocalPc98Colors
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timeboxvibe.theme.TimeBoxVibeTheme
import com.example.timeboxvibe.ui.main.AppScaffold
import com.example.timeboxvibe.ui.main.MainScreenViewModel

import android.os.Build
import android.view.WindowManager
import com.example.timeboxvibe.ui.main.dataStore
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime notification permission on startup (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Wake screen and show over keyguard when alarm triggers and activity is launched
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: MainScreenViewModel = viewModel(
                factory = MainScreenViewModel.Factory(applicationContext, applicationContext.dataStore)
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            TimeBoxVibeTheme(appTheme = state.appTheme, isBreak = state.isBreak) {
                Box(
                    modifier = Modifier.fillMaxSize().background(LocalPc98Colors.current.background)
                ) {
                    AppScaffold(viewModel = viewModel)
                }
            }
        }
    }

}
