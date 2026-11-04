package com.minesweeper.classic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minesweeper.classic.GameViewModel

@Composable
fun SettingsScreen(vm: GameViewModel, onBack: () -> Unit) {
    val theme = LocalTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
    ) {
        ScreenHeader(title = "Settings", onBack = onBack)
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Theme")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bevel(theme, raised = true, width = 3.dp)
                    .padding(vertical = 6.dp),
            ) {
                for (option in AllThemes) {
                    ThemeRow(
                        option = option,
                        selected = option.id == theme.id,
                        onClick = { vm.setTheme(option.id) },
                    )
                }
            }

            SectionTitle("Feedback")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bevel(theme, raised = true, width = 3.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Vibration", color = theme.text, fontSize = 16.sp)
                Switch(
                    checked = vm.settings.vibration,
                    onCheckedChange = { vm.setVibration(it) },
                )
            }

            SectionTitle("How to play")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bevel(theme, raised = true, width = 3.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HelpLine("Tap", "reveal a square. Tap a number whose flags are all placed to reveal its neighbours.")
                HelpLine("Long press", "place or remove a flag.")
                HelpLine("Pinch / drag", "zoom in and pan around the board.")
                HelpLine("Face", "tap the face to start a new game.")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val theme = LocalTheme.current
    Text(text = text, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
private fun HelpLine(label: String, text: String) {
    val theme = LocalTheme.current
    Row {
        Text(text = label, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(96.dp))
        Text(text = text, color = theme.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ThemeRow(option: GameTheme, selected: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A little swatch showing the theme's hidden cell, revealed cell and accent colours.
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.size(22.dp).background(option.hiddenCell).bevel(option, raised = true, width = 2.dp, fill = option.hiddenCell))
            Box(Modifier.size(22.dp).background(option.revealedCell))
            Box(Modifier.size(22.dp).background(option.accent))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = option.name,
            color = theme.text,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(text = "✓", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
