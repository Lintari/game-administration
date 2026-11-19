package splendor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import splendor.domain.components.*
import splendor.domain.gamemodel.Table
import splendor.domain.identifiers.CardId
import splendor.domain.identifiers.NobleId

class TableTest {

    private fun createCard(level: CardLevel): DevelopmentCard {
        return DevelopmentCard(
            id = CardId.generate(),
            level = level,
            bonus = GemColor.WHITE,
            points = 0,
            cost = GemCost(emptyMap())
        )
    }

    private fun createTable(
        initialTokens: GemTokens = GemTokens(),
        openCards: Map<CardLevel, List<DevelopmentCard>> =
            CardLevel.entries.associateWith { emptyList() },
        decks: Map<CardLevel, DevelopmentCardDeck> =
            CardLevel.entries.associateWith {
                DevelopmentCardDeck(it, emptyList())
            },
        nobles: List<NobleTile> = emptyList()
    ): Table {
        return Table(initialTokens, openCards, decks, nobles)
    }

    @Test
    fun `should initialize token supply`() {
        val tokens = GemTokens().apply {
            add(GemColor.WHITE, 5)
            add(GemColor.GOLD, 3)
        }

        val table = createTable(initialTokens = tokens)

        assertEquals(5, table.getTokenAmount(GemColor.WHITE))
        assertEquals(3, table.getTokenAmount(GemColor.GOLD))
    }

    @Test
    fun `should take and return tokens`() {
        val tokens = GemTokens().apply {
            add(GemColor.BLUE, 5)
        }

        val table = createTable(initialTokens = tokens)

        assertTrue(table.takeTokens(GemColor.BLUE, 2))
        assertEquals(3, table.getTokenAmount(GemColor.BLUE))

        table.returnTokens(GemColor.BLUE, 1)

        assertEquals(4, table.getTokenAmount(GemColor.BLUE))
    }

    @Test
    fun `should not allow taking more tokens than available`() {
        val tokens = GemTokens().apply {
            add(GemColor.RED, 2)
        }

        val table = createTable(initialTokens = tokens)

        assertTrue(!table.takeTokens(GemColor.RED, 3))
        assertEquals(2, table.getTokenAmount(GemColor.RED))
    }

    @Test
    fun `should replace taken open card`() {
        val firstCard = createCard(CardLevel.I)
        val replacement = createCard(CardLevel.I)

        val decks = CardLevel.entries.associateWith { level ->
            DevelopmentCardDeck(
                level,
                if (level == CardLevel.I) listOf(replacement) else emptyList()
            )
        }

        val table = createTable(
            openCards = mapOf(CardLevel.I to listOf(firstCard)),
            decks = decks
        )

        val takenCard = table.takeOpenCard(firstCard.id)

        assertEquals(firstCard.id, takenCard?.id)

        val remainingCards = table.getOpenCards(CardLevel.I)

        assertEquals(1, remainingCards.size)
        assertEquals(replacement.id, remainingCards.first().id)
    }

    @Test
    fun `should leave empty slot when deck is empty`() {
        val card = createCard(CardLevel.I)

        val table = createTable(
            openCards = mapOf(CardLevel.I to listOf(card))
        )

        table.takeOpenCard(card.id)

        assertTrue(table.getOpenCards(CardLevel.I).isEmpty())
    }

    @Test
    fun `should return null when card is not on table`() {
        val table = createTable()

        assertNull(table.takeOpenCard(CardId.generate()))
    }

    @Test
    fun `should take noble from table`() {
        val noble = NobleTile(
            id = NobleId.generate(),
            requirements = GemCost(emptyMap())
        )

        val table = createTable(nobles = listOf(noble))

        val takenNoble = table.takeNoble(noble.id)

        assertNotNull(takenNoble)
        assertEquals(noble.id, takenNoble.id)
        assertTrue(table.getNobles().isEmpty())
    }

    @Test
    fun `should not expose mutable internal collections`() {
        val card = createCard(CardLevel.I)
        val table = createTable(
            openCards = mapOf(CardLevel.I to listOf(card))
        )

        val cards = table.getOpenCards(CardLevel.I)

        assertEquals(1, cards.size)
        // Изменение полученного списка не должно менять состояние стола.
        // toList() возвращает отдельную коллекцию.
        assertEquals(1, table.getOpenCards(CardLevel.I).size)
    }

    @Test
    fun `should copy initial token supply`() {
        val tokens = GemTokens().apply {
            add(GemColor.GREEN, 4)
        }

        val table = createTable(initialTokens = tokens)

        tokens.remove(GemColor.GREEN, 2)

        assertEquals(4, table.getTokenAmount(GemColor.GREEN))
    }
}
