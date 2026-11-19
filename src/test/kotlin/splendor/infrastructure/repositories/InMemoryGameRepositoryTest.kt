package splendor.infrastructure.repositories

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import splendor.domain.components.CardLevel
import splendor.domain.components.GemTokens
import splendor.domain.gamemodel.Game
import splendor.domain.gamemodel.GameParticipant
import splendor.domain.gamemodel.Table
import splendor.domain.players.Player
import splendor.domain.identifiers.PlayerId

class InMemoryGameRepositoryTest {

    private val repository = InMemoryGameRepository()

    private fun createGame(): Game {
        val players = listOf(
            Player(PlayerId.generate(), "Alice"),
            Player(PlayerId.generate(), "Bob")
        )

        val participants = players.map { GameParticipant(it) }

        val emptyDecks = CardLevel.entries.associateWith { level ->
            splendor.domain.components.DevelopmentCardDeck(level, emptyList())
        }

        val table = Table(
            tokens = GemTokens(),
            openCards = emptyMap(),
            decks = emptyDecks,
            nobles = emptyList()
        )

        return Game(participants, table)
    }

    @Test
    fun `save and find game by id`() {
        val game = createGame()

        repository.save(game)

        assertSame(game, repository.findById(game.id))
    }

    @Test
    fun `find by id returns null for unknown game`() {
        val game = createGame()

        assertNull(repository.findById(game.id))
    }

    @Test
    fun `save replaces game with same id`() {
        val game = createGame()

        repository.save(game)
        repository.save(game)

        assertEquals(1, repository.findAll().size)
        assertSame(game, repository.findById(game.id))
    }

    @Test
    fun `find all returns saved games`() {
        val first = createGame()
        val second = createGame()

        repository.save(first)
        repository.save(second)

        assertEquals(2, repository.findAll().size)
        assertEquals(
            setOf(first.id, second.id),
            repository.findAll().map { it.id }.toSet()
        )
    }

    @Test
    fun `find all returns empty list initially`() {
        assertEquals(emptyList(), repository.findAll())
    }
}
