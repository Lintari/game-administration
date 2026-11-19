package splendor.application.services

import splendor.application.repositories.GameRepository
import splendor.application.repositories.PlayerRepository
import splendor.domain.components.DevelopmentCard
import splendor.domain.components.CardLevel
import splendor.domain.components.NobleTile
import splendor.domain.gamemodel.Game
import splendor.domain.gamemodel.GameFactory
import splendor.domain.gamemodel.GameParticipant
import splendor.domain.identifiers.GameId
import splendor.domain.identifiers.PlayerId

class GameService(
    private val gameRepository: GameRepository,
    private val playerRepository: PlayerRepository,
    private val gameFactory: GameFactory
) {

    fun createGame(
        playerIds: List<PlayerId>,
        cards: Map<CardLevel, List<DevelopmentCard>>,
        nobles: List<NobleTile>
    ): Game {
        require(playerIds.distinct().size == playerIds.size) {
            "Duplicate players are not allowed"
        }

        val players = playerIds.map { id ->
            requireNotNull(playerRepository.findById(id)) {
                "Player $id not found"
            }
        }

        val participants = players.map { player ->
            GameParticipant(player)
        }

        val game = gameFactory.createGame(
            participants = participants,
            cards = cards,
            nobles = nobles
        )

        gameRepository.save(game)

        return game
    }

    fun getGame(id: GameId): Game? =
        gameRepository.findById(id)

    fun getAllGames(): List<Game> =
        gameRepository.findAll()
}
