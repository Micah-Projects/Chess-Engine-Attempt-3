package model.movement

import DEBUG
import model.board.Piece
import model.board.Piece.Type
import model.game.ReadOnlyChessGame
import model.utils.Squares
import model.utils.square

typealias Move = Int

private const val MOVING_PIECE_SHIFT = 0 // 4 bits // 2^4 = 16, so 12 / 16 possible pieces represented
private const val TARGET_PIECE_SHIFT = MOVING_PIECE_SHIFT + 4 // 4 bits
private const val FROM_SHIFT = TARGET_PIECE_SHIFT + 4  // 6 bits
private const val TO_SHIFT = FROM_SHIFT + 6 // 6 bits
private const val PROMOTION_TYPE_SHIFT = TO_SHIFT + 6 // 3 bits
private const val CAPTURE_SHIFT = PROMOTION_TYPE_SHIFT + 3 // 1 bit
// capture  can be found by utilizing target piece

// special flags -- none can happen at the same time
// none = 000
// promotion = 001
// enpassant = 010
// short castle = 011
// long castle = 100
// pawn push = 101
// db pawn push = 110
//


private const val SPECIAL_SHIFT = PROMOTION_TYPE_SHIFT + 3 // 3 bits


private const val MOVING_PIECE_SELECTOR = 0b1111 shl MOVING_PIECE_SHIFT
private const val TARGET_PIECE_SELECTOR = 0b1111 shl TARGET_PIECE_SHIFT // 4 bits
private const val FROM_SELECTOR = 0b111111 shl FROM_SHIFT
private const val TO_SELECTOR =  0b111111 shl TO_SHIFT // 0-64
private const val PROMOTION_TYPE_SELECTOR = 0b111 shl PROMOTION_TYPE_SHIFT
private const val SPECIAL_SELECTOR = 0b111 shl SPECIAL_SHIFT

// engineering solution to my -1 <-> 11 mapping of all 13 piece types
private val pieceEncodings = IntArray(Piece.COUNT) { it + 1 }
private val pieceDecodings = IntArray(Piece.COUNT) { it - 1 }

object Moves {                                    // promotion = pawn to remove boolean checking for NONE


    fun encode(movingPiece: Piece, from: square, to: square, promotion: Type = Type.PAWN, isCapture: Boolean): Move {
        return  (movingPiece.id shl MOVING_PIECE_SHIFT) or
                (from shl FROM_SHIFT) or
                (to shl TO_SHIFT) or
                (promotion.value shl PROMOTION_TYPE_SHIFT)
    }

    // slower, but convenient
    fun autoEncode(from: square, to: square, promotion: Type = Type.PAWN, game: ReadOnlyChessGame): Move {
        val board = game.getBoard()
        val movingPiece = board.fetchPiece(from)
        val targetCandidate = board.fetchPiece(to)

        val targetPieceID = if (targetCandidate.id == -1) 0 else pieceEncodings[targetCandidate.id]
        if (DEBUG) if (movingPiece.isEmpty()) throw IllegalArgumentException("Moving piece cant be empty")

        return  (movingPiece.id shl MOVING_PIECE_SHIFT) or
                (targetPieceID shl TARGET_PIECE_SELECTOR) or
                (from shl FROM_SHIFT) or
                (to shl TO_SHIFT) or
                (promotion.value shl PROMOTION_TYPE_SHIFT)
    }
}

fun Move.movingPiece(): Piece = Piece.from((this and MOVING_PIECE_SELECTOR) shr MOVING_PIECE_SHIFT)
fun Move.targetPiece(): Piece = Piece.from(pieceDecodings[(this and TARGET_PIECE_SELECTOR) shr TARGET_PIECE_SHIFT])

fun Move.from(): Int = (this and FROM_SELECTOR) shr FROM_SHIFT
fun Move.to(): Int = (this and TO_SELECTOR) shr TO_SHIFT

fun Move.promotionType(): Type   {
    val type = ((this and PROMOTION_TYPE_SELECTOR) shr PROMOTION_TYPE_SHIFT)
    return if (type == 0) Type.NONE else Type.promotions[type - 1]
}
fun Move.isCapture(): Boolean = (this and TARGET_PIECE_SELECTOR) shr TARGET_PIECE_SHIFT != 0


// maybe remove these flags...
fun Move.isPromotion(): Boolean = (this and SPECIAL_SELECTOR) shr SPECIAL_SHIFT == 1
fun Move.isEnpassant(): Boolean = (this and SPECIAL_SELECTOR) shr SPECIAL_SHIFT == 2
fun Move.isShortCastle(): Boolean = (this and SPECIAL_SELECTOR) shr SPECIAL_SHIFT == 3
fun Move.isLongCastle(): Boolean = (this and SPECIAL_SELECTOR) shr SPECIAL_SHIFT == 4
fun Move.isSinglePawnPush(): Boolean = (this and SPECIAL_SELECTOR) shr SPECIAL_SHIFT == 5
fun Move.isDoublePawnPush(): Boolean = (this and SPECIAL_SELECTOR) shr SPECIAL_SHIFT == 6

fun Move.literal(): String = "${Squares.asText(from())}${Squares.asText(to())}${(if (promotionType() != Type.NONE) Piece.from(promotionType(), movingPiece().color).symbol else "") }"


//
//fun move.flags(): Int = (this and FLAGS_SELECTOR) shr FLAGS_BIT
//
//fun move.getPromotion(): Piece.Type = Piece.Type.playable[((flags() and PROMOTION_TYPE_FLAG) shr PROMOTION_TYPE_BIT)]
//fun move.getString(): String = getFlagNames().joinToString() + " From ${Squares.asText(start())} to ${ Squares.asText(end())}"
//fun move.literal(): String = "${Squares.asText(start())}${Squares.asText(end())}"
//
//fun move.isCapture(): Boolean = flags() and CAPTURE_FLAG != 0
//fun move.isCastle(): Boolean = flags() and CASTLE_FLAG != 0
//fun move.isPromotion(): Boolean = flags() and PROMOTION_FLAG != 0
//fun move.isEnPassant(): Boolean  = flags() and ENPASSANT_FLAG != 0
//fun move.isCheck(): Boolean  = flags() and CHECK_FLAG != 0
//fun move.isQuiet(): Boolean = flags() and (CAPTURE_FLAG or PROMOTION_FLAG or CASTLE_FLAG or ENPASSANT_FLAG or CHECK_FLAG) == 0