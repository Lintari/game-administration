package splendor.infrastructure.repositories

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.io.TempDir
import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player
import splendor.infrastructure.database.SqliteDatabase
import java.nio.file.Path

class SqlitePlayerRepositoryTest {
    @TempDir lateinit var tempDir: Path

    @Test
    fun `save find update and list players across database reopen`() {
        val databaseFile = tempDir.resolve("players-test.db")
        val firstDatabase = SqliteDatabase(databaseFile)
        val repository = SqlitePlayerRepository(firstDatabase)
        val alice = Player(PlayerId.generate(), "Alice")
        val bob = Player(PlayerId.generate(), "Bob")

        repository.save(alice)
        repository.save(bob)
        repository.save(Player(alice.id, "Alice Updated"))

        assertEquals("Alice Updated", repository.findById(alice.id)?.nickname)
        assertNull(repository.findById(PlayerId.generate()))
        assertEquals(setOf(alice.id, bob.id), repository.findAll().map { it.id }.toSet())

        val reopened = SqlitePlayerRepository(SqliteDatabase(databaseFile))
        assertEquals("Alice Updated", reopened.findById(alice.id)?.nickname)
        assertEquals(2, reopened.findAll().size)
    }
}
