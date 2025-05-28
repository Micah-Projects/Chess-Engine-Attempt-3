package misc
import board.Color
import kotlin.text.isNotEmpty
import kotlin.text.split
import board.Color.*

/**
 * A class which parses and represents chess FEN notation.
 * If information isn't correct, values will be absent or assumed to not be present.
 */
class FenString(fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") {

    init {
        require(fen.isNotEmpty()) { "Fen string cannot be empty." }
        require(fen.countChar('/') == 7) { "board representation must be filled." }
    }

    private val fenComponents = fen.split(" ")


    val board: String = fenComponents[0]
    val turn: Color = if (fenComponents.size > 1 && fenComponents[1].length == 1)
    if (fenComponents[1][0]  == 'w') WHITE else BLACK else WHITE

    private val castlingBlock = fenComponents.getOrNull(2)?: ""

    val whiteCanKingCastle: Boolean = castlingBlock.getOrNull(0) == 'K'
    val whiteCanQueenCastle: Boolean = castlingBlock.getOrNull(1) == 'Q'
    val blackCanKingCastle: Boolean =  castlingBlock.getOrNull(2) == 'k'
    val blackCanQueenCastle: Boolean = castlingBlock.getOrNull(3) == 'q'

    val enpassantSquare: Int? =
        if (fenComponents.getOrNull(3) != null && fenComponents[3] != "-" ) Squares.valueOf(fenComponents[3]) else null

    val numHalfMoves: Int = (fenComponents.getOrNull(4)?.toInt() ?: 0)
    val numFullMoves: Int = (fenComponents.getOrNull(5)?.toInt() ?: 0)

    private fun String.countChar(char: Char): Int {
        var count = 0
        for (c in this) {
            if (c == char) count++
        }
        return count
    }
}