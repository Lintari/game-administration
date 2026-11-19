package splendor.application.services

import splendor.application.dto.PlayerStatistics
import splendor.application.repositories.GameRepository
import splendor.application.repositories.PlayerRepository
import splendor.domain.gamemodel.GameStatus

class StatisticsService(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository
) {
    fun getPlayerStatistics(): List<PlayerStatistics> {
        val games = gameRepository.findAll()
        return playerRepository.findAll().map { player ->
            val relevantGames = games.filter { game ->
                game.getParticipants().any { it.playerId == player.id }
            }
            val completedGames = relevantGames.filter { it.status == GameStatus.FINISHED }
            val wins = completedGames.count { game ->
                game.getWinners().any { it.playerId == player.id }
            }
            PlayerStatistics(
                playerId = player.id.value.toString(),
                nickname = player.nickname,
                gamesPlayed = completedGames.size,
                wins = wins,
                winPercentage = if (completedGames.isEmpty()) 0.0
                else wins * 100.0 / completedGames.size
            )
        }
    }
}
