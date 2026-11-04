package com.minesweeper.classic.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {

    private fun neighborsOf(g: Game, i: Int): List<Int> {
        val r = i / g.cols
        val c = i % g.cols
        val out = ArrayList<Int>()
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until g.rows && nc in 0 until g.cols) out.add(nr * g.cols + nc)
        }
        return out
    }

    /** Starts a game with a fixed seed and the first tap in the middle. */
    private fun started(rows: Int, cols: Int, mines: Int, seed: Int = 1): Game {
        val g = Game.create(rows, cols, mines)
        g.reveal((rows / 2) * cols + cols / 2, Random(seed))
        return g
    }

    @Test
    fun firstTapIsNeverAMineAndKeepsNeighboursClear() {
        for (seed in 0 until 200) {
            val g = Game.create(9, 9, 10)
            val first = (seed % 81)
            g.reveal(first, Random(seed))
            assertFalse("seed $seed", g.mine[first])
            for (n in neighborsOf(g, first)) assertFalse("seed $seed nb $n", g.mine[n])
            assertEquals(10, g.mine.count { it })
            assertTrue(g.status == Status.PLAYING || g.status == Status.WON)
        }
    }

    @Test
    fun crowdedBoardStillGivesSafeFirstTap() {
        // 5x5 = 25 cells, 16 mines = the maximum (25 - 9)
        for (seed in 0 until 100) {
            val g = Game.create(5, 5, Game.maxMines(5, 5))
            val first = seed % 25
            g.reveal(first, Random(seed))
            assertFalse(g.mine[first])
            assertEquals(16, g.mine.count { it })
        }
    }

    @Test
    fun adjacentCountsMatchMines() {
        val g = started(16, 16, 40)
        for (i in 0 until g.size) {
            assertEquals(neighborsOf(g, i).count { g.mine[it] }, g.adjacent[i])
        }
    }

    @Test
    fun floodFillOpensZeroRegionAndItsBorder() {
        val g = started(16, 16, 10)
        // The first tap is safe with clear neighbours, so it is a zero and must spread.
        assertTrue(g.revealed.count { it } > 1)
        for (i in 0 until g.size) {
            if (g.revealed[i] && g.adjacent[i] == 0) {
                for (n in neighborsOf(g, i)) assertTrue(g.revealed[n])
            }
            if (g.revealed[i]) assertFalse(g.mine[i])
        }
    }

    @Test
    fun revealingAMineLosesAndRecordsIt() {
        val g = started(9, 9, 10)
        val mineIndex = g.mine.indexOfFirst { it }
        assertTrue(g.reveal(mineIndex))
        assertEquals(Status.LOST, g.status)
        assertEquals(mineIndex, g.exploded)
        // Nothing more can be done after losing
        assertFalse(g.reveal(0))
        assertFalse(g.toggleFlag(0))
    }

    @Test
    fun flaggedCellsCannotBeRevealed() {
        val g = started(9, 9, 10)
        val mineIndex = g.mine.indexOfFirst { it }
        assertTrue(g.toggleFlag(mineIndex))
        assertFalse(g.reveal(mineIndex))
        assertEquals(Status.PLAYING, g.status)
        assertEquals(9, g.minesLeft)
        assertTrue(g.toggleFlag(mineIndex))
        assertEquals(10, g.minesLeft)
    }

    @Test
    fun flagCountCanGoAboveMineCount() {
        val g = Game.create(9, 9, 10)
        for (i in 0 until 12) g.toggleFlag(i)
        assertEquals(-2, g.minesLeft)
    }

    @Test
    fun cannotFlagRevealedCell() {
        val g = started(9, 9, 10)
        val rev = g.revealed.indexOfFirst { it }
        assertFalse(g.toggleFlag(rev))
    }

    @Test
    fun chordWithCorrectFlagsRevealsNeighbours() {
        val g = started(16, 16, 40, seed = 7)
        // Find a revealed numbered cell with hidden non-mine neighbours, then flag its mines.
        var target = -1
        for (i in 0 until g.size) {
            if (!g.revealed[i] || g.adjacent[i] == 0) continue
            val hiddenSafe = neighborsOf(g, i).any { !g.revealed[it] && !g.mine[it] }
            if (hiddenSafe) { target = i; break }
        }
        assertTrue("test setup found no cell to chord", target >= 0)
        for (n in neighborsOf(g, target)) if (g.mine[n]) g.toggleFlag(n)
        assertTrue(g.reveal(target))
        assertTrue(g.status == Status.PLAYING || g.status == Status.WON)
        for (n in neighborsOf(g, target)) {
            if (!g.mine[n]) assertTrue(g.revealed[n])
        }
    }

    @Test
    fun chordWithWrongFlagCountDoesNothing() {
        val g = started(16, 16, 40, seed = 7)
        var target = -1
        for (i in 0 until g.size) {
            if (g.revealed[i] && g.adjacent[i] > 0 && neighborsOf(g, i).any { !g.revealed[it] }) { target = i; break }
        }
        assertTrue(target >= 0)
        val before = g.revealedCount
        assertFalse(g.reveal(target)) // zero flags placed, number is > 0
        assertEquals(before, g.revealedCount)
    }

    @Test
    fun chordWithMisplacedFlagHitsAMine() {
        val g = started(16, 16, 40, seed = 7)
        var target = -1
        for (i in 0 until g.size) {
            if (!g.revealed[i] || g.adjacent[i] != 1) continue
            val nbs = neighborsOf(g, i)
            if (nbs.any { g.mine[it] } && nbs.any { !g.mine[it] && !g.revealed[it] }) { target = i; break }
        }
        assertTrue("test setup found no cell to chord", target >= 0)
        val nbs = neighborsOf(g, target)
        val wrong = nbs.first { !g.mine[it] && !g.revealed[it] }
        g.toggleFlag(wrong) // one flag, wrong place, count matches the number
        assertTrue(g.reveal(target))
        assertEquals(Status.LOST, g.status)
    }

    @Test
    fun revealingAllSafeCellsWinsAndFlagsMines() {
        val g = started(9, 9, 10, seed = 3)
        for (i in 0 until g.size) if (!g.mine[i] && !g.revealed[i]) g.reveal(i)
        assertEquals(Status.WON, g.status)
        for (i in 0 until g.size) if (g.mine[i]) assertTrue(g.flagged[i])
        assertEquals(0, g.minesLeft)
    }

    @Test
    fun saveAndRestoreRoundTrip() {
        val g = started(16, 16, 40, seed = 5)
        g.toggleFlag(g.mine.indexOfFirst { it })
        val copy = Game.restore(
            g.rows, g.cols, g.mineCount, g.mineBits(), g.revealedBits(), g.flaggedBits(), g.status, g.exploded,
        )
        assertNotNull(copy)
        copy!!
        assertEquals(g.revealedCount, copy.revealedCount)
        assertEquals(g.flagCount, copy.flagCount)
        for (i in 0 until g.size) {
            assertEquals(g.mine[i], copy.mine[i])
            assertEquals(g.revealed[i], copy.revealed[i])
            assertEquals(g.flagged[i], copy.flagged[i])
            assertEquals(g.adjacent[i], copy.adjacent[i])
        }
    }

    @Test
    fun restoreRejectsBrokenData() {
        assertNull(Game.restore(9, 9, 10, "0", "0", "0", Status.PLAYING, -1))
        val ok = "0".repeat(81)
        // PLAYING game with no mines in the bit string is inconsistent
        assertNull(Game.restore(9, 9, 10, ok, ok, ok, Status.PLAYING, -1))
        // A READY game has no mines yet, which is fine
        assertNotNull(Game.restore(9, 9, 10, ok, ok, ok, Status.READY, -1))
    }

    @Test
    fun copyIsIndependent() {
        val g = started(9, 9, 10)
        val c = g.copy()
        val hidden = c.revealed.indexOfFirst { !it }
        c.toggleFlag(hidden)
        assertEquals(0, g.flagCount)
        assertEquals(1, c.flagCount)
    }

    @Test
    fun difficultyDetection() {
        assertEquals(Difficulty.BEGINNER, Difficulty.of(9, 9, 10))
        assertEquals(Difficulty.INTERMEDIATE, Difficulty.of(16, 16, 40))
        assertEquals(Difficulty.EXPERT, Difficulty.of(30, 16, 99))
        assertEquals(Difficulty.EXPERT, Difficulty.of(16, 30, 99))
        assertEquals(Difficulty.CUSTOM, Difficulty.of(9, 9, 11))
    }
}
