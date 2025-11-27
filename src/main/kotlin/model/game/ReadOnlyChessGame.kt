package model.game

import model.board.ChessBoard
import model.board.Color
import model.movement.move

interface ReadOnlyChessGame {

    /**
     * Returns whether the game is over.
     */
    fun isOver(): Boolean

    /**
     * Returns the current status of the game.
     */
    val status: GameStatus

    /**
     * Returns the number of plies since the game start.
     */
    val plies: Int

    /**
     * Returns the current repetition count of the current position in this game. This value should at most be 3
     */
    val repetitionCount: Int

    /**
     * Returns the current number of half-moves counted since a capture or pawn move. The game terminates if this value
     * reaches 50.
     */
    val halfMoveClock: Int

    /**
     * Returns the color which won the game. Null if the game is ongoing or tied.
     */
    val winner: Color?

    /**
     * Returns the color whose turn it is.
     */
    val turn: Color

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
     * Returns a complete fen-string representing this game.
     */
    fun toFen(): String

    /**
     * Returns a clone of this chess game.
     */
    fun clone(): ChessGame

}