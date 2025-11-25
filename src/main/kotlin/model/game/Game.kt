package model.game

import model.board.Board
import model.board.ChessBoard
import model.board.MutableChessBoard
import model.board.Color
import model.misc.FenString
import model.misc.Squares
import model.misc.isCapture
import model.misc.literal
import model.misc.move
import model.misc.movingPiece
import model.movement.BitBoardMoveGenerator
import model.movement.CastleRights
import model.movement.MoveGenerator
import java.util.Stack

class Game : ChessGame {        // maybe make a new class called "CommandedGame" for cheats and such {
    private var board: MutableChessBoard
    private var validMoves: List<move> = listOf()
    private val mg: MoveGenerator
    private var started: Boolean = false
    override var status: GameStatus = GameStatus.UNSTARTED
    override var plies: Int
    override var repetitionCount: Int
    override var halfMoveClock: Int
    override var winner: Color? = null
    override var turn: Color
    private val history: Stack<UndoInfo> = Stack()

    private class UndoInfo(
        val board: MutableChessBoard,
        val started: Boolean,
        val turn: Color,
        val plies: Int,
        val repCount: Int,
        val hmClock: Int,
        val status: GameStatus,
        val winner: Color?,
        val moves: List<move>
    )


    constructor(mg: MoveGenerator = BitBoardMoveGenerator()) {
        board = Board()
        turn = Color.WHITE
        this.mg = mg
        started = false
        plies = 0
        repetitionCount = 0
        halfMoveClock = 0
    }

    private constructor(
        board: MutableChessBoard, started: Boolean, turn: Color, plies: Int, repCount: Int,
        hmClock: Int, status: GameStatus, winner: Color?,
    ) {
        this.board = board
        this.turn = turn
        this.halfMoveClock = hmClock
        this.plies = plies
        this.repetitionCount = repCount
        this.status = status
        this.winner = winner
        this.started = started
        this.mg = BitBoardMoveGenerator()
        validMoves = mg.generateMoves(this.board, turn)
    }

    override fun clone(): ChessGame {
        return Game(board.toMutable(), started, turn, plies, repetitionCount, halfMoveClock, status, winner)
    }

    override fun start(fen: FenString): ChessGame {
        if (started) return this
        status = GameStatus.ONGOING
        started = true
        board.loadFen(fen.fen)
        turn = fen.turn
        halfMoveClock = fen.numHalfMoves // calculate full moves based on floored half this
        validMoves = mg.generateMoves(board, turn)
        return this
    }

    override fun undoMove(): ChessGame {
        if (history.isEmpty()) return this
        val previous = history.pop()
        this.board = previous.board // assuming a copy was stored in the history and not a shared reference
        this.turn = previous.turn
        this.halfMoveClock = previous.hmClock
        this.plies = previous.plies
        this.repetitionCount = previous.repCount
        this.status = previous.status
        this.winner = previous.winner
        this.started = previous.started
        this.validMoves = previous.moves // beware of weird de-syncs

        return this
    }

    override fun isOver(): Boolean {
        return status.id > 0
    }



    override fun getMoves(color: Color): List<move> {
        if (turn != color) return listOf()
        return validMoves
    }

    override fun getBoard(): ChessBoard {
        return board // cannot be modified as a ChessBoard
    }

    override fun toFen(): String {
        // "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val rights = board.getCastleRights().bits
        val wk = if (CastleRights.WHITE_KS and rights != 0) "K" else ""
        val wq = if (CastleRights.WHITE_QS and rights != 0) "Q" else ""
        val bk = if (CastleRights.BLACK_KS and rights != 0) "k" else ""
        val bq = if (CastleRights.BLACK_QS and rights != 0) "q" else ""

        val castle = (wk + wq + bk + bq).ifEmpty { "-" }

        val moveNumber = plies / 2 + 1
        val ep = board.fetchEnpassantSquare()
        val enpassant = if (ep != null) Squares.asText(ep) else '-'
        return "${board.toFen()} ${turn.symbol} $castle $enpassant $halfMoveClock $moveNumber"
    }

    override fun playMove(move: move): ChessGame {
        require(status == GameStatus.ONGOING) {
            "Cannot play move. Game status is: $status. To avoid this issue, use isOver()"
        }
        if (move !in validMoves) throw IllegalArgumentException("Move ${move.literal()} is invalid.")
        history.push(UndoInfo(board.toMutable(), started, turn, plies, repetitionCount, halfMoveClock, status, winner, validMoves.toList()))
        board.makeMove(move) // if move is capture or moving piece is pawn -> reset hmClock
        plies++
        if (move.isCapture() || move.movingPiece().isPawn()) halfMoveClock =
            0 else halfMoveClock++ // issues with captures resetting clock
        // if the board position was seen before, our rep count is the number of times it was seen before
        nextTurn()
        val inCheck = mg.isKingInCheck()


        status = when {
            inCheck && validMoves.isEmpty() -> {
                winner = turn.enemy
                if (winner == Color.WHITE) GameStatus.WIN_WHITE_CHECKMATE else GameStatus.WIN_BLACK_CHECKMATE
            }

            !inCheck && validMoves.isEmpty() -> GameStatus.STALEMATE
            halfMoveClock == 50 -> GameStatus.DRAW_FIFTY_MOVES
            repetitionCount == 3 -> GameStatus.DRAW_REPETITION
            else -> GameStatus.ONGOING
        } // check for insufficient
        return this
        // here, do checks to make sure the game is still legal
    }

    private fun nextTurn() {
        turn = if (turn == Color.WHITE) Color.BLACK else Color.WHITE
        validMoves = mg.generateMoves(board, turn)
    }
}