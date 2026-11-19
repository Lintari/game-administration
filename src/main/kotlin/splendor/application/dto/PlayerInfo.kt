package splendor.application.dto

import splendor.domain.identifiers.PlayerId

data class PlayerInfo(
    val id: PlayerId,
    val nickname: String
)
