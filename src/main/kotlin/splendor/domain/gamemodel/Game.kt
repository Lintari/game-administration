package splendor.domain.gamemodel

import splendor.domain.components.GemColor
import splendor.domain.components.NobleTile
import splendor.domain.identifiers.GameId
import splendor.domain.identifiers.NobleId
import splendor.domain.identifiers.PlayerId
import splendor.domain.logic.MoveValidator
import splendor.domain.logic.PaymentPlan
import splendor.domain.moves.*

class Game(
    participants: List<GameParticipant>,
    val table: Table,
    private val moveValidator: MoveValidator = MoveValidator(),
    private val moveHistory: MoveHistory = MoveHistory()
) {
    val id: GameId = GameId.generate()

    private val participants = participants.toList()

    var status: GameStatus = GameStatus.NOT_STARTED
        private set

    private var currentIndex = 0
    private var turnContext: TurnContext? = null

    private var finalRoundPlayerId: PlayerId? = null
    private var winners: List<GameParticipant> = emptyList()

    init {
        require(this.participants.size in 2..4) {
            "A game must have 2 to 4 participants"
        }

        require(this.participants.map { it.playerId }.distinct().size ==
                this.participants.size) {
            "A player cannot participate in the same game twice"
        }
    }

    fun start() {
        check(status == GameStatus.NOT_STARTED) {
            "Game has already started"
        }

        status = GameStatus.IN_PROGRESS
        currentIndex = 0
        createTurnContext()
    }

    fun getCurrentParticipant(): GameParticipant {
        check(status == GameStatus.IN_PROGRESS) {
            "Game is not in progress"
        }

        return participants[currentIndex]
    }

    fun getCurrentTurnContext(): TurnContext {
        check(status == GameStatus.IN_PROGRESS)

        return checkNotNull(turnContext)
    }

    fun getMoveHistory(): List<MoveRecord> =
        moveHistory.getAll()

    fun getWinners(): List<GameParticipant> = winners.toList()

    fun getParticipants(): List<GameParticipant> =
        participants.toList()

    fun submitMove(move: Move): MoveResult {
        if (status != GameStatus.IN_PROGRESS) {
            return MoveResult.Rejected(MoveRejection.INVALID_MOVE)
        }

        val participant = getCurrentParticipant()
        val context = getCurrentTurnContext()

        val validation = moveValidator.validate(
            move,
            participant,
            table,
            context
        )

        if (validation is MoveResult.Rejected) {
            return validation
        }

        val executionResult = executeMove(move, participant, context)

        if (executionResult is MoveResult.Rejected) {
            return executionResult
        }

        moveHistory.record(participant.playerId, move)

        proceedAfterAction(participant)

        return MoveResult.Accepted
    }

    private fun executeMove(
        move: Move,
        participant: GameParticipant,
        context: TurnContext
    ): MoveResult {
        return when (move) {
            is TakeGemsMove -> takeGems(move, participant, context)
            is TakeTwoGemsMove -> takeTwoGems(move, participant, context)
            is ReserveCardMove -> reserveCard(move, participant, context)
            is PurchaseCardMove -> purchaseCard(move, participant)
        }
    }

    private fun takeGems(
        move: TakeGemsMove,
        participant: GameParticipant,
        context: TurnContext
    ): MoveResult {
        for (color in move.colors) {
            if (!table.takeTokens(color, 1)) {
                return MoveResult.Rejected(
                    MoveRejection.NOT_ENOUGH_TOKENS_ON_TABLE
                )
            }

            participant.state.addTokens(color, 1)
            context.recordAcquiredTokens(color, 1)
        }

        return MoveResult.Accepted
    }

    private fun takeTwoGems(
        move: TakeTwoGemsMove,
        participant: GameParticipant,
        context: TurnContext
    ): MoveResult {
        if (!table.takeTokens(move.color, 2)) {
            return MoveResult.Rejected(
                MoveRejection.NOT_ENOUGH_TOKENS_ON_TABLE
            )
        }

        participant.state.addTokens(move.color, 2)
        context.recordAcquiredTokens(move.color, 2)

        return MoveResult.Accepted
    }

    private fun reserveCard(
        move: ReserveCardMove,
        participant: GameParticipant,
        context: TurnContext
    ): MoveResult {
        val card = when (val target = move.target) {
            is ReserveCardTarget.OpenCard -> table.takeOpenCard(target.cardId)
            is ReserveCardTarget.BlindCard -> table.drawBlindCard(target.level)
        } ?: return MoveResult.Rejected(MoveRejection.CARD_NOT_FOUND)

        check(participant.state.reserveCard(card)) {
            "Validated reservation unexpectedly failed"
        }

        if (table.takeTokens(GemColor.GOLD, 1)) {
            participant.state.addTokens(GemColor.GOLD, 1)
            context.recordAcquiredTokens(GemColor.GOLD, 1)
        }

        return MoveResult.Accepted
    }

    private fun purchaseCard(
        move: PurchaseCardMove,
        participant: GameParticipant
    ): MoveResult {
        val state = participant.state

        val reservedCard = state.getReservedCards()
            .firstOrNull { it.id == move.cardId }

        val card = reservedCard ?: table.findOpenCard(move.cardId)
            ?: return MoveResult.Rejected(MoveRejection.CARD_NOT_FOUND)

        val payment = PaymentPlan.calculate(
            cost = card.cost,
            bonuses = state.getBonuses(),
            availableTokens = state.getTokens()
        ) ?: return MoveResult.Rejected(
            MoveRejection.CANNOT_AFFORD_CARD
        )

        val tokensToSpend = payment.getTokensToSpend()

        for (color in GemColor.entries) {
            val amount = tokensToSpend.getAmount(color)

            if (amount > 0) {
                check(state.removeTokens(color, amount)) {
                    "Validated payment unexpectedly failed"
                }

                table.returnTokens(color, amount)
            }
        }

        val purchasedCard = if (reservedCard != null) {
            state.purchaseReservedCard(move.cardId)
        } else {
            table.takeOpenCard(move.cardId)
        }

        checkNotNull(purchasedCard) {
            "Validated card unexpectedly disappeared"
        }

        if (reservedCard == null) {
            check(state.purchaseCard(purchasedCard)) {
                "Could not add purchased card to player state"
            }
        }

        return MoveResult.Accepted
    }

    private fun proceedAfterAction(participant: GameParticipant) {
        val context = getCurrentTurnContext()

        if (participant.state.getTokens().total() > MAX_PLAYER_TOKENS) {
            context.changePhase(TurnPhase.TOKEN_RETURN)
            return
        }

        proceedToNobleSelection(participant)
    }

    fun returnTokens(color: GemColor, amount: Int): Boolean {
        if (status != GameStatus.IN_PROGRESS) return false

        val participant = getCurrentParticipant()
        val context = getCurrentTurnContext()

        if (context.phase != TurnPhase.TOKEN_RETURN) return false
        if (amount <= 0) return false
        if (context.getAcquiredAmount(color) < amount) return false
        if (participant.state.getTokenAmount(color) < amount) return false

        participant.state.removeTokens(color, amount)
        table.returnTokens(color, amount)
        context.removeAcquiredTokens(color, amount)

        if (participant.state.getTokens().total() <= MAX_PLAYER_TOKENS) {
            proceedToNobleSelection(participant)
        }

        return true
    }

    private fun proceedToNobleSelection(participant: GameParticipant) {
        val context = getCurrentTurnContext()
        context.changePhase(TurnPhase.NOBLE_SELECTION)

        val eligible = getEligibleNobles(participant.playerId)

        when (eligible.size) {
            0 -> completeTurn()
            1 -> {
                val noble = table.takeNoble(eligible.single().id)
                checkNotNull(noble)
                check(participant.state.addNoble(noble))
                completeTurn()
            }
            else -> Unit
        }
    }

    fun getEligibleNobles(playerId: PlayerId): List<NobleTile> {
        val participant = participants.firstOrNull {
            it.playerId == playerId
        } ?: return emptyList()

        val bonuses = participant.state.getBonuses()

        return table.getNobles().filter { noble ->
            GemColor.entries
                .filter { it != GemColor.GOLD }
                .all { color ->
                    (bonuses[color] ?: 0) >= noble.requirements.getAmount(color)
                }
        }
    }

    fun selectNoble(nobleId: NobleId): Boolean {
        if (status != GameStatus.IN_PROGRESS) return false

        val participant = getCurrentParticipant()
        val context = getCurrentTurnContext()

        if (context.phase != TurnPhase.NOBLE_SELECTION) return false

        val eligible = getEligibleNobles(participant.playerId)

        if (eligible.size <= 1) return false

        val noble = eligible.firstOrNull { it.id == nobleId }
            ?: return false

        val takenNoble = table.takeNoble(noble.id) ?: return false

        check(participant.state.addNoble(takenNoble))

        completeTurn()
        return true
    }

    private fun completeTurn() {
        val context = getCurrentTurnContext()

        check(context.phase == TurnPhase.NOBLE_SELECTION)

        context.changePhase(TurnPhase.COMPLETED)

        val currentParticipant = getCurrentParticipant()

        if (finalRoundPlayerId == null &&
            currentParticipant.state.getPrestigePoints() >= PRESTIGE_TO_WIN
        ) {
            finalRoundPlayerId = currentParticipant.playerId
        }

        currentIndex = (currentIndex + 1) % participants.size

        if (finalRoundPlayerId != null &&
            participants[currentIndex].playerId == finalRoundPlayerId
        ) {
            finishGame()
            return
        }

        createTurnContext()
    }

    private fun createTurnContext() {
        turnContext = TurnContext(
            playerId = participants[currentIndex].playerId
        )
    }

    private fun finishGame() {
        val maxPoints = participants.maxOf {
            it.state.getPrestigePoints()
        }

        val finalists = participants.filter {
            it.state.getPrestigePoints() == maxPoints
        }

        val minCards = finalists.minOf {
            it.state.getPurchasedCards().size
        }

        winners = finalists.filter {
            it.state.getPurchasedCards().size == minCards
        }

        status = GameStatus.FINISHED
        turnContext = null
    }

    companion object {
        const val MAX_PLAYER_TOKENS = 10
        const val PRESTIGE_TO_WIN = 15
    }
}
