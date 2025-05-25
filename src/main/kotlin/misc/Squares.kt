package misc


/**
 * A Type-alias for [Int] which represents chess board squares.
 */
typealias square = Int

/**
 * A collection of useful methods for handling basic information of chess squares.
 */
object Squares {
    private const val BOARD_AXIS_LENGTH = 8
    private val fileNames = arrayOf("a", "b", "c", "d", "e", "f", "g", "h")
    private val rankNames = arrayOf("1", "2", "3", "4", "5", "6", "7", "8")

    /**
     * Returns the square which is represented by [text].
     * @param text The square represented in algebraic notation.
     */
    fun valueOf(text: String): square {
        require(text.length == 2) { "Invalid square notation: $text" }
        val f = fileNames.indexOf(text.first().toString())
        val r = rankNames.indexOf(text.last().toString())
        require(f != -1 && r != -1) { "Invalid file or rank in: $text" }
        val result = r * BOARD_AXIS_LENGTH + f
        assertInBounds(result)
        return result
    }

    /**
     * Returns the [square] in algebraic notation format.
     * @param square An Int within range 0 to 63 (inclusive)
     */
    fun asText(square: square): String {
        assertInBounds(square)
        return fileNames[fileOf(square)] + rankNames[rankOf(square)]
    };

    /**
     * Returns the coordinate pair representation of a square.
     * @param square An Int within the range 0 to 63 (inclusive)
     */
    fun asCoord(square: square): Pair<Int, Int> {
        assertInBounds(square)
        return Pair<Int,Int>(fileOf(square), rankOf(square))
    }

    /**
     * Returns the row of the given [square].
     * @param square An Int within the range 0 to 63 (inclusive)
     */
    fun rankOf(square: square): Int {
        assertInBounds(square)
        return square / BOARD_AXIS_LENGTH
    }

    /**
     * Returns the column of the given [square].
     * @param square An Int within the range 0 to 63 (inclusive)
     */
    fun fileOf(square: square): Int {
        assertInBounds(square)
        return square % BOARD_AXIS_LENGTH
    }

    /**
     * Returns whether a [square] rank is [rank]
     */
    fun rankIs(square: square, rank: Int): Boolean {
        return rankOf(square) == rank
    }

    /**
     * Returns whether a [square] file is [file]
     */
    fun fileIs(square: square, file: Int): Boolean {
        return fileOf(square) == file
    }

    private fun isInBounds(square: square): Boolean = square in boardSquares

    private fun assertInBounds(square: square, message: () -> String =
        { "Square cannot be handled as it is not in bounds: $square" } )
    {
        require(isInBounds(square)) { message() }
    }
}