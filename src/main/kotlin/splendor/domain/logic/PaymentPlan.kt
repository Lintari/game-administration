package splendor.domain.logic

import splendor.domain.components.GemColor
import splendor.domain.components.GemCost
import splendor.domain.components.GemTokens

class PaymentPlan private constructor(
    private val payment: Map<GemColor, Int>
) {
    fun getAmount(color: GemColor): Int =
        payment[color] ?: 0

    fun getTotal(): Int =
        payment.values.sum()

    fun getTokensToSpend(): GemTokens {
        val result = GemTokens()

        payment.forEach { (color, amount) ->
            if (amount > 0) {
                result.add(color, amount)
            }
        }

        return result
    }

    companion object {
        fun calculate(
            cost: GemCost,
            bonuses: Map<GemColor, Int>,
            availableTokens: GemTokens
        ): PaymentPlan? {
            val payment = mutableMapOf<GemColor, Int>()
            var goldNeeded = 0

            for (color in GemColor.entries) {
                if (color == GemColor.GOLD) continue

                val remainingCost =
                    (cost.getAmount(color) - (bonuses[color] ?: 0))
                        .coerceAtLeast(0)

                val coloredUsed = minOf(
                    remainingCost,
                    availableTokens.getAmount(color)
                )

                if (coloredUsed > 0) {
                    payment[color] = coloredUsed
                }

                goldNeeded += remainingCost - coloredUsed
            }

            if (goldNeeded > availableTokens.getAmount(GemColor.GOLD)) {
                return null
            }

            if (goldNeeded > 0) {
                payment[GemColor.GOLD] = goldNeeded
            }

            return PaymentPlan(payment)
        }
    }
}
