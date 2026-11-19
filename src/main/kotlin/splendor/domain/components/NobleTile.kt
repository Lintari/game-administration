package splendor.domain.components

import splendor.domain.identifiers.NobleId

class NobleTile(
    val id: NobleId,
    val requirements: GemCost
) {
    val points: Int = 3
}
