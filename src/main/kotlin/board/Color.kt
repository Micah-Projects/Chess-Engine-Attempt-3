package board

import misc.Debug
import misc.Debug.Area.*

/**
 * The colors for two chess players.
 */
enum class Color(private val c: Int) {
    WHITE(0),
    BLACK(1);
    companion object {
        /**
         * Returns the piece represented by [value]
         * @param value The number associated with a chess piece color. 0 -> White, 1 -> Black.
         * @throws IllegalArgumentException for all values which are not either 0 or 1.
         */
        fun from(value: Int): Color = when (value) {
            0 -> WHITE
            1 -> BLACK
            else -> throw IllegalArgumentException(Debug.log(COLOR) { "There is no color represented by value: $value" })
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

    /**
     * Returns this color represented as an Int
     */
    fun get(): Int = c

}