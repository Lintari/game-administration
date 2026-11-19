package splendor.entrypoint

import splendor.application.services.GameService
import splendor.application.services.PlayerService
import splendor.application.services.StatisticsService
import splendor.domain.gamemodel.GameFactory
import splendor.infrastructure.catalog.DefaultGameContentProvider
import splendor.infrastructure.repositories.InMemoryGameRepository
import splendor.infrastructure.repositories.InMemoryPlayerRepository
import splendor.presentation.console.ConsoleInput
import splendor.presentation.console.ConsoleOutput
import splendor.presentation.console.ConsoleUI
import splendor.presentation.console.GameConsoleMenu
import splendor.presentation.console.GamePlayConsole
import splendor.presentation.gui.MainWindow
import javax.swing.SwingUtilities

class GameApplication {

    private val playerRepository = InMemoryPlayerRepository()
    private val gameRepository = InMemoryGameRepository()
    private val gameContentProvider = DefaultGameContentProvider()

    private val playerService = PlayerService(playerRepository)
    private val gameService = GameService(
        gameRepository = gameRepository,
        playerRepository = playerRepository,
        gameFactory = GameFactory(),
        contentProvider = gameContentProvider
    )
    private val statisticsService = StatisticsService(playerRepository, gameRepository)

    private lateinit var ui: ConsoleUI

    private fun configureDependencies() {
        val input = ConsoleInput()
        val output = ConsoleOutput()
        val gamePlayConsole = GamePlayConsole(input, output)
        val gameMenu = GameConsoleMenu(
            gameService = gameService,
            playerRepository = playerRepository,
            contentProvider = gameContentProvider,
            input = input,
            output = output,
            gamePlayConsole = gamePlayConsole
        )
        ui = ConsoleUI(playerService, input, output, gameMenu)
    }

    fun start() {
        configureDependencies()
        ui.run()
    }

    fun startGui() {
        SwingUtilities.invokeLater {
            MainWindow(playerService, gameService, statisticsService).isVisible = true
        }
    }
}
