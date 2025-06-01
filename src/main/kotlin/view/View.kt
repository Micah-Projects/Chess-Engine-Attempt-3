package view


import model.board.ChessBoard

interface View {

    fun viewBoard(board: ChessBoard)
}