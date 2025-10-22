package command

import model.board.ChessBoard
import model.board.MutableChessBoard

interface BoardCommand : Command {
    val board: ChessBoard
}

data class PrintBoard(override val board: ChessBoard) : BoardCommand {
    override fun exec() {
        println(board.textVisual())
    }
}