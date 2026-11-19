package splendor.domain.gamemodel

import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player

class GameParticipant(
    val player: Player,
    val nicknameAtStart: String = player.nickname
) {
    val playerId: PlayerId = player.id
    val state: PlayerState = PlayerState(playerId)
}
