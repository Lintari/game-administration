package splendor.application.repositories

import splendor.domain.gamemodel.Game
import splendor.domain.identifiers.GameId

interface GameRepository {
    fun save(game: Game)
    fun findById(id: GameId): Game?
    fun findAll(): List<Game>
}
