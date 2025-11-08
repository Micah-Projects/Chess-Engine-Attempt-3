package model.game

import model.board.ChessBoard
import model.board.Color
import model.misc.FenString
import model.misc.move

/**
 * An interface for all classes which will conduct chess games.
 */
interface ChessGame {

    /**
     * Starts the chess game.
     */
    fun start(fen: FenString = FenString())

    /**
     * Returns whether the game is over.
     */
    fun isOver(): Boolean

    /**
     * Returns the current status of the game.
     */
    fun getStatus(): GameStatus

    /**
     * Returns the color which won the game. Null if the game is ongoing or tied.
     */
    fun getWinner(): Color?

    /**
     * Returns the color whose turn it is.
     */
    fun currentTurn(): Color?

    /**
     * Returns the possible moves of the given color. If this method is called on a color, and it's not their turn,
     * this method should return an empty list.
     */
    fun getMoves(color: Color): List<move>

    /**
     * Returns a read-only view of the ongoing game. Externals can use .toMutable() if they wish to obtain
     * a mutable instance.
     */
    fun getBoard(): ChessBoard

    /**
     * Returns a clone of this chess game.
     */
    fun clone(): ChessGame

    /**
     * Plays the given move onto the board. If the move is illegal, or if it's not the moving player's turn, an
     * IllegalArgumentException is thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun playMove(move: move)

}