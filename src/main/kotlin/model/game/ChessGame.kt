package model.game

import model.board.Color
import model.misc.move

/**
 * An interface for all classes which will conduct chess games.
 */
interface ChessGame {

    /**
     * Returns whether the game is over.
     */
    fun isOver(): Boolean

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
     * Plays the given move onto the board. If the move is illegal, or not the player's turn, an
     * IllegalArgumentException is thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun playMove(move: move)

}