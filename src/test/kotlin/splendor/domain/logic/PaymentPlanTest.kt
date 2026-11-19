package splendor.domain.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import splendor.domain.components.*

class PaymentPlanTest {

    @Test
    fun `calculates payment using colored tokens`() {
        val cost = GemCost(
            mapOf(GemColor.BLUE to 2, GemColor.RED to 1)
        )

        val tokens = GemTokens().apply {
            add(GemColor.BLUE, 2)
            add(GemColor.RED, 1)
        }

        val plan = PaymentPlan.calculate(cost, emptyMap(), tokens)

        assertNotNull(plan)
        assertEquals(2, plan!!.getAmount(GemColor.BLUE))
        assertEquals(1, plan.getAmount(GemColor.RED))
        assertEquals(3, plan.getTotal())
    }

    @Test
    fun `uses bonuses to reduce payment`() {
        val cost = GemCost(mapOf(GemColor.BLUE to 3))

        val tokens = GemTokens().apply {
            add(GemColor.BLUE, 2)
        }

        val bonuses = mapOf(GemColor.BLUE to 1)

        val plan = PaymentPlan.calculate(cost, bonuses, tokens)

        assertNotNull(plan)
        assertEquals(2, plan!!.getAmount(GemColor.BLUE))
        assertEquals(0, plan.getAmount(GemColor.GOLD))
    }

    @Test
    fun `uses gold to cover missing colored tokens`() {
        val cost = GemCost(mapOf(GemColor.BLUE to 3))

        val tokens = GemTokens().apply {
            add(GemColor.BLUE, 2)
            add(GemColor.GOLD, 1)
        }

        val plan = PaymentPlan.calculate(cost, emptyMap(), tokens)

        assertNotNull(plan)
        assertEquals(2, plan!!.getAmount(GemColor.BLUE))
        assertEquals(1, plan.getAmount(GemColor.GOLD))
    }

    @Test
    fun `returns null when player cannot afford card`() {
        val cost = GemCost(mapOf(GemColor.BLUE to 3))

        val tokens = GemTokens().apply {
            add(GemColor.BLUE, 1)
            add(GemColor.GOLD, 1)
        }

        val plan = PaymentPlan.calculate(cost, emptyMap(), tokens)

        assertNull(plan)
    }

    @Test
    fun `does not spend tokens when calculating payment`() {
        val cost = GemCost(mapOf(GemColor.BLUE to 2))

        val tokens = GemTokens().apply {
            add(GemColor.BLUE, 2)
        }

        PaymentPlan.calculate(cost, emptyMap(), tokens)

        assertEquals(2, tokens.getAmount(GemColor.BLUE))
    }

    @Test
    fun `does not spend tokens when bonuses cover entire cost`() {
        val cost = GemCost(mapOf(GemColor.BLUE to 2))

        val tokens = GemTokens()

        val plan = PaymentPlan.calculate(
            cost,
            mapOf(GemColor.BLUE to 2),
            tokens
        )

        assertNotNull(plan)
        assertEquals(0, plan!!.getTotal())
    }
}
