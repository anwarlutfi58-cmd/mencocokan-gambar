package com.matchtriple.pro

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

        val catalog = ThemeData.get(theme).shuffled()
        val triplesCount = config.totalTiles / 3
        val usedTypes = catalog.take(triplesCount.coerceAtMost(catalog.size))

        val raw = mutableListOf<Tile>()
        for (type in usedTypes) {
            repeat(3) { raw.add(Tile(type.emoji, type.name, layer = 0)) }
        }
        var extraIdx = 0
        while (raw.size < config.totalTiles) {
            val t = catalog[extraIdx % catalog.size]
            raw.add(Tile(t.emoji, t.name, layer = 0))
            extraIdx++
        }

        if (config.layers > 1) {
            raw.shuffle(Random(System.currentTimeMillis()))
            val total = raw.size
            raw.forEachIndexed { i, tile ->
                val ratio = i.toFloat() / total
                val layer = when {
                    config.layers == 2 -> if (ratio < 0.65f) 0 else 1
                    config.layers == 3 -> when {
                        ratio < 0.55f -> 0
                        ratio < 0.82f -> 1
                        else -> 2
                    }
                    else -> when {
                        ratio < 0.45f -> 0
                        ratio < 0.72f -> 1
                        ratio < 0.9f -> 2
                        else -> 3
                    }
                }
                raw[i] = tile.copy(layer = layer, locked = false)
            }
        }

        raw.shuffle(Random(System.currentTimeMillis() + 1))
        board.addAll(raw)
        recomputeLocks()
    }

    fun recomputeLocks() {
        val highestLayer = HashMap<Int, Int>()
        board.forEachIndexed { idx, t ->
            if (t != null) {
                highestLayer[idx] = maxOf(highestLayer[idx] ?: 0, t.layer)
            }
        }
        board.forEachIndexed { idx, t ->
            if (t == null) return@forEachIndexed
            val maxLayerHere = highestLayer[idx] ?: t.layer
            val newLocked = t.layer < maxLayerHere
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
        return t
    }

    fun checkMatch(): Tile? {
        val counts = tray.groupingBy { it.emoji }.eachCount()
        val matched = counts.entries.firstOrNull { it.value >= 3 }?.key ?: return null
        var removed = 0
        val newTray = mutableListOf<Tile>()
        for (t in tray) {
            if (t.emoji == matched && removed < 3) { removed++ } else { newTray.add(t) }
        }
        tray.clear()
        tray.addAll(newTray)
        return Tile(matched, ThemeData.get(theme).firstOrNull { it.emoji == matched }?.name ?: "", layer = 0)
    }

    fun hasMatch(): Boolean = tray.groupingBy { it.emoji }.eachCount().values.any { it >= 3 }

    fun remaining(): Int = board.count { it != null }
    fun isWin(): Boolean = remaining() == 0 && tray.isEmpty()
    fun isLose(): Boolean = tray.size >= config.traySlots

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
        val remainingTiles = board.filterNotNull()
        val shuffled = remainingTiles.shuffled(Random(System.currentTimeMillis()))
        var i = 0
        board.forEachIndexed { idx, t ->
            if (t != null) {
                board[idx] = shuffled[i++]
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