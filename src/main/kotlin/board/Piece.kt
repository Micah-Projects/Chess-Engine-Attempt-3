package board

import board.Color.BLACK
import board.Color.WHITE
import misc.Debug
import misc.Debug.Area.*

/**
 * A chess piece.
 */
enum class Piece(private val type: Int, private val symbol: String) {

    EMPTY(-1, " "),
    WHITE_PAWN(0, "P"),
    WHITE_KNIGHT(1, "N"),
    WHITE_BISHOP(2, "B"),
    WHITE_ROOK(3, "R"),
    WHITE_QUEEN(4, "Q"),
    WHITE_KING(5, "K"),

    BLACK_PAWN(6, "p"),
    BLACK_KNIGHT(7, "n"),
    BLACK_BISHOP(8, "b"),
    BLACK_ROOK(9, "r"),
    BLACK_QUEEN(10, "q"),
    BLACK_KING(11, "k");

    companion object {
        val playable = entries.toTypedArray().copyOfRange(1, entries.size)
        fun from(index: Int): Piece = playable[index]

        /**
         * Returns a random playable piece.
         */
        fun random(): Piece {
            return playable.random()
        }

        /**
         * Returns the Piece associated with [symbol].
         * @param symbol The singular character string which represents a piece
         * @return The corresponding piece or empty if input isn't correct.
         */
        fun fromSymbol(symbol: String): Piece {
            for (piece in playable) {
                if (piece.symbol() == symbol) {
                    Debug.log(PIECE) { " $piece parsed from $symbol " }
                    return piece
                }
            }
            return EMPTY
        }
    }

    private val color: Color by lazy { if (isWhite()) WHITE else BLACK }

    /**
     * returns the Int value which represents this piece.
     */
    fun get() = type

    /**
     * returns the color of this piece.
     */
    fun color(): Color {
        return color
    }
    /**
     * returns the String representation of this piece.
     */
    fun symbol(): String = symbol

    /**
     * Returns whether this piece is empty.
     */
    fun isEmpty(): Boolean = type == -1

    /**
     * Returns whether this piece is empty.
     */
    fun isNotEmpty(): Boolean = type != -1

    /**
     * Returns whether this piece is a pawn.
     */
    fun isPawn(): Boolean = type % 6 == 0

    /**
     * Returns whether this piece is a knight.
     */
    fun isKnight(): Boolean = type % 6 == 1

    /**
     * Returns whether this piece is a bishop.
     */
    fun isBishop(): Boolean = type % 6 == 2

    /**
     * Returns whether this piece is a rook.
     */
    fun isRook(): Boolean = type % 6 == 3

    /**
     * Returns whether this piece is a queen.
     */
    fun isQueen(): Boolean = type % 6 == 4

    /**
     * Returns whether this piece is a king.
     */
    fun isKing(): Boolean = type % 6 == 5

    /**
     * Returns whether this piece is white.
     */
    fun isWhite(): Boolean = color == WHITE

    /**
     * Returns whether this piece is black.
     */
    fun isBlack(): Boolean = color == WHITE
}