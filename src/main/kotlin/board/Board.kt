package board

import misc.BitBoards
import misc.BitBoards.EMPTY_BB
import misc.BitBoard
import misc.Squares
import misc.square

import board.Piece.*
import board.Color.*
import misc.BOARD_SIZE
import misc.Debug
import misc.Debug.Area
import misc.Debug.Area.*
import misc.boardSquares

class Board : ChessBoard {

    private val bitBoards: Array<BitBoard>
    private var enpassantSquare: square?

    constructor() {
        bitBoards = Array<BitBoard>(12) { EMPTY_BB }
        enpassantSquare = null
    }

    private constructor(bitboards: Array<BitBoard>, enpassantSquare: square?) {
        this.bitBoards = bitboards.clone()
        this.enpassantSquare = enpassantSquare
    }

    override fun addPiece(piece: Piece, square: square) {
        require(isInBounds(square)) { "Cannot add piece on out-of-bounds square: $square " }
        val i = piece.get()
        val targetBB = bitBoards[i]
        for (bb in bitBoards.indices) {
            bitBoards[bb] = BitBoards.removeBit(bitBoards[bb], square)
        }
        bitBoards[i] = BitBoards.addBit(targetBB, square)
        Debug.log(BOARD) { "$piece added on $square (${Squares.asText(square)})" }
    }

    override fun removePiece(square: square) {
        require(isInBounds(square)) { "Cannot remove piece on out-of-bounds square: $square " }
        for (i in bitBoards.indices) {
            bitBoards[i] = BitBoards.removeBit(bitBoards[i], square)
            if (bitBoards[i] != BitBoards.removeBit(bitBoards[i], square)) {
                Debug.log(BOARD) { "${Piece.from(i)} added on $square -> ${Squares.asText(square)}" }
            }
        }

    }

    override fun movePiece(start: square, end: square) {
        val piece = fetchPiece(start)
        require(piece.isNotEmpty()) { "No piece found on square: $start." }
        require(isInBounds(start)) { "Start square: $start isn't in bounds." }
        require(isInBounds(end)) { "end square: $end isn't in bounds." }
        require(start != end) { "board.Piece cannot null-move: $start -> $end" }

        removePiece(start)
        addPiece(piece, end)

        Debug.log(BOARD) {
            "$piece moved from $start -> $end : ${Squares.asText(start)} -> ${Squares.asText(end)}"
        }
    }

    override fun fetchPiece(square: square): Piece {
        val end = 1uL shl square
        for (i in bitBoards.indices) {
            if (bitBoards[i] and end != 0uL) return Piece.from(i)
        }
        return EMPTY
    }

    override fun fetchPieceBitBoard(pieceType: Piece): BitBoard {
        require(pieceType.isNotEmpty()) { "There is no bit board for $EMPTY." }
        return bitBoards[pieceType.get()]
    }

    override fun fetchEnpassantSquare(): square? {
        return enpassantSquare
    }

    private fun isInBounds(square: square) = square in boardSquares

    override fun clone(): ChessBoard {
        return Board(bitBoards, enpassantSquare)
    }
    // rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
    override fun loadFen(fen: String) {
        TODO("Not yet implemented")
    }

    override fun toFen(): String {
        TODO("Not yet implemented")
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
        return if (viewFrom == WHITE) board.toString() else board.toString().reversed()
    }
}