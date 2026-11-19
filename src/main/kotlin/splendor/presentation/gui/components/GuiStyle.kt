package splendor.presentation.gui.components

import java.awt.Color
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel

object GuiStyle {
    val background = Color(0xF4F6F8)
    val surface = Color.WHITE
    val accent = Color(0x236B5A)
    val muted = Color(0x667085)

    fun title(text: String) = JLabel(text).apply {
        font = Font(Font.SANS_SERIF, Font.BOLD, 24)
        foreground = Color(0x1D2939)
        border = BorderFactory.createEmptyBorder(12, 4, 16, 4)
    }

    fun button(text: String, onClick: () -> Unit) = JButton(text).apply {
        font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        isFocusPainted = false
        addActionListener { onClick() }
    }
}
