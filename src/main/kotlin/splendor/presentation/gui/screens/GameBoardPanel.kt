package splendor.presentation.gui.screens

import splendor.application.dto.CardView
import splendor.application.dto.GameCommand
import splendor.application.dto.GameView
import splendor.application.services.GameService
import splendor.presentation.gui.components.CardPanel
import splendor.presentation.gui.components.GuiStyle
import splendor.presentation.gui.components.TokenPanel
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea

class GameBoardPanel(
    private val gameService: GameService,
    private val onBack: () -> Unit
) : JPanel(BorderLayout(10, 10)) {
    private var gameId: String? = null
    private val title = JLabel("Game")
    private val currentPlayer = JLabel()
    private val personalTokens = TokenPanel()
    private val tableTokens = TokenPanel()
    private val actionPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    private val cardsPanel = JPanel(GridLayout(1, 3, 8, 0))
    private val participantsText = JTextArea().apply { isEditable = false; background = GuiStyle.surface }
    private val historyText = JTextArea().apply { isEditable = false; background = GuiStyle.surface }
    private val noblesText = JTextArea().apply { isEditable = false; background = GuiStyle.surface }

    init {
        background = GuiStyle.background
        border = BorderFactory.createEmptyBorder(12, 14, 12, 14)
        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(title, BorderLayout.CENTER)
            add(GuiStyle.button("Back to games", onBack), BorderLayout.EAST)
        }
        title.font = title.font.deriveFont(java.awt.Font.BOLD, 22f)
        add(header, BorderLayout.NORTH)

        val left = JPanel(BorderLayout(6, 6)).apply {
            background = GuiStyle.surface
            border = BorderFactory.createTitledBorder("Current player")
            add(currentPlayer, BorderLayout.NORTH)
            add(personalTokens, BorderLayout.CENTER)
            add(actionPanel, BorderLayout.SOUTH)
        }
        val center = JPanel(BorderLayout(6, 6)).apply {
            background = GuiStyle.background
            add(panel("Table tokens", JScrollPane(tableTokens)), BorderLayout.NORTH)
            add(panel("Development cards", JScrollPane(cardsPanel)), BorderLayout.CENTER)
            add(panel("Available nobles", JScrollPane(noblesText)), BorderLayout.SOUTH)
        }
        val right = panel("Participants", JScrollPane(participantsText))
        val upper = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left,
            JSplitPane(JSplitPane.HORIZONTAL_SPLIT, center, right).apply { resizeWeight = .70 }
        ).apply { resizeWeight = .24; border = null }
        val history = panel("Move history", JScrollPane(historyText)).apply { preferredSize = java.awt.Dimension(500, 135) }
        add(JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, history).apply { resizeWeight = .76; border = null }, BorderLayout.CENTER)
    }

    fun showGame(id: String) {
        gameId = id
        val current = gameService.getGameView(id) ?: run {
            JOptionPane.showMessageDialog(this, "Game not found.")
            onBack()
            return
        }
        if (current.status == "NOT_STARTED") {
            val result = gameService.startGame(id)
            if (!result.accepted) JOptionPane.showMessageDialog(this, result.reason)
        }
        refresh()
    }

    fun refresh() {
        val id = gameId ?: return
        val game = gameService.getGameView(id) ?: return
        title.text = "Game ${game.id}  ·  ${game.status}  ·  ${game.phase ?: "Waiting"}"
        val current = game.participants.firstOrNull { it.isCurrent }
        currentPlayer.text = current?.let { "<html><b>${it.nickname}</b><br>${it.points} prestige points<br>Bonuses: ${it.bonuses.filterValues { v -> v > 0 }.entries.joinToString { "${it.key} ${it.value}" }.ifEmpty { "none" }}</html>" }
            ?: if (game.status == "FINISHED") "Game finished · winners: ${game.winners.joinToString()}" else "Game not started"
        personalTokens.showTokens(current?.tokens.orEmpty())
        tableTokens.showTokens(game.tableTokens)
        cardsPanel.removeAll()
        game.openCards.forEach { (level, cards) ->
            val column = JPanel(BorderLayout()).apply {
                background = GuiStyle.background
                add(JLabel("Level $level · deck ${game.deckCounts[level] ?: 0}"), BorderLayout.NORTH)
                add(JScrollPane(CardPanel().apply { showCards(cards) }), BorderLayout.CENTER)
            }
            cardsPanel.add(column)
        }
        cardsPanel.revalidate(); cardsPanel.repaint()
        noblesText.text = game.nobles.joinToString("\n") { "${it.points} points · ${it.requirements.filterValues { n -> n > 0 }.entries.joinToString { "${it.key} ${it.value}" }}" }.ifEmpty { "No nobles available" }
        participantsText.text = game.participants.joinToString("\n\n") { p ->
            "${if (p.isCurrent) "▶ " else ""}${p.nickname} · ${p.points} points\nTokens: ${p.tokens.filterValues { it > 0 }.entries.joinToString { "${it.key} ${it.value}" }.ifEmpty { "none" }}\nBonuses: ${p.bonuses.filterValues { it > 0 }.entries.joinToString { "${it.key} ${it.value}" }.ifEmpty { "none" }}\nCards ${p.purchasedCards.size} purchased / ${p.reservedCards.size} reserved"
        }
        historyText.text = game.history.asReversed().joinToString("\n") { "${it.timestamp}  ${it.playerName}: ${it.description}" }.ifEmpty { "No moves yet." }
        refreshActions(game, current)
    }

    private fun refreshActions(game: GameView, player: splendor.application.dto.ParticipantView?) {
        actionPanel.removeAll()
        when {
            game.status == "NOT_STARTED" -> actionPanel.add(GuiStyle.button("Start game") {
                val result = gameService.startGame(game.id)
                showResult(result.accepted, result.reason)
                refresh()
            })
            game.status == "FINISHED" -> actionPanel.add(JLabel("Winners: ${game.winners.joinToString()}"))
            game.phase == "PLAYER_ACTION" && player != null -> {
                actionPanel.add(GuiStyle.button("Take 3 colors") { takeThree(game, player) })
                actionPanel.add(GuiStyle.button("Take 2 same") { takeTwo(game, player) })
                actionPanel.add(GuiStyle.button("Reserve open") { reserveOpen(game, player) })
                actionPanel.add(GuiStyle.button("Reserve blind") { reserveBlind(game, player) })
                actionPanel.add(GuiStyle.button("Purchase") { purchase(game, player) })
            }
            game.phase == "TOKEN_RETURN" && player != null -> actionPanel.add(GuiStyle.button("Return tokens") { returnTokens(game, player) })
            game.phase == "NOBLE_SELECTION" -> {
                game.eligibleNobles.forEach { noble ->
                    actionPanel.add(GuiStyle.button("Choose noble · ${noble.points} pts") {
                        val result = gameService.selectNoble(game.id, player?.id.orEmpty(), noble.id)
                        showResult(result.accepted, result.reason); refresh()
                    })
                }
            }
        }
        actionPanel.revalidate(); actionPanel.repaint()
    }

    private fun takeThree(game: GameView, player: splendor.application.dto.ParticipantView) {
        val raw = JOptionPane.showInputDialog(this, "Enter three distinct colors (e.g. WHITE BLUE GREEN):") ?: return
        act(game, player, GameCommand.TakeGems(raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }))
    }

    private fun takeTwo(game: GameView, player: splendor.application.dto.ParticipantView) {
        val color = choose(GemColors) ?: return
        act(game, player, GameCommand.TakeTwoGems(color))
    }

    private fun reserveOpen(game: GameView, player: splendor.application.dto.ParticipantView) {
        val cards = game.openCards.values.flatten()
        if (cards.isEmpty()) { JOptionPane.showMessageDialog(this, "No open cards."); return }
        val card = chooseCard(cards) ?: return
        act(game, player, GameCommand.ReserveOpenCard(card.id))
    }

    private fun reserveBlind(game: GameView, player: splendor.application.dto.ParticipantView) {
        val level = choose(game.deckCounts.filterValues { it > 0 }.keys.toTypedArray()) ?: return
        act(game, player, GameCommand.ReserveBlindCard(level))
    }

    private fun purchase(game: GameView, player: splendor.application.dto.ParticipantView) {
        val cards = game.openCards.values.flatten() + player.reservedCards
        if (cards.isEmpty()) { JOptionPane.showMessageDialog(this, "No cards available to purchase."); return }
        val card = chooseCard(cards) ?: return
        act(game, player, GameCommand.PurchaseCard(card.id))
    }

    private fun returnTokens(game: GameView, player: splendor.application.dto.ParticipantView) {
        val options = game.acquiredTokens.filterValues { it > 0 }.keys.toTypedArray()
        if (options.isEmpty()) { JOptionPane.showMessageDialog(this, "No tokens acquired this turn."); return }
        val color = choose(options) ?: return
        val amountText = JOptionPane.showInputDialog(this, "How many $color tokens to return?") ?: return
        val amount = amountText.toIntOrNull()
        if (amount == null) { JOptionPane.showMessageDialog(this, "Enter a whole number."); return }
        val result = gameService.returnTokens(game.id, player.id, color, amount)
        showResult(result.accepted, result.reason); refresh()
    }

    private fun act(game: GameView, player: splendor.application.dto.ParticipantView, command: GameCommand) {
        val result = gameService.submitCommand(game.id, player.id, command)
        showResult(result.accepted, result.reason)
        refresh()
    }

    private fun showResult(accepted: Boolean, reason: String?) {
        if (!accepted) JOptionPane.showMessageDialog(this, reason ?: "Action rejected", "Action not accepted", JOptionPane.WARNING_MESSAGE)
    }

    private fun chooseCard(cards: List<CardView>): CardView? {
        val labels = cards.map { "${it.level} · ${it.bonus} · ${it.points} pts · ${it.cost.filterValues { v -> v > 0 }} · ${it.id}" }.toTypedArray()
        val selected = JOptionPane.showInputDialog(this, "Select a card:", "Card", JOptionPane.QUESTION_MESSAGE, null, labels, labels.firstOrNull()) as? String
        val index = labels.indexOf(selected)
        return cards.getOrNull(index)
    }

    private fun choose(values: Array<String>): String? =
        JOptionPane.showInputDialog(this, "Choose:", "Action", JOptionPane.QUESTION_MESSAGE, null, values, values.firstOrNull()) as? String

    private fun panel(title: String, child: java.awt.Component) = JPanel(BorderLayout()).apply {
        background = GuiStyle.surface
        border = BorderFactory.createTitledBorder(title)
        add(child, BorderLayout.CENTER)
    }

    companion object { private val GemColors = arrayOf("WHITE", "BLUE", "GREEN", "RED", "BLACK") }
}
