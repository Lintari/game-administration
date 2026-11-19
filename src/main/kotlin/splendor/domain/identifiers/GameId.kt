package splendor.domain.identifiers

import java.util.UUID

@JvmInline
value class GameId(val value: UUID) {
    companion object {
        fun generate(): GameId = GameId(UUID.randomUUID())
    }
}
