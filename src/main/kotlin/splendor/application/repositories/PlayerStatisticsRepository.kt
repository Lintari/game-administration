package splendor.application.repositories

import splendor.domain.identifiers.PlayerId

data class PlayerStatisticsRecord(
    val playerId: PlayerId,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val totalPoints: Int
)

interface PlayerStatisticsRepository {
    fun findAll(): List<PlayerStatisticsRecord>
    fun findByPlayerId(playerId: PlayerId): PlayerStatisticsRecord?
    fun recalculate()
}
