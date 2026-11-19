package splendor.application.services

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import splendor.application.repositories.GameRepository
import splendor.application.repositories.PlayerRepository
import splendor.domain.components.*
import splendor.domain.gamemodel.Game
import splendor.domain.gamemodel.GameFactory
import splendor.domain.identifiers.*
import splendor.domain.players.Player

class GameServiceTest {

    private val playerRepository = TestPlayerRepository()
    private val gameRepository = TestGameRepository()

    private val player1 = Player(PlayerId.generate(), "Alice")
    private val player2 = Player(PlayerId.generate(), "Bob")

    private val service = GameService(
        gameRepository = gameRepository,
        playerRepository = playerRepository,
        gameFactory = GameFactory(Random(42))
    )

    init {
        playerRepository.save(player1)
        playerRepository.save(player2)
    }

    @Test
    fun `createGame should create and save game`() {
        val game = service.createGame(
            playerIds = listOf(player1.id, player2.id),
            cards = createCards(),
            nobles = createNobles()
        )

        assertNotNull(gameRepository.findById(game.id))
        assertEquals(game, gameRepository.findById(game.id))
        assertEquals(
            setOf(player1.id, player2.id),
            game.getParticipants().map { it.playerId }.toSet()
        )
    }

    @Test
    fun `createGame should reject duplicate player IDs`() {
        assertThrows<IllegalArgumentException> {
            service.createGame(
                playerIds = listOf(player1.id, player1.id),
                cards = createCards(),
                nobles = createNobles()
            )
        }

        assertTrue(gameRepository.findAll().isEmpty())
    }

    @Test
    fun `createGame should reject unknown player ID`() {
        val unknownId = PlayerId.generate()

        assertThrows<IllegalArgumentException> {
            service.createGame(
                playerIds = listOf(player1.id, unknownId),
                cards = createCards(),
                nobles = createNobles()
            )
        }

        assertTrue(gameRepository.findAll().isEmpty())
    }

    @Test
    fun `getGame should return game by ID`() {
        val createdGame = createGame()

        val result = service.getGame(createdGame.id)

        assertEquals(createdGame, result)
    }

    @Test
    fun `getGame should return null for unknown ID`() {
        assertNull(service.getGame(GameId.generate()))
    }

    @Test
    fun `getAllGames should return all saved games`() {
        val first = createGame()
        val second = createGame()

        val result = service.getAllGames()

        assertEquals(2, result.size)
        assertTrue(result.contains(first))
        assertTrue(result.contains(second))
    }

    private fun createGame(): Game =
        service.createGame(
            playerIds = listOf(player1.id, player2.id),
            cards = createCards(),
            nobles = createNobles()
        )

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

    private class TestPlayerRepository : PlayerRepository {
        private val players = mutableMapOf<PlayerId, Player>()

        override fun save(player: Player) {
            players[player.id] = player
        }

        override fun findById(id: PlayerId): Player? =
            players[id]

        override fun findAll(): List<Player> =
            players.values.toList()
    }

    private class TestGameRepository : GameRepository {
        private val games = mutableMapOf<GameId, Game>()

        override fun save(game: Game) {
            games[game.id] = game
        }

        override fun findById(id: GameId): Game? =
            games[id]

        override fun findAll(): List<Game> =
            games.values.toList()
    }
}
