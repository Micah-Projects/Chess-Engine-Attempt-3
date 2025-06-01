package view

import model.board.Board
import model.board.ChessBoard

class TextView : View {
    var board: ChessBoard   = Board()
    override fun viewBoard(board: ChessBoard) {
        this.board = board
    }
}