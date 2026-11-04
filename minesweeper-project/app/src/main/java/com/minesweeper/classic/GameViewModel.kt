package com.minesweeper.classic

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minesweeper.classic.data.DifficultyStats
import com.minesweeper.classic.data.Haptics
import com.minesweeper.classic.data.Settings
import com.minesweeper.classic.data.Storage
import com.minesweeper.classic.engine.BoardConfig
import com.minesweeper.classic.engine.Difficulty
import com.minesweeper.classic.engine.Game
import com.minesweeper.classic.engine.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What the "You win" dialog needs to show. */
data class WinInfo(val timeMs: Long, val newBest: Boolean, val difficulty: Difficulty)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = Storage(app)
    private val haptics = Haptics(app)

    var settings by mutableStateOf(storage.loadSettings())
        private set

    var stats by mutableStateOf(storage.loadStats())
        private set

    var game by mutableStateOf(Game.create(9, 9, 10))
        private set

    /** Time played on the current game, in milliseconds. */
    var elapsedMs by mutableLongStateOf(0L)
        private set

    /** Changes every time a fresh board is started, so the screen can reset zoom and pan. */
    var gameId by mutableIntStateOf(0)
        private set

    var winInfo by mutableStateOf<WinInfo?>(null)
        private set

    private var foreground = false
    private var running = false
    private var runStartedAt = 0L
    private var baseElapsed = 0L

    init {
        val saved = storage.loadGame()
        if (saved != null) {
            game = saved.game
            elapsedMs = saved.elapsedMs
        } else {
            startFresh(Difficulty.BEGINNER.config)
        }
        viewModelScope.launch {
            while (true) {
                delay(100)
                if (running) {
                    elapsedMs = baseElapsed + (SystemClock.elapsedRealtime() - runStartedAt)
                }
            }
        }
    }

    // ---- lifecycle ----

    fun onResume() {
        foreground = true
        syncTimer()
    }

    fun onPause() {
        foreground = false
        syncTimer()
        persist()
    }

    // ---- moves ----

    /** A short press on a cell: reveal it, or chord if it is a satisfied number. */
    fun onReveal(index: Int) {
        val g = game.copy()
        if (!g.reveal(index)) return
        game = g
        syncTimer()
        if (g.status == Status.WON || g.status == Status.LOST) finishGame(g)
        persist()
    }

    /** A long press on a cell: flag or unflag it. */
    fun onFlag(index: Int) {
        val g = game.copy()
        if (!g.toggleFlag(index)) return
        game = g
        if (settings.vibration) haptics.tick()
        persist()
    }

    // ---- starting games ----

    fun newGame(config: BoardConfig = BoardConfig(game.rows, game.cols, game.mineCount)) {
        startFresh(config)
        persist()
    }

    fun startCustom(config: BoardConfig) {
        val rows = config.rows.coerceIn(Game.MIN_SIDE, Game.MAX_SIDE)
        val cols = config.cols.coerceIn(Game.MIN_SIDE, Game.MAX_SIDE)
        val mines = config.mines.coerceIn(1, Game.maxMines(rows, cols))
        val safe = BoardConfig(rows, cols, mines)
        updateSettings(settings.copy(custom = safe))
        newGame(safe)
    }

    fun dismissWin() {
        winInfo = null
    }

    // ---- settings and stats ----

    fun setTheme(id: String) = updateSettings(settings.copy(themeId = id))

    fun setVibration(on: Boolean) {
        updateSettings(settings.copy(vibration = on))
        if (on) haptics.tick()
    }

    fun resetStats() {
        stats = Difficulty.values().associateWith { DifficultyStats() }
        storage.saveStats(stats)
    }

    // ---- internals ----

    private fun updateSettings(s: Settings) {
        settings = s
        storage.saveSettings(s)
    }

    private fun startFresh(config: BoardConfig) {
        game = Game.create(config.rows, config.cols, config.mines)
        running = false
        baseElapsed = 0L
        elapsedMs = 0L
        gameId++
        winInfo = null
    }

    /** Runs the clock only while a game is in progress and the app is on screen. */
    private fun syncTimer() {
        val shouldRun = foreground && game.status == Status.PLAYING
        if (shouldRun && !running) {
            baseElapsed = elapsedMs
            runStartedAt = SystemClock.elapsedRealtime()
            running = true
        } else if (!shouldRun && running) {
            elapsedMs = baseElapsed + (SystemClock.elapsedRealtime() - runStartedAt)
            running = false
        }
    }

    private fun persist() {
        storage.saveGame(game, elapsedMs)
    }

    private fun finishGame(g: Game) {
        val won = g.status == Status.WON
        val time = elapsedMs
        val level = Difficulty.of(g.rows, g.cols, g.mineCount)
        val cur = stats[level] ?: DifficultyStats()

        val streak = if (won) {
            if (cur.streak > 0) cur.streak + 1 else 1
        } else {
            if (cur.streak < 0) cur.streak - 1 else -1
        }
        val isRecord = won && level != Difficulty.CUSTOM && (cur.bestTimeMs < 0 || time < cur.bestTimeMs)

        val updated = cur.copy(
            won = cur.won + (if (won) 1 else 0),
            lost = cur.lost + (if (won) 0 else 1),
            bestTimeMs = if (isRecord) time else cur.bestTimeMs,
            streak = streak,
            bestStreak = maxOf(cur.bestStreak, if (won) streak else 0),
        )
        stats = stats + (level to updated)
        storage.saveStats(stats)

        if (won) {
            winInfo = WinInfo(time, isRecord, level)
            if (settings.vibration) haptics.win()
        } else if (settings.vibration) {
            haptics.lose()
        }
    }
}
