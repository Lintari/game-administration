package splendor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import splendor.domain.components.*
import splendor.domain.identifiers.CardId

class DevelopmentCardTest {

    @Test
    fun `should create valid development card`() {
        val card = DevelopmentCard(
            id = CardId.generate(),
            level = CardLevel.I,
            bonus = GemColor.WHITE,
            points = 1,
            cost = GemCost(mapOf(GemColor.BLUE to 2))
        )

        assertEquals(CardLevel.I, card.level)
        assertEquals(GemColor.WHITE, card.bonus)
        assertEquals(1, card.points)
    }

    @Test
    fun `should reject gold bonus`() {
        assertFailsWith<IllegalArgumentException> {
            DevelopmentCard(
                id = CardId.generate(),
                level = CardLevel.I,
                bonus = GemColor.GOLD,
                points = 0,
                cost = GemCost(emptyMap())
            )
        }
    }

    @Test
    fun `should reject negative points`() {
        assertFailsWith<IllegalArgumentException> {
            DevelopmentCard(
                id = CardId.generate(),
                level = CardLevel.I,
                bonus = GemColor.WHITE,
                points = -1,
                cost = GemCost(emptyMap())
            )
        }
    }
}
