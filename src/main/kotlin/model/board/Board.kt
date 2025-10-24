package model.board

import model.board.Piece.*
import model.board.Color.*
import model.misc.Debug.Area.*
import model.misc.BitBoard
import model.misc.BitBoards
import model.misc.Debug
import model.misc.FenString
import model.misc.Squares
import model.misc.end
import model.misc.getPromotion
import model.misc.move
import model.misc.square
import model.misc.start
import model.movement.CastleRights
import kotlin.math.abs

class Board : MutableChessBoard {

    private var bitBoards: Array<BitBoard>
    private var enpassantSquare: square?
    private var castleRights = CastleRights.FULL

    constructor() {
        bitBoards = Array<BitBoard>(Piece.COUNT) { BitBoards.EMPTY_BB }
        enpassantSquare = null
    }

    private constructor(bitboards: Array<BitBoard>, enpassantSquare: square?, castleRights: CastleRights) {
        this.bitBoards = bitboards.clone()
        this.enpassantSquare = enpassantSquare
        this.castleRights = castleRights
    }

    override fun addPiece(piece: Piece, square: square) {
        require(isInBounds(square)) { "Cannot add piece on out-of-bounds square: $square " }
        if (piece.isEmpty()) return
        val i = piece.value
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
            // questionable code below
            if (bitBoards[i] != BitBoards.removeBit(bitBoards[i], square)) {
                Debug.log(Debug.Area.BOARD) { "${Piece.from(i)} added on $square -> ${Squares.asText(square)}" }
            }
        }

    }


    override fun makeMove(move: move) {
        movePiece(move.start(), move.end(), move.getPromotion())
    }

    override fun movePiece(start: square, end: square) {
        movePiece(start, end, EMPTY)
    }

    private fun movePiece(start: square, end: square, promotion: Piece = EMPTY) {
        val piece = fetchPiece(start)
        require(piece.isNotEmpty()) { "No piece found on square: $start." }
        require(isInBounds(start)) { "Start square: $start isn't in bounds." }
        require(isInBounds(end)) { "end square: $end isn't in bounds." }
        require(start != end) { "Piece cannot null-move: $start -> $end" }

        val promotion = if (piece.isPawn() && promotion.isEmpty()) defaultPromotion(piece.color) else promotion
        if (piece.isRook()) invalidateCastleRights(start)  // rook moves
        invalidateCastleRights(end) // rook captured (potentially)

        if (piece.isPawn() || piece.isKing()) {
            handleSpecialMovement(piece, start, end)
        } else {
            enpassantSquare = null
            removePiece(start)
            addPiece(piece, end)
        }

        Debug.log(BOARD) {
            "$piece moved from $start -> $end : ${Squares.asText(start)} -> ${Squares.asText(end)}"
        }
    }

    private fun invalidateCastleRights(square: square) {
        when (square) {
            // squares.valueOf() is more expressive here, but slower
            0 -> castleRights = castleRights.without(CastleRights.WHITE_QS) // a1
            7 -> castleRights = castleRights.without(CastleRights.WHITE_KS) // h1
            56 -> castleRights = castleRights.without(CastleRights.BLACK_QS)// a8
            63 -> castleRights = castleRights.without(CastleRights.BLACK_KS)// h8
        }
    }

    /*
     * Write tests for this
     */
    private fun handleSpecialMovement(piece: Piece, start: square, end: square) {
        val color = piece.color
        when {
            piece.isPawn() -> {
                val behindMe = squareBehind(end, color)
                // set enpassant
                if (Squares.rankIs(start, color.pawnStartRank) && Squares.rankDist(start, end) == 2) {
                    enpassantSquare = behindMe
                    // capture enpassant
                } else if (Squares.isOnDiagonal(start, end) && fetchPiece(end).isEmpty() && end == enpassantSquare) {
                    removePiece(behindMe)
                    // if not setting, or capturing, clear it
                } else {
                    enpassantSquare = null
                }
            }

            piece.isKing() -> {
                // we use direct squares, since horizontals will have a diff of 2, verticals wont (this is a fast check)
                // if this ever happens, we assume castling was possible, although the flags may advice against it
                if (abs(start - end) == Squares.KING_CASTLE_DISTANCE) {
                    val rookSquare: square
                    val rookDestination: square
                    // make a new rook, we don't waste time querying the board
                    val rook = Piece.from(Type.ROOK, color)

                    if (start > end) {
                        // queen side,
                        rookSquare = end - 2
                        rookDestination = end + 1
                    } else {
                        // king side
                        rookSquare = end + 1
                        rookDestination = end - 1
                    }

                    if (Squares.isInBounds(rookSquare) && Squares.isInBounds(rookDestination)) {
                        removePiece(rookSquare)
                        addPiece(rook, rookDestination)
                    }

                    enpassantSquare = null
                }
                castleRights = castleRights.without(CastleRights.from(color))
            }
        }
        removePiece(start)
        addPiece(piece, end)
    }

    private fun squareBehind(start: square, color: Color): Int {
        return start + (Squares.ROW_INCREMENT * color.enemy.pawnDirection)
    }

    private fun defaultPromotion(color: Color): Piece {
        return when (color) {
            WHITE -> WHITE_QUEEN
            BLACK -> BLACK_QUEEN
        }
    }

    override fun fetchPiece(square: square): Piece {
        if (square !in Squares.range) return EMPTY
        val end = 1uL shl square
        for (i in bitBoards.indices) {
            if (bitBoards[i] and end != 0uL) return Piece.from(i)
        }
        return Piece.EMPTY
    }

    override fun fetchPieceBitBoard(pieceType: Piece): BitBoard {
        require(pieceType.isNotEmpty()) { "There is no bit board for ${Piece.EMPTY}." }
        return bitBoards[pieceType.value]
    }

    override fun getCastleRights(): CastleRights {
        return castleRights
    }

    override fun getOccupancy(color: Color?): ULong {
        var result = 0uL
        when (color) {
            WHITE -> for (i in 0..5) result = result or bitBoards[i]
            BLACK -> for (i in 6..11) result = result or bitBoards[i]
            else  -> for (bb in bitBoards) result = result or bb
        }
        return result
    }

    override fun fetchEmptySquares(): ULong {
        var result = 0uL
        for (bb in bitBoards) result = bb or result
        return result.inv()
    }

    override fun fetchEnpassantSquare(): square? {
        return enpassantSquare
    }

    private fun isInBounds(square: square) = square in Squares.range

    override fun clone(): MutableChessBoard {
        return Board(bitBoards, enpassantSquare, castleRights)
    }

    override fun loadFen(fen: String) {
        val fen = FenString(fen)
        val easyBoard = simplifyFenBoard(fen.board)
        enpassantSquare = fen.enpassantSquare
        loadEasyBoard(easyBoard)
    }

    private fun loadEasyBoard(ezBoard: String) {
        clear()
        for (square in Squares.range) {
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
        for (square in Squares.range) {
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
        for (s in (Squares.COUNT- 1).downTo(0)) {
            val piece = fetchPiece(s)
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

    override fun toMutable(): MutableChessBoard {
        return this.clone()
    }
}