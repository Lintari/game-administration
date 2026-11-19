package splendor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import splendor.domain.components.GemColor
import splendor.domain.components.GemTokens
import splendor.domain.gamemodel.TurnContext
import splendor.domain.gamemodel.TurnPhase
import splendor.domain.identifiers.PlayerId

class TurnContextTest {

    @Test
    fun `should initialize with player action phase`() {
        val context = TurnContext(PlayerId.generate())

        assertEquals(TurnPhase.PLAYER_ACTION, context.phase)
    }

    @Test
    fun `should record acquired tokens`() {
        val context = TurnContext(PlayerId.generate())

        context.recordAcquiredTokens(GemColor.WHITE, 2)
        context.recordAcquiredTokens(GemColor.BLUE, 1)

        assertEquals(2, context.getAcquiredAmount(GemColor.WHITE))
        assertEquals(1, context.getAcquiredAmount(GemColor.BLUE))
    }

    @Test
    fun `should remove returned tokens from acquired tokens`() {
        val context = TurnContext(PlayerId.generate())

        context.recordAcquiredTokens(GemColor.RED, 3)

        val removed = context.removeAcquiredTokens(GemColor.RED, 2)

        assertEquals(true, removed)
        assertEquals(1, context.getAcquiredAmount(GemColor.RED))
    }

    @Test
    fun `should initialize with provided acquired tokens`() {
        val tokens = GemTokens().apply {
            add(GemColor.GREEN, 2)
        }

        val context = TurnContext(
            playerId = PlayerId.generate(),
            acquiredTokens = tokens
        )

        tokens.remove(GemColor.GREEN, 2)

        assertEquals(2, context.getAcquiredAmount(GemColor.GREEN))
    }

    @Test
    fun `should allow changing phase`() {
        val context = TurnContext(PlayerId.generate())

        context.changePhase(TurnPhase.TOKEN_RETURN)

        assertEquals(TurnPhase.TOKEN_RETURN, context.phase)
    }

    @Test
    fun `should not allow changing phase after completion`() {
        val context = TurnContext(PlayerId.generate())

        context.changePhase(TurnPhase.COMPLETED)

        assertFailsWith<IllegalArgumentException> {
            context.changePhase(TurnPhase.PLAYER_ACTION)
        }
    }

    @Test
    fun `should not expose mutable acquired tokens`() {
        val context = TurnContext(PlayerId.generate())

        context.recordAcquiredTokens(GemColor.WHITE, 3)

        val tokens = context.getAcquiredTokens()
        tokens.remove(GemColor.WHITE, 3)

        assertEquals(3, context.getAcquiredAmount(GemColor.WHITE))
    }
}
