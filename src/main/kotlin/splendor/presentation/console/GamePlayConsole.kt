package splendor.presentation.console

import splendor.domain.components.CardLevel
import splendor.domain.components.DevelopmentCard
import splendor.domain.components.GemColor
import splendor.domain.components.GemTokens
import splendor.domain.components.NobleTile
import splendor.domain.gamemodel.Game
import splendor.domain.gamemodel.GameStatus
import splendor.domain.gamemodel.TurnPhase
import splendor.domain.identifiers.CardId
import splendor.domain.moves.*
import java.util.UUID

class GamePlayConsole(
    private val input: ConsoleInput,
    private val output: ConsoleOutput
) {

    fun play(game: Game) {
        if (game.status == GameStatus.NOT_STARTED) {
            game.start()
        }

        if (game.status != GameStatus.IN_PROGRESS) {
            output.showLine("This game cannot be started.")
            return
        }

        output.showLine("\nStarting Splendor!")

        while (game.status == GameStatus.IN_PROGRESS) {
            displayState(game)

            when (game.getCurrentTurnContext().phase) {
                TurnPhase.PLAYER_ACTION -> performPlayerAction(game)
                TurnPhase.TOKEN_RETURN -> handleTokenReturn(game)
                TurnPhase.NOBLE_SELECTION -> handleNobleSelection(game)
                TurnPhase.COMPLETED -> continue
            }
        }

        displayGameResults(game)
    }

    private fun performPlayerAction(game: Game) {
        val player = game.getCurrentParticipant()

        output.showLine("\n${player.nicknameAtStart}, your turn!")
        output.showLine("1. Take 3 gems of different colors")
        output.showLine("2. Take 2 gems of the same color")
        output.showLine("3. Reserve an open card")
        output.showLine("4. Reserve a blind card")
        output.showLine("5. Purchase a card")
        output.showLine("0. Return to menu")
        output.showPrompt("> ")

        val choice = input.readText()?.trim()

        val move = when (choice) {
            "1" -> createTakeGemsMove(game)
            "2" -> createTakeTwoGemsMove()
            "3" -> createReserveOpenCardMove(game)
            "4" -> createReserveBlindCardMove()
            "5" -> createPurchaseCardMove(game)
            "0" -> return
            else -> {
                output.showLine("Invalid option.")
                return
            }
        } ?: return

        when (val result = game.submitMove(move)) {
            MoveResult.Accepted -> output.showLine("Move accepted!")
            is MoveResult.Rejected -> {
                output.showLine("Move rejected: ${result.reason}")
            }
        }
    }

    private fun createTakeGemsMove(game: Game): Move? {
        output.showLine("Available colors:")
        displayAvailableTokenColors(game)

        output.showLine("Enter three different colors separated by spaces:")
        output.showPrompt("> ")

        val colors = parseColors(input.readText()) ?: return null

        if (colors.size != 3 || colors.distinct().size != 3) {
            output.showLine("You must select exactly three different colors.")
            return null
        }

        return TakeGemsMove(colors)
    }

    private fun createTakeTwoGemsMove(): Move? {
        output.showLine("Enter color:")
        output.showPrompt("> ")

        val color = parseColor(input.readText()) ?: return null

        if (color == GemColor.GOLD) {
            output.showLine("Gold cannot be taken using this action.")
            return null
        }

        return TakeTwoGemsMove(color)
    }

    private fun createReserveOpenCardMove(game: Game): Move? {
        displayOpenCards(game)

        output.showLine("Enter card ID:")
        output.showPrompt("> ")

        val cardId = parseCardId(input.readText()) ?: return null

        return ReserveCardMove(
            ReserveCardTarget.OpenCard(cardId)
        )
    }

    private fun createReserveBlindCardMove(): Move? {
        output.showLine("Choose deck level (I, II, III):")
        output.showPrompt("> ")

        val level = parseCardLevel(input.readText()) ?: return null

        return ReserveCardMove(
            ReserveCardTarget.BlindCard(level)
        )
    }

    private fun createPurchaseCardMove(game: Game): Move? {
        displayOpenCards(game)

        val player = game.getCurrentParticipant()

        output.showLine("\nYour reserved cards:")
        val reservedCards = player.state.getReservedCards()

        if (reservedCards.isEmpty()) {
            output.showLine("None")
        } else {
            reservedCards.forEach {
                output.showLine("${it.id} — ${formatCard(it)}")
            }
        }

        output.showLine("\nEnter card ID:")
        output.showPrompt("> ")

        val cardId = parseCardId(input.readText()) ?: return null

        return PurchaseCardMove(cardId)
    }

    private fun handleTokenReturn(game: Game) {
        val player = game.getCurrentParticipant()
        val context = game.getCurrentTurnContext()

        output.showLine("\nYou have more than 10 tokens.")
        output.showLine("You must return tokens acquired during this turn.")

        while (player.state.getTokens().total() > Game.MAX_PLAYER_TOKENS) {
            output.showLine("\nYour tokens:")
            displayTokens(player.state.getTokens())

            output.showLine("Tokens acquired this turn:")
            displayTokens(context.getAcquiredTokens())

            output.showLine("Enter color to return:")
            output.showPrompt("> ")

            val color = parseColor(input.readText()) ?: continue

            output.showLine("Enter amount:")
            output.showPrompt("> ")

            val amount = input.readText()?.trim()?.toIntOrNull()

            if (amount == null || amount <= 0) {
                output.showLine("Invalid amount.")
                continue
            }

            if (game.returnTokens(color, amount)) {
                output.showLine("Returned $amount ${color.name} token(s).")
            } else {
                output.showLine("Cannot return these tokens.")
            }
        }
    }

    private fun handleNobleSelection(game: Game) {
        val player = game.getCurrentParticipant()
        val eligibleNobles = game.getEligibleNobles(player.playerId)

        if (eligibleNobles.size <= 1) {
            return
        }

        output.showLine("\nYou qualify for several nobles!")
        output.showLine("Choose one:")

        eligibleNobles.forEachIndexed { index, noble ->
            output.showLine("${index + 1}. ${formatNoble(noble)}")
            output.showLine("   ID: ${noble.id}")
        }

        output.showPrompt("> ")

        val choice = input.readText()?.trim()?.toIntOrNull()

        if (choice == null || choice !in eligibleNobles.indices.map { it + 1 }) {
            output.showLine("Invalid selection. Try again.")
            return
        }

        val noble = eligibleNobles[choice - 1]

        if (game.selectNoble(noble.id)) {
            output.showLine("Noble acquired!")
        } else {
            output.showLine("Could not acquire this noble.")
        }
    }

    private fun displayState(game: Game) {
        output.showLine("\n========================================")
        output.showLine("                 SPLENDOR")
        output.showLine("========================================")

        val currentPlayer = game.getCurrentParticipant()

        output.showLine("Current player: ${currentPlayer.nicknameAtStart}")
        output.showLine("Prestige points: ${currentPlayer.state.getPrestigePoints()}")

        displayTable(game)
        displayPlayers(game)
    }

    private fun displayTable(game: Game) {
        val table = game.table

        output.showLine("\n--- TOKEN SUPPLY ---")
        displayTokens(tableTokens(game))

        output.showLine("\n--- DEVELOPMENT CARDS ---")

        CardLevel.entries.forEach { level ->
            output.showLine("\nLevel $level (deck: ${table.getDeckRemainingCount(level)})")

            table.getOpenCards(level).forEach { card ->
                output.showLine("${card.id} — ${formatCard(card)}")
            }
        }

        output.showLine("\n--- NOBLES ---")

        val nobles = table.getNobles()

        if (nobles.isEmpty()) {
            output.showLine("None")
        } else {
            nobles.forEach {
                output.showLine("${it.id} — ${formatNoble(it)}")
            }
        }
    }

    private fun displayPlayers(game: Game) {
        output.showLine("\n--- PLAYERS ---")

        game.getParticipants().forEach { participant ->
            val state = participant.state

            output.showLine("\n${participant.nicknameAtStart}")
            output.showLine("Points: ${state.getPrestigePoints()}")
            output.showLine("Tokens:")
            displayTokens(state.getTokens())

            output.showLine("Bonuses:")
            GemColor.entries
                .filter { it != GemColor.GOLD }
                .forEach { color ->
                    val amount = state.getBonuses()[color] ?: 0
                    if (amount > 0) {
                        output.showLine("  ${color.name}: $amount")
                    }
                }

            output.showLine("Purchased cards: ${state.getPurchasedCards().size}")
            output.showLine("Reserved cards: ${state.getReservedCards().size}")
            output.showLine("Nobles: ${state.getNobles().size}")
        }
    }

    private fun displayGameResults(game: Game) {
        output.showLine("\n========================================")
        output.showLine("               GAME OVER")
        output.showLine("========================================")

        val winners = game.getWinners()

        if (winners.isNotEmpty()) {
            output.showLine("Winner(s):")
            winners.forEach {
                output.showLine("- ${it.nicknameAtStart}: ${it.state.getPrestigePoints()} points")
            }
        }

        output.showLine("\nFinal standings:")

        game.getParticipants()
            .sortedByDescending { it.state.getPrestigePoints() }
            .forEachIndexed { index, participant ->
                output.showLine(
                    "${index + 1}. ${participant.nicknameAtStart} — " +
                        "${participant.state.getPrestigePoints()} points"
                )
            }
    }

    private fun displayOpenCards(game: Game) {
        output.showLine("\nOpen cards:")

        CardLevel.entries.forEach { level ->
            output.showLine("\nLevel $level")

            game.table.getOpenCards(level).forEach {
                output.showLine("${it.id} — ${formatCard(it)}")
            }
        }
    }

    private fun displayAvailableTokenColors(game: Game) {
        GemColor.entries
            .filter { it != GemColor.GOLD }
            .forEach { color ->
                output.showLine("${color.name}: ${game.table.getTokenAmount(color)}")
            }
    }

    private fun displayTokens(tokens: GemTokens) {
        GemColor.entries.forEach { color ->
            val amount = tokens.getAmount(color)
            if (amount > 0) {
                output.showLine("  ${color.name}: $amount")
            }
        }
    }

    private fun tableTokens(game: Game) =
        GemTokens().apply {
            GemColor.entries.forEach { color ->
                val amount = game.table.getTokenAmount(color)
                if (amount > 0) add(color, amount)
            }
        }

    private fun formatCard(card: DevelopmentCard): String {
        return "${card.level} | ${card.bonus} bonus | " +
            "${card.points} points"
    }

    private fun formatNoble(noble: NobleTile): String {
        return "${noble.points} points"
    }

    private fun parseColor(input: String?): GemColor? {
        return try {
            GemColor.valueOf(input?.trim()?.uppercase() ?: "")
        } catch (_: IllegalArgumentException) {
            output.showLine("Unknown color.")
            null
        }
    }

    private fun parseColors(input: String?): List<GemColor>? {
        val parts = input?.trim()?.split(Regex("\\s+")) ?: return null

        val colors = parts.map {
            try {
                GemColor.valueOf(it.uppercase())
            } catch (_: IllegalArgumentException) {
                output.showLine("Unknown color: $it")
                return null
            }
        }

        if (GemColor.GOLD in colors) {
            output.showLine("Gold cannot be taken from the table.")
            return null
        }

        return colors
    }

    private fun parseCardId(input: String?): CardId? {
        return try {
            val value = input?.trim() ?: return null
            CardId(UUID.fromString(value))
        } catch (_: Exception) {
            output.showLine("Invalid card ID.")
            null
        }
    }

    private fun parseCardLevel(input: String?): CardLevel? {
        return try {
            CardLevel.valueOf(input?.trim()?.uppercase() ?: "")
        } catch (_: IllegalArgumentException) {
            output.showLine("Invalid card level.")
            null
        }
    }
}
