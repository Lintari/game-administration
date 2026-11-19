package splendor.application.repositories

import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player

interface PlayerRepository {

    fun save(player: Player)

    fun findById(id: PlayerId): Player?

    fun findAll(): List<Player>
}