package splendor.system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import splendor.application.services.GameService
import splendor.application.services.PlayerService
import splendor.application.services.StatisticsService
import splendor.domain.components.*
import splendor.domain.gamemodel.*
import splendor.domain.identifiers.CardId
import splendor.domain.identifiers.NobleId
import splendor.application.dto.GameCommand
import splendor.domain.players.Player
import splendor.domain.gamemodel.GameFactory
import splendor.infrastructure.database.SqliteDatabase
import splendor.infrastructure.repositories.SqliteGameRepository
import splendor.infrastructure.repositories.SqlitePlayerRepository
import splendor.infrastructure.repositories.SqlitePlayerStatisticsRepository
import java.nio.file.Path

class SqlitePersistenceSystemTest {
    @TempDir lateinit var tempDir: Path

    @Test
    fun `player service game completion history and statistics survive application restart`() {
        val dbFile = tempDir.resolve("application.db")
        val db1 = SqliteDatabase(dbFile)
        val playerRepo1 = SqlitePlayerRepository(db1)
        val gameRepo1 = SqliteGameRepository(db1)
        val statsRepo1 = SqlitePlayerStatisticsRepository(db1)
        val playerService = PlayerService(playerRepo1)
        val aliceInfo = playerService.registerPlayer("Alice")
        val bobInfo = playerService.registerPlayer("Bob")
        val alice = playerRepo1.findAll().first { it.nickname == "Alice" }
        val bob = playerRepo1.findAll().first { it.nickname == "Bob" }
        val gameService1 = GameService(gameRepo1, playerRepo1, GameFactory())
        val game = gameService1.createGame(
            listOf(alice.id, bob.id),
            createCards(),
            createNobles()
        )
        game.getParticipants().forEach { participant ->
            repeat(5) {
                assertTrue(participant.state.purchaseCard(
                    DevelopmentCard(CardId.generate(), CardLevel.I, GemColor.WHITE, 3, GemCost(emptyMap()))
                ))
            }
        }
        gameRepo1.save(game)
        assertTrue(gameService1.startGame(game.id.value.toString()).accepted)
        submitTurn(gameService1, game.id.value.toString(), alice.id.value.toString())
        submitTurn(gameService1, game.id.value.toString(), bob.id.value.toString())

        val reopenedDb = SqliteDatabase(dbFile)
        val playerRepo2 = SqlitePlayerRepository(reopenedDb)
        val gameRepo2 = SqliteGameRepository(reopenedDb)
        val statsRepo2 = SqlitePlayerStatisticsRepository(reopenedDb)
        val restored = assertNotNull(gameRepo2.findById(game.id))
        assertEquals(GameStatus.FINISHED, restored.status)
        assertEquals(2, restored.getMoveHistory().size)
        assertEquals(setOf("Alice", "Bob"), restored.getWinners().map { it.nicknameAtStart }.toSet())
        assertEquals(aliceInfo.id, playerRepo2.findById(alice.id)?.id)
        assertEquals(bobInfo.id, playerRepo2.findById(bob.id)?.id)

        val statisticsService = StatisticsService(playerRepo2, gameRepo2, statsRepo2)
        val firstStats = statisticsService.getPlayerStatistics().associateBy { it.nickname }
        assertEquals(1, firstStats.getValue("Alice").gamesPlayed)
        assertEquals(1, firstStats.getValue("Alice").wins)
        assertEquals(100.0, firstStats.getValue("Alice").winPercentage)
        assertEquals(15, firstStats.getValue("Alice").totalPoints)

        // Re-saving an already completed game is idempotent for aggregate statistics.
        gameRepo2.save(restored)
        statisticsService.recalculate()
        assertEquals(1, statsRepo2.findByPlayerId(alice.id)?.gamesPlayed)
        assertEquals(1, statsRepo2.findByPlayerId(alice.id)?.gamesWon)

        // A second completed result updates, rather than replaces, earlier aggregates.
        val gameService2 = GameService(gameRepo2, playerRepo2, GameFactory())
        val secondGame = gameService2.createGame(
            listOf(alice.id, bob.id),
            createCards(),
            createNobles()
        )
        secondGame.getParticipants().forEach { participant ->
            repeat(5) {
                participant.state.purchaseCard(
                    DevelopmentCard(CardId.generate(), CardLevel.I, GemColor.BLUE, 3, GemCost(emptyMap()))
                )
            }
        }
        gameRepo2.save(secondGame)
        assertTrue(gameService2.startGame(secondGame.id.value.toString()).accepted)
        submitTurn(gameService2, secondGame.id.value.toString(), alice.id.value.toString())
        submitTurn(gameService2, secondGame.id.value.toString(), bob.id.value.toString())

        val finalDb = SqliteDatabase(dbFile)
        val finalPlayers = SqlitePlayerRepository(finalDb)
        val finalGames = SqliteGameRepository(finalDb)
        val finalStats = SqlitePlayerStatisticsRepository(finalDb)
        assertEquals(2, finalGames.findAll().count { it.status == GameStatus.FINISHED })
        assertEquals(2, finalStats.findByPlayerId(alice.id)?.gamesPlayed)
        assertEquals(2, finalStats.findByPlayerId(alice.id)?.gamesWon)
        assertEquals(30, finalStats.findByPlayerId(alice.id)?.totalPoints)
        assertEquals(2, StatisticsService(finalPlayers, finalGames, finalStats)
            .getPlayerStatistics().first { it.nickname == "Alice" }.gamesPlayed)
    }

    private fun submitTurn(service: GameService, gameId: String, playerId: String) {
        val result = service.submitCommand(
            gameId,
            playerId,
            GameCommand.TakeGems(listOf("WHITE", "BLUE", "GREEN"))
        )
        assertTrue(result.accepted, result.reason ?: "Move rejected")
    }

    private fun createCards(): Map<CardLevel, List<DevelopmentCard>> =
        CardLevel.entries.associateWith { level ->
            (1..4).map {
                DevelopmentCard(CardId.generate(), level, GemColor.WHITE, 0, GemCost(emptyMap()))
            }
        }

    private fun createNobles(): List<NobleTile> = (1..3).map {
        NobleTile(NobleId.generate(), GemCost(mapOf(GemColor.BLACK to 4)))
    }
}
