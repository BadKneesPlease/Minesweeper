package com.minesweeper.classic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minesweeper.classic.GameViewModel
import com.minesweeper.classic.engine.Difficulty
import com.minesweeper.classic.engine.Game

private fun levelLabel(game: Game): String {
    val level = Difficulty.of(game.rows, game.cols, game.mineCount)
    return if (level == Difficulty.CUSTOM) "Custom ${game.cols}×${game.rows}" else level.label
}

@Composable
fun GameScreen(
    vm: GameViewModel,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val theme = LocalTheme.current
    val game = vm.game
    var showLevels by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RetroTextButton(text = levelLabel(game) + " ▾", onClick = { showLevels = true })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RetroTextButton(text = "Stats", onClick = onOpenStats)
                RetroTextButton(text = "Settings", onClick = onOpenSettings)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bevel(theme, raised = false, width = 3.dp)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Led(ledText(game.minesLeft))
            FaceButton(status = game.status, onClick = { vm.newGame() })
            Led(ledText((vm.elapsedMs / 1000L).toInt()))
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .bevel(theme, raised = false, width = 3.dp)
                .padding(3.dp),
        ) {
            BoardView(
                game = game,
                gameId = vm.gameId,
                onReveal = { vm.onReveal(it) },
                onFlag = { vm.onFlag(it) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showLevels) {
        LevelDialog(
            onPick = { config ->
                showLevels = false
                vm.newGame(config)
            },
            onCustom = {
                showLevels = false
                showCustom = true
            },
            onDismiss = { showLevels = false },
        )
    }

    if (showCustom) {
        CustomDialog(
            initial = vm.settings.custom,
            onStart = { config ->
                showCustom = false
                vm.startCustom(config)
            },
            onDismiss = { showCustom = false },
        )
    }

    val win = vm.winInfo
    if (win != null) {
        WinDialog(
            info = win,
            onNewGame = { vm.newGame() },
            onClose = { vm.dismissWin() },
        )
    }
}
