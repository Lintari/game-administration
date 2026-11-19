package splendor.domain.identifiers

import java.util.UUID

@JvmInline
value class PlayerId(val value: UUID) {
    companion object {
        fun generate(): PlayerId = PlayerId(UUID.randomUUID())
    }
}