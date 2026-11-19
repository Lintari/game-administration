package splendor.application.services

import splendor.application.dto.PlayerInfo
import splendor.application.repositories.PlayerRepository
import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player

class PlayerService(
    private val playerRepository: PlayerRepository
) {

    fun registerPlayer(nickname: String): PlayerInfo {
        val player = Player(
            id = PlayerId.generate(),
            nickname = nickname
        )

        playerRepository.save(player)

        return player.toPlayerInfo()
    }

    fun getPlayer(id: PlayerId): PlayerInfo? {
        return playerRepository.findById(id)?.toPlayerInfo()
    }

    fun getAllPlayers(): List<PlayerInfo> {
        return playerRepository.findAll().map { it.toPlayerInfo() }
    }

    private fun Player.toPlayerInfo() = PlayerInfo(
        id = id,
        nickname = nickname
    )
}
