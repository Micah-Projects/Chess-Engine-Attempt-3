package model.movement

import model.board.Color

@JvmInline
value class CastleRights(val bits: Int) {
    fun canWhiteKingSide() = bits and 0b0001 != 0
    fun canWhiteQueenSide() = bits and 0b0010 != 0
    fun canBlackKingSide() = bits and 0b0100 != 0
    fun canBlackQueenSide() = bits and 0b1000 != 0

    fun without(flag: Int) = CastleRights(bits and flag.inv())

    companion object {
        val FULL = CastleRights(0b1111)
        const val WHITE_KS = 0b0001
        const val WHITE_QS = 0b0010
        const val BLACK_KS = 0b0100
        const val BLACK_QS = 0b1000

        const val WHITE = 0b0011
        const val BLACK = 0b1100

        fun from(color: Color): Int {
            return when (color) {
                Color.WHITE -> WHITE
                Color.BLACK -> BLACK
            }
        }
    }
}


