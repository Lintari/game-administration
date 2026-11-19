package splendor.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.io.TempDir
import splendor.application.services.GameService
import splendor.application.services.PlayerService
import splendor.domain.components.*
import splendor.domain.gamemodel.GameFactory
import splendor.domain.identifiers.CardId
import splendor.domain.identifiers.NobleId
import splendor.infrastructure.database.SqliteDatabase
import splendor.infrastructure.repositories.SqliteGameRepository
import splendor.infrastructure.repositories.SqlitePlayerRepository
import java.nio.file.Path

class SqliteGameServiceIntegrationTest {
    @TempDir lateinit var tempDir: Path

    @Test
    fun `player and game services use sqlite repositories`() {
        val file = tempDir.resolve("services.db")
        val db = SqliteDatabase(file)
        val playerRepository = SqlitePlayerRepository(db)
        val gameRepository = SqliteGameRepository(db)
        val playerService = PlayerService(playerRepository)
        val alice = playerService.registerPlayer("Alice")
        val bob = playerService.registerPlayer("Bob")
        val gameService = GameService(gameRepository, playerRepository, GameFactory())
        val cards = CardLevel.entries.associateWith { level ->
            (1..4).map { DevelopmentCard(CardId.generate(), level, GemColor.BLUE, 0, GemCost(emptyMap())) }
        }
        val nobles = (1..3).map {
            NobleTile(NobleId.generate(), GemCost(mapOf(GemColor.BLACK to 4)))
        }

        val game = gameService.createGame(
            listOf(
                splendor.domain.identifiers.PlayerId(alice.id.value),
                splendor.domain.identifiers.PlayerId(bob.id.value)
            ),
            cards,
            nobles
        )
        val reopenedPlayers = SqlitePlayerRepository(SqliteDatabase(file))
        val reopenedGames = SqliteGameRepository(SqliteDatabase(file))

        assertEquals("Alice", reopenedPlayers.findAll().first { it.nickname == "Alice" }.nickname)
        assertNotNull(reopenedGames.findById(game.id))
        assertEquals(2, reopenedGames.findById(game.id)?.getParticipants()?.size)
    }
}
