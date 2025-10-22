package model.game

import model.board.Board
import model.board.ChessBoard
import model.board.MutableChessBoard
import model.board.Color
import model.misc.FenString
import model.misc.getString
import model.misc.literal
import model.misc.move
import model.movement.BitBoardMoveGenerator
import model.movement.MoveGenerator

class Game : ChessGame {        // maybe make a new class called "CommandedGame" for cheats and such
    private val board: MutableChessBoard = Board()
    private var turn: Color? = null
    private var validMoves: List<move> = listOf()
    private val mg: MoveGenerator = BitBoardMoveGenerator()
    private var started = false

    override fun start(fen: FenString) {
        if (started) return
        started = true
        board.loadFen(fen.fen)
        turn = Color.WHITE
        validMoves = mg.generateMoves(board, turn!!)
    }

    override fun isOver(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getStatus(): GameStatus {
        TODO("Not yet implemented")
    }

    override fun getWinner(): Color? {
        TODO("Not yet implemented")
    }

    override fun currentTurn(): Color? {
        return turn
    }

    override fun getMoves(color: Color): List<move> {
        if (turn != color) return listOf()
        return validMoves
    }

    override fun getBoard(): ChessBoard {
        return board // cant be modified as a ChessBoard
    }

    override fun playMove(move: move) {
        require(turn != null) {
            "Cannot play move. Game is over or hasn't started. (turn is null)"
        }
        if (move !in validMoves) throw IllegalArgumentException("Move ${move.getString()} is invalid.")
        board.makeMove(move)
        nextTurn()
    }

    private fun nextTurn() {
        if (turn != null)  {
            turn = if (turn == Color.WHITE) Color.BLACK else Color.WHITE
            validMoves = mg.generateMoves(board, turn!!)
        }
    }

    private fun changeTurn() {
        if (turn != null) turn = if (turn == Color.WHITE) Color.BLACK else Color.WHITE
    }
}