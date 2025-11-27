package model.movement

import model.board.Board
import model.board.ChessBoard
import model.board.Color
import model.board.Piece
import model.board.Piece.Type
import model.misc.BetterMoves
import model.misc.BitBoard
import model.misc.BitBoards
import model.misc.BitBoards.binaryFill
import model.misc.Squares
import model.misc.move
import model.misc.square
import kotlin.math.abs

class InlinedBitBoardMoveGenerator : MoveGenerator {
    companion object {
        private const val mgSliders = 2
        private const val bishopOrder = 0
        private const val rookOrder = 1

        private val kingAttackMasks = Array<ULong>(Squares.COUNT) { square -> RayCrawler.leap(square, RayCrawler.queens) }
        private val knightAttackMasks = Array<ULong>(Squares.COUNT) { square -> RayCrawler.leap(square, RayCrawler.knights) }
        private val pawnAttackMasks = Array<Array<ULong>>(Color.COUNT) { color ->
            val directions = if (color == 0) RayCrawler.pawnAttacks else RayCrawler.pawnAttacks.map { -it }
            Array(Squares.COUNT) { square ->
                RayCrawler.leap(square, directions)
            }
        }

        private val sliderMasks = Array<Array<ULong>>(mgSliders) { idx ->
            val piece = Piece.sliders[idx]
            Array(Squares.COUNT) { square ->
                RayCrawler.crawlRays(square, RayCrawler.getRays(piece))
            }
        }
        private val sliderMasksNoEdges = Array<Array<ULong>>(mgSliders) { idx ->
            val piece = Piece.sliders[idx]
            Array(Squares.COUNT) { square ->
                RayCrawler.crawlExcludeEdge(square, RayCrawler.getRays(piece))
            }
        }
        private val sliderBlockerSets = Array<Array<Array<ULong>>>(mgSliders) { idx ->
            Array(Squares.COUNT) { square ->
                binaryFill(sliderMasksNoEdges[idx][square])
            }
        }
        private val sliderMagics = MagicGenerator.getMagicKeys()
        private val sliderAttackMap = Array<Array<Array<ULong>>>(mgSliders) { type ->
            val piece = Piece.sliders[type]
            Array(Squares.COUNT) { square ->
                val blockersForSquare = sliderBlockerSets[type][square]
                val neededBits = sliderMasksNoEdges[type][square].countOneBits()
                val shiftFactor = ULong.SIZE_BITS - neededBits.toInt()

                // array sized to maximum possible magic index
                val mapSize = 1 shl neededBits
                val attacks = Array<ULong>(mapSize) { 0uL }

                for (blockers in blockersForSquare) {
                    val occupancy = blockers and sliderMasksNoEdges[type][square]
                    val magicIndex = ((sliderMagics[type][square] * occupancy) shr shiftFactor).toUInt().toInt()
                    attacks[magicIndex] = RayCrawler.crawlUntilBlockers(
                        blockers, square, RayCrawler.getRays(piece), false
                    )
                }
                attacks
            }

        }

        private val pinMasks = Array(Squares.COUNT) { kingSquare ->
            Array(Squares.COUNT) { enemySliderSquare ->
                RayCrawler.lineTo(kingSquare, enemySliderSquare, includeStart = false, includeEnd = false)
            }
        }

        private val rankMask = Array<ULong>(Squares.NUM_RANKS) { rank ->
            val b = 1uL shl rank * 8
            RayCrawler.crawlRays(b.countTrailingZeroBits(), RayCrawler.horizontals, includeStart = true)
        }
        private val fileMask = Array<ULong>(Squares.NUM_RANKS) { file ->
            val b = 1uL shl file
            RayCrawler.crawlRays(b.countTrailingZeroBits(), RayCrawler.verticals)
        }
    }


    // vars to act as global data
    // these are observations that can be computed one time, rather than multiple times in different branches

    private var pointer = 0 // index into our move stack
    private var moveBuffer: IntArray = IntArray(218) { -1 }

    private var board: ChessBoard = Board()
    private var enemyOccupancy = 0uL
    private var friendlyOccupancy = 0uL
    private var totalOccupancy = 0uL
    private var emptySquares = 0uL
    private var genColor = Color.WHITE

    private var kingPosition = -1
    private var enemyAttackMask = 0uL
    private var safeSquares = 0uL
    private var enpassant: Int? = null
    private var kingAttackers = IntArray(Squares.COUNT) { -1 }
    private var enemyContext = IntArray(Squares.COUNT)
    private var pieceSet: Array<Piece> = Piece.whitePieceSet
    private var kingBB: BitBoard = 0uL

    private var enpassantBB: BitBoard = 0uL
    private var noEnpassant: BitBoard = 0uL
    private var kingAttackerBB: BitBoard = 0uL

    private var check = false

    override fun generateMoves(board: ChessBoard, color: Color): List<move> {
        val result = mutableListOf<move>()
        genMoves(board, color)
        var i = 0
        while (i < pointer) {
            result.add(moveBuffer[i++])
        }
        return result
    }


    override fun isKingInCheck(): Boolean {
        return check
    }

    private fun getSliderAttackMask(sliderOrder: Int, square: square): BitBoard {
        val index = indexFromOccupancy(sliderOrder, square, sliderMasksNoEdges[sliderOrder][square] and totalOccupancy)
        return sliderAttackMap[sliderOrder][square][index]
    }

    private fun getSliderAttackMaskNoKing(sliderOrder: Int, square: square): BitBoard {
        val index = indexFromOccupancy(sliderOrder, square, sliderMasksNoEdges[sliderOrder][square] and totalOccupancy and kingBB.inv())
        return sliderAttackMap[sliderOrder][square][index]
    }

    private fun indexFromOccupancy(sliderIdx: Int, square: square, occupancy: ULong): Int {
        val shiftFactor = ULong.SIZE_BITS - sliderMasksNoEdges[sliderIdx][square].countOneBits()
        val hashValue = ((sliderMagics[sliderIdx][square] * occupancy) shr shiftFactor).toUInt().toInt()
        return hashValue
    }

    private fun observeBoard() {
        val king = Piece.from(Type.KING, genColor)
        kingBB = board.fetchPieceBitBoard(king) // just count the 0 bits instead of this
        kingPosition = kingBB.countTrailingZeroBits()
        enemyContext.fill(-1)
        friendlyOccupancy = board.getOccupancy(genColor)
        enemyOccupancy = board.getOccupancy(genColor.enemy)
        totalOccupancy = enemyOccupancy or friendlyOccupancy
        emptySquares = totalOccupancy.inv()
        enpassant = board.fetchEnpassantSquare()
        enpassantBB = if (enpassant == null) 0uL else 1uL shl enpassant!!
        noEnpassant = enpassantBB.inv()
    }

    private fun observeKingSafety() {
        enemyAttackMask = 0uL
        safeSquares = 0uL
        kingAttackerBB = 0uL
        kingAttackers.fill(-1)
        check = false
        BitBoards.iterateBits(enemyOccupancy) { enemyPosition ->
            val piece = board.fetchPiece(enemyPosition) // loop may cause weirdness with double checks
            val scope = getPseudoLegalAttackScope(piece, enemyPosition)

            if (scope and (kingBB) != 0uL) {
                kingAttackers[enemyPosition] = piece.id
                kingAttackerBB = kingAttackerBB or (1uL shl enemyPosition)
            }
            enemyContext[enemyPosition] = piece.id
            enemyAttackMask = enemyAttackMask or scope
        }
        safeSquares = enemyAttackMask.inv()
        check = enemyAttackMask and (kingBB) != 0uL // check if being attacked
    }

    private fun genMoves(board: ChessBoard, color: Color) {
        this.board = board
        this.genColor = color
        this.pointer = 0 // act as a clear
        this.pieceSet = if (color == Color.WHITE) Piece.whitePieceSet else Piece.blackPieceSet
        observeBoard() // occupancy, enpassant, king position
        observeKingSafety() // check, king attackers, enemy attacks, safe squares
        genPawns(pieceSet[0]) // always pawns
        val limit = pieceSet.size
        var i = 1 // set to 1 to skip pawn gen
        while( i < limit) {
            val piece = pieceSet[i++]
            BitBoards.iterateBits(board.fetchPieceBitBoard(piece)) {
                generateMovesForPiece(it, piece)
            }
        }
    }

    private fun generateMovesForPiece(position: square, piece: Piece) {
        var attackMask = getPseudoLegalAttackScope(piece, position)
        val positionBB = 1uL shl position

        if (piece.isKing()) {
            BitBoards.iterateBits(kingAttackerBB) { attackerPosition ->
                val attacker = Piece.from(kingAttackers[attackerPosition])
                enemyAttackMask = enemyAttackMask or
                        (when {
                            attacker.isRook() ->   getSliderAttackMaskNoKing(rookOrder, attackerPosition)
                            attacker.isBishop() -> getSliderAttackMaskNoKing(bishopOrder, attackerPosition)
                            attacker.isQueen() -> {
                                getSliderAttackMaskNoKing(rookOrder, attackerPosition) or getSliderAttackMaskNoKing(bishopOrder, attackerPosition)
                            }
                            else -> 0uL
                        }
                                )
            }
            safeSquares = enemyAttackMask.inv()
            // 1111 // castle rights
            // 0,1 for white, 2, 3 for black

            val rights = board.getCastleRights().bits
            if (rights > 0) {
                val validSquares = (safeSquares and emptySquares)
                val KS = 1 shl 0 + (piece.color.value * 2) and rights // king side // index rights for legality
                val QS = 1 shl 1 + (piece.color.value * 2) and rights // queen side

                val rightOne = (1uL shl (kingPosition + 1)) and validSquares //and emptySquares// KS
                val rightTwo = rightOne shl 1 and validSquares

                val leftOne = (1uL shl (kingPosition - 1)) and validSquares// and emptySquares // QS
                val leftTwo = leftOne shr 1 and validSquares
                val leftThree = (leftTwo shr 1) and emptySquares // king doesnt pass through it, so it just needs emptiness

                if (KS != 0 && !check) attackMask = attackMask or rightTwo
                if (QS != 0 && !check && leftThree != 0uL) attackMask = attackMask or leftTwo

            }
            attackMask = attackMask and safeSquares // king cannot move into danger
        } else {
            attackMask = accountedForKingSafety(piece, position,  positionBB, attackMask) // pieces must ensure king is safe
        }

        attackMask = attackMask and friendlyOccupancy.inv() // pieces can never hurt their team
        BitBoards.iterateBits(attackMask) { attack ->
            moveBuffer[pointer++] = BetterMoves.encode(piece,position, attack)
        }
    }

    // filters out moves that put or leave the king in check
    // invariant: this method never ADDS move squares
    private fun accountedForKingSafety(friendlyPiece: Piece, position: square, positionBB: BitBoard, attackMask: BitBoard): BitBoard {
        var m = attackMask
        // find and account for pins,
        // sliders vs non-sliders
        // enpassant edge cases (enpassant horizontal + vertical
        // pieces can at most be pinned to 1 direction. after a pin is found, restrict moves to that pin
        // know whether the king is currently in check / attacked
        var pinned = false
        BitBoards.iterateBits(enemyOccupancy) iterateBits@{ enemyPosition ->
            if (m == 0uL) return m
            //val  = b.countTrailingZeroBits()
            val enemyPiece = Piece.from( enemyContext[enemyPosition])

            val enemyBB = 1uL shl enemyPosition
            val enemyAttacks = getFullAttackScope(enemyPiece, enemyPosition)

            if (enemyPiece.isSlider()) {
                val pinRay = pinMasks[kingPosition][enemyPosition]
                val pinRelevance = pinRay and enemyAttacks
                val pinRelevant = pinRelevance != 0uL
                val kingInEnemyRay = kingBB and enemyAttacks != 0uL
                if (!pinRelevant && !kingInEnemyRay) {
                    return@iterateBits // this particular enemy poses no threat to our king. // local continue
                }
                val blockersInPinRay = (pinRelevance and totalOccupancy).countOneBits()
                val allDefendingSquares = pinRelevance or enemyBB // account for the actual attacker position

                when (blockersInPinRay) {
                    0 -> {
                        m = m and allDefendingSquares // satisfies invariant
                    }

                    1 -> {
                        pinned = pinRelevance and positionBB != 0uL
                        if (pinned) {
                            m = m and allDefendingSquares
                        }

                    }

                    else -> {
                        if (friendlyPiece.isPawn()) {
                            if (blockersInPinRay == 2 &&
                                Squares.isOnSameRank(kingPosition, position) &&
                                Squares.isOnSameRank(position, enemyPosition)
                            ) {
                                m = m and noEnpassant // we remove horizontal enpassant
                            }
                        }
                    }
                }
            } else {
                val enemyAttacksKing = enemyAttacks and kingBB != 0uL
                if (enemyAttacksKing) {
                    if (friendlyPiece.isPawn() && enemyPiece.isPawn()) {
                        val possibleMoves = enpassantBB or enemyBB
                        m = m and possibleMoves // allows enpassant if originally in attack mask
                    } else {
                        m = m and enemyBB
                    }
                }
            }


        }
        var b = enemyOccupancy
        while(b != 0uL) {

            b = b and (b - 1uL)
        }

        return m
    }

    private fun getFullAttackScope(piece: Piece, position: square): BitBoard {
        return when {
            piece.isPawn() -> pawnAttackMasks[piece.color.value][position]
            piece.isQueen() -> sliderMasks[bishopOrder][position] or sliderMasks[rookOrder][position]
            piece.isKnight() -> knightAttackMasks[position]
            piece.isBishop() ->  sliderMasks[bishopOrder][position]
            piece.isRook() -> sliderMasks[rookOrder][position]
            piece.isKing() -> kingAttackMasks[position]
            else -> 0uL
        }
    }

    private fun getPseudoLegalAttackScope(piece: Piece, position: square): BitBoard {
        return when {
            piece.isBishop() -> getSliderAttackMask(bishopOrder, position)
            piece.isRook() -> getSliderAttackMask(rookOrder, position)
            piece.isQueen() -> {
                getSliderAttackMask(rookOrder, position) or
                        getSliderAttackMask(bishopOrder, position)
            }
            piece.isKnight() -> knightAttackMasks[position]
            piece.isKing() -> kingAttackMasks[position]
            piece.isPawn() -> pawnAttackMasks[piece.color.value][position]
            else -> BitBoards.EMPTY_BB
        }
    }

    private fun genPawns(piece: Piece) {
       // val enpAllowed = enpassant != null
        val enpassant = enpassantBB.countTrailingZeroBits()
        //val enpassant = enpassant
        val pawnMask = board.fetchPieceBitBoard(piece)
        if (pawnMask == 0uL) return // no need to calculate pawn data further
        val startRank = piece.color.pawnStartRank
        val pawnDirection = piece.color.pawnDirection
        val promotionRank = piece.color.promotionRank
        // one step is allowed if the concerned square is empty
        // two step is allowed if the pawn is on start rank and one step exists, and the destination is free
        val oneStep = 8 * pawnDirection // called once per generation (micro opt)
        val twoStep = 16 * pawnDirection
        var onePush: ULong
        var twoPush: ULong

        if (pawnDirection == 1) {
            onePush = (pawnMask shl 8) and emptySquares
            twoPush = ((pawnMask and rankMask[startRank]) shl 16) and emptySquares and (onePush shl 8)
        } else {
            onePush = (pawnMask shr 8) and emptySquares
            twoPush = ((pawnMask and rankMask[startRank]) shr 16) and emptySquares and (onePush shr 8)
        }

        //- - - - finalize and add moves - - - -

        BitBoards.iterateBits(pawnMask) { from ->
            val next1 = from + oneStep
            val next2 = from + twoStep
            val forwardOne = 1uL shl (next1) and onePush
            val forwardTwo = 1uL shl (next2) and twoPush
            safeAddMove(piece, from, next1, forwardOne, next1 / 8 == promotionRank) // account for promotion
            safeAddMove(piece, from, next2, forwardTwo)

            BitBoards.iterateBits(pawnAttackMasks[piece.color.value][from] and enemyOccupancy) { attack ->
                safeAddMove( piece, from, attack, 1uL shl attack, attack / 8 == promotionRank)
            }
                val fromRank = from / 8
                val enpRank = enpassant / 8
                val fromFile = from % 8
                val enpFile = enpassant % 8

                val fileDist = abs(fromFile - enpFile)
                val rankDist = abs(fromRank - enpRank)

                if (fileDist == rankDist && fileDist == 1 &&
                    pawnDirection * fromRank < pawnDirection * enpRank) {
                    safeAddMove(piece, from, enpassant, enpassantBB) // should fail since enpassant bb will be empty
                }
        }
    }


    private fun safeAddMove(
        piece: Piece,
        from: Int,
        to: Int,
        attackMask: BitBoard,
        promotion: Boolean = false
    ) {
        val safeMask = accountedForKingSafety(piece,from, 1uL shl from, attackMask)
        if (safeMask and attackMask != 0uL) {
            if (promotion) {
                moveBuffer[pointer++] = BetterMoves.encode(piece, from, to, Type.ROOK)
                moveBuffer[pointer++] = BetterMoves.encode(piece, from, to, Type.QUEEN)
                moveBuffer[pointer++] = BetterMoves.encode(piece, from, to, Type.BISHOP)
                moveBuffer[pointer++] = BetterMoves.encode(piece, from, to, Type.KNIGHT)
            } else {
                moveBuffer[pointer++] = BetterMoves.encode(piece, from, to)
            }

        }

    }


}