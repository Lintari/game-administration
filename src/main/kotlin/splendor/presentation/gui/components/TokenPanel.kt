package splendor.presentation.gui.components

import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel

class TokenPanel : JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)) {
    fun showTokens(tokens: Map<String, Int>) {
        removeAll()
        tokens.filterValues { it > 0 }.forEach { (color, count) ->
            add(JLabel("$color  $count").apply {
                border = javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(GuiStyle.muted),
                    javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)
                )
            })
        }
        if (componentCount == 0) add(JLabel("None"))
        revalidate()
        repaint()
    }
}
