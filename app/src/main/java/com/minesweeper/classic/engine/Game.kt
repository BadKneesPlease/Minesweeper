package com.minesweeper.classic.engine

import kotlin.random.Random

enum class Status { READY, PLAYING, WON, LOST }

/**
 * One Minesweeper board and its rules. Plain Kotlin with no Android dependencies.
 *
 * Mines are placed on the first reveal, so the first tap is always safe (and, whenever
 * the board has room, so are all of that cell's neighbours).
 */
class Game private constructor(
    val rows: Int,
    val cols: Int,
    val mineCount: Int,
    val mine: BooleanArray,
    val revealed: BooleanArray,
    val flagged: BooleanArray,
    val adjacent: IntArray,
    var status: Status,
    var exploded: Int,
) {
    val size: Int get() = rows * cols

    var revealedCount: Int = revealed.count { it }
        private set

    var flagCount: Int = flagged.count { it }
        private set

    /** Mines minus flags placed. Can go negative, like the classic game. */
    val minesLeft: Int get() = mineCount - flagCount

    val isFinished: Boolean get() = status == Status.WON || status == Status.LOST

    fun copy(): Game = Game(
        rows, cols, mineCount,
        mine.copyOf(), revealed.copyOf(), flagged.copyOf(), adjacent.copyOf(),
        status, exploded,
    )

    /**
     * Reveals a cell. If the cell is already revealed and shows a number, this "chords":
     * when the number of adjacent flags matches the number, all other neighbours are revealed.
     * Returns true if anything changed.
     */
    fun reveal(index: Int, rng: Random = Random.Default): Boolean {
        if (index < 0 || index >= size || isFinished) return false
        if (flagged[index]) return false
        if (status == Status.READY) {
            placeMines(index, rng)
            status = Status.PLAYING
        }
        return if (revealed[index]) chord(index) else open(intArrayOf(index))
    }

    /** Flags or unflags a hidden cell. Returns true if anything changed. */
    fun toggleFlag(index: Int): Boolean {
        if (index < 0 || index >= size || isFinished || revealed[index]) return false
        if (flagged[index]) {
            flagged[index] = false
            flagCount--
        } else {
            flagged[index] = true
            flagCount++
        }
        return true
    }

    fun mineBits(): String = bitsOf(mine)
    fun revealedBits(): String = bitsOf(revealed)
    fun flaggedBits(): String = bitsOf(flagged)

    // ---- internals ----

    private inline fun forEachNeighbor(index: Int, action: (Int) -> Unit) {
        val r = index / cols
        val c = index % cols
        for (dr in -1..1) {
            val nr = r + dr
            if (nr < 0 || nr >= rows) continue
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nc = c + dc
                if (nc < 0 || nc >= cols) continue
                action(nr * cols + nc)
            }
        }
    }

    private fun placeMines(safe: Int, rng: Random) {
        val blocked = BooleanArray(size)
        blocked[safe] = true
        var neighborCount = 0
        forEachNeighbor(safe) { neighborCount++ }
        // Also keep the neighbours clear when there is room, so the first tap opens an area.
        if (size - 1 - neighborCount >= mineCount) {
            forEachNeighbor(safe) { blocked[it] = true }
        }
        val candidates = ArrayList<Int>(size)
        for (i in 0 until size) if (!blocked[i]) candidates.add(i)
        candidates.shuffle(rng)
        for (k in 0 until mineCount) mine[candidates[k]] = true
        computeAdjacent()
    }

    private fun computeAdjacent() {
        for (i in 0 until size) {
            var n = 0
            forEachNeighbor(i) { if (mine[it]) n++ }
            adjacent[i] = n
        }
    }

    private fun chord(index: Int): Boolean {
        val need = adjacent[index]
        if (need == 0) return false
        var flags = 0
        forEachNeighbor(index) { if (flagged[it]) flags++ }
        if (flags != need) return false
        val targets = ArrayList<Int>(8)
        forEachNeighbor(index) { if (!flagged[it] && !revealed[it]) targets.add(it) }
        if (targets.isEmpty()) return false
        return open(targets.toIntArray())
    }

    /** Opens the given cells (flood-filling through empty ones) and updates win/loss. */
    private fun open(starts: IntArray): Boolean {
        var changed = false
        var hit = -1
        val stack = ArrayDeque<Int>()
        for (s in starts) {
            if (revealed[s] || flagged[s]) continue
            if (mine[s]) {
                if (hit < 0) hit = s
                continue
            }
            stack.addLast(s)
        }
        while (stack.isNotEmpty()) {
            val c = stack.removeLast()
            if (revealed[c] || flagged[c]) continue
            revealed[c] = true
            revealedCount++
            changed = true
            if (adjacent[c] == 0) {
                forEachNeighbor(c) { n ->
                    if (!revealed[n] && !flagged[n]) stack.addLast(n)
                }
            }
        }
        if (hit >= 0) {
            status = Status.LOST
            exploded = hit
            return true
        }
        if (revealedCount == size - mineCount) {
            status = Status.WON
            for (i in 0 until size) {
                if (mine[i] && !flagged[i]) {
                    flagged[i] = true
                    flagCount++
                }
            }
            return true
        }
        return changed
    }

    companion object {
        const val MIN_SIDE = 5
        const val MAX_SIDE = 40

        /** The most mines a board of this size may hold, leaving room for a safe first tap. */
        fun maxMines(rows: Int, cols: Int): Int = rows * cols - 9

        fun create(rows: Int, cols: Int, mines: Int): Game {
            require(rows >= 1 && cols >= 1) { "Board must have at least one cell" }
            require(mines >= 1 && mines < rows * cols) { "Mine count out of range" }
            val n = rows * cols
            return Game(
                rows, cols, mines,
                BooleanArray(n), BooleanArray(n), BooleanArray(n), IntArray(n),
                Status.READY, -1,
            )
        }

        /** Rebuilds a saved game. Returns null if the saved data is inconsistent. */
        fun restore(
            rows: Int,
            cols: Int,
            mines: Int,
            mineBits: String,
            revealedBits: String,
            flaggedBits: String,
            status: Status,
            exploded: Int,
        ): Game? {
            if (rows < 1 || cols < 1 || mines < 1 || mines >= rows * cols) return null
            val n = rows * cols
            if (mineBits.length != n || revealedBits.length != n || flaggedBits.length != n) return null
            val game = Game(
                rows, cols, mines,
                boolsOf(mineBits), boolsOf(revealedBits), boolsOf(flaggedBits), IntArray(n),
                status, exploded,
            )
            if (status != Status.READY) {
                if (game.mine.count { it } != mines) return null
                game.computeAdjacent()
            }
            return game
        }

        private fun bitsOf(a: BooleanArray): String =
            String(CharArray(a.size) { if (a[it]) '1' else '0' })

        private fun boolsOf(s: String): BooleanArray = BooleanArray(s.length) { s[it] == '1' }
    }
}
