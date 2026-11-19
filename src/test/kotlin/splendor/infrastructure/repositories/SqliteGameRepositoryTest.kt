package splendor.infrastructure.repositories

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.io.TempDir
import splendor.domain.components.*
import splendor.domain.gamemodel.*
import splendor.domain.identifiers.*
import splendor.domain.moves.TakeGemsMove
import splendor.domain.players.Player
import splendor.infrastructure.database.SqliteDatabase
import java.nio.file.Path
import java.sql.SQLException

class SqliteGameRepositoryTest {
    @TempDir lateinit var tempDir: Path

    private fun game(): Game {
        val a = Player(PlayerId.generate(), "Alice")
        val b = Player(PlayerId.generate(), "Bob")
        val participants = listOf(GameParticipant(a), GameParticipant(b))
        val openCard = DevelopmentCard(
            CardId.generate(), CardLevel.I, GemColor.BLUE, 1,
            GemCost(mapOf(GemColor.RED to 2))
        )
        val deckCard = DevelopmentCard(
            CardId.generate(), CardLevel.I, GemColor.RED, 0,
            GemCost(emptyMap())
        )
        val noble = NobleTile(NobleId.generate(), GemCost(mapOf(GemColor.WHITE to 3)))
        val table = Table(
            tokens = GemTokens().apply {
                add(GemColor.WHITE, 4); add(GemColor.BLUE, 4); add(GemColor.GREEN, 4)
            },
            openCards = mapOf(CardLevel.I to listOf(openCard)),
            decks = CardLevel.entries.associateWith { level ->
                DevelopmentCardDeck(level, if (level == CardLevel.I) listOf(deckCard) else emptyList())
            },
            nobles = listOf(noble)
        )
        return Game(participants, table)
    }

    @Test
    fun `saves and restores active game state cards tokens and move history`() {
        val file = tempDir.resolve("game-test.db")
        val database = SqliteDatabase(file)
        val playerRepository = SqlitePlayerRepository(database)
        val gameRepository = SqliteGameRepository(database)
        val game = game()
        game.getParticipants().forEach { playerRepository.save(it.player) }
        game.start()
        assertEquals(
            splendor.domain.moves.MoveResult.Accepted,
            game.submitMove(TakeGemsMove(listOf(GemColor.WHITE, GemColor.BLUE, GemColor.GREEN)))
        )
        gameRepository.save(game)

        val restored = SqliteGameRepository(SqliteDatabase(file)).findById(game.id)

        assertNotNull(restored)
        assertEquals(game.id, restored.id)
        assertEquals(GameStatus.IN_PROGRESS, restored.status)
        assertEquals("Bob", restored.getCurrentParticipant().nicknameAtStart)
        assertEquals(1, restored.getParticipants().first().state.getTokenAmount(GemColor.WHITE))
        assertEquals(3, restored.table.getTokenAmount(GemColor.WHITE))
        assertEquals(1, restored.table.getOpenCards(CardLevel.I).size)
        assertEquals(1, restored.table.getDeckRemainingCount(CardLevel.I))
        assertEquals(1, restored.table.getNobles().size)
        assertEquals(1, restored.getMoveHistory().size)
        assertEquals(TurnPhase.PLAYER_ACTION, restored.getCurrentTurnContext().phase)
    }

    @Test
    fun `failed statistics update rolls back game and move persistence`() {
        val file = tempDir.resolve("rollback-test.db")
        val database = SqliteDatabase(file)
        val players = SqlitePlayerRepository(database)
        val repository = SqliteGameRepository(database)
        val game = game()
        game.getParticipants().forEach { players.save(it.player) }
        repository.save(game)

        database.withConnection { connection ->
            connection.createStatement().use {
                it.execute("CREATE TRIGGER fail_statistics_insert BEFORE INSERT ON player_statistics BEGIN SELECT RAISE(ABORT, 'forced failure'); END")
            }
        }
        game.start()
        game.submitMove(TakeGemsMove(listOf(GemColor.WHITE, GemColor.BLUE, GemColor.GREEN)))

        assertThrows(SQLException::class.java) { repository.save(game) }

        val restored = SqliteGameRepository(SqliteDatabase(file)).findById(game.id)
        assertNotNull(restored)
        assertEquals(GameStatus.NOT_STARTED, restored.status)
        assertEquals(0, restored.getMoveHistory().size)
        assertEquals(4, restored.table.getTokenAmount(GemColor.WHITE))
        assertEquals(0, SqlitePlayerStatisticsRepository(SqliteDatabase(file))
            .findByPlayerId(restored.getParticipants().first().playerId)?.gamesPlayed)
    }

    @Test
    fun `returns null for absent game`() {
        val repository = SqliteGameRepository(SqliteDatabase(tempDir.resolve("empty.db")))
        assertNull(repository.findById(GameId.generate()))
    }
}
