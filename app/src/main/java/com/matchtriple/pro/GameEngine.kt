package com.matchtriple.pro

import kotlin.math.ceil
import kotlin.random.Random

class GameEngine(
    private val theme: String,
    private val config: LevelConfig
) {
    val board = mutableListOf<Tile?>()
    val tray = mutableListOf<Tile>()
    var lastMove: Pair<Tile, Int>? = null

    fun generate() {
        board.clear()
        tray.clear()
        lastMove = null

        val catalog = ThemeData.get(theme).shuffled(Random(System.currentTimeMillis()))
        val triplesCount = (config.totalTiles / 3).coerceAtLeast(1)

        val usedTypes = mutableListOf<ThemeItem>()
        while (usedTypes.size < triplesCount) {
            usedTypes.addAll(catalog.shuffled())
        }

        val raw = mutableListOf<Tile>()
        for (type in usedTypes.take(triplesCount)) {
            repeat(3) {
                raw.add(Tile(type.emoji, type.name))
            }
        }
        raw.shuffle(Random(System.currentTimeMillis() + 11))

        val rows = ceil(config.totalTiles.toDouble() / config.cols.toDouble()).toInt().coerceAtLeast(1)
        val positions = mutableListOf<Pair<Int, Int>>()

        for (r in 0 until rows) {
            for (c in 0 until config.cols) {
                if (positions.size < config.totalTiles) {
                    positions.add(r to c)
                }
            }
        }

        val cells = positions.size.coerceAtLeast(1)
        val orderedPositions = positions.shuffled(Random(System.currentTimeMillis() + 7))
        val stacks = MutableList(cells) { mutableListOf<Tile>() }

        var tileIndex = 0
        var layer = 0

        while (tileIndex < raw.size) {
            val cellOrder = if (layer % 2 == 0) 0 until cells else (cells - 1) downTo 0

            for (cell in cellOrder) {
                if (tileIndex >= raw.size) break

                val pos = orderedPositions[cell]
                val tile = raw[tileIndex++]

                stacks[cell].add(
                    tile.copy(
                        row = pos.first,
                        col = pos.second,
                        layer = layer,
                        locked = false
                    )
                )
            }

            layer++
        }

        val maxLayer = stacks.maxOfOrNull { it.size } ?: 0

        for (l in (maxLayer - 1) downTo 0) {
            for (cell in 0 until cells) {
                val t = stacks[cell].getOrNull(l)
                if (t != null) board.add(t)
            }
        }

        recomputeLocks()
    }

    fun recomputeLocks() {
        val highestLayer = HashMap<String, Int>()

        board.forEach { t ->
            if (t != null) {
                val key = "${t.row}:${t.col}"
                highestLayer[key] = maxOf(highestLayer[key] ?: 0, t.layer)
            }
        }

        board.forEachIndexed { idx, t ->
            if (t == null) return@forEachIndexed

            val key = "${t.row}:${t.col}"
            val newLocked = t.layer < (highestLayer[key] ?: t.layer)

            if (newLocked != t.locked) {
                board[idx] = t.copy(locked = newLocked)
            }
        }
    }

    fun pick(idx: Int): Tile? {
        val t = board.getOrNull(idx) ?: return null
        if (t.locked) return null
        if (tray.size >= config.traySlots) return null

        board[idx] = null
        tray.add(t)
        lastMove = Pair(t, idx)
        recomputeLocks()

        return t
    }

    fun checkMatch(): Tile? {
        val counts = tray.groupingBy { it.emoji }.eachCount()
        val matched = counts.entries.firstOrNull { it.value >= 3 }?.key ?: return null

        var removed = 0
        val newTray = mutableListOf<Tile>()

        for (t in tray) {
            if (t.emoji == matched && removed < 3) {
                removed++
            } else {
                newTray.add(t)
            }
        }

        tray.clear()
        tray.addAll(newTray)

        return Tile(
            matched,
            ThemeData.get(theme).firstOrNull { it.emoji == matched }?.name ?: "",
            layer = 0
        )
    }

    fun hasMatch(): Boolean {
        return tray.groupingBy { it.emoji }.eachCount().values.any { it >= 3 }
    }

    fun remaining(): Int = board.count { it != null }

    fun isWin(): Boolean = remaining() == 0 && tray.isEmpty()

    fun isLose(): Boolean = tray.size >= config.traySlots && !hasMatch()

    fun undo(): Boolean {
        val lm = lastMove ?: return false
        val idxInTray = tray.indexOfFirst { it.id == lm.first.id }

        if (idxInTray == -1) return false

        val t = tray.removeAt(idxInTray)
        board[lm.second] = t
        lastMove = null
        recomputeLocks()

        return true
    }

    fun shuffle() {
        val remainingTiles = board
            .filterNotNull()
            .map { it.copy(locked = false) }
            .shuffled(Random(System.currentTimeMillis()))

        var i = 0

        board.forEachIndexed { idx, t ->
            if (t != null) {
                board[idx] = remainingTiles[i++]
            }
        }

        recomputeLocks()
    }

    fun findHint(): String? {
        val boardCounts = board.filterNotNull().groupingBy { it.emoji }.eachCount()
        val trayCounts = tray.groupingBy { it.emoji }.eachCount()

        for ((emoji, cnt) in boardCounts) {
            val have = trayCounts[emoji] ?: 0
            if (cnt + have >= 3) return emoji
        }

        return null
    }
}