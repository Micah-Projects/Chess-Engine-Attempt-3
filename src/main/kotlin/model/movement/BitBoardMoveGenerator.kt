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
import model.misc.BitBoards.getSetBitIndices
import model.misc.Squares
import model.misc.move
import model.misc.square
import kotlin.math.abs


class BitBoardMoveGenerator : MoveGenerator {
    private val mgSliders = 2
    private val bishopOrder = 0
    private val rookOrder = 1

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
            RayCrawler.crawlExcludeEdge(square, RayCrawler.getRays(piece))
        }
    }
    private val sliderBlockerSets = Array<Array<Array<ULong>>>(mgSliders) { idx ->
        Array(Squares.COUNT) { square ->
            binaryFill(sliderMasks[idx][square])
        }
    }
    private val sliderMagics = MagicGenerator.getMagicKeys()
    private val sliderAttackMap = Array<Array<Array<ULong>>>(mgSliders) { type ->
        val piece = Piece.sliders[type]
        Array(Squares.COUNT) { square ->
            val blockersForSquare = sliderBlockerSets[type][square]
            val neededBits = sliderMasks[type][square].countOneBits()
            val shiftFactor = ULong.SIZE_BITS - neededBits.toInt()

            // array sized to maximum possible magic index
            val mapSize = 1 shl neededBits
            val attacks = Array<ULong>(mapSize) { 0uL }

            for (blockers in blockersForSquare) {
                val occupancy = blockers and sliderMasks[type][square]
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
            RayCrawler.lineTo(kingSquare, enemySliderSquare, includeStart = false, includeEnd = true)
        }
    }
    private val xRayMasks = Array(mgSliders) { order -> // this impl is prolly not a good idea
        val piece = Piece.sliders[order]
        Array(Squares.COUNT) { attacker ->
            val dir = RayCrawler.getRays(piece)
            RayCrawler.crawlRays(attacker, dir)
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
    private var enemyPositions = listOf<Int>()
    private var enemyAttackMask = 0uL
    private var safeSquares = 0uL
    private var enpassant: Int? = null
    private var kingAttackers = mutableListOf<Int>()

    private var enpassantBB: BitBoard = 0uL

    private var check = false


    private fun getSliderAttackMask(sliderOrder: Int, square: square): BitBoard {
        val index = indexFromOccupancy(sliderOrder, square, sliderMasks[sliderOrder][square] and totalOccupancy)
        return sliderAttackMap[sliderOrder][square][index]
    }
    private fun indexFromOccupancy(sliderIdx: Int, square: square, occupancy: ULong): Int {
        val shiftFactor = ULong.SIZE_BITS - sliderMasks[sliderIdx][square].countOneBits()
        val hashValue = ((sliderMagics[sliderIdx][square] * occupancy) shr shiftFactor).toUInt().toInt()
        return hashValue
    }


//    init {
////        repeat(50) {
////            val sq = Squares.random()
////            val sqs =  BitBoards.getSetBitIndices(sliderMasks[Random.nextInt(2)][sq])
////            BitBoards.print(RayCrawler.lineTo(sq, sqs.random()))
////        }
//      //  BitBoards.print(RayCrawler.lineTo(Squares.valueOf("a1"), Squares.valueOf("h8")))
//
//    }
    //    init {
//
//        repeat(20) {
//            val sliderIdx = Random.nextInt(mgSliders)
//            val piece = Piece.sliders[sliderIdx]
//
//            var blockerMask = BitBoards.EMPTY_BB
//            val square = Squares.random()
//            val blockers = mutableListOf<square>()
//            val validSquares = getSetBitIndices(sliderMasks[sliderIdx][square])
//
//            for (i in 0..Random.nextInt(1, validSquares.size)) {
//                var pick = validSquares.random()
//                while (pick in blockers) {
//                    pick = validSquares.random()
//                }
//                blockers.add(pick)
//            }
//
////            println("Slider $piece on ${Squares.asText(square)} move mask with empty board:")
////            BitBoards.print(sliderMasks[sliderIdx][square])
//            val text = StringBuilder()
//            for (blocker in blockers) {
//                blockerMask = BitBoards.addBit(blockerMask, blocker)
//                text.append(Squares.asText(blocker) + ", ")
//            }
//
//            println("Slider $piece on ${Squares.asText(square)} move mask with blockers on $text:")
//            BitBoards.print(
//                sliderAttackMap[sliderIdx][square][indexFromOccupancy(sliderIdx, square, sliderMasks[sliderIdx][square] and blockerMask)],
//                blockerMask
//            )
//        }
//    }
    /*
    Capabilities:
        + generate Rook moves with blockers
        + generate Bishop moves with blockers
        + generate Queen moves with blockers

        + generate Knight moves
        + generate King moves

        + currently masks out friendly pieces

        + generate Pawn double jumps
        + generate enpassant (maybe use boards tracking of the enpassant square, and if a pawn is diag, opp color, etc)
        + generate pawn captures

        + restrict pieces to a pin
        + allow castling if path to king and rook is empty + king doesn't cross through enemy attack
        + calculate this kings moves by finding all squares where enemies capture themselves + ignore this king
        x+disallow moves if they dont block an ongoing check ray
     */

    // change to a move buffer array later

    private fun observeBoard() {
        val king = Piece.from(Type.KING, genColor)
        val kingBB = board.fetchPieceBitBoard(king) // just count the 0 bits instead of this
        require(kingBB != 0uL) { "$king doesn't exist on the board!" }
        kingPosition = kingBB.countTrailingZeroBits()
        friendlyOccupancy = board.getOccupancy(genColor)
        enemyOccupancy = board.getOccupancy(genColor.enemy)
        totalOccupancy = enemyOccupancy or friendlyOccupancy
        emptySquares = totalOccupancy.inv()
        enpassant = board.fetchEnpassantSquare()
        enpassantBB = if (enpassant == null) 0uL else 1uL shl enpassant!!
    }

    private fun observeKingSafety() {
        enemyPositions = getSetBitIndices(enemyOccupancy)
        enemyAttackMask = 0uL
        safeSquares = 0uL
        kingAttackers.clear()
        check = false
        for (enemyPosition in enemyPositions) {
            val piece = board.fetchPiece(enemyPosition) // loop may cause weirdness with double checks
            val scope = getPseudoLegalAttackScope(piece, enemyPosition)
            if (scope and (1uL shl kingPosition) != 0uL) {
                kingAttackers.add(enemyPosition)
            }
            enemyAttackMask = enemyAttackMask or scope
        }
        safeSquares = enemyAttackMask.inv()
        check = enemyAttackMask and (1uL shl kingPosition) != 0uL // check if being attacked
    }

    private fun genMoves(board: ChessBoard, color: Color) {
        this.board = board
        this.genColor = color
        this.pointer = 0 // act as a clear
        observeBoard() // occupancy, enpassant, king position
        observeKingSafety() // check, king attackers, enemy attacks, safe squares

        for (piece in Piece.playable) {
            if (piece.color != genColor) continue // per call for a color, 6 wasted steps (micro opt here)
            if (piece.isPawn()) {
                genPawns(piece)
                continue
            }

            val positions = getSetBitIndices(board.fetchPieceBitBoard(piece))

            for (position in positions) {
                generateMovesForPiece(position, piece)
            }
        }
    }

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

    private fun generateMovesForPiece(position: square, piece: Piece) {
        val moves = mutableListOf<move>()
        var attackMask = getPseudoLegalAttackScope(piece, position)

        if (piece.isKing()) {
            for (attackerPosition in kingAttackers) { // must account for xrays (probably over Engineered for this small case
                val attacker = board.fetchPiece(attackerPosition)
                enemyAttackMask = enemyAttackMask or when {
                    attacker.isRook() ->  xRayMasks[rookOrder][attackerPosition]
                    attacker.isBishop() -> xRayMasks[bishopOrder][attackerPosition]
                    attacker.isQueen() -> {
                        xRayMasks[rookOrder][attackerPosition] or xRayMasks[bishopOrder][attackerPosition]
                    }
                    else -> 0uL
                }
            }
            safeSquares = enemyAttackMask.inv()
            // 1111 // castle rights
            // 0,1 for white, 2, 3 for black
            val rights = board.getCastleRights().bits
            val KS = 1 shl 0 + (piece.color.value * 2) and rights // king side // index rights for legality
            val QS = 1 shl 1 + (piece.color.value * 2) and rights // queen side

            val rightOne = (1uL shl (kingPosition + 1)) and safeSquares and emptySquares// KS
            val rightTwo = rightOne shl 1 //and safeSquares

            val leftOne = (1uL shl (kingPosition - 1)) and safeSquares and emptySquares // QS
            val leftTwo = leftOne shr 1 //and safeSquares // we dont check safe squares here, filter at end instead

            if (KS != 0 && !check) attackMask = attackMask or rightTwo
            if (QS != 0 && !check) attackMask = attackMask or leftTwo

            attackMask = attackMask and safeSquares // king cannot move into danger
        } else {
            attackMask = accountedForKingSafety(piece, position, attackMask) // pieces must ensure king is safe
        }

        attackMask = attackMask and friendlyOccupancy.inv() // pieces can never hurt their team
        BitBoards.iterateBits(attackMask) { attack ->
            addMove(BetterMoves.encode(piece,position, attack))
        }
    }

    // filters out moves that put or leave the king in check
    private fun accountedForKingSafety(piece: Piece ,position: square, attackMask: BitBoard): BitBoard {
        var m = attackMask
        for (enemyPosition in enemyPositions) {
            ///////////
            val enemyAttacker = board.fetchPiece(enemyPosition)
            val enemyAttacks: BitBoard = getFullAttackScope(enemyAttacker, enemyPosition)

            if (enemyAttacker.isSlider()) {
                val pinMask = pinMasks[kingPosition][enemyPosition] // what are the possible pin squares?
                val pinRelevance = pinMask and enemyAttacks  // does the attacker see these squares?
                if (pinRelevance == 0uL) continue // this attacker doesnt see these squares
                val defendingSquares = pinMask and enemyAttacks or (1uL shl enemyPosition) // where can we go to defend?
                val itemsInPinRay = (pinMask and enemyAttacks and totalOccupancy).countOneBits() // how many pieces defend?
                val isOnlyDefender = itemsInPinRay == 1 && pinRelevance and (1uL shl position) != 0uL // are we the only defender?

                if (itemsInPinRay == 0 || isOnlyDefender) { // no defenders? -> check
                    m = defendingSquares and attackMask // we must defend where we can
                    break // we're already pinned, or forced to defend
                } else {
                    if (piece.isPawn()) { // done here to reduce most lookups
                       if (itemsInPinRay == 2 && abs(position - enemyPosition) < 8) m = m and enpassantBB.inv() // edge case enpassant reveals king
                        // <8 to ensure the block is active only for the horizontal case
                    }
                    // otherwise not pinned by this enemy piece
               }
            } else  { // all other pieces can't be blocked from attack, no need to check for enemy king attack
                if ((1uL shl kingPosition) and enemyAttacks != 0uL) { // our king is attacked
                    m = attackMask and (1uL shl enemyPosition)
                    if (piece.isPawn()) m = m or (attackMask and enpassantBB)
                    // edge case enpassant to remove pawn attacker
                   // we can only capture that piece if we even see it
                }
            }
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
        require(piece.isPawn()) { // can remove in future for speed (micro opt)
            "Cannot generate pawn moves for non pawn piece: $piece"
        }
        val enpassant = enpassant
        val pawnMask = board.fetchPieceBitBoard(piece)
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

        // push 1 - can promote
        BitBoards.iterateBits(onePush) { to -> // for each pawn
            safeAddMove(piece, to - oneStep, to, 1uL shl to,
                promotion = Squares.rankOf(to) == promotionRank)
        }
        // push 2 - will never promote
        BitBoards.iterateBits(twoPush) { to ->
            safeAddMove(piece, to - twoStep, to, 1uL shl to)

        }

        // capture enemies - can promote
        BitBoards.iterateBits(pawnMask)  { square ->
            BitBoards.iterateBits(pawnAttackMasks[piece.color.value][square] and enemyOccupancy) { attack ->
                safeAddMove( piece, square, attack,
                    1uL shl attack,
                    promotion = Squares.rankOf(attack) == promotionRank
                )
            }

            if (enpassant != null) {
                if (Squares.fileDist(square, enpassant) == 1 &&
                    Squares.isOnDiagonal(square, enpassant) &&
                    pawnDirection * Squares.rankOf(square) < Squares.rankOf(enpassant)) {
                    safeAddMove(piece, square, enpassant, 1uL shl enpassant)
                }
            }
        }
    }

    private fun addMove(move: move) {
        moveBuffer[pointer++] = move
    }

    private fun safeAddMove(
        piece: Piece,
        from: Int,
        to: Int,
        attackMask: BitBoard,
        promotion: Boolean = false
    ) {
        val safeMask = accountedForKingSafety(piece,from, attackMask)
        if ((safeMask and (1uL shl to)) != 0uL) {
            if (promotion) {
                addMove(BetterMoves.encode(piece, from, to, Type.ROOK))
                addMove(BetterMoves.encode(piece, from, to, Type.QUEEN))
                addMove(BetterMoves.encode(piece, from, to, Type.BISHOP))
                addMove(BetterMoves.encode(piece, from, to, Type.KNIGHT))
            } else {
                addMove(BetterMoves.encode(piece, from, to))
            }

        }

    }


}