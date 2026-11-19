package splendor.domain.gamemodel

import splendor.domain.components.*
import kotlin.random.Random

class GameFactory(
    private val random: Random = Random.Default
) {

    fun createGame(
        participants: List<GameParticipant>,
        cards: Map<CardLevel, List<DevelopmentCard>>,
        nobles: List<NobleTile>
    ): Game {
        require(participants.size in 2..4) {
            "A game must have 2 to 4 participants"
        }

        require(participants.map { it.playerId }.distinct().size == participants.size) {
            "Participants must have unique player IDs"
        }

        require(cards.keys.containsAll(CardLevel.entries)) {
            "Cards must be provided for every level"
        }

        require(nobles.size >= participants.size + 1) {
            "Not enough nobles for this game"
        }

        val shuffledCards = CardLevel.entries.associateWith { level ->
            cards.getValue(level).shuffled(random)
        }

        val openCards = shuffledCards.mapValues { (_, deck) ->
            deck.take(4)
        }

        val decks = CardLevel.entries.associateWith { level ->
            DevelopmentCardDeck(
                level = level,
                cards = shuffledCards.getValue(level).drop(4),
                random = random
            )
        }

        val tokensPerColor = when (participants.size) {
            2 -> 4
            3 -> 5
            else -> 7
        }

        val tokens = GemTokens().apply {
            GemColor.entries
                .filter { it != GemColor.GOLD }
                .forEach { add(it, tokensPerColor) }

            add(GemColor.GOLD, 5)
        }

        val selectedNobles = nobles.shuffled(random)
            .take(participants.size + 1)

        val table = Table(
            tokens = tokens,
            openCards = openCards,
            decks = decks,
            nobles = selectedNobles
        )

        return Game(
            participants = participants,
            table = table
        )
    }
}
