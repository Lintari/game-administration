package splendor.presentation.gui.components

import splendor.application.dto.PlayerView
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

class PlayerInfoPanel : JPanel(BorderLayout()) {
    private val label = JLabel("Select a player to view details.")

    init {
        background = GuiStyle.surface
        border = javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16)
        add(label, BorderLayout.NORTH)
    }

    fun showPlayer(player: PlayerView?) {
        label.text = player?.let { "<html><b>${it.nickname}</b><br>ID: ${it.id}</html>" }
            ?: "Select a player to view details."
    }
}
