package com.minesweeper.classic.engine

data class BoardConfig(val rows: Int, val cols: Int, val mines: Int)

/**
 * The classic levels. Expert is the classic 16x30 board turned on its side (30 rows by
 * 16 columns) so it suits a portrait phone screen.
 */
enum class Difficulty(val label: String, val rows: Int, val cols: Int, val mines: Int) {
    BEGINNER("Beginner", 9, 9, 10),
    INTERMEDIATE("Intermediate", 16, 16, 40),
    EXPERT("Expert", 30, 16, 99),
    CUSTOM("Custom", 0, 0, 0);

    val config: BoardConfig get() = BoardConfig(rows, cols, mines)

    companion object {
        val presets: List<Difficulty> = listOf(BEGINNER, INTERMEDIATE, EXPERT)

        fun of(rows: Int, cols: Int, mines: Int): Difficulty {
            for (d in presets) {
                val sameShape = (d.rows == rows && d.cols == cols) || (d.rows == cols && d.cols == rows)
                if (sameShape && d.mines == mines) return d
            }
            return CUSTOM
        }
    }
}
