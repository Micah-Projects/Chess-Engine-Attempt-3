package view


import model.game.ReadOnlyChessGame

interface View {

    fun viewGame(game: ReadOnlyChessGame)
}