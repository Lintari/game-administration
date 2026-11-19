package splendor.domain.components

import kotlin.random.Random

class DevelopmentCardDeck(
    val level: CardLevel,
    cards: List<DevelopmentCard>,
    random: Random = Random.Default
) {
    private val cards: MutableList<DevelopmentCard>

    init {
        require(cards.all { it.level == level }) {
            "All cards in the deck must have the same level"
        }

        this.cards = cards.shuffled(random).toMutableList()
    }

    fun draw(): DevelopmentCard? {
        if (cards.isEmpty()) return null
        return cards.removeAt(0)
    }

    fun isEmpty(): Boolean = cards.isEmpty()

    fun remainingCount(): Int = cards.size
}
