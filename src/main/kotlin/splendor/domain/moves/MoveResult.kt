package splendor.domain.moves

sealed interface MoveResult {

    data object Accepted : MoveResult

    data class Rejected(
        val reason: MoveRejection
    ) : MoveResult
}

enum class MoveRejection {
    WRONG_PHASE,
    INVALID_MOVE,
    NOT_ENOUGH_TOKENS,
    NOT_ENOUGH_TOKENS_ON_TABLE,
    TOO_MANY_RESERVED_CARDS,
    CARD_NOT_FOUND,
    NOBLE_NOT_FOUND,
    DECK_EMPTY,
    CANNOT_AFFORD_CARD
}
