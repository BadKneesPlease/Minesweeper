package com.minesweeper.classic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minesweeper.classic.engine.Status

/** Draws the classic 3D frame: raised (buttons) or sunken (panels and displays). */
fun Modifier.bevel(
    theme: GameTheme,
    raised: Boolean,
    width: Dp = 3.dp,
    fill: Color = theme.panel,
): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height
    val b = width.toPx()
    drawRect(fill)

    val topLeftColor = if (raised) theme.light else theme.shadow
    val bottomRightColor = if (raised) theme.shadow else theme.light

    val topLeft = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w - b, b)
        lineTo(b, b)
        lineTo(b, h - b)
        lineTo(0f, h)
        close()
    }
    val bottomRight = Path().apply {
        moveTo(w, h)
        lineTo(w, 0f)
        lineTo(w - b, b)
        lineTo(w - b, h - b)
        lineTo(b, h - b)
        lineTo(0f, h)
        close()
    }
    drawPath(topLeft, topLeftColor)
    drawPath(bottomRight, bottomRightColor)
}

/** A raised button that looks pressed in while a finger is on it. */
@Composable
fun RetroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val theme = LocalTheme.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .bevel(theme, raised = !pressed, width = 2.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun RetroTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    RetroButton(onClick = onClick, modifier = modifier.defaultMinSize(minHeight = 40.dp)) {
        Text(
            text = text,
            color = theme.text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** A red-on-black seven-segment style number display. */
@Composable
fun Led(text: String) {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .bevel(theme, raised = false, width = 2.dp, fill = theme.ledBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = theme.ledText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
        )
    }
}

/** The face button between the two displays. Tapping it starts a new game. */
@Composable
fun FaceButton(status: Status, onClick: () -> Unit) {
    val face = when (status) {
        Status.WON -> "😎"
        Status.LOST -> "😵"
        else -> "🙂"
    }
    RetroButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        Text(text = face, fontSize = 28.sp)
    }
}

/** Formats a counter for the LED displays: three digits, or a minus sign and two digits. */
fun ledText(value: Int): String =
    if (value < 0) {
        "-" + (-value).coerceAtMost(99).toString().padStart(2, '0')
    } else {
        value.coerceAtMost(999).toString().padStart(3, '0')
    }

/** Milliseconds as seconds with one decimal, e.g. 83400 -> "83.4 s". */
fun formatTime(ms: Long): String {
    val tenths = ms / 100
    return "${tenths / 10}.${tenths % 10} s"
}
