package splendor.domain.components

class GemCost(costs: Map<GemColor, Int>) {

    private val amounts = costs.toMap()

    init {
        require(GemColor.GOLD !in amounts) {
            "Gold cannot be included in a cost"
        }

        require(amounts.values.all { it >= 0 }) {
            "Cost cannot contain negative amounts"
        }
    }

    fun getAmount(color: GemColor): Int {
        return amounts[color] ?: 0
    }

    fun total(): Int {
        return amounts.values.sum()
    }
}
