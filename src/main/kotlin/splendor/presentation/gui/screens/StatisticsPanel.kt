package splendor.presentation.gui.screens

import splendor.application.services.StatisticsService
import splendor.presentation.gui.components.GuiStyle
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class StatisticsPanel(
    private val statisticsService: StatisticsService,
    private val onBack: () -> Unit
) : JPanel(BorderLayout(10, 10)) {
    private val model = object : DefaultTableModel(arrayOf("Player", "Games played", "Wins", "Win rate", "Total points"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    init {
        background = GuiStyle.background
        border = javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18)
        val header = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            isOpaque = false
            add(GuiStyle.title("Player statistics"))
            add(GuiStyle.button("Refresh", ::refresh))
            add(GuiStyle.button("Back", onBack))
        }
        add(header, BorderLayout.NORTH)
        add(JScrollPane(JTable(model).apply { rowHeight = 26; fillsViewportHeight = true }), BorderLayout.CENTER)
        refresh()
    }

    fun refresh() {
        model.rowCount = 0
        statisticsService.getPlayerStatistics().forEach {
            model.addRow(arrayOf<Any>(it.nickname, it.gamesPlayed, it.wins, "%.1f%%".format(it.winPercentage), it.totalPoints))
        }
    }
}
