package model.game

import model.board.ChessBoard
import model.board.Color
import model.misc.FenString
import model.misc.move

/**
 * An interface for all classes which will conduct chess games.
 */
interface ChessGame : ReadOnlyChessGame {

    /**
     * Starts the chess game.
     */
    fun start(fen: FenString = FenString()): ChessGame

    /**
     * undoes the most recently made move.
     */
    fun undoMove(): ChessGame


    /**
     * Plays the given move onto the board. If the move is illegal, or if it's not the moving player's turn, an
     * IllegalArgumentException is thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun playMove(move: move): ChessGame

}