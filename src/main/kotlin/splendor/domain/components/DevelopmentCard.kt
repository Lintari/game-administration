package splendor.domain.components

import splendor.domain.identifiers.CardId

class DevelopmentCard(
    val id: CardId,
    val level: CardLevel,
    val bonus: GemColor,
    val points: Int,
    val cost: GemCost
) {
    init {
        require(bonus != GemColor.GOLD) {
            "Development card bonus cannot be gold"
        }

        require(points >= 0) {
            "Points cannot be negative"
        }
    }
}
