package splendor.presentation.gui.components

import splendor.application.dto.CardView
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class CardPanel : JPanel(GridLayout(0, 1, 4, 4)) {
    fun showCards(cards: List<CardView>) {
        removeAll()
        if (cards.isEmpty()) add(JLabel("No cards"))
        cards.forEach { card ->
            val cost = card.cost.filterValues { it > 0 }.entries
                .joinToString(", ") { "${it.key} ${it.value}" }.ifEmpty { "Free" }
            add(JLabel("${card.level} • ${card.bonus} bonus • ${card.points} points\nCost: $cost").apply {
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GuiStyle.muted),
                    BorderFactory.createEmptyBorder(7, 8, 7, 8)
                )
                isOpaque = true
                background = GuiStyle.surface
            })
        }
        revalidate()
        repaint()
    }
}
