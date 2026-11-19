package splendor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import splendor.domain.components.GemColor
import splendor.domain.components.GemCost
import splendor.domain.components.NobleTile
import splendor.domain.identifiers.NobleId

class NobleTileTest {

    @Test
    fun `should create noble tile worth three points`() {
        val noble = NobleTile(
            id = NobleId.generate(),
            requirements = GemCost(
                mapOf(
                    GemColor.WHITE to 3,
                    GemColor.BLUE to 3,
                    GemColor.GREEN to 3
                )
            )
        )

        assertEquals(3, noble.points)
        assertEquals(3, noble.requirements.getAmount(GemColor.WHITE))
    }
}
