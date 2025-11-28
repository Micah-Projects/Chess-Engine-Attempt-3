package view

import model.game.ReadOnlyChessGame

class TextView : View {
    lateinit var game: ReadOnlyChessGame
    override fun viewGame(game: ReadOnlyChessGame) {
        this.game = game
    }
}