package model.movement

import model.board.ReadOnlyChessBoard
import model.board.Color

interface MoveGenerator {
    fun generateMoves(board: ReadOnlyChessBoard, color: Color): List<Move>
    fun isKingInCheck(): Boolean
}

