package splendor.presentation.gui.screens

import splendor.application.services.PlayerService
import splendor.presentation.gui.components.GuiStyle
import splendor.presentation.gui.components.PlayerInfoPanel
import splendor.presentation.gui.components.PlayerTable
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField

class PlayerManagementPanel(
    private val playerService: PlayerService,
    private val onBack: () -> Unit
) : JPanel(BorderLayout(12, 12)) {
    private val table = PlayerTable()
    private val details = PlayerInfoPanel()
    private val search = JTextField(18)

    init {
        background = GuiStyle.background
        border = javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18)
        val header = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            isOpaque = false
            add(GuiStyle.title("Players"))
            add(search)
            add(GuiStyle.button("Search", ::refresh))
            add(GuiStyle.button("Clear", { search.text = ""; refresh() }))
            add(GuiStyle.button("Add player", ::addPlayer))
            add(GuiStyle.button("Back", onBack))
        }
        add(header, BorderLayout.NORTH)
        val split = javax.swing.JSplitPane(
            javax.swing.JSplitPane.HORIZONTAL_SPLIT,
            JScrollPane(table),
            details
        ).apply { resizeWeight = 0.68; border = null }
        add(split, BorderLayout.CENTER)
        table.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) showSelected()
        }
        refresh()
    }

    fun refresh() {
        val filter = search.text.trim()
        table.showPlayers(playerService.getPlayerViews().filter {
            filter.isEmpty() || it.nickname.contains(filter, ignoreCase = true) ||
                it.id.contains(filter, ignoreCase = true)
        })
        showSelected()
    }

    private fun showSelected() {
        val id = table.selectedPlayerId()
        val player = id?.let { key ->
            playerService.getPlayerViews().firstOrNull { it.id == key }
        }
        details.showPlayer(player)
    }

    private fun addPlayer() {
        val nickname = JOptionPane.showInputDialog(this, "Player nickname:") ?: return
        try {
            val created = playerService.registerPlayer(nickname)
            refresh()
            JOptionPane.showMessageDialog(this, "Registered ${created.nickname}")
        } catch (exception: IllegalArgumentException) {
            JOptionPane.showMessageDialog(this, exception.message, "Could not register player", JOptionPane.ERROR_MESSAGE)
        }
    }
}
