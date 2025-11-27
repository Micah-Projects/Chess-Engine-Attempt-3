package model.movement

import model.board.ChessBoard
import model.board.Color
import model.misc.move

interface MoveGenerator {
    fun generateMoves(board: ChessBoard, color: Color): List<move>
    fun isKingInCheck(): Boolean
}


/* moves buffer:
        fields:

        resultList field where sort, filter, etc will be stored (to avoid allocations)
        buffer: the int array of size 218
        pointer: the index of the next free space in the buffer
        - - -

        inline methods
        sorted by it.isCapture, or enpassant, etc -> list
        filter by it.movingPiece.isPawn, it.capture -> list
        for each

        operator fun getMove(index)
            if index > pointer -> throw

        clear()
        add()

 */
