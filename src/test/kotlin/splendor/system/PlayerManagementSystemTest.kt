package splendor.system

import kotlin.test.Test
import kotlin.test.assertTrue

import splendor.application.services.GameService
import splendor.application.services.PlayerService
import splendor.domain.gamemodel.GameFactory
import splendor.infrastructure.catalog.DefaultGameContentProvider
import splendor.infrastructure.repositories.InMemoryGameRepository
import splendor.infrastructure.repositories.InMemoryPlayerRepository
import splendor.presentation.console.ConsoleInput
import splendor.presentation.console.ConsoleOutput
import splendor.presentation.console.ConsoleUI
import splendor.presentation.console.GameConsoleMenu

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class PlayerManagementSystemTest {

    private fun createGameMenu(
        repository: InMemoryPlayerRepository,
        input: ConsoleInput,
        output: ConsoleOutput
    ) =
        GameConsoleMenu(
            gameService = GameService(
                gameRepository = InMemoryGameRepository(),
                playerRepository = repository,
                gameFactory = GameFactory()
            ),
            playerRepository = repository,
            contentProvider = DefaultGameContentProvider(),
            input = input,
            output = output,
            gamePlayConsole = splendor.presentation.console.GamePlayConsole(input, output)
        )

    @Test
    fun `user can register and view players`() {
        val input = ByteArrayInputStream(
            """
            1
            1
            Alice
            1
            Bob
            2
            0
            0
            """.trimIndent().toByteArray()
        )

        val outputStream = ByteArrayOutputStream()
        val output = PrintStream(outputStream)

        val repository = InMemoryPlayerRepository()
        val service = PlayerService(repository)

        val consoleInput = ConsoleInput(input)
        val consoleOutput = ConsoleOutput(output)
        val ui = ConsoleUI(
            playerService = service,
            input = consoleInput,
            output = consoleOutput,
            gameMenu = createGameMenu(repository, consoleInput, consoleOutput)
        )

        ui.run()

        val result = outputStream.toString()

        assertTrue(result.contains("Alice"))
        assertTrue(result.contains("Bob"))
        assertTrue(result.contains("Player registered successfully!"))
        assertTrue(result.contains("Registered players:"))

        assertTrue(repository.findAll().size == 2)
    }

    @Test
    fun `user receives error when entering invalid player id`() {
        val input = ByteArrayInputStream(
            """
            1
            3
            not-a-uuid
            0
            0
            """.trimIndent().toByteArray()
        )

        val outputStream = ByteArrayOutputStream()
        val output = PrintStream(outputStream)

        val repository = InMemoryPlayerRepository()
        val service = PlayerService(repository)

        val consoleInput = ConsoleInput(input)
        val consoleOutput = ConsoleOutput(output)
        val ui = ConsoleUI(
            playerService = service,
            input = consoleInput,
            output = consoleOutput,
            gameMenu = createGameMenu(repository, consoleInput, consoleOutput)
        )

        ui.run()

        val result = outputStream.toString()

        assertTrue(result.contains("Invalid player ID format."))
        assertTrue(repository.findAll().isEmpty())
    }
}
