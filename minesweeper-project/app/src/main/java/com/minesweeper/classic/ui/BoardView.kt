package com.minesweeper.classic.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.minesweeper.classic.engine.Game
import com.minesweeper.classic.engine.Status
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Sizing and panning maths for the board.
 *
 * At zoom 1 the whole board fits the view. Zooming in makes cells bigger, and the board
 * can then be dragged around, but never off the edge of the view.
 */
private class Viewport(
    val viewW: Float,
    val viewH: Float,
    val rows: Int,
    val cols: Int,
    maxBaseCellPx: Float,
    maxCellPx: Float,
) {
    /** Cell size, in pixels, when the board is fitted to the view. */
    val baseCell: Float = min(viewW / cols, viewH / rows).coerceAtMost(maxBaseCellPx)

    val maxScale: Float = (maxCellPx / baseCell).coerceAtLeast(1f)

    /** Where the board's top-left corner sits, with panning limited to the board's edges. */
    fun origin(scale: Float, offset: Offset): Offset {
        val boardW = cols * baseCell * scale
        val boardH = rows * baseCell * scale
        val x = if (boardW <= viewW) (viewW - boardW) / 2f else offset.x.coerceIn(viewW - boardW, 0f)
        val y = if (boardH <= viewH) (viewH - boardH) / 2f else offset.y.coerceIn(viewH - boardH, 0f)
        return Offset(x, y)
    }
}

/**
 * The playing field.
 *
 * - Short press: reveal (or chord on a number)
 * - Long press: flag / unflag
 * - Pinch to zoom, drag to pan
 */
@Composable
fun BoardView(
    game: Game,
    gameId: Int,
    onReveal: (Int) -> Unit,
    onFlag: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    val density = LocalDensity.current

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(gameId) { mutableFloatStateOf(1f) }
    var offset by remember(gameId) { mutableStateOf(Offset.Zero) }

    val revealCallback by rememberUpdatedState(onReveal)
    val flagCallback by rememberUpdatedState(onFlag)

    val rows = game.rows
    val cols = game.cols

    val maxBaseCellPx = with(density) { 56.dp.toPx() }
    val maxCellPx = with(density) { 80.dp.toPx() }

    val viewport: Viewport? =
        if (viewSize.width > 0 && viewSize.height > 0) {
            Viewport(
                viewW = viewSize.width.toFloat(),
                viewH = viewSize.height.toFloat(),
                rows = rows,
                cols = cols,
                maxBaseCellPx = maxBaseCellPx,
                maxCellPx = maxCellPx,
            )
        } else {
            null
        }

    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val path = remember { Path() }

    /** Which cell is under a touch position, or null if it is outside the board. */
    fun cellAt(position: Offset): Int? {
        val v = viewport ?: return null
        val cellSize = v.baseCell * scale
        val origin = v.origin(scale, offset)
        val c = floor((position.x - origin.x) / cellSize).toInt()
        val r = floor((position.y - origin.y) / cellSize).toInt()
        if (r < 0 || r >= rows || c < 0 || c >= cols) return null
        return r * cols + c
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(gameId, viewSize) {
                detectTapGestures(
                    onTap = { position -> cellAt(position)?.let { revealCallback(it) } },
                    onLongPress = { position -> cellAt(position)?.let { flagCallback(it) } },
                )
            }
            .pointerInput(gameId, viewSize) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val v = viewport ?: return@detectTransformGestures
                    val newScale = (scale * zoom).coerceIn(1f, v.maxScale)
                    val current = v.origin(scale, offset)
                    val ratio = newScale / scale
                    // Keep the point between the fingers fixed on screen while zooming.
                    val moved = Offset(
                        x = centroid.x - (centroid.x - current.x) * ratio + pan.x,
                        y = centroid.y - (centroid.y - current.y) * ratio + pan.y,
                    )
                    scale = newScale
                    offset = v.origin(newScale, moved)
                }
            },
    ) {
        drawRect(theme.panel)
        val v = viewport ?: return@Canvas

        val cellSize = v.baseCell * scale
        val origin = v.origin(scale, offset)

        val firstCol = max(0, floor(-origin.x / cellSize).toInt())
        val lastCol = min(cols - 1, floor((v.viewW - origin.x) / cellSize).toInt())
        val firstRow = max(0, floor(-origin.y / cellSize).toInt())
        val lastRow = min(rows - 1, floor((v.viewH - origin.y) / cellSize).toInt())

        for (r in firstRow..lastRow) {
            // Rounding to whole pixels keeps neighbouring cells exactly edge to edge.
            val top = round(origin.y + r * cellSize)
            val bottom = round(origin.y + (r + 1) * cellSize)
            for (c in firstCol..lastCol) {
                val left = round(origin.x + c * cellSize)
                val right = round(origin.x + (c + 1) * cellSize)
                drawCell(game, r * cols + c, left, top, right, bottom, theme, paint, path)
            }
        }
    }
}

private fun DrawScope.drawCell(
    game: Game,
    i: Int,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    theme: GameTheme,
    paint: Paint,
    path: Path,
) {
    val w = right - left
    val h = bottom - top
    val lost = game.status == Status.LOST
    val isMine = game.mine[i]
    val isFlagged = game.flagged[i]
    val isRevealed = game.revealed[i]
    val showMine = lost && isMine && !isFlagged
    val wrongFlag = lost && isFlagged && !isMine

    if (isRevealed || showMine || wrongFlag) {
        val background = if (i == game.exploded) theme.explosion else theme.revealedCell
        drawRect(background, Offset(left, top), Size(w, h))
        val line = max(1f, w * 0.04f)
        drawRect(theme.gridLine, Offset(left, top), Size(w, line))
        drawRect(theme.gridLine, Offset(left, top), Size(line, h))

        if (isRevealed) {
            val n = game.adjacent[i]
            if (n in 1..8) {
                paint.color = theme.numbers[n - 1].toArgb()
                paint.textSize = h * 0.65f
                val baseline = top + h / 2f - (paint.descent() + paint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(n.toString(), left + w / 2f, baseline, paint)
            }
        } else {
            drawMine(left, top, w, h, theme.mine)
            if (wrongFlag) drawCross(left, top, w, h)
        }
    } else {
        drawRect(theme.hiddenCell, Offset(left, top), Size(w, h))
        val b = max(1.5f, w * 0.12f)

        path.reset()
        path.moveTo(left, top)
        path.lineTo(right, top)
        path.lineTo(right - b, top + b)
        path.lineTo(left + b, top + b)
        path.lineTo(left + b, bottom - b)
        path.lineTo(left, bottom)
        path.close()
        drawPath(path, theme.light)

        path.reset()
        path.moveTo(right, bottom)
        path.lineTo(right, top)
        path.lineTo(right - b, top + b)
        path.lineTo(right - b, bottom - b)
        path.lineTo(left + b, bottom - b)
        path.lineTo(left, bottom)
        path.close()
        drawPath(path, theme.shadow)

        if (isFlagged) drawFlag(left, top, w, h, theme, path)
    }
}

private fun DrawScope.drawFlag(
    left: Float,
    top: Float,
    w: Float,
    h: Float,
    theme: GameTheme,
    path: Path,
) {
    val stroke = max(1f, w * 0.07f)
    val poleX = left + w * 0.52f
    drawLine(theme.mine, Offset(poleX, top + h * 0.2f), Offset(poleX, top + h * 0.78f), strokeWidth = stroke)

    path.reset()
    path.moveTo(poleX, top + h * 0.2f)
    path.lineTo(poleX, top + h * 0.52f)
    path.lineTo(left + w * 0.22f, top + h * 0.36f)
    path.close()
    drawPath(path, theme.flag)

    drawRect(theme.mine, Offset(left + w * 0.32f, top + h * 0.74f), Size(w * 0.4f, h * 0.07f))
    drawRect(theme.mine, Offset(left + w * 0.24f, top + h * 0.8f), Size(w * 0.56f, h * 0.08f))
}

private fun DrawScope.drawMine(left: Float, top: Float, w: Float, h: Float, color: Color) {
    val cx = left + w / 2f
    val cy = top + h / 2f
    val radius = w * 0.22f
    val spike = w * 0.34f
    val diagonal = spike * 0.72f
    val stroke = max(1f, w * 0.07f)

    drawLine(color, Offset(cx - spike, cy), Offset(cx + spike, cy), strokeWidth = stroke)
    drawLine(color, Offset(cx, cy - spike), Offset(cx, cy + spike), strokeWidth = stroke)
    drawLine(color, Offset(cx - diagonal, cy - diagonal), Offset(cx + diagonal, cy + diagonal), strokeWidth = stroke)
    drawLine(color, Offset(cx - diagonal, cy + diagonal), Offset(cx + diagonal, cy - diagonal), strokeWidth = stroke)
    drawCircle(color, radius = radius, center = Offset(cx, cy))
    drawCircle(Color.White, radius = radius * 0.28f, center = Offset(cx - radius * 0.35f, cy - radius * 0.35f))
}

/** The red cross drawn over a mine when a wrongly placed flag is revealed. */
private fun DrawScope.drawCross(left: Float, top: Float, w: Float, h: Float) {
    val red = Color(0xFFD50000)
    val stroke = max(1.5f, w * 0.09f)
    drawLine(red, Offset(left + w * 0.2f, top + h * 0.2f), Offset(left + w * 0.8f, top + h * 0.8f), strokeWidth = stroke)
    drawLine(red, Offset(left + w * 0.8f, top + h * 0.2f), Offset(left + w * 0.2f, top + h * 0.8f), strokeWidth = stroke)
}
