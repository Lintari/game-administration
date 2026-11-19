package splendor.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import splendor.application.dto.GameCommand
import splendor.application.services.GameService
import splendor.domain.components.GemColor
import splendor.domain.gamemodel.GameFactory
import splendor.domain.players.Player
import splendor.domain.identifiers.PlayerId
import splendor.infrastructure.catalog.DefaultGameContentProvider
import splendor.infrastructure.repositories.InMemoryGameRepository
import splendor.infrastructure.repositories.InMemoryPlayerRepository

class GameApplicationApiIntegrationTest {
    @Test
    fun `application api creates starts and returns updated game view`() {
        val playerRepository = InMemoryPlayerRepository()
        val gameRepository = InMemoryGameRepository()
        val alice = Player(PlayerId.generate(), "Alice")
        val bob = Player(PlayerId.generate(), "Bob")
        playerRepository.save(alice)
        playerRepository.save(bob)
        val gameService = GameService(
            gameRepository = gameRepository,
            playerRepository = playerRepository,
            gameFactory = GameFactory(kotlin.random.Random(7)),
            contentProvider = DefaultGameContentProvider()
        )

        val game = gameService.createGameFromPlayerIds(
            listOf(alice.id.value.toString(), bob.id.value.toString())
        )
        assertTrue(gameService.startGame(game.id).accepted)
        val before = gameService.getGameView(game.id)!!
        assertEquals("PLAYER_ACTION", before.phase)

        val result = gameService.submitCommand(
            gameId = game.id,
            playerId = alice.id.value.toString(),
            command = GameCommand.TakeGems(listOf("WHITE", "BLUE", "GREEN"))
        )

        assertTrue(result.accepted)
        val after = gameService.getGameView(game.id)!!
        assertEquals("Bob", after.participants.single { it.isCurrent }.nickname)
        assertEquals(1, after.participants.first { it.nickname == "Alice" }.tokens.getValue(GemColor.WHITE.name))
        assertEquals(1, after.history.size)
        assertEquals("Took WHITE, BLUE, GREEN gems", after.history.single().description)
    }
}
