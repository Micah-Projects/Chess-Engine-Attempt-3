package model.misc

import model.board.Piece

typealias move = Int

private const val START_SQUARE_BIT = 0
private const val END_SQUARE_BIT = 6
private const val FLAGS_BIT = 12

private const val START_SQUARE_SELECTOR =   0b000000000000000111111
private const val END_SQUARE_SELECTOR =     0b000000000111111000000
private const val FLAGS_SELECTOR =          0b111111111000000000000

// 000000 000000 000000

private const val CAPTURE_BIT = 0
private const val CASTLE_BIT = 1
private const val PROMOTION_BIT = 2
private const val ENPASSANT_BIT = 3
private const val CHECK_BIT = 4
private const val PROMOTION_TYPE_BIT = 5

private const val CAPTURE_FLAG =    1 shl CAPTURE_BIT               // 0b000000001
private const val CASTLE_FLAG =     1 shl CASTLE_BIT                // 0b000000010
private const val PROMOTION_FLAG =  1 shl PROMOTION_BIT             // 0b000000100
private const val ENPASSANT_FLAG =  1 shl ENPASSANT_BIT             // 0b000001000
private const val CHECK_FLAG =      1 shl CHECK_BIT                 // 0b000010000
private const val PROMOTION_TYPE_FLAG = 15 shl PROMOTION_TYPE_BIT   // 0b111100000

object Moves {
    fun encode(startSquare: Int, endSquare: Int, flags: Int = 0): move {
        return (startSquare shl START_SQUARE_BIT) or (endSquare shl END_SQUARE_BIT) or (flags shl FLAGS_BIT)
    }

    fun encodeFlags(
        capture: Boolean = false,
        castle: Boolean = false,
        promotion: Boolean = false,
        enpassant: Boolean = false,
        check: Boolean  = false,
        promotionType: Piece = Piece.EMPTY
    ): Int {
        return ((if (capture) 1 else 0) shl CAPTURE_BIT) or
                ((if (castle) 1 else 0) shl CASTLE_BIT) or
                ((if (promotion) 1 else 0) shl PROMOTION_BIT) or
                ((if (enpassant) 1 else 0) shl ENPASSANT_BIT) or
                ((if (check) 1 else 0) shl CHECK_BIT) or
                ((if (promotionType != Piece.EMPTY) promotionType else Piece.EMPTY).value shl PROMOTION_TYPE_BIT)
    }

    fun addFlags(move: move, flags: Int = 0): move = (move) or  (flags shl FLAGS_BIT)
    fun addPromotionType(flags: Int, type: Int): Int = flags or (type shl PROMOTION_TYPE_BIT)
    fun addIfCheck(flags: Int, isCheck: Boolean): Int = flags or ( (if (isCheck) 1 else 0) shl CHECK_BIT)
    fun getPromotionType(flags: Int): Int = (flags and START_SQUARE_SELECTOR) shr START_SQUARE_BIT
}

fun move.start(): Int = (this and START_SQUARE_SELECTOR) shr START_SQUARE_BIT
fun move.end(): Int = (this and END_SQUARE_SELECTOR) shr END_SQUARE_BIT
fun move.flags(): Int = (this and FLAGS_SELECTOR) shr FLAGS_BIT

fun move.getPromotion(): Piece = Piece.from ((flags() and PROMOTION_TYPE_FLAG) shr PROMOTION_TYPE_BIT)
fun move.getString(): String = getFlagNames().joinToString() + " From ${Squares.asText(start())} to ${ Squares.asText(end())}"
fun move.literal(): String = "${Squares.asText(start())}${Squares.asText(end())}"

fun move.isCapture(): Boolean = flags() and CAPTURE_FLAG != 0
fun move.isCastle(): Boolean = flags() and CASTLE_FLAG != 0
fun move.isPromotion(): Boolean = flags() and PROMOTION_FLAG != 0
fun move.isEnPassant(): Boolean  = flags() and ENPASSANT_FLAG != 0
fun move.isCheck(): Boolean  = flags() and CHECK_FLAG != 0
fun move.isQuiet(): Boolean = flags() and (CAPTURE_FLAG or PROMOTION_FLAG or CASTLE_FLAG or ENPASSANT_FLAG or CHECK_FLAG) == 0

fun move.getFlagNames(): List<String> {
    val names = mutableListOf<String>()
    if (isCapture()) names.add("Capture")
    if (isEnPassant()) names.add("En Passant")
    if (isCastle()) names.add("Castle")
    if (isCheck()) names.add("Check")
    if (isPromotion()) names.add("Promotion to ${getPromotion()}")
    if (names.isEmpty()) names.add("Quiet Move")

    return names
}