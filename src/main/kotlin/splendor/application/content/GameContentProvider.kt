package splendor.application.content

import splendor.domain.components.CardLevel
import splendor.domain.components.DevelopmentCard
import splendor.domain.components.NobleTile

interface GameContentProvider {
    fun getCards(): Map<CardLevel, List<DevelopmentCard>>
    fun getNobles(): List<NobleTile>
}
