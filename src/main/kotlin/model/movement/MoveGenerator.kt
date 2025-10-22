package model.movement

import model.board.MutableChessBoard
import model.board.Color
import model.misc.move

interface MoveGenerator {
    fun generateMoves(board: MutableChessBoard, color: Color): List<move>
}