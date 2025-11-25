package model.misc
import model.board.Color
import model.movement.CastleRights
import kotlin.text.isNotEmpty
import kotlin.text.split

/**
 * A class which parses and represents chess FEN notation.
 * If information isn't correct, values will be absent or assumed to not be present.
 */
class FenString(val fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") {

    init {
        require(fen.isNotEmpty()) { "Fen string cannot be empty." }
        require(fen.countChar('/') == 7) { "board representation must be filled." }
    }

    private val fenComponents = fen.split(" ")


    val board: String = fenComponents[0]
    val turn: Color = if (fenComponents.size > 1 && fenComponents[1].length == 1)
    if (fenComponents[1][0]  == 'w') Color.WHITE else Color.BLACK else Color.WHITE

    private val castlingBlock: String get() {
        val b = fenComponents.getOrNull(2)?: ""
        return b.ifEmpty { "-" }
    }

    val whiteCanKingCastle: Boolean = castlingBlock.contains("K")
    val whiteCanQueenCastle: Boolean = castlingBlock.contains("Q")
    val blackCanKingCastle: Boolean =  castlingBlock.contains("k")
    val blackCanQueenCastle: Boolean = castlingBlock.contains("q")

    val castleRights = CastleRights(
        (if (whiteCanKingCastle) CastleRights.WHITE_KS else 0) or
                (if (whiteCanQueenCastle) CastleRights.WHITE_QS else 0) or
                (if (blackCanKingCastle) CastleRights.BLACK_KS else 0) or
                (if (blackCanQueenCastle) CastleRights.BLACK_QS else 0)
    )

    val enpassantSquare: Int? = (if (fenComponents.getOrNull(3) != null && fenComponents[3] != "-" ) Squares.valueOf(fenComponents[3]) else null) //.also { println(it) }

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