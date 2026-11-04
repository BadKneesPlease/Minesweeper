package com.minesweeper.classic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minesweeper.classic.GameViewModel
import com.minesweeper.classic.data.DifficultyStats
import com.minesweeper.classic.engine.Difficulty

@Composable
fun StatsScreen(vm: GameViewModel, onBack: () -> Unit) {
    val theme = LocalTheme.current
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
    ) {
        ScreenHeader(title = "Statistics", onBack = onBack)
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (level in Difficulty.values()) {
                val s = vm.stats[level] ?: DifficultyStats()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bevel(theme, raised = true, width = 3.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = level.label,
                        color = theme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    if (level != Difficulty.CUSTOM) {
                        StatRow("Best time", if (s.bestTimeMs < 0) "–" else formatTime(s.bestTimeMs))
                    }
                    StatRow("Played", s.played.toString())
                    StatRow("Won / lost", "${s.won} / ${s.lost}")
                    StatRow("Win rate", if (s.played == 0) "–" else "${s.won * 100 / s.played}%")
                    StatRow("Current streak", streakText(s.streak))
                    StatRow("Best win streak", s.bestStreak.toString())
                }
            }
            Spacer(Modifier.height(4.dp))
            RetroTextButton(
                text = "Reset statistics",
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset statistics?",
            message = "This clears all best times, win/loss counts and streaks. It can't be undone.",
            confirmText = "Reset",
            onConfirm = {
                vm.resetStats()
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
}

private fun streakText(streak: Int): String = when {
    streak > 0 -> "$streak won"
    streak < 0 -> "${-streak} lost"
    else -> "–"
}

@Composable
private fun StatRow(label: String, value: String) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = theme.text, fontSize = 15.sp)
        Text(text = value, color = theme.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Back button and a title, shared by the Statistics and Settings screens. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RetroTextButton(text = "‹ Back", onClick = onBack)
        Text(text = title, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}
