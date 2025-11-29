import model.board.ChessBoard
import model.board.Board
import model.board.Piece
import model.board.Piece.*
import model.utils.Debug
import org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows

import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * A class containing tests for [Board].
 */
class BoardTest {
    lateinit var board: ChessBoard
    lateinit var random: Random

    @BeforeEach
    fun setup() {
        board = Board()
        random = Random(1)
    }

    @Test
    fun addPieceAddsPiece() {
        repeat(1000) {
            board = Board()
            val s = random.nextInt(64)
            val p = Piece.random()

            board.placePiece(p, s)
            assertTrue(board.fetchPiece(s) == p)
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s) != 0uL)
        }
    }

    @Test
    fun addPieceRemovesDifferentPieceFromSquare() {
        // Add a White Knight to a square
        board.placePiece(WHITE_KNIGHT, 0)
        assertTrue(board.fetchPiece(0) == WHITE_KNIGHT)
        assertTrue(board.fetchPieceBitBoard(WHITE_KNIGHT) and (1uL shl 0) != 0uL)

        // Add a black king onto the same square
        board.placePiece(BLACK_KING, 0)
        assertTrue(board.fetchPiece(0) == BLACK_KING)
        assertTrue(board.fetchPieceBitBoard(BLACK_KING) and (1uL shl 0) != 0uL)

        // Ensure there's no longer a knight on that same square
        assertTrue(board.fetchPieceBitBoard(WHITE_KNIGHT) and (1uL shl 0) == 0uL)
    }

    @Test
    fun addPieceThrowsForIllegalSquare() {
        assertThrows<IllegalArgumentException> { board.placePiece(WHITE_KNIGHT, 73) }
    }

    @Test
    fun removePieceRemovesPiece() {
        repeat(1000) {
            board = Board()
            val s = random.nextInt(64)
            val p = Piece.random()

            board.placePiece(p, s)
            assertTrue(board.fetchPiece(s) == p)
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s) != 0uL)
            board.clearPiece(s)
            assertTrue(board.fetchPiece(s).isEmpty())
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s) == 0uL)

        }

    }

    @Test
    fun removePieceThrowsForIllegalSquare() {
        assertThrows<IllegalArgumentException> { board.clearPiece(-4) }
    }

    @Test
    fun movePieceMovesPiece() {
        repeat(1000) {
            board = Board()
            var s1 = 0
            var s2 = 0

            // Get two random non-identical squares and a random Piece.
            while (s1 == s2) {
                s1 = Random.nextInt(64)
                s2 = Random.nextInt(64)
            }
            val p = Piece.random()

            // Add the random piece onto random square 1
            board.placePiece(p, s1)
            assertTrue(board.fetchPiece(s1) == p)
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s1) != 0uL)

            // Move the random piece to random square 2
            board.movePiece(s1, s2)

            // Ensure that the piece is now on random square 2
            assertTrue(board.fetchPiece(s2) == p)
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s2) != 0uL)

            // Ensure that random square 1 is Empty
            assertTrue(board.fetchPiece(s1).isEmpty())
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s1) == 0uL)

        }
    }

    @Test
    fun movePieceRemovesOtherPieceOnSquare() {
        repeat(1000) {
            board = Board()
            var s1 = 0
            var s2 = 0

            // Get two random non-identical squares and two random Pieces.
            while (s1 == s2) {
                s1 = Random.nextInt(64)
                s2 = Random.nextInt(64)
            }
            val p1 = Piece.random()
            val p2 = Piece.random()

            // Add the random pieces onto their respective random squares
            board.placePiece(p1, s1)
            board.placePiece(p2, s2)
            assertTrue(board.fetchPiece(s1) == p1)
            assertTrue(board.fetchPieceBitBoard(p1) and (1uL shl s1) != 0uL)
            assertTrue(board.fetchPiece(s2) == p2)
            assertTrue(board.fetchPieceBitBoard(p2) and (1uL shl s2) != 0uL)

            // Move random piece 1 to random square 2
            board.movePiece(s1, s2)

            // Ensure that random piece 1 is now on random square 2
            assertTrue(board.fetchPiece(s2) == p1)
            assertTrue(board.fetchPieceBitBoard(p1) and (1uL shl s2) != 0uL)

            // Ensure that random square 1 is Empty
            assertTrue(board.fetchPiece(s1).isEmpty())
            assertTrue(board.fetchPieceBitBoard(p1) and (1uL shl s1) == 0uL)

            // Ensure that random piece 2 is no longer on random square 2

            assertTrue(
                if (p1 == p2) (board.fetchPieceBitBoard(p2).countOneBits() == 1) else
                        (board.fetchPieceBitBoard(p2) and (1uL shl s2) == 0uL))
            { "p1: $p1, p2: $p2" }

        }
    }

    @Test
    fun movePieceThrowsForNullMove() {
        board.placePiece(BLACK_KNIGHT, 32)
        assertThrows<IllegalArgumentException> { board.movePiece(32, 32) }
    }

    @Test
    fun movePieceThrowsForEmptySquareMove() {
        assertTrue(board.fetchPiece(4) == EMPTY)
        assertThrows<IllegalArgumentException> { board.movePiece(4, 57) }
    }

    @Test
    fun movePieceThrowsForIllegalStartSquare() {
        assertThrows<IllegalArgumentException> { board.movePiece(-2, 5) }
    }

    @Test
    fun movePieceThrowsForIllegalEndSquare() {
        board.placePiece(BLACK_PAWN, 1)
        assertThrows<IllegalArgumentException> { board.movePiece(1, 64) }
    }

    @Test
    fun fetchPieceReturnsCorrectPiece() {
        board.loadFen("8/8/8/8/8/8/8/R7 w - - 0 1") // White rook on a1
        println(board.textVisual())
        assertTrue(board.fetchPiece(0) == WHITE_ROOK)
    }

    @Test
    fun fetchPieceBitBoardReturnsCorrectly() {
        repeat(1000) {
            board = Board()
            val bb = random.nextULong()
            val p = Piece.random()
            for (s in 0.until(64)) {
                if ((1uL shl s) and bb != 0uL ) {
                    board.placePiece(p, s)
                }
            }
            assertEquals(bb, board.fetchPieceBitBoard(p) )
        }
    }

    @Test
    fun cloneAddPieceDoesNotAffectOriginalState() {
        repeat(1000) {
            val p = Piece.random()
            val s = random.nextInt(64)
            val clone = board.clone()

            clone.placePiece(p, s)
            assertNotEquals(board.fetchPieceBitBoard(p), clone.fetchPieceBitBoard(p))
        }

    }

    @Test
    fun cloneMovePieceDoesNotAffectOriginalState() {
        repeat(1000) {
            board = Board()
            val p = Piece.random()
            var s1 = 0
            var s2 = 0
            while (s1 == s2) {
                s1 = random.nextInt(64)
                s2 = random.nextInt(64)
            }

            board.placePiece(p, s1)
            val clone = board.clone()
            clone.movePiece(s1, s2)


            assertNotEquals(board.fetchPieceBitBoard(p), clone.fetchPieceBitBoard(p))
        }
    }

    @Test
    fun cloneIsNotExactSame() {
        val clone = board.clone()
        assertFalse(clone === board)
    }

    @Test
    fun toFenReturnsCorrectFen() {
        val fens = listOf<String>(
            "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
            "r3k2r/pp1b1ppp/2n1pn2/2bp4/4P3/2N1BN2/PPP2PPP/R2QKB1R w KQkq - 6 8",
            "2rq1rk1/pp2bppp/2np1n2/4p3/2B1P3/2NP1N1P/PPP2PP1/R1BQ1RK1 b - - 5 10",
            "r1bq1rk1/ppp1bppp/2n2n2/3p4/3P4/2NBPN2/PP3PPP/R1BQ1RK1 b - - 7 7",
            "3r1rk1/1pp1qppp/p1npbn2/4p3/2P1P3/1PN1BN1P/PB3PP1/2RQ1RK1 w - - 10 12"
        )

        for (fen in fens) {
            board.loadFen(fen)
            assertEquals(fen.split(" ")[0], board.toFen())
        }
    }


    @Test
    fun doStuffForDebug() {

        repeat(5) { board.placePiece(Piece.random(), random.nextInt(64)) }
        repeat(5) {
            board = Board()
            var s1 = 0
            var s2 = 0

            // Get two random non-identical squares and two random Pieces.
            while (s1 == s2) {
                s1 = Random.nextInt(64)
                s2 = Random.nextInt(64)
            }
            val p1 = Piece.random()
            val p2 = Piece.random()

            // Add the random pieces onto their respective random squares
            board.placePiece(p1, s1)
            board.placePiece(p2, s2)
            assertTrue(board.fetchPiece(s1) == p1)
            assertTrue(board.fetchPieceBitBoard(p1) and (1uL shl s1) != 0uL)
            assertTrue(board.fetchPiece(s2) == p2)
            assertTrue(board.fetchPieceBitBoard(p2) and (1uL shl s2) != 0uL)

            // Move random piece 1 to random square 2
            board.movePiece(s1, s2)

            // Ensure that random piece 1 is now on random square 2
            assertTrue(board.fetchPiece(s2) == p1)
            assertTrue(board.fetchPieceBitBoard(p1) and (1uL shl s2) != 0uL)

            // Ensure that random square 1 is Empty
            assertTrue(board.fetchPiece(s1).isEmpty())
            assertTrue(board.fetchPieceBitBoard(p1) and (1uL shl s1) == 0uL)

            // Ensure that random piece 2 is no longer on random square 2

            assertTrue(
                if (p1 == p2) (board.fetchPieceBitBoard(p2).countOneBits() == 1) else
                    (board.fetchPieceBitBoard(p2) and (1uL shl s2) == 0uL))
            { "p1: $p1, p2: $p2" }

        }
        println(board.textVisual())
        println(Debug.getLogs())
    }



}