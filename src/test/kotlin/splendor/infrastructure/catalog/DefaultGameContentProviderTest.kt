package splendor.infrastructure.catalog

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import splendor.domain.components.CardLevel
import splendor.domain.components.GemColor

class DefaultGameContentProviderTest {

    private val provider = DefaultGameContentProvider()

    @Test
    fun `should provide all 90 development cards`() {
        val cards = provider.getCards()

        assertEquals(40, cards.getValue(CardLevel.I).size)
        assertEquals(30, cards.getValue(CardLevel.II).size)
        assertEquals(20, cards.getValue(CardLevel.III).size)
        assertEquals(90, cards.values.sumOf { it.size })
    }

    @Test
    fun `should provide 10 noble tiles`() {
        assertEquals(10, provider.getNobles().size)
    }

    @Test
    fun `cards should not have gold bonus`() {
        val cards = provider.getCards().values.flatten()

        assertTrue(cards.none { it.bonus == GemColor.GOLD })
    }
}
