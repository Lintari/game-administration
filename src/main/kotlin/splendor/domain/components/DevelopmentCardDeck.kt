package splendor.domain.components

import kotlin.random.Random

class DevelopmentCardDeck(
    val level: CardLevel,
    cards: List<DevelopmentCard>,
    random: Random = Random.Default,
    shuffle: Boolean = true
) {
    private val cards: MutableList<DevelopmentCard>

    init {
        require(cards.all { it.level == level }) {
            "All cards in the deck must have the same level"
        }

        this.cards = (if (shuffle) cards.shuffled(random) else cards).toMutableList()
    }

    fun draw(): DevelopmentCard? {
        if (cards.isEmpty()) return null
        return cards.removeAt(0)
    }

    fun isEmpty(): Boolean = cards.isEmpty()

    fun remainingCount(): Int = cards.size

    fun getRemainingCards(): List<DevelopmentCard> = cards.toList()
}
