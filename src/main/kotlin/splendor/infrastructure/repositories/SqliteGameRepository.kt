package splendor.infrastructure.repositories

import splendor.application.repositories.GameRepository
import splendor.domain.components.*
import splendor.domain.gamemodel.*
import splendor.domain.identifiers.*
import splendor.domain.moves.*
import splendor.domain.players.Player
import splendor.infrastructure.database.SqliteDatabase
import java.sql.Connection
import java.time.Instant
import java.util.UUID

class SqliteGameRepository(
    private val database: SqliteDatabase
) : GameRepository {

    override fun save(game: Game) {
        database.transaction { connection -> saveGame(connection, game) }
    }

    override fun findById(id: GameId): Game? = database.withConnection { connection ->
        loadGame(connection, id)
    }

    override fun findAll(): List<Game> = database.withConnection { connection ->
        val ids = connection.prepareStatement("SELECT id FROM games ORDER BY rowid").use { statement ->
            statement.executeQuery().use { result ->
                buildList { while (result.next()) add(GameId(UUID.fromString(result.getString(1)))) }
            }
        }
        ids.mapNotNull { loadGame(connection, it) }
    }

    private fun saveGame(connection: Connection, game: Game) {
        val participants = game.getParticipants()
        val status = game.status
        val now = Instant.now().toString()
        val context = game.persistenceTurnContext()
        val currentPlayerId = if (status == GameStatus.IN_PROGRESS) {
            game.getCurrentParticipant().playerId.value.toString()
        } else null
        val winners = game.getWinners()
        val singleWinner = winners.singleOrNull()?.playerId?.value?.toString()
        insert(connection, """
            INSERT INTO games(id,status,started_at,finished_at,current_index,final_round_player_id,turn_player_id,turn_phase,winner_id)
            VALUES(?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
              status=excluded.status,
              started_at=CASE WHEN excluded.started_at IS NOT NULL THEN COALESCE(games.started_at, excluded.started_at) ELSE games.started_at END,
              finished_at=CASE WHEN excluded.finished_at IS NOT NULL THEN COALESCE(games.finished_at, excluded.finished_at) ELSE NULL END,
              current_index=excluded.current_index,
              final_round_player_id=excluded.final_round_player_id,
              turn_player_id=excluded.turn_player_id,
              turn_phase=excluded.turn_phase,
              winner_id=excluded.winner_id
        """.trimIndent(),
            game.id.value.toString(), status.name,
            if (status == GameStatus.NOT_STARTED) null else now,
            if (status == GameStatus.FINISHED) now else null,
            game.persistenceCurrentIndex(),
            game.persistenceFinalRoundPlayerId()?.value?.toString(), currentPlayerId,
            context?.phase?.name, singleWinner
        )

        listOf(
            "moves", "turn_tokens", "participant_nobles", "participant_cards",
            "participant_tokens", "open_cards", "deck_cards", "game_nobles",
            "table_tokens", "game_players"
        ).forEach { table ->
            insert(connection, "DELETE FROM $table WHERE game_id = ?", game.id.value.toString())
        }

        val finished = status == GameStatus.FINISHED
        val scores = participants.associate { it.playerId to it.state.getPrestigePoints() }
        val rankedScores = scores.values.distinct().sortedDescending()
        participants.forEachIndexed { index, participant ->
            val score = scores.getValue(participant.playerId)
            val placement = if (finished) 1 + participants.count {
                it.state.getPrestigePoints() > score
            } else null
            insert(connection, """
                INSERT INTO game_players(game_id,player_id,player_order,nickname_at_start,final_score,placement,is_winner)
                VALUES(?,?,?,?,?,?,?)
            """.trimIndent(),
                game.id.value.toString(), participant.playerId.value.toString(), index,
                participant.nicknameAtStart, if (finished) score else null,
                placement, if (winners.any { it.playerId == participant.playerId }) 1 else 0
            )
        }

        GemColor.entries.forEach { color ->
            insert(connection, "INSERT INTO table_tokens(game_id,color,amount) VALUES(?,?,?)",
                game.id.value.toString(), color.name, game.table.getTokenAmount(color))
        }
        CardLevel.entries.forEach { level ->
            game.table.getOpenCards(level).forEachIndexed { index, card ->
                saveCard(connection, card)
                insert(connection, "INSERT INTO open_cards(game_id,level,position,card_id) VALUES(?,?,?,?)",
                    game.id.value.toString(), level.name, index, card.id.value.toString())
            }
            game.table.getDeckCards(level).forEachIndexed { index, card ->
                saveCard(connection, card)
                insert(connection, "INSERT INTO deck_cards(game_id,level,position,card_id) VALUES(?,?,?,?)",
                    game.id.value.toString(), level.name, index, card.id.value.toString())
            }
        }
        game.table.getNobles().forEachIndexed { index, noble ->
            saveNoble(connection, noble)
            insert(connection, "INSERT INTO game_nobles(game_id,position,noble_id) VALUES(?,?,?)",
                game.id.value.toString(), index, noble.id.value.toString())
        }

        participants.forEach { participant ->
            val playerId = participant.playerId.value.toString()
            GemColor.entries.forEach { color ->
                insert(connection, "INSERT INTO participant_tokens(game_id,player_id,color,amount) VALUES(?,?,?,?)",
                    game.id.value.toString(), playerId, color.name, participant.state.getTokenAmount(color))
            }
            participant.state.getPurchasedCards().forEachIndexed { index, card ->
                saveCard(connection, card)
                insert(connection, "INSERT INTO participant_cards(game_id,player_id,card_id,card_status,position) VALUES(?,?,?,'PURCHASED',?)",
                    game.id.value.toString(), playerId, card.id.value.toString(), index)
            }
            participant.state.getReservedCards().forEachIndexed { index, card ->
                saveCard(connection, card)
                insert(connection, "INSERT INTO participant_cards(game_id,player_id,card_id,card_status,position) VALUES(?,?,?,'RESERVED',?)",
                    game.id.value.toString(), playerId, card.id.value.toString(), index)
            }
            participant.state.getNobles().forEachIndexed { index, noble ->
                saveNoble(connection, noble)
                insert(connection, "INSERT INTO participant_nobles(game_id,player_id,noble_id,position) VALUES(?,?,?,?)",
                    game.id.value.toString(), playerId, noble.id.value.toString(), index)
            }
        }

        context?.let { turn ->
            GemColor.entries.forEach { color ->
                insert(connection, "INSERT INTO turn_tokens(game_id,color,amount) VALUES(?,?,?)",
                    game.id.value.toString(), color.name, turn.getAcquiredAmount(color))
            }
        }

        game.getMoveHistory().forEachIndexed { index, record ->
            val (type, payload, description) = encodeMove(record.move)
            insert(connection, """
                INSERT INTO moves(game_id,move_number,player_id,move_type,payload,description,created_at)
                VALUES(?,?,?,?,?,?,?)
            """.trimIndent(), game.id.value.toString(), index + 1,
                record.playerId.value.toString(), type, payload, description, record.timestamp.toString())
        }

        recalculateStatistics(connection)
    }

    private fun saveCard(connection: Connection, card: DevelopmentCard) {
        insert(connection, "INSERT INTO cards(id,level,bonus,points) VALUES(?,?,?,?) ON CONFLICT(id) DO UPDATE SET level=excluded.level,bonus=excluded.bonus,points=excluded.points",
            card.id.value.toString(), card.level.name, card.bonus.name, card.points)
        insert(connection, "DELETE FROM card_costs WHERE card_id = ?", card.id.value.toString())
        GemColor.entries.filter { it != GemColor.GOLD }.forEach { color ->
            insert(connection, "INSERT INTO card_costs(card_id,color,amount) VALUES(?,?,?)",
                card.id.value.toString(), color.name, card.cost.getAmount(color))
        }
    }

    private fun saveNoble(connection: Connection, noble: NobleTile) {
        insert(connection, "INSERT INTO nobles(id,points) VALUES(?,?) ON CONFLICT(id) DO UPDATE SET points=excluded.points",
            noble.id.value.toString(), noble.points)
        insert(connection, "DELETE FROM noble_requirements WHERE noble_id = ?", noble.id.value.toString())
        GemColor.entries.filter { it != GemColor.GOLD }.forEach { color ->
            insert(connection, "INSERT INTO noble_requirements(noble_id,color,amount) VALUES(?,?,?)",
                noble.id.value.toString(), color.name, noble.requirements.getAmount(color))
        }
    }

    private fun loadGame(connection: Connection, id: GameId): Game? {
        val row = connection.prepareStatement("SELECT * FROM games WHERE id = ?").use { statement ->
            statement.setString(1, id.value.toString())
            statement.executeQuery().use { result ->
                if (!result.next()) null else GameRow(
                    status = GameStatus.valueOf(result.getString("status")),
                    currentIndex = result.getInt("current_index"),
                    finalRoundPlayerId = result.getString("final_round_player_id")?.let(::playerId),
                    turnPlayerId = result.getString("turn_player_id")?.let(::playerId),
                    turnPhase = result.getString("turn_phase")?.let(TurnPhase::valueOf)
                )
            }
        } ?: return null

        val participants = connection.prepareStatement("""
            SELECT gp.player_id, gp.nickname_at_start, p.nickname
            FROM game_players gp JOIN players p ON p.id=gp.player_id
            WHERE gp.game_id=? ORDER BY gp.player_order
        """.trimIndent()).use { statement ->
            statement.setString(1, id.value.toString())
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        val pid = playerId(result.getString("player_id"))
                        val player = Player(pid, result.getString("nickname"))
                        val participant = GameParticipant(player, result.getString("nickname_at_start"))
                        restoreParticipantState(connection, id, participant)
                        add(participant)
                    }
                }
            }
        }

        val tableTokens = readTokens(connection, "table_tokens", id.value.toString())
        val openCards = CardLevel.entries.associateWith { level ->
            queryCards(connection, "SELECT card_id FROM open_cards WHERE game_id=? AND level=? ORDER BY position", id.value.toString(), level.name)
        }
        val decks = CardLevel.entries.associateWith { level ->
            DevelopmentCardDeck(
                level = level,
                cards = queryCards(connection, "SELECT card_id FROM deck_cards WHERE game_id=? AND level=? ORDER BY position", id.value.toString(), level.name),
                shuffle = false
            )
        }
        val tableNobles = queryIds(connection, "SELECT noble_id FROM game_nobles WHERE game_id=? ORDER BY position", id.value.toString()).map { readNoble(connection, it) }
        val table = Table(tableTokens, openCards, decks, tableNobles)
        val history = readMoveHistory(connection, id)
        val context = if (row.turnPlayerId != null && row.turnPhase != null) {
            TurnContext(
                playerId = row.turnPlayerId,
                initialPhase = row.turnPhase,
                acquiredTokens = readTokens(connection, "turn_tokens", id.value.toString())
            )
        } else null
        val winners = connection.prepareStatement("SELECT player_id FROM game_players WHERE game_id=? AND is_winner=1 ORDER BY player_order").use { statement ->
            statement.setString(1, id.value.toString())
            statement.executeQuery().use { result ->
                val winnerIds = buildSet { while (result.next()) add(result.getString(1)) }
                participants.filter { it.playerId.value.toString() in winnerIds }
            }
        }
        return Game.restore(
            id = id,
            participants = participants,
            table = table,
            status = row.status,
            currentIndex = row.currentIndex,
            finalRoundPlayerId = row.finalRoundPlayerId,
            turnContext = context,
            winners = winners,
            moveHistory = history
        )
    }

    private fun restoreParticipantState(connection: Connection, gameId: GameId, participant: GameParticipant) {
        readParticipantTokens(connection, gameId, participant.playerId).forEach { (color, amount) ->
            if (amount > 0) participant.state.addTokens(color, amount)
        }
        connection.prepareStatement("SELECT card_id,card_status FROM participant_cards WHERE game_id=? AND player_id=? ORDER BY card_status,position").use { statement ->
            statement.setString(1, gameId.value.toString())
            statement.setString(2, participant.playerId.value.toString())
            statement.executeQuery().use { result ->
                while (result.next()) {
                    val card = readCard(connection, result.getString("card_id"))
                    when (result.getString("card_status")) {
                        "PURCHASED" -> check(participant.state.purchaseCard(card))
                        "RESERVED" -> check(participant.state.reserveCard(card))
                    }
                }
            }
        }
        connection.prepareStatement("SELECT noble_id FROM participant_nobles WHERE game_id=? AND player_id=? ORDER BY position").use { statement ->
            statement.setString(1, gameId.value.toString())
            statement.setString(2, participant.playerId.value.toString())
            statement.executeQuery().use { result ->
                while (result.next()) check(participant.state.addNoble(readNoble(connection, result.getString(1))))
            }
        }
    }

    private fun readParticipantTokens(connection: Connection, gameId: GameId, playerId: PlayerId): Map<GemColor, Int> =
        connection.prepareStatement("SELECT color,amount FROM participant_tokens WHERE game_id=? AND player_id=?").use { statement ->
            statement.setString(1, gameId.value.toString())
            statement.setString(2, playerId.value.toString())
            statement.executeQuery().use { result ->
                buildMap { while (result.next()) put(GemColor.valueOf(result.getString("color")), result.getInt("amount")) }
            }
        }

    private fun readTokens(connection: Connection, table: String, gameId: String): GemTokens {
        require(table == "table_tokens" || table == "turn_tokens")
        val tokens = GemTokens()
        connection.prepareStatement("SELECT color,amount FROM $table WHERE game_id=?").use { statement ->
            statement.setString(1, gameId)
            statement.executeQuery().use { result ->
                while (result.next()) {
                    val amount = result.getInt("amount")
                    if (amount > 0) tokens.add(GemColor.valueOf(result.getString("color")), amount)
                }
            }
        }
        return tokens
    }

    private fun queryCards(connection: Connection, sql: String, vararg params: String): List<DevelopmentCard> =
        connection.prepareStatement(sql).use { statement ->
            params.forEachIndexed { index, value -> statement.setString(index + 1, value) }
            statement.executeQuery().use { result ->
                buildList { while (result.next()) add(readCard(connection, result.getString(1))) }
            }
        }

    private fun queryIds(connection: Connection, sql: String, vararg params: String): List<String> =
        connection.prepareStatement(sql).use { statement ->
            params.forEachIndexed { index, value -> statement.setString(index + 1, value) }
            statement.executeQuery().use { result -> buildList { while (result.next()) add(result.getString(1)) } }
        }

    private fun readCard(connection: Connection, rawId: String): DevelopmentCard {
        val row = connection.prepareStatement("SELECT level,bonus,points FROM cards WHERE id=?").use { statement ->
            statement.setString(1, rawId)
            statement.executeQuery().use { result ->
                check(result.next()) { "Card $rawId not found" }
                Triple(CardLevel.valueOf(result.getString(1)), GemColor.valueOf(result.getString(2)), result.getInt(3))
            }
        }
        val cost = connection.prepareStatement("SELECT color,amount FROM card_costs WHERE card_id=?").use { statement ->
            statement.setString(1, rawId)
            statement.executeQuery().use { result ->
                buildMap { while (result.next()) put(GemColor.valueOf(result.getString(1)), result.getInt(2)) }
            }
        }
        return DevelopmentCard(CardId(UUID.fromString(rawId)), row.first, row.second, row.third, GemCost(cost))
    }

    private fun readNoble(connection: Connection, rawId: String): NobleTile {
        val points = connection.prepareStatement("SELECT points FROM nobles WHERE id=?").use { statement ->
            statement.setString(1, rawId)
            statement.executeQuery().use { result -> check(result.next()); result.getInt(1) }
        }
        val requirements = connection.prepareStatement("SELECT color,amount FROM noble_requirements WHERE noble_id=?").use { statement ->
            statement.setString(1, rawId)
            statement.executeQuery().use { result ->
                buildMap { while (result.next()) put(GemColor.valueOf(result.getString(1)), result.getInt(2)) }
            }
        }
        check(points == 3) { "Unsupported noble point value: $points" }
        return NobleTile(NobleId(UUID.fromString(rawId)), GemCost(requirements))
    }

    private fun readMoveHistory(connection: Connection, gameId: GameId): MoveHistory {
        val history = MoveHistory()
        connection.prepareStatement("SELECT player_id,move_type,payload,created_at FROM moves WHERE game_id=? ORDER BY move_number").use { statement ->
            statement.setString(1, gameId.value.toString())
            statement.executeQuery().use { result ->
                while (result.next()) {
                    val playerId = playerId(result.getString("player_id"))
                    val move = decodeMove(result.getString("move_type"), result.getString("payload"))
                    history.record(playerId, move, Instant.parse(result.getString("created_at")))
                }
            }
        }
        return history
    }

    private fun encodeMove(move: Move): Triple<String, String, String> = when (move) {
        is TakeGemsMove -> Triple("TAKE_GEMS", move.colors.joinToString(",") { it.name }, "Took ${move.colors.joinToString()} gems")
        is TakeTwoGemsMove -> Triple("TAKE_TWO_GEMS", move.color.name, "Took 2 ${move.color} gems")
        is ReserveCardMove -> when (val target = move.target) {
            is ReserveCardTarget.OpenCard -> Triple("RESERVE_OPEN", target.cardId.value.toString(), "Reserved card ${target.cardId.value}")
            is ReserveCardTarget.BlindCard -> Triple("RESERVE_BLIND", target.level.name, "Reserved a blind ${target.level} card")
        }
        is PurchaseCardMove -> Triple("PURCHASE_CARD", move.cardId.value.toString(), "Purchased card ${move.cardId.value}")
    }

    private fun decodeMove(type: String, payload: String): Move = when (type) {
        "TAKE_GEMS" -> TakeGemsMove(payload.split(',').filter(String::isNotBlank).map(GemColor::valueOf))
        "TAKE_TWO_GEMS" -> TakeTwoGemsMove(GemColor.valueOf(payload))
        "RESERVE_OPEN" -> ReserveCardMove(ReserveCardTarget.OpenCard(CardId(UUID.fromString(payload))))
        "RESERVE_BLIND" -> ReserveCardMove(ReserveCardTarget.BlindCard(CardLevel.valueOf(payload)))
        "PURCHASE_CARD" -> PurchaseCardMove(CardId(UUID.fromString(payload)))
        else -> error("Unsupported persisted move type: $type")
    }

    private fun playerId(value: String) = PlayerId(UUID.fromString(value))

    private fun insert(connection: Connection, sql: String, vararg values: Any?) {
        connection.prepareStatement(sql).use { statement ->
            values.forEachIndexed { index, value ->
                when (value) {
                    null -> statement.setObject(index + 1, null)
                    is String -> statement.setString(index + 1, value)
                    is Int -> statement.setInt(index + 1, value)
                    else -> statement.setObject(index + 1, value)
                }
            }
            statement.executeUpdate()
        }
    }

    private fun recalculateStatistics(connection: Connection) {
        connection.createStatement().use { statement -> statement.executeUpdate("DELETE FROM player_statistics") }
        connection.createStatement().use { statement ->
            statement.executeUpdate("""
                INSERT INTO player_statistics(player_id,games_played,games_won,total_points)
                SELECT p.id,
                       COALESCE(SUM(CASE WHEN g.status='FINISHED' THEN 1 ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN g.status='FINISHED' AND gp.is_winner=1 THEN 1 ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN g.status='FINISHED' THEN gp.final_score ELSE 0 END),0)
                FROM players p
                LEFT JOIN game_players gp ON gp.player_id=p.id
                LEFT JOIN games g ON g.id=gp.game_id
                GROUP BY p.id
            """.trimIndent())
        }
    }

    private data class GameRow(
        val status: GameStatus,
        val currentIndex: Int,
        val finalRoundPlayerId: PlayerId?,
        val turnPlayerId: PlayerId?,
        val turnPhase: TurnPhase?
    )
}
