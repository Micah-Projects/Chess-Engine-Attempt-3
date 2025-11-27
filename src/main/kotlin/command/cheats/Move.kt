package command.cheats

import command.BoardCommand
import model.board.MutableChessBoard
import model.movement.move

data class Move(val move: move, override val board: MutableChessBoard) : BoardCommand {
    override fun exec() {
        board.makeMove(move)
    }
}