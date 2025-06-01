package command

import model.board.ChessBoard
import model.misc.move

data class Move(val move: move, override val board: ChessBoard) : BoardCommand {

    override fun exec() {
        board.makeMove(move)
    }
}