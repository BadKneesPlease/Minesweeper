package com.minesweeper.classic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.minesweeper.classic.WinInfo
import com.minesweeper.classic.engine.BoardConfig
import com.minesweeper.classic.engine.Difficulty
import com.minesweeper.classic.engine.Game

@Composable
private fun DialogButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val theme = LocalTheme.current
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text = text,
            color = if (enabled) theme.accent else theme.shadow,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** A dialog in the current theme's colours. */
@Composable
fun ThemedDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val theme = LocalTheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = theme.panel,
        titleContentColor = theme.text,
        textContentColor = theme.text,
    )
}

/** Pick Beginner, Intermediate, Expert or Custom. */
@Composable
fun LevelDialog(
    onPick: (BoardConfig) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit,
) {
    ThemedDialog(
        title = "New game",
        onDismiss = onDismiss,
        confirmButton = {},
        dismissButton = { DialogButton("Cancel", onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (d in Difficulty.presets) {
                RetroTextButton(
                    text = "${d.label}  •  ${d.cols} × ${d.rows}, ${d.mines} mines",
                    onClick = { onPick(d.config) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            RetroTextButton(
                text = "Custom…",
                onClick = onCustom,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Choose your own width, height and number of mines. */
@Composable
fun CustomDialog(
    initial: BoardConfig,
    onStart: (BoardConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var widthText by remember { mutableStateOf(initial.cols.toString()) }
    var heightText by remember { mutableStateOf(initial.rows.toString()) }
    var minesText by remember { mutableStateOf(initial.mines.toString()) }

    val cols = widthText.toIntOrNull()
    val rows = heightText.toIntOrNull()
    val mineCount = minesText.toIntOrNull()

    val colsOk = cols != null && cols in Game.MIN_SIDE..Game.MAX_SIDE
    val rowsOk = rows != null && rows in Game.MIN_SIDE..Game.MAX_SIDE
    val maxMines = if (colsOk && rowsOk && cols != null && rows != null) Game.maxMines(rows, cols) else null
    val minesOk = mineCount != null && maxMines != null && mineCount in 1..maxMines
    val valid = colsOk && rowsOk && minesOk

    ThemedDialog(
        title = "Custom board",
        onDismiss = onDismiss,
        confirmButton = {
            DialogButton(
                text = "Start",
                enabled = valid,
                onClick = {
                    if (cols != null && rows != null && mineCount != null) {
                        onStart(BoardConfig(rows = rows, cols = cols, mines = mineCount))
                    }
                },
            )
        },
        dismissButton = { DialogButton("Cancel", onDismiss) },
    ) {
        Column {
            NumberField(
                label = "Width (${Game.MIN_SIDE}–${Game.MAX_SIDE})",
                value = widthText,
                error = !colsOk,
                onChange = { widthText = it },
            )
            Spacer(Modifier.height(8.dp))
            NumberField(
                label = "Height (${Game.MIN_SIDE}–${Game.MAX_SIDE})",
                value = heightText,
                error = !rowsOk,
                onChange = { heightText = it },
            )
            Spacer(Modifier.height(8.dp))
            NumberField(
                label = if (maxMines != null) "Mines (1–$maxMines)" else "Mines",
                value = minesText,
                error = !minesOk,
                onChange = { minesText = it },
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, error: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }.take(4)) },
        label = { Text(label) },
        singleLine = true,
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Shown when the board is cleared. */
@Composable
fun WinDialog(
    info: WinInfo,
    onNewGame: () -> Unit,
    onClose: () -> Unit,
) {
    ThemedDialog(
        title = "You win!",
        onDismiss = onClose,
        confirmButton = { DialogButton("New game", onNewGame) },
        dismissButton = { DialogButton("Close", onClose) },
    ) {
        Column {
            Text("Time: ${formatTime(info.timeMs)}")
            if (info.newBest) {
                Text(
                    text = "New best time for ${info.difficulty.label}!",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ThemedDialog(
        title = title,
        onDismiss = onDismiss,
        confirmButton = { DialogButton(confirmText, onConfirm) },
        dismissButton = { DialogButton("Cancel", onDismiss) },
    ) {
        Text(message)
    }
}
