package com.minesweeper.classic.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.minesweeper.classic.GameViewModel

private enum class Screen { GAME, STATS, SETTINGS }

@Composable
fun MinesweeperApp(vm: GameViewModel) {
    val theme = themeById(vm.settings.themeId)
    var screen by remember { mutableStateOf(Screen.GAME) }

    // Make the status bar and navigation bar icons readable on the current theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !theme.isDark
            controller.isAppearanceLightNavigationBars = !theme.isDark
        }
    }

    BackHandler(enabled = screen != Screen.GAME) { screen = Screen.GAME }

    CompositionLocalProvider(
        LocalTheme provides theme,
        LocalContentColor provides theme.text,
    ) {
        MaterialTheme(colorScheme = theme.materialScheme()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.background)
                    .systemBarsPadding(),
            ) {
                when (screen) {
                    Screen.GAME -> GameScreen(
                        vm = vm,
                        onOpenStats = { screen = Screen.STATS },
                        onOpenSettings = { screen = Screen.SETTINGS },
                    )
                    Screen.STATS -> StatsScreen(vm = vm, onBack = { screen = Screen.GAME })
                    Screen.SETTINGS -> SettingsScreen(vm = vm, onBack = { screen = Screen.GAME })
                }
            }
        }
    }
}
