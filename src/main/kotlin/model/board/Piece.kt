package model.board

import model.misc.Debug

/**
 * A chess piece.
 */
enum class Piece(val value: Int, val symbol: String) {

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
                    Debug.log(Debug.Area.PIECE) { " $piece parsed from $symbol " }
                    return piece
                }
            }
            return EMPTY
        }
    }

    val color: Color by lazy { if (isWhite()) Color.WHITE else Color.BLACK }

    /**
     * returns the String representation of this piece.
     */
    fun symbol(): String = symbol

    /**
     * Returns whether this piece is empty.
     */
    fun isEmpty(): Boolean = value == -1

    /**
     * Returns whether this piece is empty.
     */
    fun isNotEmpty(): Boolean = value != -1

    /**
     * Returns whether this piece is a pawn.
     */
    fun isPawn(): Boolean = value % 6 == 0

    /**
     * Returns whether this piece is a knight.
     */
    fun isKnight(): Boolean = value % 6 == 1

    /**
     * Returns whether this piece is a bishop.
     */
    fun isBishop(): Boolean = value % 6 == 2

    /**
     * Returns whether this piece is a rook.
     */
    fun isRook(): Boolean = value % 6 == 3

    /**
     * Returns whether this piece is a queen.
     */
    fun isQueen(): Boolean = value % 6 == 4

    /**
     * Returns whether this piece is a king.
     */
    fun isKing(): Boolean = value % 6 == 5

    /**
     * Returns whether this piece is white.
     */
    fun isWhite(): Boolean = value <= 5

    /**
     * Returns whether this piece is black.
     */
    fun isBlack(): Boolean = value >= 6
}