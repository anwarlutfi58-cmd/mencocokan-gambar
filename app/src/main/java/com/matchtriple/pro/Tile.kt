package com.matchtriple.pro

data class Tile(
    val emoji: String,
    val name: String,
    val id: String = System.nanoTime().toString() + Math.random().toString().takeLast(6),
    val layer: Int = 0,
    var locked: Boolean = false
)