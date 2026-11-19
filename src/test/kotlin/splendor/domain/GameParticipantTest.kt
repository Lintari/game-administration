package splendor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import splendor.domain.gamemodel.GameParticipant
import splendor.domain.players.Player
import splendor.domain.identifiers.PlayerId

class GameParticipantTest {

    @Test
    fun `should create participant from player`() {
        val player = Player(
            id = PlayerId.generate(),
            nickname = "Alice"
        )

        val participant = GameParticipant(player)

        assertEquals(player.id, participant.playerId)
        assertEquals("Alice", participant.nicknameAtStart)
        assertEquals(player.id, participant.state.playerId)
    }

    @Test
    fun `should preserve nickname at game start`() {
        val player = Player(
            id = PlayerId.generate(),
            nickname = "Alice"
        )

        val participant = GameParticipant(player)

        player.rename("Bob")

        assertEquals("Bob", player.nickname)
        assertEquals("Alice", participant.nicknameAtStart)
    }

    @Test
    fun `each participant should have independent player state`() {
        val player = Player(
            id = PlayerId.generate(),
            nickname = "Alice"
        )

        val firstParticipant = GameParticipant(player)
        val secondParticipant = GameParticipant(player)

        assertNotSame(firstParticipant.state, secondParticipant.state)
    }
}
