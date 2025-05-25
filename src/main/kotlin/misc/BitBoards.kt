package misc

/**
 * A Type-alias for [ULong] which represents chess bit boards.
 */
typealias BitBoard = ULong

/**
 * A collection of useful methods for handling basic information of chess bit boards.
 */
object BitBoards {
    const val EMPTY_BB = 0uL
    fun removeBit(bb: BitBoard, bit: Int): BitBoard = bb and (1uL shl bit).inv()

    fun addBit(bb: BitBoard, where: Int): BitBoard = bb or (1uL shl where)

    fun moveBit(bb: BitBoard, from: Int, to: Int) = (bb and (1uL shl from).inv()) or (1uL shl to)
}