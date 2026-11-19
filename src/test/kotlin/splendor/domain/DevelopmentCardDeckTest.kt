package splendor.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import splendor.domain.components.*
import splendor.domain.identifiers.CardId

class DevelopmentCardDeckTest {

    private fun createCard(level: CardLevel): DevelopmentCard {
        return DevelopmentCard(
            id = CardId.generate(),
            level = level,
            bonus = GemColor.WHITE,
            points = 0,
            cost = GemCost(emptyMap())
        )
    }

    @Test
    fun `should draw card from deck`() {
        val cards = listOf(
            createCard(CardLevel.I),
            createCard(CardLevel.I)
        )

        val deck = DevelopmentCardDeck(
            level = CardLevel.I,
            cards = cards,
            random = Random(42)
        )

        val drawnCard = deck.draw()

        assertNotNull(drawnCard)
        assertEquals(1, deck.remainingCount())
    }

    @Test
    fun `should return null when deck is empty`() {
        val deck = DevelopmentCardDeck(
            level = CardLevel.I,
            cards = emptyList()
        )

        assertNull(deck.draw())
        assertTrue(deck.isEmpty())
    }

    @Test
    fun `should remove drawn cards from deck`() {
        val cards = listOf(
            createCard(CardLevel.I),
            createCard(CardLevel.I),
            createCard(CardLevel.I)
        )

        val deck = DevelopmentCardDeck(
            level = CardLevel.I,
            cards = cards,
            random = Random(42)
        )

        deck.draw()
        deck.draw()

        assertEquals(1, deck.remainingCount())
    }

    @Test
    fun `should reject cards with different levels`() {
        val cards = listOf(
            createCard(CardLevel.I),
            createCard(CardLevel.II)
        )

        assertFailsWith<IllegalArgumentException> {
            DevelopmentCardDeck(
                level = CardLevel.I,
                cards = cards
            )
        }
    }
}
