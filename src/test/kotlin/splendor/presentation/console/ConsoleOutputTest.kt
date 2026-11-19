package splendor.presentation.console

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertContains

class ConsoleOutputTest {

    private fun output(): Pair<ConsoleOutput, ByteArrayOutputStream> {
        val stream = ByteArrayOutputStream()
        val printStream = PrintStream(stream)

        return ConsoleOutput(printStream) to stream
    }

    @Test
    fun `show main menu prints available actions`() {
        val (output, stream) = output()

        output.showMainMenu()

        val result = stream.toString()

        assertContains(result, "SPLENDOR MANAGER")
        assertContains(result, "Manage players")
        assertContains(result, "Manage games")
        assertContains(result, "Exit")
    }

    @Test
    fun `show player menu prints available actions`() {
        val (output, stream) = output()

        output.showPlayerMenu()

        val result = stream.toString()

        assertContains(result, "Register player")
        assertContains(result, "Show all players")
        assertContains(result, "Find player by ID")
    }

    @Test
    fun `show message prints text`() {
        val (output, stream) = output()

        output.showMessage("Test message")

        assertContains(stream.toString(), "Test message")
    }

    @Test
    fun `show error adds error prefix`() {
        val (output, stream) = output()

        output.showError("Something went wrong")

        assertContains(stream.toString(), "Error: Something went wrong")
    }

    @Test
    fun `show players handles empty list`() {
        val (output, stream) = output()

        output.showPlayers(emptyList())

        assertContains(stream.toString(), "No registered players.")
    }
}
