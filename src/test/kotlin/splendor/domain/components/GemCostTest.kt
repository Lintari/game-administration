package splendor.domain.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GemCostTest {

    @Test
    fun `getAmount returns correct cost`() {
        val cost = GemCost(
            mapOf(
                GemColor.WHITE to 2,
                GemColor.BLUE to 3
            )
        )

        assertEquals(2, cost.getAmount(GemColor.WHITE))
        assertEquals(3, cost.getAmount(GemColor.BLUE))
        assertEquals(0, cost.getAmount(GemColor.RED))
    }

    @Test
    fun `total returns sum of costs`() {
        val cost = GemCost(
            mapOf(
                GemColor.WHITE to 2,
                GemColor.BLUE to 3,
                GemColor.RED to 1
            )
        )

        assertEquals(6, cost.total())
    }

    @Test
    fun `gold cannot be included in cost`() {
        assertFailsWith<IllegalArgumentException> {
            GemCost(mapOf(GemColor.GOLD to 2))
        }
    }

    @Test
    fun `negative cost is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            GemCost(mapOf(GemColor.BLUE to -1))
        }
    }

    @Test
    fun `empty cost is allowed`() {
        val cost = GemCost(emptyMap())

        assertEquals(0, cost.total())
    }
}
