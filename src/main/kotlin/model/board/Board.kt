package model.board

import DEBUG
import model.board.Piece.*
import model.board.Color.*
import model.utils.BitBoard
import model.utils.BitBoards
import model.utils.FenString
import model.utils.Squares
import model.movement.from
import model.movement.Move
import model.movement.movingPiece
import model.movement.promotionType
import model.utils.square
import model.movement.to
import model.utils.Zobrist
import kotlin.math.abs

class Board : ChessBoard {

    private var bitBoards: Array<BitBoard>
    private var enpassantSquare: square?
    private var castleRights: CastleRights
    override var hash: ULong private set
    /*
    updating hash would go as follows:
        if a piece is added ANYWHERE, we xor it into the hash
        if a piece is removed ANYWHERE we xor it out of the hash
        if a piece is moved, then we have from and to squares to check

        from is xor'ed out
        to is xor'ed into the to position
        piece on to is xor'ed out
     */

    constructor() {
        bitBoards = Array<BitBoard>(Piece.COUNT) { BitBoards.EMPTY_BB }
        enpassantSquare = null
        castleRights = CastleRights.FULL
        hash = 0uL
    }

    private constructor(bitboards: Array<BitBoard>, enpassantSquare: square?, castleRights: CastleRights, hash: ULong) {
        this.bitBoards = bitboards.clone()
        this.enpassantSquare = enpassantSquare
        this.castleRights = castleRights
        this.hash = hash
    }

    private fun addPiece(square: square, piece: Piece) {
        bitBoards[piece.id] = bitBoards[piece.id] or Squares.selectors[square]
        hash = hash xor Zobrist.pieceKeys[piece.id][square]
    }

    private fun removePiece(square: square, piece: Piece) {
        bitBoards[piece.id] = bitBoards[piece.id] and Squares.selectors[square].inv()
        hash = hash xor Zobrist.pieceKeys[piece.id][square]
    }

    override fun placePiece(piece: Piece, square: Int) {
        if (DEBUG) {
            if (!isInBounds(square)) throw IllegalArgumentException("Cannot add piece on out-of-bounds square: $square")
            if (!fetchPiece(square).isEmpty()) throw IllegalArgumentException("Cannot add piece to occupied square. Occupied by ${fetchPiece(square)}")
        }

        if (piece.isEmpty()) {
            if (DEBUG) throw IllegalArgumentException("Cannot add an empty piece onto the board.")
            else return
        }

        val other = fetchPiece(square)
        if (!other.isEmpty()) removePiece(square, other)
        addPiece(square, piece)
    }

    override fun clearPiece(square: Int) {
        if (DEBUG) {
            if (!isInBounds(square)) throw IllegalArgumentException("Cannot remove piece on out-of-bounds square: $square ")
        }
        val p = fetchPiece(square)
        if (p.isEmpty()) return
        removePiece(square, p)
    }

    override fun makeMove(move: Move) {
        movePiece(move.from(), move.to(), move.promotionType(), move.movingPiece())
    }

    override fun movePiece(start: square, end: square) {
        movePiece(start, end, Type.PAWN)
    }

    private fun movePiece(start: square, end: square, promotion: Type, movingPiece: Piece = fetchPiece(start), capturedPiece: Piece = fetchPiece(end)) {
        if (DEBUG) {
            require(movingPiece.isNotEmpty()) { "No piece found on square: $start." }
            require(isInBounds(start)) { "Start square: $start isn't in bounds." }
            require(isInBounds(end)) { "end square: $end isn't in bounds." }
            require(start != end) { "Piece cannot null-move: $start -> $end" }
        }
        val enpassantSquare = enpassantSquare
        clearEnpassant()
        if (movingPiece.isRook()) invalidateCastleRights(start)  // rook moves
        invalidateCastleRights(end) // rook captured (potentially)

        if (movingPiece.isPawn() || movingPiece.isKing()) {
            handleSpecialMovement(movingPiece, start, end, promotion, capturedPiece, enpassantSquare)
        } else {
            removePiece(start, movingPiece)
            clearPiece(end)
            addPiece(end, movingPiece)
        }
    }

    private fun removeRights(rights: Int) {
        var relevance = rights and castleRights.bits // protect against nonsense input although this is a private method
        castleRights = castleRights.without(relevance)
        while (relevance != 0) {
            hash = hash xor Zobrist.castleKeys[relevance.countTrailingZeroBits()]
            relevance = relevance and (relevance - 1)
        }
    }

    private fun clearEnpassant() {
        val file = Squares.fileOf(enpassantSquare ?: return)
        enpassantSquare = null
        hash = hash xor Zobrist.enpassantKeys[file]
    }

    private fun setEnpassant(square: square) {
        val file = Squares.fileOf(square)
        enpassantSquare = square
        hash = hash xor Zobrist.enpassantKeys[file]
    }


    private fun invalidateCastleRights(square: square) {
        val removal = when (square) {
            // squares.valueOf() is more expressive here, but slower
            0 -> CastleRights.WHITE_QS // a1
            7 -> CastleRights.WHITE_KS // h1
            56 ->CastleRights.BLACK_QS// a8
            63 -> CastleRights.BLACK_KS// h8
            else -> return
        }
        removeRights(removal)
    }

    /*
     * Write tests for this
     */
    private fun handleSpecialMovement(piece: Piece, start: square, end: square, promotion: Type, capturedPiece: Piece, enpassantSquare: square?) {
        val color = piece.color
        var finalPiece = piece
        when {
            piece.isPawn() -> {
                val behindMe = end - color.forwardOne
                // set enpassant - assume moves were correctly allowed for board speed
                if (Squares.rankDist(start, end) == 2) {
                     setEnpassant(behindMe)
                    // capture enpassant -
                } else if (capturedPiece.isEmpty() && end == enpassantSquare) {
                    clearPiece(behindMe) // may be able to remove this
                    // promote
                } else if (Squares.rankOf(end) == color.promotionRank) {
                    finalPiece = Piece.from(promotion, color)
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
                        clearPiece(rookSquare)
                        addPiece(rookDestination, rook)
                    }
                }
                removeRights(CastleRights.from(color))
            }
        }

        removePiece(start, piece)
        clearPiece(end)
        addPiece(end, finalPiece)
    }


    override fun fetchPiece(square: square): Piece {
        if (square !in Squares.range) return EMPTY
        val end = Squares.selectors[square]
        var id = 0
        while (id < Piece.COUNT) {
            if (bitBoards[id] and end != 0uL) return Piece.from(id)
            id++
        }
        return Piece.EMPTY
    }

    override fun fetchPieceBitBoard(pieceType: Piece): BitBoard {
        require(pieceType.isNotEmpty()) { "There is no bit board for ${Piece.EMPTY}." }
        return bitBoards[pieceType.id]
    }

    override fun fetchPieceMask(vararg pieces: Piece): BitBoard {
        var mask = 0uL
        for (piece in pieces) {
            mask = mask or bitBoards[piece.id]
        }
        return mask
    }

    override fun fetchBitboards(): Array<BitBoard> {
        return bitBoards.clone()
    }


    override fun getCastleRights(): CastleRights {
        return castleRights
    }

    override fun getOccupancy(color: Color?): ULong {
        var result = 0uL

        if (color == null) {
            for (bb in bitBoards) result = result or bb
            return result
        }
        var i = 6 * color.value
        val end = i + 6
        while (i < end) result = result or bitBoards[i++]
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

    override fun clone(): ChessBoard {
        return Board(bitBoards, enpassantSquare, castleRights, hash)
    }

    override fun loadFen(fen: String) {
        val fen = FenString(fen)
        val easyBoard = simplifyFenBoard(fen.board)
        loadEasyBoard(easyBoard) // this clears the enpassant
        enpassantSquare = fen.enpassantSquare
        castleRights = fen.castleRights

    }

    private fun loadEasyBoard(ezBoard: String) {
        bitBoards = Array<BitBoard>(12) { BitBoards.EMPTY_BB }
        clearEnpassant()
        hash = 0uL
        for (square in Squares.range) {
            val piece = Piece.fromSymbol(ezBoard[square].toString())
            if (!piece.isEmpty()) addPiece(square, piece)
        }
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

    override fun toMutable(): ChessBoard {
        return this.clone()
    }
}