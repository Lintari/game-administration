package splendor.application.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

import splendor.application.repositories.PlayerRepository
import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player

class PlayerServiceTest {

    private class FakePlayerRepository : PlayerRepository {
        private val players = mutableMapOf<PlayerId, Player>()

        override fun save(player: Player) {
            players[player.id] = player
        }

        override fun findById(id: PlayerId): Player? {
            return players[id]
        }

        override fun findAll(): List<Player> {
            return players.values.toList()
        }
    }

    private val repository = FakePlayerRepository()
    private val service = PlayerService(repository)

    @Test
    fun `registerPlayer creates and stores player`() {
        val result = service.registerPlayer("Alice")

        assertEquals("Alice", result.nickname)

        val storedPlayer = repository.findById(result.id)

        assertNotNull(storedPlayer)
        assertEquals(result.nickname, storedPlayer.nickname)
    }

    @Test
    fun `getPlayer returns player info`() {
        val registered = service.registerPlayer("Alice")

        val result = service.getPlayer(registered.id)

        assertNotNull(result)
        assertEquals(registered, result)
    }

    @Test
    fun `getPlayer returns null for unknown id`() {
        val result = service.getPlayer(PlayerId.generate())

        assertNull(result)
    }

    @Test
    fun `getAllPlayers returns all registered players`() {
        service.registerPlayer("Alice")
        service.registerPlayer("Bob")

        val result = service.getAllPlayers()

        assertEquals(2, result.size)
        assertEquals(
            setOf("Alice", "Bob"),
            result.map { it.nickname }.toSet()
        )
    }
}
