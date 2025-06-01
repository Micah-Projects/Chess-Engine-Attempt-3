package model.misc
import model.board.Board

import kotlin.math.abs
import kotlin.math.sign


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
     * @throws IllegalArgumentException if The text is not in correct format or the parsed square doesn't exist.
     */
    fun valueOf(text: String): square {
        require(text.length == 2) { "Invalid square notation: $text" }
        val f = fileNames.indexOf(text.first().toString())
        val r = rankNames.indexOf(text.last().toString())
        require(f != -1 && r != -1) { "Invalid file or rank in: $text" }
        val result = r * BOARD_AXIS_LENGTH + f
        return result
    }

    /**
     * Returns the [square] in algebraic notation format.
     * @param square An Int within range 0 to 63 (inclusive)
     * @throws IllegalArgumentException if the [square] is not in bounds.
     */
    fun asText(square: square): String {
        assertInBounds(square)
        return fileNames[fileOf(square)] + rankNames[rankOf(square)]
    };

    /**
     * Returns the coordinate pair representation of a square.
     * @param square An Int within the range 0 to 63 (inclusive)
     * @throws IllegalArgumentException if the [square] is not in bounds.
     */
    fun asCoord(square: square): Pair<Int, Int> {
        assertInBounds(square)
        return Pair<Int,Int>(fileOf(square), rankOf(square))
    }

    /**
     * Returns the column of the given [square].
     * @param square An Int within the range 0 to 63 (inclusive)
     * @throws IllegalArgumentException if the [square] is not in bounds.
     */
    fun fileOf(square: square): Int {
        assertInBounds(square)
        return square % BOARD_AXIS_LENGTH
    }

    /**
     * Returns the row of the given [square].
     * @param square An Int within the range 0 to 63 (inclusive)
     * @throws IllegalArgumentException if the square is not in bounds.
     */
    fun rankOf(square: square): Int {
        assertInBounds(square)
        return square / BOARD_AXIS_LENGTH
    }

    /**
     * Returns whether a [square] file is [file]
     * @param square An Int within the range 0 to 63 (inclusive)
     * @param file The column/x position of [square] on the board.
     */
    fun fileIs(square: square, file: Int): Boolean = fileOf(square) == file

    /**
     * Returns whether a [square] rank is [rank]
     * @param square An Int within the range 0 to 63 (inclusive)
     * @param rank The row/y position of [square] on the board.
     */
    fun rankIs(square: square, rank: Int): Boolean = rankOf(square) == rank

    /**
     * Returns the file distance between [start] and [end].
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun fileDist(start: square, end: square): Int = abs(fileOf(start) - fileOf(end))

    /**
     * Returns the rank distance between [start] and [end].
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun rankDist(start: square, end: square): Int = abs(rankOf(start) - rankOf(end))


    /**
     * Returns whether the rank distance between [start] and [end] is not the same.
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun isOnDiffFile(start: square, end: square): Boolean = fileDist(start, end) != 0

    /**
     * Returns whether the rank distance between [start] and [end] is not the same.
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun isOnDiffRank(start: square, end: square): Boolean = rankDist(start, end) != 0

    /**
     * Returns whether the rank distance between [start] and [end] is not the same.
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun isOnSameFile(start: square, end: square): Boolean = fileDist(start, end) == 0

    /**
     * Returns whether the rank distance between [start] and [end] is not the same.
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun isOnSameRank(start: square, end: square): Boolean = rankDist(start, end) == 0

    /**
     * returns whether [start] and [end] are diagonal.
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun isOnDiagonal(start: square, end: square): Boolean = fileDist(start, end) == rankDist(start, end)

    /**
     * Returns the directional vector between two squares. There are 9 possible directions, including zero if there's
     * no difference in the squares.
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun vectorBetween(start: square, end: square): Int {
        val difference = abs(end - start)
        val sign = (end - start).sign
        val vectorBetween = when {
            rankDist(start, end) == 0 -> 1
            difference % 8 == 0 -> 8
            difference % 9 == 0 -> 9
            difference % 7 == 0 -> 7
            else -> difference
        } * sign
        return vectorBetween
    }

    private fun isInBounds(square: square): Boolean = square in 0..63

    private fun assertInBounds(square: square, message: () -> String =
        { "Square cannot be handled as it is not in bounds: $square" } )
    {
        require(isInBounds(square)) { message() }
    }
}