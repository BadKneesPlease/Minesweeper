package com.minesweeper.classic.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.minesweeper.classic.engine.BoardConfig
import com.minesweeper.classic.engine.Difficulty
import com.minesweeper.classic.engine.Game
import com.minesweeper.classic.engine.Status
import org.json.JSONObject

data class Settings(
    val themeId: String = "classic",
    val vibration: Boolean = true,
    /** The last custom board the player chose, to prefill the custom dialog. */
    val custom: BoardConfig = BoardConfig(rows = 16, cols = 16, mines = 40),
)

data class DifficultyStats(
    val won: Int = 0,
    val lost: Int = 0,
    /** Fastest win in milliseconds, or -1 if there is none yet. */
    val bestTimeMs: Long = -1L,
    /** Positive = current winning streak, negative = current losing streak. */
    val streak: Int = 0,
    val bestStreak: Int = 0,
) {
    val played: Int get() = won + lost
}

data class SavedGame(val game: Game, val elapsedMs: Long)

class Storage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("minesweeper", Context.MODE_PRIVATE)

    // ---- settings ----

    fun loadSettings(): Settings {
        val d = Settings()
        return Settings(
            themeId = prefs.getString("theme", d.themeId) ?: d.themeId,
            vibration = prefs.getBoolean("vibration", d.vibration),
            custom = BoardConfig(
                rows = prefs.getInt("custom_rows", d.custom.rows),
                cols = prefs.getInt("custom_cols", d.custom.cols),
                mines = prefs.getInt("custom_mines", d.custom.mines),
            ),
        )
    }

    fun saveSettings(s: Settings) {
        prefs.edit {
            putString("theme", s.themeId)
            putBoolean("vibration", s.vibration)
            putInt("custom_rows", s.custom.rows)
            putInt("custom_cols", s.custom.cols)
            putInt("custom_mines", s.custom.mines)
        }
    }

    // ---- statistics ----

    fun loadStats(): Map<Difficulty, DifficultyStats> {
        val out = LinkedHashMap<Difficulty, DifficultyStats>()
        for (d in Difficulty.values()) {
            val p = "stats_${d.name}_"
            out[d] = DifficultyStats(
                won = prefs.getInt(p + "won", 0),
                lost = prefs.getInt(p + "lost", 0),
                bestTimeMs = prefs.getLong(p + "best", -1L),
                streak = prefs.getInt(p + "streak", 0),
                bestStreak = prefs.getInt(p + "best_streak", 0),
            )
        }
        return out
    }

    fun saveStats(stats: Map<Difficulty, DifficultyStats>) {
        prefs.edit {
            for ((d, s) in stats) {
                val p = "stats_${d.name}_"
                putInt(p + "won", s.won)
                putInt(p + "lost", s.lost)
                putLong(p + "best", s.bestTimeMs)
                putInt(p + "streak", s.streak)
                putInt(p + "best_streak", s.bestStreak)
            }
        }
    }

    // ---- the game in progress ----

    fun saveGame(game: Game, elapsedMs: Long) {
        val json = JSONObject()
            .put("rows", game.rows)
            .put("cols", game.cols)
            .put("mines", game.mineCount)
            .put("mine", game.mineBits())
            .put("revealed", game.revealedBits())
            .put("flagged", game.flaggedBits())
            .put("status", game.status.name)
            .put("exploded", game.exploded)
            .put("elapsed", elapsedMs)
        prefs.edit { putString("game", json.toString()) }
    }

    fun loadGame(): SavedGame? {
        val raw = prefs.getString("game", null) ?: return null
        return try {
            val o = JSONObject(raw)
            val game = Game.restore(
                rows = o.getInt("rows"),
                cols = o.getInt("cols"),
                mines = o.getInt("mines"),
                mineBits = o.getString("mine"),
                revealedBits = o.getString("revealed"),
                flaggedBits = o.getString("flagged"),
                status = Status.valueOf(o.getString("status")),
                exploded = o.getInt("exploded"),
            )
            if (game == null) null else SavedGame(game, o.optLong("elapsed", 0L))
        } catch (e: Exception) {
            null
        }
    }
}
