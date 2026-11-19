package splendor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import splendor.domain.components.*
import splendor.domain.gamemodel.PlayerState
import splendor.domain.identifiers.*

class PlayerStateTest {

    private fun createCard(
        bonus: GemColor = GemColor.WHITE,
        points: Int = 0
    ) = DevelopmentCard(
        id = CardId.generate(),
        level = CardLevel.I,
        bonus = bonus,
        points = points,
        cost = GemCost(emptyMap())
    )

    private fun createNoble() = NobleTile(
        id = NobleId.generate(),
        requirements = GemCost(emptyMap())
    )

    private fun createPlayerState() =
        PlayerState(PlayerId.generate())

    @Test
    fun `should initialize with empty state`() {
        val state = createPlayerState()

        assertEquals(0, state.getPrestigePoints())
        assertEquals(0, state.getTokenAmount(GemColor.WHITE))
        assertTrue(state.getPurchasedCards().isEmpty())
        assertTrue(state.getReservedCards().isEmpty())
        assertTrue(state.getNobles().isEmpty())
    }

    @Test
    fun `should add and remove tokens`() {
        val state = createPlayerState()

        state.addTokens(GemColor.BLUE, 4)

        assertEquals(4, state.getTokenAmount(GemColor.BLUE))
        assertTrue(state.removeTokens(GemColor.BLUE, 2))
        assertEquals(2, state.getTokenAmount(GemColor.BLUE))
    }

    @Test
    fun `should not remove unavailable tokens`() {
        val state = createPlayerState()

        state.addTokens(GemColor.RED, 2)

        assertFalse(state.removeTokens(GemColor.RED, 3))
        assertEquals(2, state.getTokenAmount(GemColor.RED))
    }

    @Test
    fun `should reserve card`() {
        val state = createPlayerState()
        val card = createCard()

        assertTrue(state.reserveCard(card))
        assertEquals(1, state.getReservedCards().size)
    }

    @Test
    fun `should not reserve more than three cards`() {
        val state = createPlayerState()

        repeat(3) {
            assertTrue(state.reserveCard(createCard()))
        }

        assertFalse(state.reserveCard(createCard()))
        assertEquals(3, state.getReservedCards().size)
    }

    @Test
    fun `should not reserve same card twice`() {
        val state = createPlayerState()
        val card = createCard()

        assertTrue(state.reserveCard(card))
        assertFalse(state.reserveCard(card))
    }

    @Test
    fun `should purchase card`() {
        val state = createPlayerState()
        val card = createCard(points = 2)

        assertTrue(state.purchaseCard(card))
        assertEquals(2, state.getPrestigePoints())
        assertEquals(1, state.getPurchasedCards().size)
    }

    @Test
    fun `should move reserved card to purchased cards`() {
        val state = createPlayerState()
        val card = createCard(points = 1)

        state.reserveCard(card)

        val purchased = state.purchaseReservedCard(card.id)

        assertNotNull(purchased)
        assertEquals(0, state.getReservedCards().size)
        assertEquals(1, state.getPurchasedCards().size)
        assertEquals(1, state.getPrestigePoints())
    }

    @Test
    fun `should return null when purchasing unknown reserved card`() {
        val state = createPlayerState()

        assertNull(state.purchaseReservedCard(CardId.generate()))
    }

    @Test
    fun `should calculate bonuses from purchased cards`() {
        val state = createPlayerState()

        state.purchaseCard(createCard(bonus = GemColor.WHITE))
        state.purchaseCard(createCard(bonus = GemColor.WHITE))
        state.purchaseCard(createCard(bonus = GemColor.BLUE))

        val bonuses = state.getBonuses()

        assertEquals(2, bonuses.getValue(GemColor.WHITE))
        assertEquals(1, bonuses.getValue(GemColor.BLUE))
        assertEquals(0, bonuses.getValue(GemColor.RED))
    }

    @Test
    fun `should not count reserved cards as bonuses`() {
        val state = createPlayerState()

        state.reserveCard(createCard(bonus = GemColor.GREEN))

        assertEquals(0, state.getBonuses().getValue(GemColor.GREEN))
    }

    @Test
    fun `should calculate prestige points from cards and nobles`() {
        val state = createPlayerState()

        state.purchaseCard(createCard(points = 2))
        state.purchaseCard(createCard(points = 1))
        state.addNoble(createNoble())

        assertEquals(6, state.getPrestigePoints())
    }

    @Test
    fun `should not add same noble twice`() {
        val state = createPlayerState()
        val noble = createNoble()

        assertTrue(state.addNoble(noble))
        assertFalse(state.addNoble(noble))
        assertEquals(1, state.getNobles().size)
    }

    @Test
    fun `should not expose mutable internal collections`() {
        val state = createPlayerState()
        state.purchaseCard(createCard())

        val cards = state.getPurchasedCards()

        assertEquals(1, cards.size)
        assertEquals(1, state.getPurchasedCards().size)
    }

    @Test
    fun `should not expose internal tokens`() {
        val state = createPlayerState()
        state.addTokens(GemColor.WHITE, 3)

        val tokens = state.getTokens()
        tokens.remove(GemColor.WHITE, 3)

        assertEquals(3, state.getTokenAmount(GemColor.WHITE))
    }
}
