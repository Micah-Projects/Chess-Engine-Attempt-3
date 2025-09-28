package model.game

import model.board.Board
import model.board.ChessBoard
import model.board.Color
import model.misc.move

class Game : ChessGame {
    private val board: ChessBoard = Board()
    private var turn: Color? = Color.WHITE
    private var validMoves: List<move> = listOf()

    override fun isOver(): Boolean {
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
        TODO("Not yet implemented")
    }

    override fun playMove(move: move) {
        if (move !in validMoves) throw IllegalArgumentException("Move $move is invalid.")
        board.makeMove(move)
    }
}