package misc
import board.Color
import kotlin.collections.reversed
import kotlin.text.digitToInt
import kotlin.text.isDigit
import kotlin.text.isNotEmpty
import kotlin.text.padStart
import kotlin.text.split
import board.Color.*

class FenString {
    val board: String
    val turn: Color
    val whiteCanKingCastle: Boolean
    val whiteCanQueenCastle: Boolean
    val blackCanKingCastle: Boolean
    val blackCanQueenCastle: Boolean

    val enpassantSquare: Int?
    val numHalfMoves: Int
    val numFullMoves: Int

    // rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
    constructor(fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") {
        require(fen.isNotEmpty()) { "Fen string cannot be empty." }
        require(fen.countChar('/') == 7) { "board representation must be filled." }

        val fenComponents = fen.split(" ")

        board = simplifyFenBoard(fenComponents[0])
        turn = if (fenComponents.size > 1 && fenComponents[1].length == 1)
            if (fenComponents[1][0]  == 'w') WHITE else BLACK else WHITE

        val castlingBlock = if (fenComponents.size > 2) fenComponents[2] else ""

        whiteCanKingCastle = if (castlingBlock.isNotEmpty())  castlingBlock[0] == 'K' else true
        whiteCanQueenCastle = if (castlingBlock.length > 1)  castlingBlock[1] == 'Q' else true
        blackCanKingCastle = if (castlingBlock.length > 2)  castlingBlock[2] == 'k' else true
        blackCanQueenCastle = if (castlingBlock.length > 3)  castlingBlock[3] == 'q' else true

        enpassantSquare = if (fenComponents.size > 3 && fenComponents[3][0] != '-') {
            Squares.valueOf(fenComponents[3])
        } else null

        // TODO: Implement these
        numHalfMoves = fenComponents[4].toInt()
        numFullMoves = 0
    }


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