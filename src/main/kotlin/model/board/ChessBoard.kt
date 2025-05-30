package model.board

import model.misc.BitBoard

/**
 * An interface for chess boards.
 */
interface ChessBoard {

    /**
     * Adds a [piece] onto the given [square]
     * @param square The square index (0–63).
     * @param piece The chess piece
     */
    fun addPiece(piece: Piece, square: Int)

    /**
     * Removes the piece on the given [square]
     * @param square The square index (0–63).
     */
    fun removePiece(square: Int)

    /**
     * Moves a piece from [start] to [end]
     * @param start Start square index
     * @param end End square index
     */
    fun movePiece(start: Int, end: Int)

    /**
     * Returns the piece on the given [square].
     * @param square The square index (0–63).
     * @return [Piece] on this square.
     */
    fun fetchPiece(square: Int): Piece

    /**
     * @return The [BitBoard] of the given [pieceType]
     */
    fun fetchPieceBitBoard(pieceType: Piece): BitBoard

    /**
     * Returns the current En-Passant Square on this board.
     * @return The Square at which En-Passant is possible.
     */
    fun fetchEnpassantSquare(): Int?

    /**
     * Creates a clone of this ChessBoard.
     * @return [ChessBoard]
     */
    fun clone(): ChessBoard

    /**
     * Loads a board state from the given [fen] string.
     * @param fen Forsyth–Edwards Notation string representing a board state.
     */
    fun loadFen(fen: String)

    /**
     * @return The FEN string representation of this board.
     */
    fun toFen(): String

    /**
     * Gets a string representation of this ChessBoard.
     * @return [String]
     */
    fun textVisual(viewFrom: Color = Color.WHITE): String
}