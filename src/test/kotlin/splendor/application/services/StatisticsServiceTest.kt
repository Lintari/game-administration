package splendor.application.services

import kotlin.test.Test
import kotlin.test.assertEquals
import splendor.domain.components.CardLevel
import splendor.domain.components.DevelopmentCard
import splendor.domain.components.DevelopmentCardDeck
import splendor.domain.components.GemColor
import splendor.domain.components.GemCost
import splendor.domain.components.GemTokens
import splendor.domain.gamemodel.Game
import splendor.domain.gamemodel.GameParticipant
import splendor.domain.gamemodel.Table
import splendor.domain.identifiers.CardId
import splendor.domain.identifiers.PlayerId
import splendor.domain.moves.TakeGemsMove
import splendor.domain.players.Player
import splendor.infrastructure.repositories.InMemoryGameRepository
import splendor.infrastructure.repositories.InMemoryPlayerRepository

class StatisticsServiceTest {
    @Test
    fun `statistics include registered players with no completed games`() {
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        val player = Player(PlayerId.generate(), "Alice")
        players.save(player)

        val stats = StatisticsService(players, games).getPlayerStatistics().single()

        assertEquals(player.nickname, stats.nickname)
        assertEquals(0, stats.gamesPlayed)
        assertEquals(0, stats.wins)
        assertEquals(0.0, stats.winPercentage)
    }

    @Test
    fun `statistics count finished games and winners`() {
        val playerRepository = InMemoryPlayerRepository()
        val gameRepository = InMemoryGameRepository()
        val alice = Player(PlayerId.generate(), "Alice")
        val bob = Player(PlayerId.generate(), "Bob")
        playerRepository.save(alice)
        playerRepository.save(bob)
        val participants = listOf(GameParticipant(alice), GameParticipant(bob))
        participants.forEach { participant ->
            repeat(5) {
                participant.state.purchaseCard(
                    DevelopmentCard(
                        id = CardId.generate(),
                        level = CardLevel.I,
                        bonus = GemColor.WHITE,
                        points = 3,
                        cost = GemCost(emptyMap())
                    )
                )
            }
        }
        val table = Table(
            tokens = GemTokens().apply {
                add(GemColor.WHITE, 4)
                add(GemColor.BLUE, 4)
                add(GemColor.GREEN, 4)
            },
            openCards = emptyMap(),
            decks = CardLevel.entries.associateWith { DevelopmentCardDeck(it, emptyList()) },
            nobles = emptyList()
        )
        val game = Game(participants, table)
        game.start()
        game.submitMove(TakeGemsMove(listOf(GemColor.WHITE, GemColor.BLUE, GemColor.GREEN)))
        game.submitMove(TakeGemsMove(listOf(GemColor.WHITE, GemColor.BLUE, GemColor.GREEN)))
        gameRepository.save(game)

        val statistics = StatisticsService(playerRepository, gameRepository)
            .getPlayerStatistics().associateBy { it.nickname }

        assertEquals(1, statistics.getValue("Alice").gamesPlayed)
        assertEquals(1, statistics.getValue("Alice").wins)
        assertEquals(100.0, statistics.getValue("Alice").winPercentage)
        assertEquals(1, statistics.getValue("Bob").wins)
    }
}
