package model.misc

import model.board.Piece
import model.board.Piece.Type
typealias move = Int

private const val MOVING_PIECE_SHIFT = 0 // 4 bits
private const val FROM_SHIFT = MOVING_PIECE_SHIFT + 4  // 6 bits
private const val TO_SHIFT = FROM_SHIFT + 6 // 6 bits
private const val PROMOTION_SHIFT = TO_SHIFT + 6 // 3 bits
private const val CAPTURE_SHIFT = PROMOTION_SHIFT + 3 // 1 bit
//private const val CASTLE_SHIFT = CAPTURE_SHIFT + 1 // 1 bit
//private const val ENPASSANT_SHIFT = CASTLE_SHIFT + 1 // 1 bit


private const val MOVING_PIECE_SELECTOR = 0b1111 shl MOVING_PIECE_SHIFT
private const val FROM_SELECTOR = 0b111111 shl FROM_SHIFT
private const val TO_SELECTOR =  0b111111 shl TO_SHIFT // 0-64
private const val PROMOTION_SELECTOR = 0b111 shl PROMOTION_SHIFT
private const val CAPTURE_SELECTOR = 0b1 shl CAPTURE_SHIFT

object BetterMoves {                                    // promotion = pawn to remove boolean checking for NONE
    fun encode(movingPiece: Piece, from: square, to: square, promotion: Type = Type.PAWN, isCapture: Boolean = false): move {
        return  (movingPiece.value shl MOVING_PIECE_SHIFT) or
                (from shl FROM_SHIFT) or
                (to shl TO_SHIFT) or
                (promotion.value shl PROMOTION_SHIFT) or
                (if (isCapture) 1 else 0 shl CAPTURE_SHIFT)
    }
}

fun move.from(): Int = (this and FROM_SELECTOR) shr FROM_SHIFT
fun move.to(): Int = (this and TO_SELECTOR) shr TO_SHIFT
fun move.promotionType(): Piece.Type   {
    val type = ((this and PROMOTION_SELECTOR) shr PROMOTION_SHIFT)
    return if (type == 0) Piece.Type.NONE else Type.promotions[type - 1]
}
fun move.isCapture(): Boolean = (this and CAPTURE_SELECTOR) shr CAPTURE_SHIFT == 1
fun move.literal(): String = "${Squares.asText(from())}${Squares.asText(to())}${(if (promotionType() != Type.NONE) Piece.from(promotionType(), movingPiece().color).symbol else "") }"
fun move.movingPiece(): Piece = Piece.from((this and MOVING_PIECE_SELECTOR) shr MOVING_PIECE_SHIFT)



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