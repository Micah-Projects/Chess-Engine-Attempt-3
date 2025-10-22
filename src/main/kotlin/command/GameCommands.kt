package command

import model.board.Color
import model.game.ChessGame
import model.misc.move

interface GameCommand : Command {
    val game: ChessGame
}

data class MakeMove(val move: move, override val game: ChessGame) : GameCommand {
    override fun exec() {
        game.playMove(move)
    }
}

data class PrintStats(override val game: ChessGame) : GameCommand {
    override fun exec() {
        println(game.currentTurn())
    }
}

//data class PrintBoard(override val game: ChessGame, val viewFrom: Color = Color.WHITE) : GameCommand {
//    override fun exec() {
//        println(game.getBoard().textVisual(viewFrom))
//    }
//}