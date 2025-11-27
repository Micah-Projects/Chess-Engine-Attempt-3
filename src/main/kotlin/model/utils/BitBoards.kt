package model.utils

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

    fun hasBit(bb: BitBoard, where: Int): Boolean = (bb and (1uL shl where) != 0uL)

    fun getSetBitIndices(bb: ULong): List<Int> {
        var bb = bb
        var idx = 0
        val indices = mutableListOf<Int>()
        iterateBits(bb) {
            indices.add(it)
        }
        return indices
    }


    fun binaryFill(attackMap: ULong): Array<ULong> {
        val squares = mutableListOf<Int>()
        var map = attackMap
        var index = 0

        // collect indices of set bits
        while (map != 0uL) {
            if ((map and 1uL) != 0uL) {
                squares.add(index)
            }
            map = map shr 1
            index++
        }

        val results = mutableListOf<ULong>()
        val power = squares.size
        val combinations = 1 shl power

        for (mask in 0 until combinations) {
            var subset = 0uL
            for (i in 0 until power) {
                if ((mask and (1 shl i)) != 0) {
                    subset = subset or (1uL shl squares[i])
                }
            }
            results.add(subset)
        }

        return results.toTypedArray()
    }

    inline fun iterateBits(bb: BitBoard, action: (Int) -> Unit) {
        var b = bb
        while(b != EMPTY_BB) {
            val position = b.countTrailingZeroBits()
            action(position)
            b = b and (b - 1uL)
        }
    }

    fun print(bb: ULong, bb2: ULong = EMPTY_BB) {
        for (rank in 7 downTo 0) {
            for (file in 0..7) {
                val square = rank * 8 + file
                val bit = (bb shr square) and 1uL
                val xBit = (bb2 shr square) and 1uL
                val pixel = when {
                    (bit == 1uL) && (xBit == 1uL) -> "x "
                    (bit == 1uL) ->  "1 "
                    (xBit == 1uL) -> "! "
                    else -> ". "
                }
                print(pixel)
            }
            println("  ${rank + 1}")
        }
        println("a b c d e f g h\n")
    }
}