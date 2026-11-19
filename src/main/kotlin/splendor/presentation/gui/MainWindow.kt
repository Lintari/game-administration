package splendor.presentation.gui

import splendor.application.services.GameService
import splendor.application.services.PlayerService
import splendor.application.services.StatisticsService
import splendor.presentation.gui.components.GuiStyle
import splendor.presentation.gui.screens.GameBoardPanel
import splendor.presentation.gui.screens.GameHistoryPanel
import splendor.presentation.gui.screens.GameSetupPanel
import splendor.presentation.gui.screens.MainMenuPanel
import splendor.presentation.gui.screens.PlayerManagementPanel
import splendor.presentation.gui.screens.StatisticsPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.WindowConstants

class MainWindow(
    private val playerService: PlayerService,
    private val gameService: GameService,
    private val statisticsService: StatisticsService
) : JFrame("Splendor Manager") {
    private val cardLayout = CardLayout()
    private val screens = JPanel(cardLayout)
    private lateinit var playersPanel: PlayerManagementPanel
    private lateinit var gamesPanel: GameSetupPanel
    private lateinit var statisticsPanel: StatisticsPanel
    private lateinit var historyPanel: GameHistoryPanel
    private lateinit var gameBoardPanel: GameBoardPanel

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        minimumSize = Dimension(1100, 720)
        size = Dimension(1360, 860)
        setLocationRelativeTo(null)
        contentPane.background = GuiStyle.background
        contentPane.layout = BorderLayout()
        contentPane.add(screens, BorderLayout.CENTER)
        buildScreens()
        showScreen(MENU)
    }

    private fun buildScreens() {
        screens.add(MainMenuPanel(
            onPlayers = { playersPanel.refresh(); showScreen(PLAYERS) },
            onCreateGame = { gamesPanel.refresh(); showScreen(GAMES) },
            onGames = { gamesPanel.refresh(); showScreen(GAMES) },
            onStatistics = { statisticsPanel.refresh(); showScreen(STATISTICS) },
            onHistory = { historyPanel.refresh(); showScreen(HISTORY) },
            onExit = { dispose() }
        ), MENU)

        playersPanel = PlayerManagementPanel(playerService) { showScreen(MENU) }
        screens.add(playersPanel, PLAYERS)

        gameBoardPanel = GameBoardPanel(gameService) {
            gamesPanel.refresh()
            showScreen(GAMES)
        }
        gamesPanel = GameSetupPanel(playerService, gameService, { gameId ->
            gameBoardPanel.showGame(gameId)
            gameBoardPanel.refresh()
            showScreen(BOARD)
        }) { showScreen(MENU) }
        screens.add(gamesPanel, GAMES)
        screens.add(gameBoardPanel, BOARD)

        statisticsPanel = StatisticsPanel(statisticsService) { showScreen(MENU) }
        screens.add(statisticsPanel, STATISTICS)
        historyPanel = GameHistoryPanel(gameService) { showScreen(MENU) }
        screens.add(historyPanel, HISTORY)
    }

    private fun showScreen(name: String) {
        cardLayout.show(screens, name)
    }

    companion object {
        private const val MENU = "menu"
        private const val PLAYERS = "players"
        private const val GAMES = "games"
        private const val BOARD = "board"
        private const val STATISTICS = "statistics"
        private const val HISTORY = "history"
    }
}
