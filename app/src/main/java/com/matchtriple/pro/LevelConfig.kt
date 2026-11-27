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
            level <= 2 -> LevelConfig(36, 6, 2, 7, 0, 3, 3, 3, 1, R.color.tier_easy)
            level <= 4 -> LevelConfig(54, 6, 3, 7, 0, 3, 3, 3, 1, R.color.tier_easy)
            level <= 6 -> LevelConfig(72, 7, 3, 7, 0, 2, 2, 2, 1, R.color.tier_normal)
            level == 7 -> LevelConfig(90, 7, 4, 7, 0, 2, 2, 2, 1, R.color.tier_normal)
            level <= 9 -> LevelConfig(108, 8, 4, 6, 0, 2, 2, 2, 1, R.color.tier_hard)
            level <= 12 -> LevelConfig(132, 8, 5, 6, 0, 2, 1, 2, 1, R.color.tier_hard)
            level <= 15 -> LevelConfig(156, 8, 5, 6, 180, 1, 2, 1, 2, R.color.tier_expert)
            level <= 18 -> LevelConfig(180, 9, 6, 6, 160, 1, 2, 1, 2, R.color.tier_expert)
            level <= 24 -> LevelConfig(216, 9, 6, 5, 150, 1, 1, 1, 3, R.color.tier_master)
            else -> LevelConfig((240 + (level - 24) * 12).coerceAtMost(600), 10, 7, 5, 140, 1, 1, 1, 4, R.color.tier_master)
        }
    }
}