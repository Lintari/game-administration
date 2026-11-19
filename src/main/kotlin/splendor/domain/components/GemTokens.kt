package splendor.domain.components

class GemTokens {

    private val amounts = GemColor.entries.associateWith { 0 }.toMutableMap()

    fun getAmount(color: GemColor): Int {
        return amounts.getValue(color)
    }

    fun total(): Int {
        return amounts.values.sum()
    }

    fun add(color: GemColor, amount: Int) {
        require(amount > 0) {
            "Amount must be positive"
        }

        amounts[color] = getAmount(color) + amount
    }

    fun remove(color: GemColor, amount: Int): Boolean {
        require(amount > 0) {
            "Amount must be positive"
        }

        val currentAmount = getAmount(color)

        if (currentAmount < amount) {
            return false
        }

        amounts[color] = currentAmount - amount
        return true
    }


    fun copy(): GemTokens {
        val copy = GemTokens()

        for (color in GemColor.entries) {
            val amount = getAmount(color)
            if (amount > 0) {
                copy.add(color, amount)
            }
        }

        return copy
    }
}
