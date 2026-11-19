package splendor.application.dto

data class CardView(
    val id: String,
    val level: String,
    val bonus: String,
    val points: Int,
    val cost: Map<String, Int>
)

data class NobleView(
    val id: String,
    val points: Int,
    val requirements: Map<String, Int>
)

data class ParticipantView(
    val id: String,
    val nickname: String,
    val points: Int,
    val tokens: Map<String, Int>,
    val bonuses: Map<String, Int>,
    val purchasedCards: List<CardView>,
    val reservedCards: List<CardView>,
    val nobles: List<NobleView>,
    val isCurrent: Boolean
)

data class MoveView(
    val playerName: String,
    val description: String,
    val timestamp: String
)

data class GameView(
    val id: String,
    val status: String,
    val phase: String?,
    val currentPlayerId: String?,
    val tableTokens: Map<String, Int>,
    val openCards: Map<String, List<CardView>>,
    val deckCounts: Map<String, Int>,
    val nobles: List<NobleView>,
    val participants: List<ParticipantView>,
    val acquiredTokens: Map<String, Int>,
    val eligibleNobles: List<NobleView>,
    val winners: List<String>,
    val history: List<MoveView>
)

data class PlayerStatistics(
    val playerId: String,
    val nickname: String,
    val gamesPlayed: Int,
    val wins: Int,
    val winPercentage: Double,
    val totalPoints: Int = 0
)

sealed interface GameCommand {
    data class TakeGems(val colors: List<String>) : GameCommand
    data class TakeTwoGems(val color: String) : GameCommand
    data class ReserveOpenCard(val cardId: String) : GameCommand
    data class ReserveBlindCard(val level: String) : GameCommand
    data class PurchaseCard(val cardId: String) : GameCommand
}

data class GameActionResult(
    val accepted: Boolean,
    val reason: String? = null
)

data class PlayerView(
    val id: String,
    val nickname: String
)
