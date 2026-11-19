package splendor.domain.moves

import splendor.domain.components.CardLevel
import splendor.domain.identifiers.CardId

sealed interface ReserveCardTarget {

    data class OpenCard(
        val cardId: CardId
    ) : ReserveCardTarget

    data class BlindCard(
        val level: CardLevel
    ) : ReserveCardTarget
}

data class ReserveCardMove(
    val target: ReserveCardTarget
) : Move
