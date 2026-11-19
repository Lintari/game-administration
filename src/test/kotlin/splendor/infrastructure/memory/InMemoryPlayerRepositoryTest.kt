package splendor.infrastructure.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player
import splendor.infrastructure.repositories.InMemoryPlayerRepository

class InMemoryPlayerRepositoryTest {

    private val repository = InMemoryPlayerRepository()

    @Test
    fun `save stores player`() {
        val player = Player(PlayerId.generate(), "Alice")

        repository.save(player)

        assertEquals(player, repository.findById(player.id))
    }

    @Test
    fun `findById returns null for unknown id`() {
        val result = repository.findById(PlayerId.generate())

        assertNull(result)
    }

    @Test
    fun `findAll returns all saved players`() {
        val alice = Player(PlayerId.generate(), "Alice")
        val bob = Player(PlayerId.generate(), "Bob")

        repository.save(alice)
        repository.save(bob)

        assertEquals(listOf(alice, bob), repository.findAll())
    }

    @Test
    fun `save replaces player with same id`() {
        val id = PlayerId.generate()
        val alice = Player(id, "Alice")
        val updatedAlice = Player(id, "AliceUpdated")

        repository.save(alice)
        repository.save(updatedAlice)

        assertEquals(updatedAlice, repository.findById(id))
        assertEquals(1, repository.findAll().size)
    }
}
