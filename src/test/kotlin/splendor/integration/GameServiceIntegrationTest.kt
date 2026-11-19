package splendor.integration

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import splendor.application.services.GameService
import splendor.domain.components.*
import splendor.domain.gamemodel.GameFactory
import splendor.domain.identifiers.*
import splendor.domain.players.Player
import splendor.infrastructure.repositories.InMemoryGameRepository
import splendor.infrastructure.repositories.InMemoryPlayerRepository

class GameServiceIntegrationTest {

    @Test
    fun `players should be used to create and store a game`() {
        // Arrange: настоящие репозитории и фабрика
        val playerRepository = InMemoryPlayerRepository()
        val gameRepository = InMemoryGameRepository()

        val player1 = Player(PlayerId.generate(), "Alice")
        val player2 = Player(PlayerId.generate(), "Bob")

        playerRepository.save(player1)
        playerRepository.save(player2)

        val gameService = GameService(
            gameRepository = gameRepository,
            playerRepository = playerRepository,
            gameFactory = GameFactory(Random(42))
        )

        // Act: создаём игру по ID существующих игроков
        val game = gameService.createGame(
            playerIds = listOf(player1.id, player2.id),
            cards = createCards(),
            nobles = createNobles()
        )

        // Assert: игра сохранена и содержит нужных участников
        val savedGame = gameRepository.findById(game.id)

        assertNotNull(savedGame)
        assertEquals(
            setOf(player1.id, player2.id),
            savedGame!!.getParticipants().map { it.playerId }.toSet()
        )

        assertEquals(game, gameService.getGame(game.id))
        assertEquals(1, gameService.getAllGames().size)
    }

    private fun createCards(): Map<CardLevel, List<DevelopmentCard>> =
        CardLevel.entries.associateWith { level ->
            (1..4).map {
                DevelopmentCard(
                    id = CardId.generate(),
                    level = level,
                    bonus = GemColor.WHITE,
                    points = 0,
                    cost = GemCost(emptyMap())
                )
            }
        }

    private fun createNobles(): List<NobleTile> =
        (1..3).map {
            NobleTile(
                id = NobleId.generate(),
                requirements = GemCost(emptyMap())
            )
        }
}
