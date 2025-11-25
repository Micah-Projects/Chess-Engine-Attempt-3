package view

import model.board.Board
import model.board.ChessBoard
import model.board.MutableChessBoard
import model.game.ReadOnlyChessGame

class TextView : View {
    lateinit var game: ReadOnlyChessGame
    override fun viewGame(game: ReadOnlyChessGame) {
        this.game = game
    }
}