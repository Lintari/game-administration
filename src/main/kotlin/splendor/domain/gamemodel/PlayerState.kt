package splendor.domain.gamemodel

import splendor.domain.components.DevelopmentCard
import splendor.domain.components.GemColor
import splendor.domain.components.GemTokens
import splendor.domain.components.NobleTile
import splendor.domain.identifiers.CardId
import splendor.domain.identifiers.NobleId
import splendor.domain.identifiers.PlayerId

class PlayerState(
    val playerId: PlayerId
) {
    private val tokens = GemTokens()

    private val purchasedCards = mutableListOf<DevelopmentCard>()
    private val reservedCards = mutableListOf<DevelopmentCard>()
    private val nobles = mutableListOf<NobleTile>()

    // --- Tokens ---

    fun getTokens(): GemTokens = tokens.copy()

    fun getTokenAmount(color: GemColor): Int =
        tokens.getAmount(color)

    fun addTokens(color: GemColor, amount: Int) {
        tokens.add(color, amount)
    }

    fun removeTokens(color: GemColor, amount: Int): Boolean =
        tokens.remove(color, amount)

    // --- Cards ---

    fun getPurchasedCards(): List<DevelopmentCard> =
        purchasedCards.toList()

    fun getReservedCards(): List<DevelopmentCard> =
        reservedCards.toList()

    fun reserveCard(card: DevelopmentCard): Boolean {
        if (reservedCards.size >= MAX_RESERVED_CARDS) return false
        if (containsCard(card.id)) return false

        reservedCards.add(card)
        return true
    }

    fun purchaseCard(card: DevelopmentCard): Boolean {
        if (containsCard(card.id)) return false

        purchasedCards.add(card)
        return true
    }

    fun purchaseReservedCard(cardId: CardId): DevelopmentCard? {
        val index = reservedCards.indexOfFirst { it.id == cardId }

        if (index == -1) return null

        val card = reservedCards.removeAt(index)
        purchasedCards.add(card)

        return card
    }

    // --- Nobles ---

    fun getNobles(): List<NobleTile> =
        nobles.toList()

    fun addNoble(noble: NobleTile): Boolean {
        if (nobles.any { it.id == noble.id }) return false

        nobles.add(noble)
        return true
    }

    // --- Calculated properties ---

    fun getBonuses(): Map<GemColor, Int> {
        return GemColor.entries
            .filter { it != GemColor.GOLD }
            .associateWith { color ->
                purchasedCards.count { it.bonus == color }
            }
    }

    fun getPrestigePoints(): Int {
        val cardPoints = purchasedCards.sumOf { it.points }
        val noblePoints = nobles.sumOf { it.points }

        return cardPoints + noblePoints
    }

    private fun containsCard(cardId: CardId): Boolean {
        return purchasedCards.any { it.id == cardId } ||
            reservedCards.any { it.id == cardId }
    }

    companion object {
        const val MAX_RESERVED_CARDS = 3
    }
}
