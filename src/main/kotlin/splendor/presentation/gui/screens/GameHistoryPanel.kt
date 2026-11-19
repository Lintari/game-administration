package splendor.presentation.gui.screens

import splendor.application.services.GameService
import splendor.presentation.gui.components.GuiStyle
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class GameHistoryPanel(
    private val gameService: GameService,
    private val onBack: () -> Unit
) : JPanel(BorderLayout(10, 10)) {
    private val model = object : DefaultTableModel(arrayOf("Game ID", "Participants", "Winners", "Moves", "Status"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    init {
        background = GuiStyle.background
        border = javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18)
        val header = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            isOpaque = false
            add(GuiStyle.title("Game history"))
            add(GuiStyle.button("Refresh", ::refresh))
            add(GuiStyle.button("Back", onBack))
        }
        add(header, BorderLayout.NORTH)
        add(JScrollPane(JTable(model).apply { rowHeight = 26; fillsViewportHeight = true }), BorderLayout.CENTER)
        refresh()
    }

    fun refresh() {
        model.rowCount = 0
        gameService.getGameViews().filter { it.status == "FINISHED" }.forEach { game ->
            model.addRow(arrayOf<Any>(
                game.id,
                game.participants.joinToString(", ") { it.nickname },
                game.winners.joinToString(", "),
                game.history.size,
                game.status
            ))
        }
    }
}
