package command

import model.board.ReadOnlyChessBoard

interface BoardCommand : Command {
    val board: ReadOnlyChessBoard
}

data class PrintBoard(override val board: ReadOnlyChessBoard) : BoardCommand {
    override fun exec() {
        println(board.textVisual())
    }
}