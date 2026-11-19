package splendor.infrastructure.repositories

import splendor.application.repositories.PlayerRepository
import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player

class InMemoryPlayerRepository : PlayerRepository {

    private val players = mutableMapOf<PlayerId, Player>()

    override fun save(player: Player) {
        players[player.id] = player
    }

    override fun findById(id: PlayerId): Player? {
        return players[id]
    }

    override fun findAll(): List<Player> {
        return players.values.toList()
    }
}