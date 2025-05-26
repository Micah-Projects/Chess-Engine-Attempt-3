package misc
import board.Color
import kotlin.collections.reversed
import kotlin.text.digitToInt
import kotlin.text.isDigit
import kotlin.text.isNotEmpty
import kotlin.text.padStart
import kotlin.text.split
import board.Color.*
import kotlin.jvm.Throws


class FenString(fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") {
    companion object {
        fun simplifyFenBoard(fenBoard: String): String {
            val fenBuilder = StringBuilder()
            val fenBoard = fenBoard.split("/").reversed()
            for (rank in 0..7) {
                val fenRank = fenBoard[rank]
                for (char in fenRank) {
                    if (char.isDigit()) {
                        fenBuilder.append("0".padStart(char.digitToInt(), '0'), )
                    } else {
                        fenBuilder.append(char)
                    }
                }
            }
            // println(fenRebuild)
            return fenBuilder.toString()

        }

        fun String.countChar(char: Char): Int {
            var count = 0
            for (c in this) {
                if (c == char) count++
            }
            return count
        }
    }

    //require(fen.isNotEmpty()) { "Fen string cannot be empty." }
    //require(fen.countChar('/') == 7) { "board representation must be filled." }

    val fenComponents = fen.split(" ")
    val castlingBlock = if (fenComponents.size > 2) fenComponents[2] else ""


    val board: String = simplifyFenBoard(fenComponents[0])
    val turn: Color = if (fenComponents.size > 1 && fenComponents[1].length == 1)
    if (fenComponents[1][0]  == 'w') WHITE else BLACK else WHITE

    val whiteCanKingCastle: Boolean = if (castlingBlock.isNotEmpty())  castlingBlock[0] == 'K' else true
    val whiteCanQueenCastle: Boolean = if (castlingBlock.length > 1)  castlingBlock[1] == 'Q' else true
    val blackCanKingCastle: Boolean = if (castlingBlock.length > 2)  castlingBlock[2] == 'k' else true
    val blackCanQueenCastle: Boolean = if (castlingBlock.length > 3)  castlingBlock[3] == 'q' else true

    val enpassantSquare: Int? = if (fenComponents.size > 3 && fenComponents[3][0] != '-') {
        Squares.valueOf(fenComponents[3])
    } else null

    val numHalfMoves: Int = fenComponents[4].toInt()
    val numFullMoves: Int = fenComponents[5].toInt()


}