package splendor.domain.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class GemTokensTest {

    @Test
    fun `all token amounts are initially zero`() {
        val tokens = GemTokens()

        GemColor.entries.forEach { color ->
            assertEquals(0, tokens.getAmount(color))
        }
    }

    @Test
    fun `add increases token amount`() {
        val tokens = GemTokens()

        tokens.add(GemColor.BLUE, 3)

        assertEquals(3, tokens.getAmount(GemColor.BLUE))
    }

    @Test
    fun `total returns sum of all tokens`() {
        val tokens = GemTokens()

        tokens.add(GemColor.BLUE, 3)
        tokens.add(GemColor.RED, 2)
        tokens.add(GemColor.GOLD, 1)

        assertEquals(6, tokens.total())
    }

    @Test
    fun `remove decreases token amount`() {
        val tokens = GemTokens()
        tokens.add(GemColor.GREEN, 4)

        val result = tokens.remove(GemColor.GREEN, 2)

        assertTrue(result)
        assertEquals(2, tokens.getAmount(GemColor.GREEN))
    }

    @Test
    fun `remove returns false when tokens are insufficient`() {
        val tokens = GemTokens()
        tokens.add(GemColor.WHITE, 2)

        val result = tokens.remove(GemColor.WHITE, 3)

        assertFalse(result)
        assertEquals(2, tokens.getAmount(GemColor.WHITE))
    }

    @Test
    fun `add rejects non-positive amount`() {
        val tokens = GemTokens()

        assertFailsWith<IllegalArgumentException> {
            tokens.add(GemColor.BLUE, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            tokens.add(GemColor.BLUE, -1)
        }
    }

    @Test
    fun `remove rejects non-positive amount`() {
        val tokens = GemTokens()

        assertFailsWith<IllegalArgumentException> {
            tokens.remove(GemColor.BLUE, 0)
        }
    }
}
