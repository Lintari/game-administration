package splendor.presentation.gui.components

import splendor.application.dto.PlayerView
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

class PlayerTable : JTable() {
    private val model = object : DefaultTableModel(arrayOf("Nickname", "ID"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    init {
        setModel(model)
        rowHeight = 26
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        fillsViewportHeight = true
    }

    fun showPlayers(players: List<PlayerView>) {
        model.rowCount = 0
        players.forEach { model.addRow(arrayOf(it.nickname, it.id)) }
    }

    fun selectedPlayerId(): String? = selectedRow.takeIf { it >= 0 }?.let {
        model.getValueAt(convertRowIndexToModel(it), 1) as String
    }
}
