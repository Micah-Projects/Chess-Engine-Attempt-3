package view


import model.board.ChessBoard
import model.board.MutableChessBoard

interface View {

    fun viewBoard(board: ChessBoard)
}