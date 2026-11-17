package com.matchtriple.pro

data class ThemeItem(val emoji: String, val name: String)

object ThemeData {
    val fruit = listOf(
        ThemeItem("🍎", "Apel"), ThemeItem("🍊", "Jeruk"),
        ThemeItem("🍇", "Anggur"), ThemeItem("🍓", "Stroberi"),
        ThemeItem("🍌", "Pisang"), ThemeItem("🍉", "Semangka"),
        ThemeItem("🍑", "Persik"), ThemeItem("🥝", "Kiwi")
    )
    val animal = listOf(
        ThemeItem("🐱", "Kucing"), ThemeItem("🐶", "Anjing"),
        ThemeItem("🐰", "Kelinci"), ThemeItem("🐼", "Panda"),
        ThemeItem("🦊", "Rubah"), ThemeItem("🐸", "Katak"),
        ThemeItem("🐨", "Koala"), ThemeItem("🦁", "Singa")
    )
    val flower = listOf(
        ThemeItem("🌹", "Mawar"), ThemeItem("🌻", "Bunga Matahari"),
        ThemeItem("🌷", "Tulip"), ThemeItem("🌸", "Sakura"),
        ThemeItem("🌼", "Daisy"), ThemeItem("💐", "Buket"),
        ThemeItem("🌺", "Kamboja"), ThemeItem("🪷", "Teratai")
    )
    fun get(theme: String): List<ThemeItem> = when (theme) {
        "animal" -> animal
        "flower" -> flower
        else -> fruit
    }
    fun getName(theme: String): String = when (theme) {
        "animal" -> "Hewan"
        "flower" -> "Bunga"
        else -> "Buah"
    }
}