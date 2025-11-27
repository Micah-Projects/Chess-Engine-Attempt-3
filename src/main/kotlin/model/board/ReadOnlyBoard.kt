package model.board

import model.utils.BitBoard

class ReadOnlyBoard(private val board: ChessBoard = Board()) : ChessBoard {

    override fun fetchPiece(square: Int): Piece {
        return board.fetchPiece(square)
    }

    override fun fetchPieceBitBoard(pieceType: Piece): BitBoard {
        return board.fetchPieceBitBoard(pieceType)
    }

    override fun fetchPieceMask(vararg pieces: Piece): BitBoard {
        return board.fetchPieceMask(*pieces)
    }

    override fun fetchBitboards(): Array<BitBoard> {
        return board.fetchBitboards()
    }

    override fun getOccupancy(color: Color?): ULong {
        return board.getOccupancy(color)
    }

    override fun getCastleRights(): CastleRights {
        return board.getCastleRights()
    }

    override fun fetchEmptySquares(): ULong {
        return board.fetchEmptySquares()
    }

    override fun fetchEnpassantSquare(): Int? {
        return board.fetchEnpassantSquare()
    }

    override fun clone(): MutableChessBoard {
        return board.clone()
    }

    override fun toFen(): String {
        return board.toFen()
    }

    override fun textVisual(viewFrom: Color): String {
        return board.textVisual(viewFrom)
    }

    override fun toMutable(): MutableChessBoard {
        return board.toMutable()
    }
}