import command.cheats.Move
import model.board.Color
import model.game.ChessGame
import model.game.Game
import model.misc.BitBoards
import model.misc.FenString
import model.misc.Squares
import model.misc.square
import model.movement.BitBoardMoveGenerator
import model.movement.MoveGenerator
import kotlin.test.Test
import kotlin.test.assertEquals

class BitBoardMoveGeneratorTest {
    private var mg: MoveGenerator = BitBoardMoveGenerator()

    fun perft(depth: Int, position: FenString = FenString(), turn: Color = Color.WHITE): Int {
        var count = 0
        if (depth == 0) {
            return 1
        }

        val game: ChessGame = Game(position, turn)
        game.start(position)
        val moves = game.getMoves(turn)

        for (move in moves) {
            val next = game.clone()
            next.playMove(move)
            count += perft(depth - 1, FenString(next.toFen()),next.currentTurn())
        }
        return count
    }

    fun benchGenSpeed(timeLimit: Long = 1_000_000_000L, position: FenString = FenString(), turn: Color = Color.WHITE) {
        val game = Game(position, turn)
        val oneSecond = 1_000_000_000L
        game.start()
        var count = 0
                var now = 0L
                var i = 0L
                val limit = timeLimit
                while (now < limit) {
                    val start = System.nanoTime()
                   // i++
                    mg.generateMoves(game.getBoard(), Color.BLACK)
                    val end = System.nanoTime()
                    val time = end - start
                    count++
                    now += time
                }

              //  println("incs: $i")

        println("moves generated: $count times in ${timeLimit/oneSecond.toDouble()} second(s) with ${String.format("%.6f", limit / count.toDouble() )}ns per call ")
    }

    @Test
    fun testPerft1() {
        val nodes = perft(1)
        assertEquals(20, nodes)
    }

    @Test
    fun testPerft5() {
        for (i in 0..10) {
            println("nodes at depth: $i: ${perft(i)}")
        }
    }

    @Test
    fun testGenSpeed1Second() {
        benchGenSpeed()
    }

}