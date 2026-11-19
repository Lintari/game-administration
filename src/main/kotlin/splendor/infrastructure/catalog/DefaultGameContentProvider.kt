package splendor.infrastructure.catalog

import splendor.application.content.GameContentProvider
import splendor.domain.components.*
import splendor.domain.identifiers.CardId
import splendor.domain.identifiers.NobleId

class DefaultGameContentProvider : GameContentProvider {

    private data class CardRow(
        val level: CardLevel,
        val bonus: GemColor,
        val points: Int,
        val cost: List<Int>
    )

    private val cardRows = listOf(
        // LEVEL I — 40 cards
        CardRow(CardLevel.I, GemColor.BLACK, 0, listOf(1,1,1,1,0)),
        CardRow(CardLevel.I, GemColor.BLACK, 0, listOf(1,1,1,2,0)),
        CardRow(CardLevel.I, GemColor.BLACK, 0, listOf(2,0,1,2,0)),
        CardRow(CardLevel.I, GemColor.BLACK, 0, listOf(0,1,0,3,1)),
        CardRow(CardLevel.I, GemColor.BLACK, 0, listOf(0,0,2,1,0)),
        CardRow(CardLevel.I, GemColor.BLACK, 0, listOf(2,0,2,0,0)),
        CardRow(CardLevel.I, GemColor.BLACK, 0, listOf(0,0,3,0,0)),
        CardRow(CardLevel.I, GemColor.BLACK, 1, listOf(0,4,0,0,0)),

        CardRow(CardLevel.I, GemColor.BLUE, 0, listOf(1,0,1,1,1)),
        CardRow(CardLevel.I, GemColor.BLUE, 0, listOf(1,0,1,2,1)),
        CardRow(CardLevel.I, GemColor.BLUE, 0, listOf(1,0,2,2,0)),
        CardRow(CardLevel.I, GemColor.BLUE, 0, listOf(0,1,3,1,0)),
        CardRow(CardLevel.I, GemColor.BLUE, 0, listOf(1,2,0,0,0)),
        CardRow(CardLevel.I, GemColor.BLUE, 0, listOf(2,0,2,0,0)),
        CardRow(CardLevel.I, GemColor.BLUE, 0, listOf(0,3,0,0,0)),
        CardRow(CardLevel.I, GemColor.BLUE, 1, listOf(0,0,0,4,0)),

        CardRow(CardLevel.I, GemColor.WHITE, 0, listOf(0,1,1,1,1)),
        CardRow(CardLevel.I, GemColor.WHITE, 0, listOf(0,1,2,1,1)),
        CardRow(CardLevel.I, GemColor.WHITE, 0, listOf(0,2,2,0,1)),
        CardRow(CardLevel.I, GemColor.WHITE, 0, listOf(1,1,0,0,3)),
        CardRow(CardLevel.I, GemColor.WHITE, 0, listOf(0,1,0,2,0)),
        CardRow(CardLevel.I, GemColor.WHITE, 0, listOf(2,2,0,0,0)),
        CardRow(CardLevel.I, GemColor.WHITE, 0, listOf(0,0,3,0,0)),
        CardRow(CardLevel.I, GemColor.WHITE, 1, listOf(0,0,4,0,0)),

        CardRow(CardLevel.I, GemColor.GREEN, 0, listOf(1,1,0,1,1)),
        CardRow(CardLevel.I, GemColor.GREEN, 0, listOf(1,1,0,1,2)),
        CardRow(CardLevel.I, GemColor.GREEN, 0, listOf(2,1,0,2,0)),
        CardRow(CardLevel.I, GemColor.GREEN, 0, listOf(1,0,3,1,0)),
        CardRow(CardLevel.I, GemColor.GREEN, 0, listOf(0,0,1,0,2)),
        CardRow(CardLevel.I, GemColor.GREEN, 0, listOf(0,2,0,2,0)),
        CardRow(CardLevel.I, GemColor.GREEN, 0, listOf(0,0,0,3,0)),
        CardRow(CardLevel.I, GemColor.GREEN, 1, listOf(0,0,0,0,4)),

        CardRow(CardLevel.I, GemColor.RED, 0, listOf(1,1,1,0,1)),
        CardRow(CardLevel.I, GemColor.RED, 0, listOf(2,1,1,0,1)),
        CardRow(CardLevel.I, GemColor.RED, 0, listOf(2,0,1,0,2)),
        CardRow(CardLevel.I, GemColor.RED, 0, listOf(1,3,0,0,1)),
        CardRow(CardLevel.I, GemColor.RED, 0, listOf(0,0,2,1,0)),
        CardRow(CardLevel.I, GemColor.RED, 0, listOf(0,2,0,0,2)),
        CardRow(CardLevel.I, GemColor.RED, 0, listOf(0,0,0,0,3)),
        CardRow(CardLevel.I, GemColor.RED, 1, listOf(0,0,0,0,4)),

        // LEVEL II — 30 cards
        CardRow(CardLevel.II, GemColor.BLACK, 1, listOf(3,2,2,0,0)),
        CardRow(CardLevel.II, GemColor.BLACK, 1, listOf(3,2,0,0,2)),
        CardRow(CardLevel.II, GemColor.BLACK, 2, listOf(0,1,4,2,0)),
        CardRow(CardLevel.II, GemColor.BLACK, 2, listOf(0,0,5,3,0)),
        CardRow(CardLevel.II, GemColor.BLACK, 2, listOf(5,0,0,0,0)),
        CardRow(CardLevel.II, GemColor.BLACK, 3, listOf(0,0,0,0,6)),
        CardRow(CardLevel.II, GemColor.BLUE, 1, listOf(0,2,2,3,0)),
        CardRow(CardLevel.II, GemColor.BLUE, 1, listOf(0,3,2,0,3)),
        CardRow(CardLevel.II, GemColor.BLUE, 2, listOf(5,0,3,0,0)),
        CardRow(CardLevel.II, GemColor.BLUE, 2, listOf(2,4,0,1,0)),
        CardRow(CardLevel.II, GemColor.BLUE, 2, listOf(0,0,5,0,0)),
        CardRow(CardLevel.II, GemColor.BLUE, 3, listOf(0,0,0,6,0)),
        CardRow(CardLevel.II, GemColor.WHITE, 1, listOf(0,0,3,2,2)),
        CardRow(CardLevel.II, GemColor.WHITE, 1, listOf(2,0,0,3,3)),
        CardRow(CardLevel.II, GemColor.WHITE, 2, listOf(0,0,1,4,2)),
        CardRow(CardLevel.II, GemColor.WHITE, 2, listOf(0,3,0,5,0)),
        CardRow(CardLevel.II, GemColor.WHITE, 2, listOf(0,0,0,0,5)),
        CardRow(CardLevel.II, GemColor.WHITE, 3, listOf(0,0,0,0,6)),
        CardRow(CardLevel.II, GemColor.GREEN, 1, listOf(3,0,0,3,2)),
        CardRow(CardLevel.II, GemColor.GREEN, 1, listOf(2,3,0,0,2)),
        CardRow(CardLevel.II, GemColor.GREEN, 2, listOf(0,2,0,0,4)),
        CardRow(CardLevel.II, GemColor.GREEN, 2, listOf(0,0,5,3,0)),
        CardRow(CardLevel.II, GemColor.GREEN, 2, listOf(0,5,0,0,0)),
        CardRow(CardLevel.II, GemColor.GREEN, 3, listOf(0,0,6,0,0)),
        CardRow(CardLevel.II, GemColor.RED, 1, listOf(2,0,0,2,3)),
        CardRow(CardLevel.II, GemColor.RED, 1, listOf(2,0,3,0,3)),
        CardRow(CardLevel.II, GemColor.RED, 2, listOf(1,0,0,4,2)),
        CardRow(CardLevel.II, GemColor.RED, 2, listOf(0,0,0,3,5)),
        CardRow(CardLevel.II, GemColor.RED, 2, listOf(0,0,0,0,5)),
        CardRow(CardLevel.II, GemColor.RED, 3, listOf(0,0,0,6,0)),

        // LEVEL III — 20 cards
        CardRow(CardLevel.III, GemColor.BLACK, 3, listOf(3,3,5,3,0)),
        CardRow(CardLevel.III, GemColor.BLACK, 4, listOf(0,0,0,7,0)),
        CardRow(CardLevel.III, GemColor.BLACK, 4, listOf(3,0,0,6,3)),
        CardRow(CardLevel.III, GemColor.BLACK, 5, listOf(3,0,0,7,0)),
        CardRow(CardLevel.III, GemColor.BLUE, 3, listOf(3,0,3,3,5)),
        CardRow(CardLevel.III, GemColor.BLUE, 4, listOf(7,0,0,0,0)),
        CardRow(CardLevel.III, GemColor.BLUE, 4, listOf(6,3,0,0,3)),
        CardRow(CardLevel.III, GemColor.BLUE, 5, listOf(7,0,0,0,3)),
        CardRow(CardLevel.III, GemColor.WHITE, 3, listOf(0,3,3,3,5)),
        CardRow(CardLevel.III, GemColor.WHITE, 4, listOf(0,0,0,0,7)),
        CardRow(CardLevel.III, GemColor.WHITE, 4, listOf(3,6,0,0,3)),
        CardRow(CardLevel.III, GemColor.WHITE, 5, listOf(0,7,0,0,3)),
        CardRow(CardLevel.III, GemColor.GREEN, 3, listOf(5,3,0,3,3)),
        CardRow(CardLevel.III, GemColor.GREEN, 4, listOf(0,7,0,0,0)),
        CardRow(CardLevel.III, GemColor.GREEN, 4, listOf(3,3,6,0,0)),
        CardRow(CardLevel.III, GemColor.GREEN, 5, listOf(0,0,7,3,0)),
        CardRow(CardLevel.III, GemColor.RED, 3, listOf(3,3,3,0,5)),
        CardRow(CardLevel.III, GemColor.RED, 4, listOf(0,0,7,0,0)),
        CardRow(CardLevel.III, GemColor.RED, 4, listOf(3,0,3,6,0)),
        CardRow(CardLevel.III, GemColor.RED, 5, listOf(0,3,7,0,0))
    )

    override fun getCards(): Map<CardLevel, List<DevelopmentCard>> =
        cardRows.map { row ->
            DevelopmentCard(
                id = CardId.generate(),
                level = row.level,
                bonus = row.bonus,
                points = row.points,
                cost = GemCost(
                    GemColor.entries
                        .filter { it != GemColor.GOLD }
                        .mapIndexed { index, color -> color to row.cost[index] }
                        .toMap()
                )
            )
        }.groupBy { it.level }

    override fun getNobles(): List<NobleTile> =
        listOf(
            listOf(0, 0, 4, 4, 0),
            listOf(0, 4, 4, 0, 0),
            listOf(4, 4, 0, 0, 0),
            listOf(4, 0, 0, 0, 4),
            listOf(0, 0, 0, 4, 4),
            listOf(0, 3, 3, 3, 0),
            listOf(3, 0, 3, 3, 0),
            listOf(3, 3, 0, 0, 3),
            listOf(0, 0, 3, 3, 3),
            listOf(3, 3, 3, 0, 0)
        ).map { requirements ->
            NobleTile(
                id = NobleId.generate(),
                requirements = GemCost(
                    GemColor.entries
                        .filter { it != GemColor.GOLD }
                        .mapIndexed { index, color -> color to requirements[index] }
                        .toMap()
                )
            )
        }
}
