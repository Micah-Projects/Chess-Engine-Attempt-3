package model.board

import model.misc.BitBoard

interface ChessBoard {

    /**
     * Returns the piece on the given [square].
     * @param square The square index (0–63).
     * @return [Piece] on this square.
     */
    fun fetchPiece(square: Int): Piece

    /**
     * @return The [BitBoard] of the given [pieceType].
     */
    fun fetchPieceBitBoard(pieceType: Piece): BitBoard

    /**
     * @return a [BitBoard] representing the positions occupied by [color].
     * If color is null, the entire occupancy is retrieved.
     */
    fun getOccupancy(color: Color? = null): ULong

    /**
     * @return a [BitBoard] representing all empty squares on the board.
     */
    fun fetchEmptySquares(): ULong

    /**
     * Returns the current En-Passant Square on this board.
     * @return The Square at which En-Passant is possible.
     */
    fun fetchEnpassantSquare(): Int?

    /**
     * Creates a clone of this ChessBoard.
     * @return [MutableChessBoard]
     */
    fun clone(): MutableChessBoard

    /**
     * @return The FEN string representation of this board.
     */
    fun toFen(): String

    /**
     * Gets a string representation of this ChessBoard.
     * @return [String]
     */
    fun textVisual(viewFrom: Color = Color.WHITE): String

    /**
     * Returns a new mutable instance with this board data.
     */
    fun toMutable(): MutableChessBoard
}