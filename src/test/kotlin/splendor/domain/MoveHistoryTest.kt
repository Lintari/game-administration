package splendor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import splendor.domain.gamemodel.MoveHistory
import splendor.domain.identifiers.PlayerId
import splendor.domain.moves.TakeTwoGemsMove
import splendor.domain.components.GemColor
import java.time.Instant

class MoveHistoryTest {

    @Test
    fun `should record move`() {
        val history = MoveHistory()
        val playerId = PlayerId.generate()
        val move = TakeTwoGemsMove(GemColor.BLUE)
        val timestamp = Instant.parse("2026-11-19T12:00:00Z")

        history.record(playerId, move, timestamp)

        val records = history.getAll()

        assertEquals(1, records.size)
        assertEquals(playerId, records.first().playerId)
        assertEquals(move, records.first().move)
        assertEquals(timestamp, records.first().timestamp)
    }

    @Test
    fun `should return moves by player`() {
        val history = MoveHistory()

        val alice = PlayerId.generate()
        val bob = PlayerId.generate()

        history.record(alice, TakeTwoGemsMove(GemColor.BLUE))
        history.record(bob, TakeTwoGemsMove(GemColor.RED))
        history.record(alice, TakeTwoGemsMove(GemColor.GREEN))

        assertEquals(2, history.getByPlayer(alice).size)
        assertEquals(1, history.getByPlayer(bob).size)
    }

    @Test
    fun `should return empty list for player without moves`() {
        val history = MoveHistory()

        assertEquals(0, history.getByPlayer(PlayerId.generate()).size)
    }

    @Test
    fun `should not expose mutable internal records`() {
        val history = MoveHistory()

        history.record(
            PlayerId.generate(),
            TakeTwoGemsMove(GemColor.BLUE)
        )

        val records = history.getAll()

        assertEquals(1, records.size)
        assertEquals(1, history.size())
    }
}
