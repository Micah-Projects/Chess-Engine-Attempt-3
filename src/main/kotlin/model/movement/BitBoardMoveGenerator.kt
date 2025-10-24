package model.movement

import model.board.MutableChessBoard
import model.board.Color
import model.board.Piece
import model.misc.BitBoard
import model.misc.BitBoards
import model.misc.BitBoards.binaryFill
import model.misc.BitBoards.getSetBitIndices
import model.misc.Moves
import model.misc.Squares
import model.misc.move
import model.misc.square
import kotlin.random.Random
import kotlin.random.nextULong

class BitBoardMoveGenerator : MoveGenerator {
    private val mgSliders = 2

    private val kingMasks = Array<ULong>(Squares.COUNT) { square -> RayCrawler.leap(square, RayCrawler.queens) }
    private val knightMasks = Array<ULong>(Squares.COUNT) { square -> RayCrawler.leap(square, RayCrawler.knights) }

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
                    blockers, square, RayCrawler.getRays(piece), true
                )
            }
            attacks
        }

    }

    private val rankMask = Array<ULong>(Squares.NUM_RANKS) { rank ->
        val b = 1uL shl rank * 8
        RayCrawler.crawlRays(b.countTrailingZeroBits(), RayCrawler.horizontals)
    }
    private val fileMask = Array<ULong>(Squares.NUM_RANKS) { file ->
        val b = 1uL shl file
        RayCrawler.crawlRays(b.countTrailingZeroBits(), RayCrawler.verticals)
    }

    private val bishopOrder = 0
    private val rookOrder = 1

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
//
    private fun indexFromOccupancy(sliderIdx: Int, square: square, occupancy: ULong): Int {
        val shiftFactor = ULong.SIZE_BITS - sliderMasks[sliderIdx][square].countOneBits()
        val hashValue = ((sliderMagics[sliderIdx][square] * occupancy) shr shiftFactor).toUInt().toInt()
        return hashValue
    }

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

        x restrict pieces to a pin
        x allow castling if path to king and rook is empty + king doesn't cross through enemy attack
        x calculate this kings moves by finding all squares where enemies capture themselves + ignore this king
        x disallow moves if they dont block an ongoing check ray



     */

    // change to a move buffer array later
    override fun generateMoves(board: MutableChessBoard, color: Color): List<move> {
        // can move occupancy fetching here, 1 time call vs 6 time call (micro opt)
        val result = mutableListOf<move>()
        for (piece in Piece.playable) {
            if (piece.color != color) continue // per call for a color, 6 wasted steps (micro opt here)
            if (piece.isPawn()) {
               result.addAll(genPawns(piece, board))
                continue
            }
            val bb = board.fetchPieceBitBoard(piece)
            val positions = getSetBitIndices(bb)
            for (position in positions) {
                result.addAll(generateMovesForPiece(position, piece, board))
            }
        }
       return result
    }

    private fun generateMovesForPiece(square: square, piece: Piece, board: MutableChessBoard): List<move> {
        val moves = mutableListOf<move>()
        val color = piece.color
        val enemyOccupancy = board.getOccupancy(color.enemy)
        val friendlyOccupancy = board.getOccupancy(color)

        val totalOccupancy = enemyOccupancy or friendlyOccupancy
        val emptySquares = totalOccupancy.inv()

        var attackMask = when {
            piece.isBishop() -> getSliderAttackMask(bishopOrder, square, totalOccupancy)
            piece.isRook() -> getSliderAttackMask(rookOrder, square, totalOccupancy)
            piece.isQueen() -> {
                getSliderAttackMask(rookOrder, square, totalOccupancy) or
                getSliderAttackMask(bishopOrder, square, totalOccupancy)
            }
            piece.isKnight() -> knightMasks[square]
            piece.isKing() -> kingMasks[square]
            else -> BitBoards.EMPTY_BB
        }
        attackMask = attackMask and friendlyOccupancy.inv()
        BitBoards.iterateBits(attackMask) { attack ->
            moves.add(Moves.encode(square, attack))
        }
        return moves
    }

    private fun genPawns(piece: Piece, board: MutableChessBoard): List<move> {
        require(piece.isPawn()) { // can remove in future for speed (micro opt)
            "Cannot generate pawn moves for non pawn piece: $piece"
        }
        val moves = mutableListOf<move>()
        val emptySquares = board.fetchEmptySquares()
        val enemyOccupancy = board.getOccupancy(piece.color.enemy)
        val enpassant = board.fetchEnpassantSquare()
        val pawnMask = board.fetchPieceBitBoard(piece)
        val startRank = piece.color.pawnStartRank
        val pawnDirection = piece.color.pawnDirection
        val promotionRank = piece.color.promotionRank
        // one step is allowed if the concerned square is empty
        // two step is allowed if the pawn is on start rank and one step exists, and the destination is free
        val oneStep = 8 * pawnDirection // called once per generation (micro opt)
        val twoStep = 16 * pawnDirection
        val onePush: ULong
        val twoPush: ULong

        if (pawnDirection == 1) {
            onePush = (pawnMask shl 8) and emptySquares
            twoPush = ((pawnMask and rankMask[startRank]) shl 16) and emptySquares and (emptySquares shl 8)
        } else {
            onePush = (pawnMask shr 8) and emptySquares
            twoPush = ((pawnMask and rankMask[startRank]) shr 16) and emptySquares and (emptySquares shl 8)
        }

        // push 1 - can promote
        BitBoards.iterateBits(onePush) { to ->
            if (Squares.rankOf(to) == promotionRank) {
                getPromotions(to - oneStep, to)
            } else {
                moves.add(Moves.encode(to - oneStep, to))
            }
        }
        // push 2 - will never promote
        BitBoards.iterateBits(twoPush) { to ->
            moves.add(Moves.encode(to - twoStep, to))
        }
        // capture enemies - can promote
        BitBoards.iterateBits(pawnMask)  { square ->
            BitBoards.iterateBits(pawnAttackMasks[piece.color.value][square] and enemyOccupancy) { attack ->
                if (Squares.rankOf(attack) == promotionRank) {
                    moves.addAll(getPromotions(square, attack))
                } else {
                    moves.add(Moves.encode(square, attack))
                }
            }

            if (enpassant != null) {            // yes, that is a magic number. fight me
                if (Squares.fileDist(square, enpassant) == 1 && Squares.isOnDiagonal(square, enpassant)) {
                   moves.add(Moves.encode(square, enpassant))
                }
            }
        }

        return moves
    }

    private fun getPromotions(from: square, to: square, wasCapture: Boolean = false): List<move> {
        val moves = mutableListOf<move>()
        for (type in Piece.Type.promotions) {
            moves.add(Moves.encode(from, to, Moves.encodeFlags(
                promotion = true,
                capture = wasCapture,
                promotionType = type
            )))
        }
        return moves
    }

    private fun getSliderAttackMask(sliderOrder: Int, square: square, totalOccupancy: ULong): ULong {
        val index = indexFromOccupancy(sliderOrder, square, sliderMasks[sliderOrder][square] and totalOccupancy)
        return sliderAttackMap[sliderOrder][square][index]
    }

}