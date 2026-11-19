package splendor.application.services

import splendor.application.dto.PlayerStatistics
import splendor.application.repositories.GameRepository
import splendor.application.repositories.PlayerRepository
import splendor.application.repositories.PlayerStatisticsRepository
import splendor.domain.gamemodel.GameStatus

class StatisticsService(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
    private val statisticsRepository: PlayerStatisticsRepository? = null
) {
    fun getPlayerStatistics(): List<PlayerStatistics> {
        val persisted = statisticsRepository?.findAll()?.associateBy { it.playerId }
        if (persisted != null) {
            return playerRepository.findAll().map { player ->
                val record = persisted[player.id]
                val played = record?.gamesPlayed ?: 0
                val wins = record?.gamesWon ?: 0
                PlayerStatistics(
                    playerId = player.id.value.toString(),
                    nickname = player.nickname,
                    gamesPlayed = played,
                    wins = wins,
                    winPercentage = if (played == 0) 0.0 else wins * 100.0 / played,
                    totalPoints = record?.totalPoints ?: 0
                )
            }
        }

        val games = gameRepository.findAll()
        return playerRepository.findAll().map { player ->
            val completedGames = games.filter { game ->
                game.status == GameStatus.FINISHED &&
                    game.getParticipants().any { it.playerId == player.id }
            }
            val wins = completedGames.count { game ->
                game.getWinners().any { it.playerId == player.id }
            }
            val totalPoints = completedGames.sumOf { game ->
                game.getParticipants().first { it.playerId == player.id }
                    .state.getPrestigePoints()
            }
            PlayerStatistics(
                playerId = player.id.value.toString(),
                nickname = player.nickname,
                gamesPlayed = completedGames.size,
                wins = wins,
                winPercentage = if (completedGames.isEmpty()) 0.0
                else wins * 100.0 / completedGames.size,
                totalPoints = totalPoints
            )
        }
    }

    fun recalculate() {
        statisticsRepository?.recalculate()
    }
}
