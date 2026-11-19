package splendor.domain.gamemodel

import splendor.domain.identifiers.PlayerId
import splendor.domain.moves.Move
import java.time.Instant

data class MoveRecord(
    val playerId: PlayerId,
    val move: Move,
    val timestamp: Instant
)
