package view


import model.board.ChessBoard
import model.board.MutableChessBoard
import model.game.ReadOnlyChessGame

interface View {

    fun viewGame(game: ReadOnlyChessGame)
}