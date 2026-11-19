package splendor.domain.players

import kotlin.test.Test
import kotlin.test.assertFailsWith

import splendor.domain.identifiers.PlayerId

class PlayerTest {

    @Test
    fun `creation rejects blank nickname`() {
        assertFailsWith<IllegalArgumentException> {
            Player(PlayerId.generate(), "")
        }
    }
}
