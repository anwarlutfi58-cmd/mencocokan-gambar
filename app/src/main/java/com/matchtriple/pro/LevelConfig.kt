package com.matchtriple.pro

data class LevelConfig(
    val totalTiles: Int,
    val cols: Int,
    val layers: Int,
    val traySlots: Int,
    val timeLimitSec: Int,
    val boosterUndo: Int,
    val boosterShuffle: Int,
    val boosterHint: Int,
    val scoreMultiplier: Int,
    val tierColorRes: Int
)

object LevelConfigs {
    fun forLevel(level: Int): LevelConfig {
        return when {
            level <= 2 -> LevelConfig(36, 4, 3, 7, 180, 3, 3, 3, 1, R.color.tier_easy)
            level <= 4 -> LevelConfig(54, 5, 4, 7, 210, 3, 3, 3, 1, R.color.tier_easy)
            level <= 6 -> LevelConfig(72, 5, 5, 7, 240, 2, 2, 2, 1, R.color.tier_normal)
            level == 7 -> LevelConfig(90, 6, 5, 7, 260, 2, 2, 2, 1, R.color.tier_normal)
            level <= 9 -> LevelConfig(108, 6, 6, 6, 280, 2, 2, 2, 1, R.color.tier_hard)
            level <= 12 -> LevelConfig(132, 6, 7, 6, 300, 2, 1, 2, 1, R.color.tier_hard)
            level <= 15 -> LevelConfig(156, 7, 7, 6, 320, 1, 2, 1, 2, R.color.tier_expert)
            level <= 18 -> LevelConfig(180, 7, 8, 6, 340, 1, 2, 1, 2, R.color.tier_expert)
            level <= 24 -> LevelConfig(216, 8, 8, 5, 360, 1, 1, 1, 3, R.color.tier_master)
            else -> LevelConfig((240 + (level - 24) * 12).coerceAtMost(600), 8, 9, 5, 380, 1, 1, 1, 4, R.color.tier_master)
        }
    }
}