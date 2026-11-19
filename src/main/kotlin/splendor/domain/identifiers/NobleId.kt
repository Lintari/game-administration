package splendor.domain.identifiers

import java.util.UUID

@JvmInline
value class NobleId(val value: UUID) {
    companion object {
        fun generate(): NobleId = NobleId(UUID.randomUUID())
    }
}
