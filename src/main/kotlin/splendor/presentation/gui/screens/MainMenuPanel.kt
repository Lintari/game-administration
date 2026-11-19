package splendor.presentation.gui.screens

import splendor.presentation.gui.components.GuiStyle
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class MainMenuPanel(
    onPlayers: () -> Unit,
    onCreateGame: () -> Unit,
    onGames: () -> Unit,
    onStatistics: () -> Unit,
    onHistory: () -> Unit,
    onExit: () -> Unit
) : JPanel(GridBagLayout()) {
    init {
        background = GuiStyle.background
        val content = JPanel(GridBagLayout()).apply { background = GuiStyle.background }
        val c = GridBagConstraints().apply {
            gridx = 0; fill = GridBagConstraints.HORIZONTAL
            insets = Insets(8, 8, 8, 8); weightx = 1.0
        }
        c.gridy = 0
        content.add(GuiStyle.title("Splendor Manager"), c)
        c.gridy++
        content.add(JLabel("Manage players, start a game, and review results."), c)
        val actions = listOf(
            "Manage players" to onPlayers,
            "Create a new game" to onCreateGame,
            "View games" to onGames,
            "Player statistics" to onStatistics,
            "Completed game history" to onHistory,
            "Exit" to onExit
        )
        actions.forEach { (label, action) ->
            c.gridy++
            content.add(GuiStyle.button(label, action).apply { preferredSize = java.awt.Dimension(280, 38) }, c)
        }
        add(content)
    }
}
