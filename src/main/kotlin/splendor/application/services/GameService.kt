package splendor.application.services

import splendor.application.content.GameContentProvider
import splendor.application.dto.*
import splendor.application.repositories.GameRepository
import splendor.application.repositories.PlayerRepository
import splendor.domain.components.CardLevel
import splendor.domain.components.DevelopmentCard
import splendor.domain.components.GemColor
import splendor.domain.components.NobleTile
import splendor.domain.gamemodel.Game
import splendor.domain.gamemodel.GameFactory
import splendor.domain.gamemodel.GameParticipant
import splendor.domain.gamemodel.GameStatus
import splendor.domain.identifiers.CardId
import splendor.domain.identifiers.GameId
import splendor.domain.identifiers.NobleId
import splendor.domain.identifiers.PlayerId
import splendor.domain.moves.*
import java.util.UUID

class GameService(
    private val gameRepository: GameRepository,
    private val playerRepository: PlayerRepository,
    private val gameFactory: GameFactory,
    private val contentProvider: GameContentProvider? = null
) {

    fun createGameFromPlayerIds(playerIds: List<String>): GameView {
        val parsedIds = playerIds.map { raw ->
            try {
                PlayerId(UUID.fromString(raw))
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid player ID: $raw")
            }
        }
        val game = createGame(parsedIds)
        return checkNotNull(getGameView(game.id.value.toString()))
    }

    fun createGame(playerIds: List<PlayerId>): Game {
        val provider = requireNotNull(contentProvider) {
            "Game content provider is not configured"
        }
        return createGame(playerIds, provider.getCards(), provider.getNobles())
    }

    fun createGame(
        playerIds: List<PlayerId>,
        cards: Map<CardLevel, List<DevelopmentCard>>,
        nobles: List<NobleTile>
    ): Game {
        require(playerIds.distinct().size == playerIds.size) {
            "Duplicate players are not allowed"
        }

        val players = playerIds.map { id ->
            requireNotNull(playerRepository.findById(id)) {
                "Player $id not found"
            }
        }

        val participants = players.map { player -> GameParticipant(player) }
        val game = gameFactory.createGame(participants, cards, nobles)
        gameRepository.save(game)
        return game
    }

    fun getGame(id: GameId): Game? = gameRepository.findById(id)

    fun saveProgress(game: Game) = gameRepository.save(game)

    fun getAllGames(): List<Game> = gameRepository.findAll()

    fun getGameViews(): List<GameView> =
        gameRepository.findAll().mapNotNull { getGameView(it.id.value.toString()) }

    fun getGameView(gameId: String): GameView? {
        val game = findGame(gameId) ?: return null
        val currentId = if (game.status == GameStatus.IN_PROGRESS) {
            game.getCurrentParticipant().playerId.value.toString()
        } else null
        val context = if (game.status == GameStatus.IN_PROGRESS) {
            game.getCurrentTurnContext()
        } else null
        val playerNames = game.getParticipants().associate {
            it.playerId to it.nicknameAtStart
        }
        return GameView(
            id = game.id.value.toString(),
            status = game.status.name,
            phase = context?.phase?.name,
            currentPlayerId = currentId,
            tableTokens = GemColor.entries.associate { color ->
                color.name to game.table.getTokenAmount(color)
            },
            openCards = CardLevel.entries.associate { level ->
                level.name to game.table.getOpenCards(level).map(::toCardView)
            },
            deckCounts = CardLevel.entries.associate { level ->
                level.name to game.table.getDeckRemainingCount(level)
            },
            nobles = game.table.getNobles().map(::toNobleView),
            participants = game.getParticipants().map { participant ->
                val state = participant.state
                ParticipantView(
                    id = participant.playerId.value.toString(),
                    nickname = participant.nicknameAtStart,
                    points = state.getPrestigePoints(),
                    tokens = GemColor.entries.associate { color ->
                        color.name to state.getTokenAmount(color)
                    },
                    bonuses = state.getBonuses().mapKeys { it.key.name },
                    purchasedCards = state.getPurchasedCards().map(::toCardView),
                    reservedCards = state.getReservedCards().map(::toCardView),
                    nobles = state.getNobles().map(::toNobleView),
                    isCurrent = participant.playerId.value.toString() == currentId
                )
            },
            acquiredTokens = GemColor.entries.associate { color ->
                color.name to (context?.getAcquiredAmount(color) ?: 0)
            },
            eligibleNobles = if (currentId == null) emptyList() else {
                game.getEligibleNobles(game.getCurrentParticipant().playerId)
                    .map(::toNobleView)
            },
            winners = game.getWinners().map { it.nicknameAtStart },
            history = game.getMoveHistory().map { record ->
                MoveView(
                    playerName = playerNames[record.playerId] ?: record.playerId.value.toString(),
                    description = describeMove(record.move),
                    timestamp = record.timestamp.toString()
                )
            }
        )
    }

    fun startGame(gameId: String): GameActionResult {
        val game = findGame(gameId)
            ?: return GameActionResult(false, "Game not found")
        if (game.status != GameStatus.NOT_STARTED) {
            return GameActionResult(false, "Game has already started")
        }
        game.start()
        gameRepository.save(game)
        return GameActionResult(true)
    }

    fun submitCommand(gameId: String, playerId: String, command: GameCommand): GameActionResult {
        val game = findGame(gameId)
            ?: return GameActionResult(false, "Game not found")
        if (game.status != GameStatus.IN_PROGRESS) {
            return GameActionResult(false, "Game is not in progress")
        }
        val currentId = game.getCurrentParticipant().playerId.value.toString()
        if (currentId != playerId) return GameActionResult(false, "It is not this player's turn")
        val move = try {
            command.toDomainMove()
        } catch (exception: IllegalArgumentException) {
            return GameActionResult(false, exception.message ?: "Invalid action")
        }
        return when (val result = game.submitMove(move)) {
            MoveResult.Accepted -> {
                gameRepository.save(game)
                GameActionResult(true)
            }
            is MoveResult.Rejected -> GameActionResult(false, result.reason.name)
        }
    }

    fun returnTokens(gameId: String, playerId: String, color: String, amount: Int): GameActionResult {
        val game = findGame(gameId)
            ?: return GameActionResult(false, "Game not found")
        if (game.status != GameStatus.IN_PROGRESS) return GameActionResult(false, "Game is not in progress")
        if (game.getCurrentParticipant().playerId.value.toString() != playerId) {
            return GameActionResult(false, "It is not this player's turn")
        }
        val gemColor = parseColor(color)
            ?: return GameActionResult(false, "Unknown gem color")
        return if (game.returnTokens(gemColor, amount)) {
            gameRepository.save(game)
            GameActionResult(true)
        } else GameActionResult(false, "Cannot return these tokens")
    }

    fun selectNoble(gameId: String, playerId: String, nobleId: String): GameActionResult {
        val game = findGame(gameId)
            ?: return GameActionResult(false, "Game not found")
        if (game.status != GameStatus.IN_PROGRESS) return GameActionResult(false, "Game is not in progress")
        if (game.getCurrentParticipant().playerId.value.toString() != playerId) {
            return GameActionResult(false, "It is not this player's turn")
        }
        val id = try { NobleId(UUID.fromString(nobleId)) }
        catch (_: IllegalArgumentException) { return GameActionResult(false, "Invalid noble ID") }
        return if (game.selectNoble(id)) {
            gameRepository.save(game)
            GameActionResult(true)
        } else GameActionResult(false, "Noble cannot be selected")
    }

    private fun findGame(id: String): Game? = try {
        gameRepository.findById(GameId(UUID.fromString(id)))
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun GameCommand.toDomainMove(): Move = when (this) {
        is GameCommand.TakeGems -> TakeGemsMove(colors.map { parseColor(it) ?: throw IllegalArgumentException("Unknown gem color: $it") })
        is GameCommand.TakeTwoGems -> TakeTwoGemsMove(parseColor(color) ?: throw IllegalArgumentException("Unknown gem color: $color"))
        is GameCommand.ReserveOpenCard -> ReserveCardMove(ReserveCardTarget.OpenCard(CardId(UUID.fromString(cardId))))
        is GameCommand.ReserveBlindCard -> ReserveCardMove(ReserveCardTarget.BlindCard(CardLevel.valueOf(level)))
        is GameCommand.PurchaseCard -> PurchaseCardMove(CardId(UUID.fromString(cardId)))
    }

    private fun parseColor(value: String): GemColor? =
        try { GemColor.valueOf(value.trim().uppercase()) } catch (_: IllegalArgumentException) { null }

    private fun toCardView(card: DevelopmentCard) = CardView(
        id = card.id.value.toString(),
        level = card.level.name,
        bonus = card.bonus.name,
        points = card.points,
        cost = GemColor.entries.filter { it != GemColor.GOLD }
            .associate { it.name to card.cost.getAmount(it) }
    )

    private fun toNobleView(noble: NobleTile) = NobleView(
        id = noble.id.value.toString(),
        points = noble.points,
        requirements = GemColor.entries.filter { it != GemColor.GOLD }
            .associate { it.name to noble.requirements.getAmount(it) }
    )

    private fun describeMove(move: Move): String = when (move) {
        is TakeGemsMove -> "Took ${move.colors.joinToString()} gems"
        is TakeTwoGemsMove -> "Took 2 ${move.color} gems"
        is ReserveCardMove -> when (val target = move.target) {
            is ReserveCardTarget.OpenCard -> "Reserved card ${target.cardId.value}"
            is ReserveCardTarget.BlindCard -> "Reserved a blind ${target.level} card"
        }
        is PurchaseCardMove -> "Purchased card ${move.cardId.value}"
    }
}
