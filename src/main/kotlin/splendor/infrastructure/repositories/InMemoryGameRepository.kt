package splendor.infrastructure.repositories

import splendor.application.repositories.GameRepository
import splendor.domain.gamemodel.Game
import splendor.domain.identifiers.GameId

class InMemoryGameRepository : GameRepository {

    private val games = mutableMapOf<GameId, Game>()

    override fun save(game: Game) {
        games[game.id] = game
    }

    override fun findById(id: GameId): Game? =
        games[id]

    override fun findAll(): List<Game> =
        games.values.toList()
}
