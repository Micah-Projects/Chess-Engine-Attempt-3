import board.ChessBoard
import board.Board
import board.Piece
import board.Piece.*
import misc.Debug
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

            board.addPiece(p, s)
            assertTrue(board.fetchPiece(s) == p)
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s) != 0uL)
        }
    }

    @Test
    fun addPieceRemovesDifferentPieceFromSquare() {
        // Add a White Knight to a square
        board.addPiece(WHITE_KNIGHT, 0)
        assertTrue(board.fetchPiece(0) == WHITE_KNIGHT)
        assertTrue(board.fetchPieceBitBoard(WHITE_KNIGHT) and (1uL shl 0) != 0uL)

        // Add a black king onto the same square
        board.addPiece(BLACK_KING, 0)
        assertTrue(board.fetchPiece(0) == BLACK_KING)
        assertTrue(board.fetchPieceBitBoard(BLACK_KING) and (1uL shl 0) != 0uL)

        // Ensure there's no longer a knight on that same square
        assertTrue(board.fetchPieceBitBoard(WHITE_KNIGHT) and (1uL shl 0) == 0uL)
    }

    @Test
    fun addPieceThrowsForIllegalSquare() {
        assertThrows<IllegalArgumentException> { board.addPiece(WHITE_KNIGHT, 73) }
    }

    @Test
    fun removePieceRemovesPiece() {
        repeat(1000) {
            board = Board()
            val s = random.nextInt(64)
            val p = Piece.random()

            board.addPiece(p, s)
            assertTrue(board.fetchPiece(s) == p)
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s) != 0uL)
            board.removePiece(s)
            assertTrue(board.fetchPiece(s).isEmpty())
            assertTrue(board.fetchPieceBitBoard(p) and (1uL shl s) == 0uL)

        }

    }

    @Test
    fun removePieceThrowsForIllegalSquare() {
        assertThrows<IllegalArgumentException> { board.removePiece(-4) }
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
            board.addPiece(p, s1)
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
            board.addPiece(p1, s1)
            board.addPiece(p2, s2)
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
        board.addPiece(BLACK_KNIGHT, 32)
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
        board.addPiece(BLACK_PAWN, 1)
        assertThrows<IllegalArgumentException> { board.movePiece(1, 64) }
    }

    @Test
    fun fetchPieceReturnsCorrectPiece() {
        board.loadFen("8/8/8/8/8/8/8/R7 w - - 0 1") // White rook on a1
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
                    board.addPiece(p, s)
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

            clone.addPiece(p, s)
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

            board.addPiece(p, s1)
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
    fun doStuffForDebug() {

        repeat(5) { board.addPiece(Piece.random(), random.nextInt(64)) }
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
            board.addPiece(p1, s1)
            board.addPiece(p2, s2)
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