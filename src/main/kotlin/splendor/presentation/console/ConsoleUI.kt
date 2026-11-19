package splendor.presentation.console

import splendor.application.services.PlayerService
import splendor.domain.identifiers.PlayerId
import java.util.UUID

class ConsoleUI(
    private val playerService: PlayerService,
    private val input: ConsoleInput,
    private val output: ConsoleOutput,
    private val gameMenu: GameConsoleMenu
) {

    fun run() {
        while (true) {
            output.showMainMenu()

            when (input.readChoice()) {
                1 -> handlePlayerActions()
                2 -> gameMenu.run()
                0, null -> {
                    output.showMessage("Goodbye!")
                    return
                }
                else -> output.showError("Invalid menu option.")
            }
        }
    }

    private fun handlePlayerActions() {
        while (true) {
            output.showPlayerMenu()

            when (input.readChoice()) {
                1 -> registerPlayer()
                2 -> showAllPlayers()
                3 -> findPlayer()
                0, null -> return
                else -> output.showError("Invalid menu option.")
            }
        }
    }

    private fun registerPlayer() {
        output.showMessage("Enter player nickname:")

        val nickname = input.readText()

        if (nickname.isNullOrBlank()) {
            output.showError("Nickname cannot be empty.")
            return
        }

        try {
            val player = playerService.registerPlayer(nickname)
            output.showMessage("Player registered successfully!")
            output.showPlayer(player)
        } catch (exception: IllegalArgumentException) {
            output.showError(exception.message ?: "Could not register player.")
        }
    }

    private fun showAllPlayers() {
        val players = playerService.getAllPlayers()
        output.showPlayers(players)
    }

    private fun findPlayer() {
        output.showMessage("Enter player ID:")

        val rawId = input.readText() ?: return

        val uuid = try {
            UUID.fromString(rawId)
        } catch (exception: IllegalArgumentException) {
            output.showError("Invalid player ID format.")
            return
        }

        val playerId = PlayerId(uuid)
        val player = playerService.getPlayer(playerId)

        if (player == null) {
            output.showError("Player not found.")
        } else {
            output.showPlayer(player)
        }
    }
}
