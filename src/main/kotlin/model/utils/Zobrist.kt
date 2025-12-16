package model.utils

import model.board.Board
import model.board.Piece
import model.board.ReadOnlyChessBoard
import kotlin.random.Random
import kotlin.random.nextLong
import kotlin.random.nextULong

object Zobrist {
    val castleKeys = ULongArray(4) {
        Random.nextULong()
    }

    val enpassantKeys = ULongArray(8) { // account for the file, not rank
        Random.nextULong()
    }

    val pieceKeys = Array<ULongArray>(Piece.COUNT) { pieceId -> // 768 indices
        ULongArray(Squares.COUNT) { Random.nextULong() }
    }

    val turnKey = Random.nextULong()

}