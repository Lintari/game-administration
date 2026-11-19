package splendor.presentation.console

import splendor.application.content.GameContentProvider
import splendor.application.services.GameService
import splendor.application.repositories.PlayerRepository
import splendor.domain.components.CardLevel
import splendor.domain.components.DevelopmentCard
import splendor.domain.components.GemColor
import splendor.domain.components.NobleTile
import splendor.domain.gamemodel.Game
import splendor.domain.gamemodel.GameParticipant
import splendor.domain.identifiers.PlayerId

class GameConsoleMenu(
    private val gameService: GameService,
    private val playerRepository: PlayerRepository,
    private val contentProvider: GameContentProvider,
    private val input: ConsoleInput,
    private val output: ConsoleOutput,
    private val gamePlayConsole: GamePlayConsole
) {
    fun run() {
        while (true) {
            output.showLine()
            output.showLine("=== Manage games ===")
            output.showLine("1. Create game")
            output.showLine("2. List games")
            output.showLine("3. View game")
            output.showLine("4. Play game")
            output.showLine("0. Back")
            output.showPrompt("> ")

            when (input.readText()?.trim()) {
                "1" -> createGame()
                "2" -> listGames()
                "3" -> selectGameToView()
                "4" -> selectGameToPlay()
                "0" -> return
                else -> output.showLine("Invalid option.")
            }
        }
    }

    private fun createGame() {
        val players = playerRepository.findAll()

        if (players.size < 2) {
            output.showLine("At least 2 registered players are required.")
            return
        }

        output.showLine("\nAvailable players:")
        players.forEachIndexed { index, player ->
            output.showLine("${index + 1}. ${player.nickname}")
        }

        output.showLine("Enter player numbers separated by spaces (2-4 players):")
        output.showPrompt("> ")

        val input = input.readText()?.trim() ?: return
        val selectedIndexes = input.split(Regex("\\s+"))
            .map { it.toIntOrNull() ?: return output.showLine("Invalid player selection.") }

        if (
            selectedIndexes.size !in 2..4 ||
            selectedIndexes.distinct().size != selectedIndexes.size ||
            selectedIndexes.any { it !in 1..players.size }
        ) {
            output.showLine("Invalid player selection.")
            return
        }

        val playerIds: List<PlayerId> =
            selectedIndexes.map { players[it - 1].id }

        try {
            val game = gameService.createGame(
                playerIds = playerIds,
                cards = contentProvider.getCards(),
                nobles = contentProvider.getNobles()
            )

            output.showLine("\nGame created successfully!")
            output.showLine("Game ID: ${game.id}")
            output.showLine("Players:")

            game.getParticipants().forEach {
                output.showLine("- ${it.nicknameAtStart}")
            }
        } catch (exception: IllegalArgumentException) {
            output.showLine("Could not create game: ${exception.message}")
        }
    }

    private fun listGames() {
        val games = gameService.getAllGames()

        if (games.isEmpty()) {
            output.showLine("No games found.")
            return
        }

        output.showLine("\n=== Games ===")

        games.forEachIndexed { index, game ->
            output.showLine("${index + 1}. Game ${game.id}")
            game.getParticipants().forEach {
                output.showLine("   - ${it.nicknameAtStart}")
            }
        }
    }

    private fun selectGameToView() {
        val games = gameService.getAllGames()

        if (games.isEmpty()) {
            output.showLine("No games found.")
            return
        }

        output.showLine("\n=== Select game ===")

        games.forEachIndexed { index, game ->
            val players = game.getParticipants()
                .joinToString(", ") { it.nicknameAtStart }

            output.showLine("${index + 1}. $players")
        }

        output.showPrompt("> ")

        val index = input.readText()?.trim()?.toIntOrNull()?.minus(1)

        if (index == null || index !in games.indices) {
            output.showLine("Invalid game selection.")
            return
        }

        displayGame(games[index])
    }

    private fun displayGame(game: Game) {
        output.showLine("\n========================================")
        output.showLine("                GAME")
        output.showLine("========================================")
        output.showLine("ID: ${game.id}")

        displayTable(game)
        displayParticipants(game.getParticipants())

        output.showLine("========================================")
    }

    private fun displayTable(game: Game) {
        val table = game.table

        output.showLine("\n--- TABLE ---")

        output.showLine("\nToken supply:")
        GemColor.entries.forEach { color ->
            output.showLine("  ${color.name}: ${table.getTokenAmount(color)}")
        }

        output.showLine("\nDevelopment cards:")

        CardLevel.entries.forEach { level ->
            output.showLine("\n  Level $level")
            output.showLine("  Cards remaining in deck: ${table.getDeckRemainingCount(level)}")

            val cards = table.getOpenCards(level)

            if (cards.isEmpty()) {
                output.showLine("  No open cards.")
            } else {
                cards.forEachIndexed { index, card ->
                    output.showLine("  ${index + 1}. ${formatCard(card)}")
                }
            }
        }

        output.showLine("\nNobles:")
        val nobles = table.getNobles()

        if (nobles.isEmpty()) {
            output.showLine("  No nobles available.")
        } else {
            nobles.forEachIndexed { index, noble ->
                output.showLine("  ${index + 1}. ${formatNoble(noble)}")
            }
        }
    }

    private fun displayParticipants(participants: List<GameParticipant>) {
        output.showLine("\n--- PLAYERS ---")

        participants.forEach { participant ->
            val state = participant.state

            output.showLine("\n${participant.nicknameAtStart}")
            output.showLine("  Prestige points: ${state.getPrestigePoints()}")

            output.showLine("  Tokens:")
            GemColor.entries.forEach { color ->
                val amount = state.getTokenAmount(color)
                if (amount > 0) {
                    output.showLine("    ${color.name}: $amount")
                }
            }

            output.showLine("  Bonuses:")
            val bonuses = state.getBonuses()
            GemColor.entries
                .filter { it != GemColor.GOLD }
                .forEach { color ->
                    val amount = bonuses[color] ?: 0
                    if (amount > 0) {
                        output.showLine("    ${color.name}: $amount")
                    }
                }

            output.showLine("  Purchased cards:")
            val purchasedCards = state.getPurchasedCards()

            if (purchasedCards.isEmpty()) {
                output.showLine("    None")
            } else {
                purchasedCards.forEach {
                    output.showLine("    ${formatCard(it)}")
                }
            }

            output.showLine("  Reserved cards:")
            val reservedCards = state.getReservedCards()

            if (reservedCards.isEmpty()) {
                output.showLine("    None")
            } else {
                reservedCards.forEach {
                    output.showLine("    ${formatCard(it)}")
                }
            }

            output.showLine("  Nobles:")
            val nobles = state.getNobles()

            if (nobles.isEmpty()) {
                output.showLine("    None")
            } else {
                nobles.forEach {
                    output.showLine("    ${formatNoble(it)}")
                }
            }
        }
    }

    private fun formatCard(card: DevelopmentCard): String {
        val cost = GemColor.entries
            .filter { it != GemColor.GOLD }
            .mapNotNull { color ->
                val amount = card.cost.getAmount(color)
                if (amount > 0) "${color.name}: $amount" else null
            }
            .joinToString(", ")
            .ifEmpty { "free" }

        return "[${card.level}] ${card.bonus} bonus, " +
            "${card.points} points; cost: $cost"
    }

    private fun formatNoble(noble: NobleTile): String {
        val requirements = GemColor.entries
            .filter { it != GemColor.GOLD }
            .mapNotNull { color ->
                val amount = noble.requirements.getAmount(color)
                if (amount > 0) "${color.name}: $amount" else null
            }
            .joinToString(", ")
            .ifEmpty { "none" }

        return "${noble.points} points; requires: $requirements"
    }

    private fun selectGameToPlay() {
        val games = gameService.getAllGames()

        if (games.isEmpty()) {
            output.showLine("No games found.")
            return
        }

        output.showLine("\n=== Select game to play ===")

        games.forEachIndexed { index, game ->
            val players = game.getParticipants()
                .joinToString(", ") { it.nicknameAtStart }

            output.showLine("${index + 1}. $players (${game.status})")
        }

        output.showPrompt("> ")

        val index = input.readText()?.trim()?.toIntOrNull()?.minus(1)

        if (index == null || index !in games.indices) {
            output.showLine("Invalid game selection.")
            return
        }

        gamePlayConsole.play(games[index])
    }
}
