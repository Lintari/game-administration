package splendor.domain.moves

import splendor.domain.identifiers.CardId

data class PurchaseCardMove(
    val cardId: CardId
) : Move
