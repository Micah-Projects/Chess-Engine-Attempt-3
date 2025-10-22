package controller

import command.MakeMove
import command.PrintBoard
import model.board.Board
import model.board.MutableChessBoard
import model.board.Piece.EMPTY
import model.game.ChessGame
import model.game.Game
import model.misc.Moves
import model.misc.Squares
import model.misc.end
import model.misc.square
import model.misc.start
import view.GuiGame

object Controller : Features {
    var game: ChessGame = Game()
    override fun safeTryMove(from: square, to: square) {
        if (validMoveCriteria(from, to)) {
            makeMove(from, to)
           // GuiGame.orientation = game.currentTurn()!!
        }

    }

    fun setHighlights(square: square) {
        GuiGame.highlights = game.getMoves(game.currentTurn()!!).filter { it.start() == square }.map { it.end()}
    }

    fun clearHighlights() {
        GuiGame.highlights = listOf()
    }

    private fun validMoveCriteria(start: square, end: square): Boolean =
        Squares.allInBounds(start, end)
                && start != end
                && game.getBoard().toMutable().fetchPiece(start).isNotEmpty()


    override fun makeMove(from: square, to: square) {
        Runner.receiveCommand(MakeMove( Moves.encode(from, to), game) )
    }

    override fun startNewGame() {
        game = Game()
        GuiGame.viewBoard(game.getBoard())
        game.start()
    }

    override fun printBoard() {
        Runner.receiveCommand(PrintBoard(game.getBoard()))
    }
}