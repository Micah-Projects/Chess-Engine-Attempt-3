package command

import model.game.ChessGame
import model.movement.Move

interface GameCommand : Command {
    val game: ChessGame
}

data class MakeMove(val move: Move, override val game: ChessGame) : GameCommand {
    override fun exec() {
        game.playMove(move)
    }
}

data class PrintStats(override val game: ChessGame) : GameCommand {
    override fun exec() {
        println(game.turn)
    }
}