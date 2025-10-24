package model.board

import model.misc.Debug

/**
 * A chess piece.
 */
enum class Piece(val value: Int, val type: Type, val symbol: String) {

    EMPTY(-1, Type.NONE," "),
    WHITE_PAWN(0, Type.PAWN, "P"),
    WHITE_KNIGHT(1, Type.KNIGHT, "N"),
    WHITE_BISHOP(2, Type.BISHOP, "B"),
    WHITE_ROOK(3, Type.ROOK,"R"),
    WHITE_QUEEN(4, Type.QUEEN, "Q"),
    WHITE_KING(5, Type.KING,"K"),

    BLACK_PAWN(6, Type.PAWN,"p"),
    BLACK_KNIGHT(7, Type.KNIGHT,"n"),
    BLACK_BISHOP(8, Type.BISHOP,"b"),
    BLACK_ROOK(9, Type.ROOK,"r"),
    BLACK_QUEEN(10, Type.QUEEN,"q"),
    BLACK_KING(11, Type.KING,"k");

    enum class Type(val value: Int) {
        NONE(-1),
        PAWN(0),
        KNIGHT(1),
        BISHOP(2),
        ROOK(3),
        QUEEN(4),
        KING(5);

        companion object {
            val playable = entries.toTypedArray().copyOfRange(1, entries.size)
            val promotions = entries.toTypedArray().copyOfRange(1, entries.size - 1)
        }
    }

    companion object {
        const val COUNT = 12
        const val SLIDER_COUNT = 3 // because each piece including color is distinct
        const val LEAPER_COUNT = 2
        const val TYPES = 6
        val playable = entries.toTypedArray().copyOfRange(1, entries.size)
        val sliders = listOf<Type>(Type.BISHOP, Type.ROOK, Type.QUEEN)
        val leapers = listOf(Type.KNIGHT, Type.KING)

        fun from(index: Int): Piece = playable[index]
        fun from(type: Type, color: Color): Piece {
            return from(type.value + color.value * this.TYPES)
        }

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
    fun isPawn(): Boolean = type == Type.PAWN

    /**
     * Returns whether this piece is a knight.
     */
    fun isKnight(): Boolean = type == Type.KNIGHT

    /**
     * Returns whether this piece is a bishop.
     */
    fun isBishop(): Boolean = type == Type.BISHOP

    /**
     * Returns whether this piece is a rook.
     */
    fun isRook(): Boolean = type == Type.ROOK

    /**
     * Returns whether this piece is a queen.
     */
    fun isQueen(): Boolean = type == Type.QUEEN

    /**
     * Returns whether this piece is a king.
     */
    fun isKing(): Boolean = type == Type.KING

    /**
     * Returns whether this piece is white.
     */
    fun isWhite(): Boolean = value <= 5

    /**
     * Returns whether this piece is black.
     */
    fun isBlack(): Boolean = value >= 6

    fun isSlider(): Boolean = isBishop() || isQueen() || isRook()

    fun isLeaper(): Boolean = isKing() || isKnight()
}