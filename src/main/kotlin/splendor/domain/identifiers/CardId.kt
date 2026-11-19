package splendor.domain.identifiers

import java.util.UUID

@JvmInline
value class CardId(val value: UUID) {
    companion object {
        fun generate(): CardId = CardId(UUID.randomUUID())
    }
}
