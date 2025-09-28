package command

import model.board.ChessBoard

interface BoardCommand : Command {
    val board: ChessBoard
}

data class Print(override val board: ChessBoard) : BoardCommand {
    override fun exec() {
        println(board.textVisual())
    }
}