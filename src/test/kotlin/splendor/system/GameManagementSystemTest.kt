package splendor.system

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import splendor.application.services.GameService
import splendor.domain.gamemodel.GameFactory
import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player
import splendor.infrastructure.catalog.DefaultGameContentProvider
import splendor.infrastructure.repositories.InMemoryGameRepository
import splendor.infrastructure.repositories.InMemoryPlayerRepository
import splendor.presentation.console.ConsoleInput
import splendor.presentation.console.ConsoleOutput
import splendor.presentation.console.GameConsoleMenu
import splendor.presentation.console.GamePlayConsole

class GameManagementSystemTest {

    @Test
    fun `user can create a game through console menu`() {
        val playerRepository = InMemoryPlayerRepository()
        val gameRepository = InMemoryGameRepository()

        playerRepository.save(
            Player(PlayerId.generate(), "Alice")
        )
        playerRepository.save(
            Player(PlayerId.generate(), "Bob")
        )

        val gameService = GameService(
            gameRepository = gameRepository,
            playerRepository = playerRepository,
            gameFactory = GameFactory()
        )

        val input = ConsoleInput(
            ByteArrayInputStream("1\n1 2\n0\n".toByteArray())
        )
        val outputBuffer = ByteArrayOutputStream()
        val output = ConsoleOutput(PrintStream(outputBuffer))

        val menu = GameConsoleMenu(
            gameService = gameService,
            playerRepository = playerRepository,
            contentProvider = DefaultGameContentProvider(),
            input = input,
            output = output,
            gamePlayConsole = GamePlayConsole(input, output)
        )

        menu.run()

        val games = gameRepository.findAll()
        val consoleOutput = outputBuffer.toString()

        assertEquals(1, games.size)
        assertEquals(2, games.single().getParticipants().size)

        assertContainsText(consoleOutput, "Game created successfully!")
        assertContainsText(consoleOutput, "Alice")
        assertContainsText(consoleOutput, "Bob")
    }

    private fun assertContainsText(
        actual: String,
        expected: String
    ) {
        assertTrue(
            actual.contains(expected),
            "Expected output to contain '$expected', actual output:\n$actual"
        )
    }
}
