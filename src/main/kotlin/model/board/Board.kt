package model.board

import model.board.Piece.*
import model.board.Color.*
import model.misc.Debug.Area.*
import model.misc.BitBoard
import model.misc.BitBoards
import model.misc.Debug
import model.misc.FenString
import model.misc.Squares
import model.misc.square

class Board : ChessBoard {
    companion object {
        const val BOARD_SIZE = 64;
        val boardSquares: IntRange = 0..63
        private const val WHITE_PROMOTION_RANK = 8
        private const val BLACK_PROMOTION_RANK = 0
        private const val KING_CASTLE_DISTANCE = 2
    }

    private var bitBoards: Array<BitBoard>
    private var enpassantSquare: square?

    constructor() {
        bitBoards = Array<BitBoard>(12) { BitBoards.EMPTY_BB }
        enpassantSquare = null
    }

    private constructor(bitboards: Array<BitBoard>, enpassantSquare: square?) {
        this.bitBoards = bitboards.clone()
        this.enpassantSquare = enpassantSquare
    }

    override fun addPiece(piece: Piece, square: square) {
        require(isInBounds(square)) { "Cannot add piece on out-of-bounds square: $square " }
        if (piece.isEmpty()) return
        val i = piece.get()
        val targetBB = bitBoards[i]
        for (bb in bitBoards.indices) {
            bitBoards[bb] = BitBoards.removeBit(bitBoards[bb], square)
        }
        bitBoards[i] = BitBoards.addBit(targetBB, square)
        Debug.log(Debug.Area.BOARD) { "$piece added on $square (${Squares.asText(square)})" }
    }

    override fun removePiece(square: square) {
        require(isInBounds(square)) { "Cannot remove piece on out-of-bounds square: $square " }
        for (i in bitBoards.indices) {
            bitBoards[i] = BitBoards.removeBit(bitBoards[i], square)
            if (bitBoards[i] != BitBoards.removeBit(bitBoards[i], square)) {
                Debug.log(Debug.Area.BOARD) { "${Piece.from(i)} added on $square -> ${Squares.asText(square)}" }
            }
        }

    }

    /*
        - Needs to support Enpassant:
          Pawns need to...
            - update the enpassant square
            - move diagonally
            - remove the pawn behind its capture square

        - Needs to support promotions
          Pawns need to...
            - replace itself with a promotion when on its respective promotion rank

        - Needs to support Castling:
          Kings need to...
            - move two squares horizontally
            - move the corresponding rook to its opposite side


     */

    override fun movePiece(start: square, end: square) {
        val piece = fetchPiece(start)
        require(piece.isNotEmpty()) { "No piece found on square: $start." }
        require(isInBounds(start)) { "Start square: $start isn't in bounds." }
        require(isInBounds(end)) { "end square: $end isn't in bounds." }
        require(start != end) { "board.Piece cannot null-move: $start -> $end" }

        removePiece(start)
        addPiece(piece, end)

        Debug.log(Debug.Area.BOARD) {
            "$piece moved from $start -> $end : ${Squares.asText(start)} -> ${Squares.asText(end)}"
        }
    }

    override fun fetchPiece(square: square): Piece {
        val end = 1uL shl square
        for (i in bitBoards.indices) {
            if (bitBoards[i] and end != 0uL) return Piece.from(i)
        }
        return Piece.EMPTY
    }

    override fun fetchPieceBitBoard(pieceType: Piece): BitBoard {
        require(pieceType.isNotEmpty()) { "There is no bit board for ${Piece.EMPTY}." }
        return bitBoards[pieceType.get()]
    }

    override fun fetchEnpassantSquare(): square? {
        return enpassantSquare
    }

    private fun isInBounds(square: square) = square in boardSquares

    override fun clone(): ChessBoard {
        return Board(bitBoards, enpassantSquare)
    }

    override fun loadFen(fen: String) {
        val fen = FenString(fen)
        val easyBoard = simplifyFenBoard(fen.board)
        enpassantSquare = fen.enpassantSquare
        loadEasyBoard(easyBoard)
    }

    private fun loadEasyBoard(ezBoard: String) {
        clear()
        for (square in boardSquares) {
            val piece = Piece.fromSymbol(ezBoard[square].toString())
            addPiece(piece, square)
        }
    }

    private fun clear() {
        bitBoards = Array<BitBoard>(12) { BitBoards.EMPTY_BB }
        enpassantSquare = null
    }

    private fun simplifyFenBoard(fenBoard: String): String {
        val fenBuilder = StringBuilder()
        val fenBoard = fenBoard.split("/").reversed()
        for (rank in 0..7) {
            val fenRank = fenBoard[rank]
            for (char in fenRank) {
                if (char.isDigit()) {
                    fenBuilder.append("0".padStart(char.digitToInt(), '0'), )
                } else {
                    fenBuilder.append(char)
                }
            }
        }
        return fenBuilder.toString()

    }

    override fun toFen(): String {
        val fen = StringBuilder()
        var segment = StringBuilder()
        var counter = 0
        for (square in boardSquares) {
           val piece = fetchPiece(square)
            when (piece) {
                Piece.EMPTY -> counter++
                else -> {
                    if (counter > 0) {
                        segment.append(counter)
                        counter = 0
                    }
                    segment.append(piece.symbol())
                }
            }
            if (Squares.fileIs(square, 7)) {
                if (counter > 0) {
                    segment.append(counter)
                    counter = 0
                }
                fen.append(segment.reversed())
                if (square != 63)fen.append('/')
                segment = StringBuilder()
            }
        }

        return fen.toString().reversed()
    }

    override fun textVisual(viewFrom: Color): String {
        val board = StringBuilder()
        val line = StringBuilder()
        val letters = "     A   B   C   D   E   F   G   H     "
        board.append(letters + "\n")
        board.append("   +---+---+---+---+---+---+---+---+   \n")
        line.append("|")
        for (s in (BOARD_SIZE - 1).downTo(0)) {
            var piece =  fetchPiece(s)
            line.append(" ${piece.symbol()} |")
            if (Squares.fileIs(s, 0)) {
                line.append(" ${Squares.rankOf(s) + 1}")
                board.append(" ${line.reversed()} ${Squares.rankOf(s) + 1} \n") //)
                line.clear()
                board.append("   +---+---+---+---+---+---+---+---+   \n")
                line.append("|")
            }
        }
        board.append(letters)
        return if (viewFrom == Color.WHITE) board.toString() else board.toString().reversed()
    }
}