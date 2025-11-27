package model.board

import model.movement.move

/**
 * An interface for chess boards.
 */
interface MutableChessBoard : ChessBoard {
    /**
     * Adds a [piece] onto the given [square].
     * @param square The square index (0–63).
     * @param piece The chess piece
     */
    fun addPiece(piece: Piece, square: Int)

    /**
     * Removes the piece on the given [square].
     * @param square The square index (0–63).
     */
    fun removePiece(square: Int)

    /**
     * Moves a piece from [start] to [end] regardless of legality.
     * @param start Start square index
     * @param end End square index
     */
    fun movePiece(start: Int, end: Int)

    /**
     * Makes a legal move on the board.
     * @param [move] An encoded Int containing start, end, and various move flags.
     */
    fun makeMove(move: move)

    /**
     * Loads a board state from the given [fen] string.
     * @param fen Forsyth–Edwards Notation string representing a board state.
     */
    fun loadFen(fen: String)



}