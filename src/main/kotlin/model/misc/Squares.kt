package model.misc
import model.board.Board

import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random


/**
 * A Type-alias for [Int] which represents chess board squares.
 */
typealias square = Int

/**
 * A collection of useful methods for handling basic information of chess squares.
 */
object Squares {
    const val BOARD_AXIS_LENGTH = 8
    const val COUNT = 64
    const val NUM_FILES = 8
    const val NUM_RANKS = 8
    val range = 0..63
    private val fileNames = arrayOf("a", "b", "c", "d", "e", "f", "g", "h")
    private val rankNames = arrayOf("1", "2", "3", "4", "5", "6", "7", "8")

    /**
     * Returns the square which is represented by [text].
     * @param text The square represented in algebraic notation.
     * @throws IllegalArgumentException if The text is not in correct format or the parsed square doesn't exist.
     */
    fun valueOf(text: String): square {
        require(text.length == 2) { "Invalid square notation: $text" }
        val converted = text.lowercase()
        val f = fileNames.indexOf(converted.first().toString())
        val r = rankNames.indexOf(converted.last().toString())
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

    fun fromFileRank(file: Int, rank: Int): square {
        return rank * 8 + file
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
     * Returns the Manhattan Distance between two squares.
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun manhatDist(start: square, end: square): Int = fileDist(start, end) + rankDist(start, end)


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
     * returns whether [start] and [end] are diagonal
     * @param start An Int within the range 0 to 63 (inclusive)
     * @param end An Int within the range 0 to 63 (inclusive)
     */
    fun isOnDiagonal(start: square, end: square): Boolean = fileDist(start, end) == rankDist(start, end)

    /**
     * Returns true if the [square] is on any of the 4 edges of the board.
     */
    fun isOnEdge(square: square) = fileOf(square) == 0 || fileOf(square) == 7 || rankOf(square) == 0 || rankOf(square) == 7

    /**
     * Returns true if the [square] is on a side edge
     */
    fun isOnSideEdge(square: square) = fileOf(square) == 0 || fileOf(square) == 7
    /**
     * Returns true if the [square] is on a vertical edge
     */
    fun isOnVerticalEdge(square: square) = rankOf(square) == 0 || rankOf(square) == 7

    /**
     * Gets a random square
     * @return A random Int within the range 0 to 63 (inclusive)
     */
    fun random(): Int {
        return Random.nextInt(0, 64)
    }

    /**
     * Returns true if the square is in the range 0 to 63 (inclusive)
     */
    fun isInBounds(square: square): Boolean = square in 0..63

    fun allInBounds(vararg squares: square): Boolean = squares.all { it in 0..63 }

    private fun assertInBounds(square: square, message: () -> String =
        { "Square cannot be handled as it is not in bounds: $square" } )
    {
        require(isInBounds(square)) { message() }
    }
}