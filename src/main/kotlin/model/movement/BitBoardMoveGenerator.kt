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
    private var enemyPositions = listOf<Int>()
    private var enemyAttackMask = 0uL
    private var safeSquares = 0uL
    private var enpassant: Int? = null
    private var kingAttackers = mutableListOf<Int>()
    private var pieceSet: Array<Piece> = Piece.whitePieceSet
    private var kingBB: BitBoard = 0uL

    private var enpassantBB: BitBoard = 0uL

    private var check = false


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
        kingBB = board.fetchPieceBitBoard(king) // just count the 0 bits instead of this
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
        var attackMask = getPseudoLegalAttackScope(piece, position)

        if (piece.isKing()) {
            for (attackerPosition in kingAttackers) { // must account for xrays (probably over Engineered for this small case
                val attacker = board.fetchPiece(attackerPosition)
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
            attackMask = accountedForKingSafety(piece, position, attackMask) // pieces must ensure king is safe
        }

        attackMask = attackMask and friendlyOccupancy.inv() // pieces can never hurt their team
        BitBoards.iterateBits(attackMask) { attack ->
            addMove(BetterMoves.encode(piece,position, attack))
        }
    }

    // filters out moves that put or leave the king in check
    private fun accountedForKingSafety(friendlyPiece: Piece, position: square, attackMask: BitBoard): BitBoard {
        var m = attackMask
        for (enemyPosition in enemyPositions) {
            ///////////
            val enemyAttacker = board.fetchPiece(enemyPosition)
            val enemyAttacks: BitBoard = getFullAttackScope(enemyAttacker, enemyPosition)
            val kingAttackedByEnemy = (kingBB) and enemyAttacks != 0uL
            val enemyBB = (1uL shl enemyPosition)
            if (enemyAttacker.isSlider()) {
                val pinMask = pinMasks[kingPosition][enemyPosition] // what are the possible pin squares?
                val pinRelevance = pinMask and enemyAttacks  // does the attacker see these squares?
                if (pinMask == 0uL && !kingAttackedByEnemy) continue // if there isnt even a pin mask, just move onto the next

                if (pinRelevance == 0uL) { // if theres a pin mask, but the attacker doesnt see it,
//                    if( friendlyPiece.isQueen() && position == Squares.valueOf("f3")) {
//                      //  println("Queen here")
//                    }
                    if (kingAttackedByEnemy) {
                        m = (1uL shl enemyPosition) and attackMask // before, without the break, the queen's moves are being invalidated multiple times
                        //break
                    }
                    continue
                } // this attacker doesnt see these squares,
                val defendingSquares = (pinMask and enemyAttacks) or (enemyBB) // where can we go to defend?


                val itemsInPinRay = (pinMask and enemyAttacks and totalOccupancy).countOneBits()  // how many pieces defend?
               // if (friendlyPiece.isPawn() && position == Squares.valueOf("c7")) println("Items in pin ray: $itemsInPinRay")
                val isOnlyDefender = itemsInPinRay == 1 && pinRelevance and (1uL shl position) != 0uL // are we the only defender?
//
                if (itemsInPinRay == 0 || isOnlyDefender) { // no defenders? -> check
                        m = defendingSquares and m //) // we must defend where we can

                   // break // we're already pinned, or forced to defend
                } else { // youre not in the pin, and you cant move into the pin..
                  ///  if (check) m = attackMask and defendingSquares// .
                    if (friendlyPiece.isPawn()) { // done here to reduce most lookups
                       if (itemsInPinRay == 2 &&
                           Squares.isOnSameRank(kingPosition, position) &&
                           Squares.isOnSameRank(position, enemyPosition)) {
                           m = m and enpassantBB.inv() // edge case enpassant reveals king
                       }
                        // <8 to ensure the block is active only for the horizontal case
                    }
                }
            } else  { // all other pieces can't be blocked from attack, no need to check for enemy king attack
                if (kingAttackedByEnemy) { // our king is attacked
                    // if this isnt an enpassantable move
                    if (friendlyPiece.isPawn() && enemyAttacker.isPawn()) {
                        val pm = enpassantBB or enemyBB
                        m = m and pm
                        // else keep pawn moves until invalidated later in the loop
                    } else {
                        m = m and enemyBB
                    }

                   // if (friendlyPiece.isPawn()) m = m //or (attackMask and enpassantBB)
                    //break
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
        BitBoards.iterateBits(pawnMask)  { position ->
            BitBoards.iterateBits(pawnAttackMasks[piece.color.value][position] and enemyOccupancy) { attack ->
                safeAddMove( piece, position, attack,
                    1uL shl attack,
                    promotion = Squares.rankOf(attack) == promotionRank
                )
            }

            if (enpassant != null) {
                if (Squares.fileDist(position, enpassant) == 1 &&
                    Squares.isOnDiagonal(position, enpassant) &&
                    pawnDirection * Squares.rankOf(position) < pawnDirection * Squares.rankOf(enpassant)) {
                    safeAddMove(piece, position, enpassant, 1uL shl enpassant)
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