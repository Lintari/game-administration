package splendor.presentation.console

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConsoleInputTest {

    private fun inputOf(text: String) =
        ConsoleInput(ByteArrayInputStream(text.toByteArray()))

    @Test
    fun `read text trims whitespace`() {
        val input = inputOf("  Alice  \n")

        assertEquals("Alice", input.readText())
    }

    @Test
    fun `read integer parses valid number`() {
        val input = inputOf("42\n")

        assertEquals(42, input.readInteger())
    }

    @Test
    fun `read integer returns null for invalid input`() {
        val input = inputOf("abc\n")

        assertNull(input.readInteger())
    }

    @Test
    fun `read choice parses integer`() {
        val input = inputOf("2\n")

        assertEquals(2, input.readChoice())
    }

    @Test
    fun `read text returns null at end of input`() {
        val input = inputOf("")

        assertNull(input.readText())
    }
}
