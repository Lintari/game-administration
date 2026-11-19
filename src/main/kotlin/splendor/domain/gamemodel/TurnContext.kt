package splendor.domain.gamemodel

import splendor.domain.components.GemColor
import splendor.domain.components.GemTokens
import splendor.domain.identifiers.PlayerId

class TurnContext(
    val playerId: PlayerId,
    initialPhase: TurnPhase = TurnPhase.PLAYER_ACTION,
    acquiredTokens: GemTokens = GemTokens()
) {
    var phase: TurnPhase = initialPhase
        private set

    private val acquiredTokens = acquiredTokens.copy()

    fun getAcquiredTokens(): GemTokens =
        acquiredTokens.copy()

    fun getAcquiredAmount(color: GemColor): Int =
        acquiredTokens.getAmount(color)

    fun recordAcquiredTokens(color: GemColor, amount: Int) {
        acquiredTokens.add(color, amount)
    }

    fun removeAcquiredTokens(color: GemColor, amount: Int): Boolean =
        acquiredTokens.remove(color, amount)

    fun changePhase(newPhase: TurnPhase) {
        require(phase != TurnPhase.COMPLETED) {
            "Completed turn cannot change its phase"
        }

        phase = newPhase
    }
}
