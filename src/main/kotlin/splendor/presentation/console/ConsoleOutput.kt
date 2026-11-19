package splendor.presentation.console

import splendor.application.dto.PlayerInfo
import java.io.PrintStream

class ConsoleOutput(
    private val output: PrintStream = System.out
) {

    fun showMainMenu() {
        output.println()
        output.println("========== SPLENDOR MANAGER ==========")
        output.println("1. Manage players")
        output.println("2. Manage games")
        output.println("0. Exit")
        output.print("Select option: ")
    }

    fun showPlayerMenu() {
        output.println()
        output.println("========== PLAYERS ==========")
        output.println("1. Register player")
        output.println("2. Show all players")
        output.println("3. Find player by ID")
        output.println("0. Back")
        output.print("Select option: ")
    }

    fun showMessage(message: String) {
        output.println(message)
    }

    fun showError(message: String) {
        output.println("Error: $message")
    }

    fun showPlayers(players: List<PlayerInfo>) {
        if (players.isEmpty()) {
            output.println("No registered players.")
            return
        }

        output.println()
        output.println("Registered players:")

        players.forEachIndexed { index, player ->
            output.println("${index + 1}. ${player.nickname} (ID: ${player.id.value})")
        }
    }

    fun showPlayer(player: PlayerInfo) {
        output.println("Player: ${player.nickname}")
        output.println("ID: ${player.id.value}")
    }

    fun showLine(message: String = "") {
        output.println(message)
    }

    fun showPrompt(message: String) {
        output.print(message)
        output.flush()
    }
}
