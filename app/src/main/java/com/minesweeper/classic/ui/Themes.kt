package com.minesweeper.classic.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** All the colours the game uses. One of these is active at a time. */
data class GameTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val panel: Color,
    val light: Color,
    val shadow: Color,
    val hiddenCell: Color,
    val revealedCell: Color,
    val gridLine: Color,
    val text: Color,
    val ledBackground: Color,
    val ledText: Color,
    val flag: Color,
    val mine: Color,
    val explosion: Color,
    /** Colours for the numbers 1 to 8. */
    val numbers: List<Color>,
    val accent: Color,
)

val ClassicGray = GameTheme(
    id = "classic",
    name = "Classic Gray",
    isDark = false,
    background = Color(0xFFC0C0C0),
    panel = Color(0xFFC0C0C0),
    light = Color(0xFFFFFFFF),
    shadow = Color(0xFF808080),
    hiddenCell = Color(0xFFC0C0C0),
    revealedCell = Color(0xFFC0C0C0),
    gridLine = Color(0xFF808080),
    text = Color(0xFF000000),
    ledBackground = Color(0xFF000000),
    ledText = Color(0xFFFF0000),
    flag = Color(0xFFFF0000),
    mine = Color(0xFF000000),
    explosion = Color(0xFFFF0000),
    numbers = listOf(
        Color(0xFF0000FF), Color(0xFF008000), Color(0xFFFF0000), Color(0xFF000080),
        Color(0xFF800000), Color(0xFF008080), Color(0xFF000000), Color(0xFF808080),
    ),
    accent = Color(0xFF000080),
)

val DarkTheme = GameTheme(
    id = "dark",
    name = "Dark",
    isDark = true,
    background = Color(0xFF1E1E22),
    panel = Color(0xFF2B2B31),
    light = Color(0xFF50505A),
    shadow = Color(0xFF0E0E10),
    hiddenCell = Color(0xFF3A3A42),
    revealedCell = Color(0xFF18181B),
    gridLine = Color(0xFF2A2A30),
    text = Color(0xFFE6E6E6),
    ledBackground = Color(0xFF000000),
    ledText = Color(0xFFFF4D4D),
    flag = Color(0xFFFF5252),
    mine = Color(0xFFE6E6E6),
    explosion = Color(0xFFB71C1C),
    numbers = listOf(
        Color(0xFF5AA9FF), Color(0xFF4CD08A), Color(0xFFFF6B6B), Color(0xFFB58CFF),
        Color(0xFFFF9F43), Color(0xFF3DD6D0), Color(0xFFE6E6E6), Color(0xFF9AA0A6),
    ),
    accent = Color(0xFF5AA9FF),
)

val OceanTheme = GameTheme(
    id = "ocean",
    name = "Ocean",
    isDark = false,
    background = Color(0xFFD6ECF7),
    panel = Color(0xFFB8DCEF),
    light = Color(0xFFFFFFFF),
    shadow = Color(0xFF6FA3BF),
    hiddenCell = Color(0xFF8EC9E8),
    revealedCell = Color(0xFFEAF6FC),
    gridLine = Color(0xFFB5D6E6),
    text = Color(0xFF0B3C5D),
    ledBackground = Color(0xFF06283D),
    ledText = Color(0xFF5CE1E6),
    flag = Color(0xFFFF7043),
    mine = Color(0xFF0B3C5D),
    explosion = Color(0xFFEF5350),
    numbers = listOf(
        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFC62828), Color(0xFF4527A0),
        Color(0xFFAD1457), Color(0xFF00838F), Color(0xFF37474F), Color(0xFF607D8B),
    ),
    accent = Color(0xFF1976D2),
)

val SunsetTheme = GameTheme(
    id = "sunset",
    name = "Sunset",
    isDark = false,
    background = Color(0xFFFFE9D6),
    panel = Color(0xFFFFD3B0),
    light = Color(0xFFFFF7EE),
    shadow = Color(0xFFD08A5B),
    hiddenCell = Color(0xFFF4B183),
    revealedCell = Color(0xFFFFF4E8),
    gridLine = Color(0xFFE8C4A4),
    text = Color(0xFF5A2A0C),
    ledBackground = Color(0xFF2B1408),
    ledText = Color(0xFFFFB74D),
    flag = Color(0xFFD32F2F),
    mine = Color(0xFF3E2723),
    explosion = Color(0xFFE53935),
    numbers = listOf(
        Color(0xFF1E5FBF), Color(0xFF2E7D32), Color(0xFFD32F2F), Color(0xFF6A1B9A),
        Color(0xFFBF360C), Color(0xFF00796B), Color(0xFF4E342E), Color(0xFF757575),
    ),
    accent = Color(0xFFE65100),
)

val AllThemes: List<GameTheme> = listOf(ClassicGray, DarkTheme, OceanTheme, SunsetTheme)

fun themeById(id: String): GameTheme = AllThemes.firstOrNull { it.id == id } ?: ClassicGray

val LocalTheme = staticCompositionLocalOf { ClassicGray }

/** Material colours (used by dialogs, switches and text fields) matching a game theme. */
fun GameTheme.materialScheme(): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            background = background,
            onBackground = text,
            surface = panel,
            onSurface = text,
            surfaceVariant = hiddenCell,
            onSurfaceVariant = text,
            outline = shadow,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = background,
            onBackground = text,
            surface = panel,
            onSurface = text,
            surfaceVariant = hiddenCell,
            onSurfaceVariant = text,
            outline = shadow,
        )
    }
