package splendor.domain.logic

import splendor.domain.components.GemColor
import splendor.domain.gamemodel.GameParticipant
import splendor.domain.gamemodel.PlayerState
import splendor.domain.gamemodel.Table
import splendor.domain.gamemodel.TurnContext
import splendor.domain.gamemodel.TurnPhase
import splendor.domain.logic.PaymentPlan
import splendor.domain.moves.*

class MoveValidator {

    fun validate(
        move: Move,
        participant: GameParticipant,
        table: Table,
        turnContext: TurnContext
    ): MoveResult {
        if (turnContext.playerId != participant.playerId ||
            turnContext.phase != TurnPhase.PLAYER_ACTION
        ) {
            return MoveResult.Rejected(MoveRejection.WRONG_PHASE)
        }

        return when (move) {
            is TakeGemsMove ->
                validateTakeGems(move, table)

            is TakeTwoGemsMove ->
                validateTakeTwoGems(move, table)

            is ReserveCardMove ->
                validateReserveCard(move, participant, table)

            is PurchaseCardMove ->
                validatePurchaseCard(move, participant, table)
        }
    }

    private fun validateTakeGems(
        move: TakeGemsMove,
        table: Table
    ): MoveResult {
        val colors = move.colors

        if (colors.isEmpty() || colors.size > 3) {
            return MoveResult.Rejected(MoveRejection.INVALID_MOVE)
        }

        if (GemColor.GOLD in colors || colors.distinct().size != colors.size) {
            return MoveResult.Rejected(MoveRejection.INVALID_MOVE)
        }

        if (colors.any { table.getTokenAmount(it) <= 0 }) {
            return MoveResult.Rejected(
                MoveRejection.NOT_ENOUGH_TOKENS_ON_TABLE
            )
        }

        if (colors.size < 3) {
            val availableColors = GemColor.entries.count {
                it != GemColor.GOLD && table.getTokenAmount(it) > 0
            }

            if (availableColors >= 3) {
                return MoveResult.Rejected(MoveRejection.INVALID_MOVE)
            }
        }

        return MoveResult.Accepted
    }

    private fun validateTakeTwoGems(
        move: TakeTwoGemsMove,
        table: Table
    ): MoveResult {
        if (move.color == GemColor.GOLD) {
            return MoveResult.Rejected(MoveRejection.INVALID_MOVE)
        }

        if (table.getTokenAmount(move.color) < 4) {
            return MoveResult.Rejected(
                MoveRejection.NOT_ENOUGH_TOKENS_ON_TABLE
            )
        }

        return MoveResult.Accepted
    }

    private fun validateReserveCard(
        move: ReserveCardMove,
        participant: GameParticipant,
        table: Table
    ): MoveResult {
        val state = participant.state

        if (state.getReservedCards().size >= PlayerState.MAX_RESERVED_CARDS) {
            return MoveResult.Rejected(
                MoveRejection.TOO_MANY_RESERVED_CARDS
            )
        }

        return when (val target = move.target) {
            is ReserveCardTarget.OpenCard -> {
                if (table.findOpenCard(target.cardId) == null) {
                    MoveResult.Rejected(MoveRejection.CARD_NOT_FOUND)
                } else {
                    MoveResult.Accepted
                }
            }

            is ReserveCardTarget.BlindCard -> {
                if (table.getDeckRemainingCount(target.level) == 0) {
                    MoveResult.Rejected(MoveRejection.DECK_EMPTY)
                } else {
                    MoveResult.Accepted
                }
            }
        }
    }

    private fun validatePurchaseCard(
        move: PurchaseCardMove,
        participant: GameParticipant,
        table: Table
    ): MoveResult {
        val state = participant.state

        val card = state.getReservedCards()
            .firstOrNull { it.id == move.cardId }
            ?: table.findOpenCard(move.cardId)
            ?: return MoveResult.Rejected(MoveRejection.CARD_NOT_FOUND)

        val payment = PaymentPlan.calculate(
            cost = card.cost,
            bonuses = state.getBonuses(),
            availableTokens = state.getTokens()
        )

        return if (payment != null) {
            MoveResult.Accepted
        } else {
            MoveResult.Rejected(MoveRejection.CANNOT_AFFORD_CARD)
        }
    }
}
