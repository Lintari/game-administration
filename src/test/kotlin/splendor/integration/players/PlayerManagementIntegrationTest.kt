package splendor.integration.players

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

import splendor.application.services.PlayerService
import splendor.domain.identifiers.PlayerId
import splendor.infrastructure.repositories.InMemoryPlayerRepository

class PlayerManagementIntegrationTest {

    private val repository = InMemoryPlayerRepository()
    private val service = PlayerService(repository)

    @Test
    fun `registered player can be retrieved from repository`() {
        val registered = service.registerPlayer("Alice")

        val stored = repository.findById(registered.id)

        assertNotNull(stored)
        assertEquals("Alice", stored.nickname)
    }

    @Test
    fun `registered players can be retrieved through service`() {
        service.registerPlayer("Alice")
        service.registerPlayer("Bob")

        val players = service.getAllPlayers()

        assertEquals(2, players.size)
        assertEquals(
            setOf("Alice", "Bob"),
            players.map { it.nickname }.toSet()
        )
    }

    @Test
    fun `renamed player has updated nickname in repository`() {
        val registered = service.registerPlayer("Alice")

        val stored = repository.findById(registered.id)
        assertNotNull(stored)

        stored.rename("AliceNew")

        val retrieved = service.getPlayer(registered.id)

        assertNotNull(retrieved)
        assertEquals("AliceNew", retrieved.nickname)
    }

    @Test
    fun `unknown player cannot be retrieved`() {
        val result = service.getPlayer(PlayerId.generate())

        assertNull(result)
    }
}
