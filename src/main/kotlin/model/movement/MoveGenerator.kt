package model.movement

import model.board.ChessBoard
import model.board.Color
import model.misc.move

interface MoveGenerator {
    fun generateMoves(board: ChessBoard, color: Color): List<move>
    fun isKingInCheck(): Boolean
}