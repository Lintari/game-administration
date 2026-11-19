package splendor.domain.moves

import splendor.domain.components.GemColor

data class TakeGemsMove(
    val colors: List<GemColor>
) : Move
