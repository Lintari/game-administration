package splendor.infrastructure.repositories

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.io.TempDir
import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player
import splendor.infrastructure.database.SqliteDatabase
import java.nio.file.Path

class SqlitePlayerStatisticsRepositoryTest {
    @TempDir lateinit var tempDir: Path

    @Test
    fun `creates zero statistics and supports recalculation`() {
        val database = SqliteDatabase(tempDir.resolve("stats.db"))
        val playerRepository = SqlitePlayerRepository(database)
        val statisticsRepository = SqlitePlayerStatisticsRepository(database)
        val player = Player(PlayerId.generate(), "Alice")
        playerRepository.save(player)

        val before = assertNotNull(statisticsRepository.findByPlayerId(player.id))
        assertEquals(0, before.gamesPlayed)
        assertEquals(0, before.gamesWon)
        assertEquals(0, before.totalPoints)

        statisticsRepository.recalculate()

        assertEquals(before, statisticsRepository.findByPlayerId(player.id))
        assertEquals(1, statisticsRepository.findAll().size)
    }
}
