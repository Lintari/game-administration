package splendor.domain.gamemodel

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import splendor.domain.components.*
import splendor.domain.identifiers.PlayerId
import splendor.domain.moves.*
import splendor.domain.players.Player

class GameTest {

    private fun createParticipant(name: String): GameParticipant {
        val player = Player(
            id = PlayerId.generate(),
            nickname = name
        )
        return GameParticipant(player)
    }

    private fun createTable(): Table {
        val tokens = GemTokens().apply {
            add(GemColor.WHITE, 4)
            add(GemColor.BLUE, 4)
            add(GemColor.GREEN, 4)
            add(GemColor.RED, 4)
            add(GemColor.BLACK, 4)
            add(GemColor.GOLD, 5)
        }

        val decks = CardLevel.entries.associateWith { level ->
            DevelopmentCardDeck(level, emptyList())
        }

        val openCards = CardLevel.entries.associateWith {
            emptyList<DevelopmentCard>()
        }

        return Table(
            tokens = tokens,
            openCards = openCards,
            decks = decks,
            nobles = emptyList()
        )
    }

    private fun createGame(): Game {
        return Game(
            participants = listOf(
                createParticipant("Alice"),
                createParticipant("Bob")
            ),
            table = createTable()
        )
    }

    @Test
    fun `game should start with first participant`() {
        val game = createGame()

        game.start()

        assertEquals(GameStatus.IN_PROGRESS, game.status)
        assertEquals("Alice", game.getCurrentParticipant()?.nicknameAtStart)
        assertEquals(
            TurnPhase.PLAYER_ACTION,
            game.getCurrentTurnContext()?.phase
        )
    }

    @Test
    fun `taking three different gems should complete turn`() {
        val game = createGame()
        game.start()

        val participant = game.getCurrentParticipant()!!

        val result = game.submitMove(
            TakeGemsMove(
                listOf(
                    GemColor.WHITE,
                    GemColor.BLUE,
                    GemColor.GREEN
                )
            )
        )

        assertEquals(MoveResult.Accepted, result)

        assertEquals(1, participant.state.getTokenAmount(GemColor.WHITE))
        assertEquals(1, participant.state.getTokenAmount(GemColor.BLUE))
        assertEquals(1, participant.state.getTokenAmount(GemColor.GREEN))

        assertEquals(3, game.table.getTokenAmount(GemColor.WHITE))
        assertEquals("Bob", game.getCurrentParticipant()?.nicknameAtStart)
        assertEquals(1, game.getMoveHistory().size)
    }

    @Test
    fun `invalid move should not change game state`() {
        val game = createGame()
        game.start()

        val participant = game.getCurrentParticipant()!!

        // Нельзя взять два одинаковых жетона, если их на столе меньше четырёх.
        val result = game.submitMove(
            TakeTwoGemsMove(GemColor.GOLD)
        )

        assertEquals(
            MoveResult.Rejected(MoveRejection.INVALID_MOVE),
            result
        )

        assertEquals("Alice", game.getCurrentParticipant()?.nicknameAtStart)
        assertEquals(0, participant.state.getTokens().total())
        assertEquals(0, game.getMoveHistory().size)
    }
}
