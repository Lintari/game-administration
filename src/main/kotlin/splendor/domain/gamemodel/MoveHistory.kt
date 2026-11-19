package splendor.domain.gamemodel

import splendor.domain.identifiers.PlayerId
import splendor.domain.moves.Move
import java.time.Instant

class MoveHistory {

    private val records = mutableListOf<MoveRecord>()

    fun record(
        playerId: PlayerId,
        move: Move,
        timestamp: Instant = Instant.now()
    ) {
        records.add(
            MoveRecord(
                playerId = playerId,
                move = move,
                timestamp = timestamp
            )
        )
    }

    fun getAll(): List<MoveRecord> =
        records.toList()

    fun getByPlayer(playerId: PlayerId): List<MoveRecord> =
        records.filter { it.playerId == playerId }

    fun size(): Int = records.size
}
