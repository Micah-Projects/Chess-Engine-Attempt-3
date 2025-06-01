package command

import model.board.ChessBoard

interface BoardCommand : Command {
    val board: ChessBoard
}