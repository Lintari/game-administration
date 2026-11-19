package splendor.domain.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import splendor.domain.components.*
import splendor.domain.gamemodel.*
import splendor.domain.identifiers.*
import splendor.domain.moves.*
import splendor.domain.players.Player

class MoveValidatorTest {

    private val validator = MoveValidator()

    private fun createParticipant(): GameParticipant {
        val player = Player(PlayerId.generate(), "TestPlayer")
        return GameParticipant(player)
    }

    private fun createTable(
        tokens: GemTokens = GemTokens(),
        cards: List<DevelopmentCard> = emptyList(),
        decks: Map<CardLevel, DevelopmentCardDeck> = emptyMap()
    ): Table {
        val allDecks = CardLevel.entries.associateWith {
            decks[it] ?: DevelopmentCardDeck(it, emptyList())
        }

        val openCards = CardLevel.entries.associateWith {
            cards.filter { card -> card.level == it }.toMutableList()
        }

        return Table(
            tokens = tokens,
            openCards = openCards,
            decks = allDecks,
            nobles = emptyList()
        )
    }

    private fun createCard(
        level: CardLevel = CardLevel.I,
        bonus: GemColor = GemColor.BLUE,
        points: Int = 0,
        cost: GemCost = GemCost(emptyMap())
    ): DevelopmentCard {
        return DevelopmentCard(
            id = CardId.generate(),
            level = level,
            bonus = bonus,
            points = points,
            cost = cost
        )
    }

    private fun createTurnContext(
        participant: GameParticipant
    ): TurnContext {
        return TurnContext(participant.playerId)
    }

    // ---------- TakeGemsMove ----------

    @Test
    fun `taking three different colors is accepted`() {
        val participant = createParticipant()

        val tokens = GemTokens().apply {
            add(GemColor.WHITE, 4)
            add(GemColor.BLUE, 4)
            add(GemColor.GREEN, 4)
        }

        val table = createTable(tokens = tokens)

        val move = TakeGemsMove(
            listOf(GemColor.WHITE, GemColor.BLUE, GemColor.GREEN)
        )

        val result = validator.validate(
            move, participant, table, createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `taking two different colors is rejected when three colors are available`() {
        val participant = createParticipant()

        val tokens = GemTokens().apply {
            add(GemColor.WHITE, 4)
            add(GemColor.BLUE, 4)
            add(GemColor.GREEN, 4)
        }

        val table = createTable(tokens = tokens)

        val move = TakeGemsMove(
            listOf(GemColor.WHITE, GemColor.BLUE)
        )

        val result = validator.validate(
            move, participant, table, createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.INVALID_MOVE),
            result
        )
    }

    @Test
    fun `taking two different colors is accepted when fewer than three colors are available`() {
        val participant = createParticipant()

        val tokens = GemTokens().apply {
            add(GemColor.WHITE, 4)
            add(GemColor.BLUE, 4)
        }

        val table = createTable(tokens = tokens)

        val move = TakeGemsMove(
            listOf(GemColor.WHITE, GemColor.BLUE)
        )

        val result = validator.validate(
            move, participant, table, createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `taking gold through TakeGemsMove is rejected`() {
        val participant = createParticipant()
        val table = createTable()

        val move = TakeGemsMove(listOf(GemColor.GOLD))

        val result = validator.validate(
            move, participant, table, createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.INVALID_MOVE),
            result
        )
    }

    // ---------- TakeTwoGemsMove ----------

    @Test
    fun `taking two same colors is accepted when table has at least four`() {
        val participant = createParticipant()

        val tokens = GemTokens().apply {
            add(GemColor.RED, 4)
        }

        val table = createTable(tokens = tokens)

        val result = validator.validate(
            TakeTwoGemsMove(GemColor.RED),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `taking two same colors is rejected when table has fewer than four`() {
        val participant = createParticipant()

        val tokens = GemTokens().apply {
            add(GemColor.RED, 3)
        }

        val table = createTable(tokens = tokens)

        val result = validator.validate(
            TakeTwoGemsMove(GemColor.RED),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.NOT_ENOUGH_TOKENS_ON_TABLE),
            result
        )
    }

    @Test
    fun `taking two gold tokens is rejected`() {
        val participant = createParticipant()
        val table = createTable()

        val result = validator.validate(
            TakeTwoGemsMove(GemColor.GOLD),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.INVALID_MOVE),
            result
        )
    }

    // ---------- ReserveCardMove ----------

    @Test
    fun `reserving an open card is accepted`() {
        val participant = createParticipant()
        val card = createCard()
        val table = createTable(cards = listOf(card))

        val result = validator.validate(
            ReserveCardMove(ReserveCardTarget.OpenCard(card.id)),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `reserving a nonexistent open card is rejected`() {
        val participant = createParticipant()
        val table = createTable()

        val result = validator.validate(
            ReserveCardMove(
                ReserveCardTarget.OpenCard(CardId.generate())
            ),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.CARD_NOT_FOUND),
            result
        )
    }

    @Test
    fun `blind reservation is rejected when deck is empty`() {
        val participant = createParticipant()
        val table = createTable()

        val result = validator.validate(
            ReserveCardMove(
                ReserveCardTarget.BlindCard(CardLevel.I)
            ),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.DECK_EMPTY),
            result
        )
    }

    // ---------- PurchaseCardMove ----------

    @Test
    fun `purchasing affordable card is accepted`() {
        val participant = createParticipant()

        participant.state.addTokens(GemColor.BLUE, 2)

        val card = createCard(
            cost = GemCost(mapOf(GemColor.BLUE to 2))
        )

        val table = createTable(cards = listOf(card))

        val result = validator.validate(
            PurchaseCardMove(card.id),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `purchasing card with gold substitution is accepted`() {
        val participant = createParticipant()

        participant.state.addTokens(GemColor.BLUE, 1)
        participant.state.addTokens(GemColor.GOLD, 1)

        val card = createCard(
            cost = GemCost(mapOf(GemColor.BLUE to 2))
        )

        val table = createTable(cards = listOf(card))

        val result = validator.validate(
            PurchaseCardMove(card.id),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `purchasing unaffordable card is rejected`() {
        val participant = createParticipant()

        participant.state.addTokens(GemColor.BLUE, 1)

        val card = createCard(
            cost = GemCost(mapOf(GemColor.BLUE to 3))
        )

        val table = createTable(cards = listOf(card))

        val result = validator.validate(
            PurchaseCardMove(card.id),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.CANNOT_AFFORD_CARD),
            result
        )
    }

    @Test
    fun `purchasing nonexistent card is rejected`() {
        val participant = createParticipant()
        val table = createTable()

        val result = validator.validate(
            PurchaseCardMove(CardId.generate()),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.CARD_NOT_FOUND),
            result
        )
    }

    // ---------- Turn phase ----------

    @Test
    fun `move is rejected outside player action phase`() {
        val participant = createParticipant()
        val table = createTable()

        val context = createTurnContext(participant)
        context.changePhase(TurnPhase.TOKEN_RETURN)

        val result = validator.validate(
            TakeTwoGemsMove(GemColor.RED),
            participant,
            table,
            context
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.WRONG_PHASE),
            result
        )
    }

    @Test
    fun `reserving a card is rejected when player already has three reserved cards`() {
        val participant = createParticipant()

        repeat(PlayerState.MAX_RESERVED_CARDS) {
            assertTrue(participant.state.reserveCard(createCard()))
        }

        val card = createCard()
        val table = createTable(cards = listOf(card))

        val result = validator.validate(
            ReserveCardMove(ReserveCardTarget.OpenCard(card.id)),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.TOO_MANY_RESERVED_CARDS),
            result
        )
    }

    @Test
    fun `blind reservation is accepted when deck is not empty`() {
        val participant = createParticipant()
        val card = createCard()

        val deck = DevelopmentCardDeck(
            CardLevel.I,
            listOf(card)
        )

        val table = createTable(
            decks = mapOf(CardLevel.I to deck)
        )

        val result = validator.validate(
            ReserveCardMove(
                ReserveCardTarget.BlindCard(CardLevel.I)
            ),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `purchasing card with development card bonus is accepted`() {
        val participant = createParticipant()

        val bonusCard = createCard(
            bonus = GemColor.BLUE
        )

        assertTrue(participant.state.purchaseCard(bonusCard))
        participant.state.addTokens(GemColor.BLUE, 1)

        val cardToBuy = createCard(
            cost = GemCost(mapOf(GemColor.BLUE to 2))
        )

        val table = createTable(cards = listOf(cardToBuy))

        val result = validator.validate(
            PurchaseCardMove(cardToBuy.id),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `purchasing own reserved card is accepted`() {
        val participant = createParticipant()

        val card = createCard(
            cost = GemCost(mapOf(GemColor.RED to 1))
        )

        assertTrue(participant.state.reserveCard(card))
        participant.state.addTokens(GemColor.RED, 1)

        val table = createTable()

        val result = validator.validate(
            PurchaseCardMove(card.id),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
    }

    @Test
    fun `validation does not change tokens or open cards`() {
        val participant = createParticipant()

        val tokens = GemTokens().apply {
            add(GemColor.WHITE, 4)
            add(GemColor.BLUE, 4)
            add(GemColor.GREEN, 4)
        }

        val card = createCard()
        val table = createTable(
            tokens = tokens,
            cards = listOf(card)
        )

        val tokensBefore = GemColor.entries.associateWith {
            table.getTokenAmount(it)
        }
        val cardsBefore = table.getOpenCards(CardLevel.I).toList()

        val result = validator.validate(
            ReserveCardMove(ReserveCardTarget.OpenCard(card.id)),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)

        val tokensAfter = GemColor.entries.associateWith {
            table.getTokenAmount(it)
        }
        val cardsAfter = table.getOpenCards(CardLevel.I)

        assertEquals(tokensBefore, tokensAfter)
        assertEquals(cardsBefore, cardsAfter)
        assertTrue(participant.state.getReservedCards().isEmpty())
    }

    @Test
    fun `validation does not draw card from deck`() {
        val participant = createParticipant()

        val deck = DevelopmentCardDeck(
            CardLevel.I,
            listOf(createCard())
        )

        val table = createTable(
            decks = mapOf(CardLevel.I to deck)
        )

        val cardsBefore = table.getDeckRemainingCount(CardLevel.I)

        val result = validator.validate(
            ReserveCardMove(
                ReserveCardTarget.BlindCard(CardLevel.I)
            ),
            participant,
            table,
            createTurnContext(participant)
        )

        assertEquals(MoveResult.Accepted, result)
        assertEquals(
            cardsBefore,
            table.getDeckRemainingCount(CardLevel.I)
        )
        assertTrue(participant.state.getReservedCards().isEmpty())
    }

    @Test
    fun `move is rejected when turn belongs to another player`() {
        val participant = createParticipant()
        val anotherParticipant = createParticipant()
        val table = createTable()

        val result = validator.validate(
            TakeTwoGemsMove(GemColor.RED),
            participant,
            table,
            createTurnContext(anotherParticipant)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.WRONG_PHASE),
            result
        )
    }
}
