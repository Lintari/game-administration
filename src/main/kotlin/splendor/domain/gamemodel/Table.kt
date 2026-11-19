package splendor.domain.gamemodel

import splendor.domain.components.*
import splendor.domain.identifiers.CardId
import splendor.domain.identifiers.NobleId

class Table(
    tokens: GemTokens,
    openCards: Map<CardLevel, List<DevelopmentCard>>,
    decks: Map<CardLevel, DevelopmentCardDeck>,
    nobles: List<NobleTile>
) {
    private val tokens: GemTokens = tokens.copy()

    private val openCards: MutableMap<CardLevel, MutableList<DevelopmentCard>> =
        CardLevel.entries.associateWith { level ->
            openCards[level]?.toMutableList() ?: mutableListOf()
        }.toMutableMap()

    private val decks: Map<CardLevel, DevelopmentCardDeck> = decks.toMap()

    private val nobles: MutableList<NobleTile> = nobles.toMutableList()

    init {
        require(decks.keys.containsAll(CardLevel.entries)) {
            "A deck must be provided for every card level"
        }

        for (level in CardLevel.entries) {
            require(this.openCards.getValue(level).size <= 4) {
                "There cannot be more than four open cards of one level"
            }

            require(this.openCards.getValue(level).all { it.level == level }) {
                "Open cards must match their level"
            }

            require(decks.getValue(level).level == level) {
                "Deck level does not match its map key"
            }
        }
    }

    fun getTokenAmount(color: GemColor): Int =
        tokens.getAmount(color)

    fun takeTokens(color: GemColor, amount: Int): Boolean =
        tokens.remove(color, amount)

    fun returnTokens(color: GemColor, amount: Int) {
        tokens.add(color, amount)
    }

    fun getOpenCards(level: CardLevel): List<DevelopmentCard> =
        openCards.getValue(level).toList()

    fun drawBlindCard(level: CardLevel): DevelopmentCard? {
        return decks[level]?.draw()
    }

    fun getDeckRemainingCount(level: CardLevel): Int {
        return decks[level]?.remainingCount() ?: 0
    }

    fun findOpenCard(cardId: CardId): DevelopmentCard? {
        return openCards.values
            .asSequence()
            .flatten()
            .firstOrNull { it.id == cardId }
    }

    fun takeOpenCard(cardId: CardId): DevelopmentCard? {
        for (level in CardLevel.entries) {
            val cards = openCards.getValue(level)
            val index = cards.indexOfFirst { it.id == cardId }

            if (index != -1) {
                val card = cards.removeAt(index)
                val replacement = decks.getValue(level).draw()

                if (replacement != null) {
                    cards.add(index, replacement)
                }

                return card
            }
        }

        return null
    }

    fun getNobles(): List<NobleTile> =
        nobles.toList()

    fun takeNoble(nobleId: NobleId): NobleTile? {
        val index = nobles.indexOfFirst { it.id == nobleId }

        if (index == -1) return null

        return nobles.removeAt(index)
    }
}
