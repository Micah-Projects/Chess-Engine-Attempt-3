package command.cheats

import command.BoardCommand
import model.board.ChessBoard
import model.movement.Move

data class Move(val move: Move, override val board: ChessBoard) : BoardCommand {
    override fun exec() {
        board.makeMove(move)
    }
}