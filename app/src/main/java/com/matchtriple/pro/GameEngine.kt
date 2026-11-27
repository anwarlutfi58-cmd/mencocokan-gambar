package com.matchtriple.pro

import kotlin.math.ceil
import kotlin.random.Random

class GameEngine(
    private val theme: String,
    private val config: LevelConfig
) {
    val board = mutableListOf<Tile?>()
    val tray = mutableListOf<Tile>()

    private val hidden = HashMap<Int, MutableList<Tile>>()
    private var lastPicked: Tile? = null
    private var lastPile: Int = -1
    private var lastRevealed: Tile? = null

    var firstOpenIdx: Int = -1
        private set

    fun generate() {
        board.clear()
        tray.clear()
        hidden.clear()
        lastPicked = null
        lastPile = -1
        lastRevealed = null
        firstOpenIdx = -1

        val catalog = ThemeData.get(theme).shuffled(Random(System.currentTimeMillis()))
        val triplesCount = (config.totalTiles / 3).coerceAtLeast(1)
        val usedTypes = mutableListOf<ThemeItem>()
        while (usedTypes.size < triplesCount) usedTypes.addAll(catalog.shuffled())

        val cards = mutableListOf<Tile>()
        for (type in usedTypes.take(triplesCount)) {
            repeat(3) { cards.add(Tile(type.emoji, type.name)) }
        }
        cards.shuffle(Random(System.currentTimeMillis() + 11))

        val pileCount = ceil(config.totalTiles.toDouble() / config.layers.toDouble()).toInt().coerceAtLeast(1)
        val piles = MutableList(pileCount) { mutableListOf<Tile>() }

        var cardIndex = 0
        var round = 0
        while (cardIndex < cards.size) {
            val order = if (round % 2 == 0) 0 until pileCount else (pileCount - 1) downTo 0
            for (p in order) {
                if (cardIndex >= cards.size) break
                piles[p].add(cards[cardIndex++].copy(pile = p, faceUp = false))
            }
            round++
        }

        for (p in 0 until pileCount) {
            val stack = piles[p]
            val top = stack.removeAt(stack.lastIndex)
            hidden[p] = stack
            board.add(top.copy(stackBelow = stack.size, locked = false, faceUp = false))
        }
    }

    fun recomputeLocks() {
        board.forEachIndexed { idx, t ->
            if (t != null) {
                val below = hidden[idx]?.size ?: 0
                board[idx] = t.copy(stackBelow = below, locked = false)
            }
        }
    }

    fun isFaceUp(idx: Int): Boolean = board.getOrNull(idx)?.faceUp == true

    fun reveal(idx: Int): Tile? {
        val t = board.getOrNull(idx) ?: return null
        if (t.faceUp) return null
        val opened = t.copy(faceUp = true)
        board[idx] = opened
        if (firstOpenIdx == -1) firstOpenIdx = idx
        return opened
    }

    fun closeCard(idx: Int) {
        val t = board.getOrNull(idx) ?: return
        board[idx] = t.copy(faceUp = false)
        if (firstOpenIdx == idx) firstOpenIdx = -1
    }

    fun closeAllOpen() {
        board.forEachIndexed { idx, t ->
            if (t != null && t.faceUp) board[idx] = t.copy(faceUp = false)
        }
        firstOpenIdx = -1
    }

    fun emojiAt(idx: Int): String? = board.getOrNull(idx)?.emoji

    fun pick(idx: Int): Tile? {
        val top = board.getOrNull(idx) ?: return null
        if (!top.faceUp) return null
        if (tray.size >= config.traySlots) return null

        lastPicked = top
        lastPile = idx
        lastRevealed = null

        tray.add(top)

        val stack = hidden[idx]
        if (!stack.isNullOrEmpty()) {
            val next = stack.removeAt(stack.lastIndex)
            lastRevealed = next
            board[idx] = next.copy(stackBelow = stack.size, locked = false, faceUp = false)
        } else {
            board[idx] = null
        }

        if (firstOpenIdx == idx) firstOpenIdx = -1
        return top
    }

    fun checkMatch(): Tile? {
        val counts = tray.groupingBy { it.emoji }.eachCount()
        val matched = counts.entries.firstOrNull { it.value >= 3 }?.key ?: return null

        var removed = 0
        val newTray = mutableListOf<Tile>()
        for (t in tray) {
            if (t.emoji == matched && removed < 3) removed++ else newTray.add(t)
        }
        tray.clear()
        tray.addAll(newTray)

        return Tile(matched, ThemeData.get(theme).firstOrNull { it.emoji == matched }?.name ?: "")
    }

    fun hasMatch(): Boolean = tray.groupingBy { it.emoji }.eachCount().values.any { it >= 3 }

    fun remaining(): Int {
        val visible = board.count { it != null }
        val below = hidden.values.sumOf { it.size }
        return visible + below
    }

    fun isWin(): Boolean = remaining() == 0 && tray.isEmpty()
    fun isLose(): Boolean = tray.size >= config.traySlots && !hasMatch()

    fun undo(): Boolean {
        val picked = lastPicked ?: return false
        val p = lastPile
        if (p < 0) return false

        val trayIndex = tray.indexOfFirst { it.id == picked.id }
        if (trayIndex == -1) return false
        tray.removeAt(trayIndex)

        val revealed = lastRevealed
        if (revealed != null) {
            val stack = hidden.getOrPut(p) { mutableListOf() }
            stack.add(revealed.copy(faceUp = false))
        }

        board[p] = picked.copy(stackBelow = hidden[p]?.size ?: 0, locked = false, faceUp = false)

        lastPicked = null
        lastPile = -1
        lastRevealed = null
        return true
    }

    fun shuffle() {
        val all = mutableListOf<Tile>()
        board.forEach { if (it != null) all.add(it) }
        hidden.values.forEach { all.addAll(it) }
        all.shuffle(Random(System.currentTimeMillis()))

        val oldSizes = MutableList(board.size) { idx ->
            (if (board[idx] != null) 1 else 0) + (hidden[idx]?.size ?: 0)
        }

        hidden.clear()
        firstOpenIdx = -1
        var i = 0
        for (p in oldSizes.indices) {
            val size = oldSizes[p]
            if (size <= 0) {
                board[p] = null
                continue
            }
            val stack = mutableListOf<Tile>()
            repeat(size) { stack.add(all[i++].copy(pile = p, faceUp = false)) }
            val top = stack.removeAt(stack.lastIndex)
            hidden[p] = stack
            board[p] = top.copy(stackBelow = stack.size, locked = false, faceUp = false)
        }
    }

    fun hintReveal(): Int {
        val candidates = board.indices.filter { board[it] != null && board[it]?.faceUp == false }
        if (candidates.isEmpty()) return -1
        val idx = candidates.random()
        reveal(idx)
        return idx
    }

    fun findHint(): String? = null
}