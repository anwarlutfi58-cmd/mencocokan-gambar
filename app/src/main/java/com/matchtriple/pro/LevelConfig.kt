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
            level <= 2 -> LevelConfig(9, 3, 1, 7, 0, 3, 3, 3, 1, R.color.tier_easy)
            level <= 4 -> LevelConfig(12, 4, 1, 7, 0, 3, 3, 3, 1, R.color.tier_easy)
            level <= 6 -> LevelConfig(15, 5, 1, 7, 0, 2, 2, 2, 1, R.color.tier_normal)
            level == 7 -> LevelConfig(18, 6, 2, 7, 0, 2, 2, 2, 1, R.color.tier_normal)
            level <= 9 -> LevelConfig(21, 6, 2, 6, 0, 2, 2, 2, 1, R.color.tier_hard)
            level <= 12 -> LevelConfig(27, 6, 2, 6, 0, 2, 1, 2, 1, R.color.tier_hard)
            level <= 15 -> LevelConfig(30, 6, 3, 6, 120, 1, 2, 1, 1, R.color.tier_expert)
            level <= 18 -> LevelConfig(33, 6, 3, 6, 100, 1, 2, 1, 1, R.color.tier_expert)
            level <= 24 -> LevelConfig(36, 6, 3, 5, 90, 1, 1, 1, 2, R.color.tier_master)
            else -> LevelConfig(39 + (level - 24) * 3, 6, 4, 5, 80, 1, 1, 1, 3, R.color.tier_master)
        }
    }
}