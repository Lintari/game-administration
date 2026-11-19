package splendor.presentation.gui.screens

import splendor.application.services.GameService
import splendor.application.services.PlayerService
import splendor.presentation.gui.components.GuiStyle
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.ListSelectionModel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane

class GameSetupPanel(
    private val playerService: PlayerService,
    private val gameService: GameService,
    private val onOpenGame: (String) -> Unit,
    private val onBack: () -> Unit
) : JPanel(BorderLayout(12, 12)) {
    private val playerList = JList<String>()
    private var playerIds: List<String> = emptyList()
    private val gamesList = javax.swing.DefaultListModel<String>()
    private val gameList = JList(gamesList)
    private var gameIds: List<String> = emptyList()

    init {
        background = GuiStyle.background
        border = javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18)
        playerList.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        val header = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            isOpaque = false
            add(GuiStyle.title("Games"))
            add(GuiStyle.button("Create game", ::createGame))
            add(GuiStyle.button("Open selected", ::openSelected))
            add(GuiStyle.button("Refresh", ::refresh))
            add(GuiStyle.button("Back", onBack))
        }
        add(header, BorderLayout.NORTH)
        val content = JPanel(java.awt.GridLayout(1, 2, 12, 0)).apply {
            isOpaque = false
            add(panel("Select 2–4 players", JScrollPane(playerList)))
            add(panel("Existing games", JScrollPane(gameList)))
        }
        add(content, BorderLayout.CENTER)
        refresh()
    }

    fun refresh() {
        val players = playerService.getPlayerViews()
        playerIds = players.map { it.id }
        playerList.setListData(players.map { "${it.nickname}  ·  ${it.id}" }.toTypedArray())
        gamesList.clear()
        val games = gameService.getGameViews()
        gameIds = games.map { it.id }
        games.forEach { gamesList.addElement("${it.status}  ·  ${it.participants.joinToString(", ") { p -> p.nickname }}  ·  ${it.id}") }
    }

    private fun createGame() {
        val selected = playerList.selectedIndices
        if (selected.size !in 2..4) {
            JOptionPane.showMessageDialog(this, "Select between two and four players.")
            return
        }
        try {
            val game = gameService.createGameFromPlayerIds(selected.map { playerIds[it] })
            refresh()
            onOpenGame(game.id)
        } catch (exception: IllegalArgumentException) {
            JOptionPane.showMessageDialog(this, exception.message, "Could not create game", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun openSelected() {
        val index = gameList.selectedIndex
        if (index in gameIds.indices) onOpenGame(gameIds[index])
    }

    private fun panel(title: String, child: java.awt.Component) = JPanel(BorderLayout(8, 8)).apply {
        background = GuiStyle.surface
        border = javax.swing.BorderFactory.createTitledBorder(title)
        add(child, BorderLayout.CENTER)
    }
}
