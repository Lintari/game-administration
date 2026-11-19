package splendor.domain.players

import splendor.domain.identifiers.PlayerId

class Player(
    val id: PlayerId,
    nickname: String
) {
    var nickname: String = nickname
        private set

    init {
        require(nickname.isNotBlank()) {
            "Nickname cannot be blank"
        }
    }

    fun rename(newNickname: String) {
        require(newNickname.isNotBlank()) {
            "Nickname cannot be blank"
        }

        nickname = newNickname
    }
}