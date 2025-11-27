package model.board

import model.utils.Debug

/**
 * The colors for two chess players.
 */
enum class Color(val value: Int, val symbol: String) {
    WHITE(0, "w"),
    BLACK(1, "b");
    companion object {
        const val COUNT = 2
        /**
         * Returns the piece represented by [value]
         * @param value The number associated with a chess piece color. 0 -> White, 1 -> Black.
         * @throws IllegalArgumentException for all values which are not either 0 or 1.
         */
        fun from(value: Int): Color = when (value) {
            0 -> WHITE
            1 -> BLACK
            else -> throw IllegalArgumentException(Debug.log(Debug.Area.COLOR) { "There is no color represented by value: $value" })
        }
    }

    val enemy: Color by lazy {
        when (this) {
            WHITE -> BLACK
            BLACK -> WHITE
        }
    }

    val pawnDirection: Int by lazy {
        when (this) {
            WHITE -> 1
            BLACK -> -1
        }
    }

    val pawnStartRank: Int by lazy {
        when (this) {
            WHITE -> 1
            BLACK -> 6
        }
    }

    val promotionRank: Int by lazy {
        when (this) {
            WHITE -> 7
            BLACK -> 0
        }
    }

}