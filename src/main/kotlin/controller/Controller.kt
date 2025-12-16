package controller

import command.Command
import command.MakeMove
import command.PrintBoard
import kotlinx.coroutines.*
import kotlinx.coroutines.coroutineScope
import model.board.Color
import model.board.Piece
import model.game.ChessGame
import model.game.Game
import model.matches.MatchMaker
import model.movement.Moves
import model.utils.FenString
import model.utils.Squares
import model.movement.from
import model.movement.literal
import model.utils.square
import model.movement.to
import model.movement.BitBoardMoveGenerator
import model.movement.MoveGenerator
import view.GuiGame
import view.GuiMenu
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.system.exitProcess

fun main() = Controller.go(gui = true)

object Controller : Features {
    var game: ChessGame = Game()
    var promotion: Piece.Type? = null
    private var tempFrom: Int = -1
    private var tempTo: Int = -1
    private var events = ConcurrentLinkedQueue<(Controller.() -> Unit)>()
    private var commandQueue = ConcurrentLinkedQueue<Command>()

    // - - - - - - - - - - - - - - - functions to be called from other threads

    //** called from the gui thread
    fun callBack(callBack: (Controller.() -> Unit)) {
        events.add(callBack)
    }

    override fun printBoard() {
        commandQueue.add(PrintBoard(game.getBoard()))
    }

    fun startGameWith(fen: FenString) {
        events.add {
            game = Game()
            GuiGame.currentState = GuiGame.States.BOARD_STATE
            GuiGame.viewGame(game)
            game.start(fen)
        }
    }

    override fun startNewGame() {
        events.add {
            game = Game()
            GuiGame.currentState = GuiGame.States.BOARD_STATE
            GuiGame.viewGame(game)
            game.start()
        }
    }

    fun shutDown() {
        exitProcess(0)
    }

    override fun tryMove(from: square, to: square) {
        events.add {
            if (validMoveCriteria(from, to)) {
                makeMove(from, to)
            }
            GuiGame.clickedSquare = -1
        }
    }

    // gui stuff - - - -
    fun updateHighlights(square: square) {
        events.add {
            GuiGame.moveHighlights = game.getMoves(game.turn).filter { it.from() == square }.map { it.to()}.toSet()
        }
    }

    fun clearHighlights() {
        events.add {
            GuiGame.moveHighlights = setOf()
        }
    }

    // for move calls coming from gui / user
    private fun makeMove(from: square, to: square) {
        val p = game.getBoard().fetchPiece(from)
        val p2 = game.getBoard().fetchPiece(to)
        var moveAllowed = true
        if (!game.getMoves(p.color).any { it.from() == from && it.to() == to }) { /// maybe move this somewhere else...
            return
        }

        if (p.isPawn() && Squares.rankOf(to) == p.color.promotionRank) {
            if (promotion == null) {
                GuiGame.promptForPromotion(p.color)
                tempFrom = from
                tempTo = to
                moveAllowed = false
            }
        }

        if (moveAllowed) {
            val pro = promotion ?: Piece.Type.PAWN
            commandQueue.add(MakeMove(Moves.encode(p, from, to, pro, game.getBoard().fetchPiece(to).isNotEmpty()), game))
            promotion = null
        }
    }

    fun handleUndo() {
        events.add {
            game.undoMove()

//            if (!history.isEmpty()) {
//                val previous = history.pop()
//                game = previous
//                GuiGame.currentState = GuiGame.States.BOARD_STATE
//                GuiGame.viewBoard(previous.getBoard())
//
//            }
        }
    }



    private fun validMoveCriteria(start: square, end: square): Boolean =
        Squares.allInBounds(start, end)
                && start != end
                && game.getBoard().fetchPiece(start).isNotEmpty()

    //make private
    override fun pollEvents() {
        while (events.isNotEmpty()) {
            events.poll()?.invoke(this)
        }

        if (GuiGame.currentState == GuiGame.States.PROMOTION_PROMPT) {

            if (promotion != null) {
                makeMove(tempFrom, tempTo)
                promotion = null
                tempFrom = -1
                tempTo = -1
                GuiGame.currentState = GuiGame.States.BOARD_STATE
            }
        }
    }

    private fun flushCommands() {
        while (commandQueue.isNotEmpty()) {
            val command = commandQueue.poll()
            try {
                command?.exec()
            } catch (e: Exception) {
                println(e.message)
                for (i in e.stackTrace) {
                    println(i.toString())
                }
            }
        }
    }


    // - - - - - - - - - - - - - - -
    // entry to the program
    fun go(gui: Boolean = true) {
        // testPerft5()
        MatchMaker.initMatchThread()
        if (gui) thread(isDaemon = true) { javafx.application.Application.launch(GuiMenu::class.java) }
        while (true) {
            pollEvents()
            flushCommands()
            Thread.sleep(Config.ENGINE_TICK_RATE)
        }
    }

    fun benchGenSpeed(timeLimit: Long = 1_000_000_000L, position: FenString = FenString(), withGenerator: MoveGenerator = BitBoardMoveGenerator(), printResult: Boolean = true): Int {
        val game = Game()
        val mg = withGenerator
        val oneSecond = 1_000_000_000L
        game.start(position)
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
        if (printResult) {
            println("moves generated: $count times in ${timeLimit/oneSecond.toDouble()} second(s) with ${String.format("%.3f", limit / count.toDouble() )}ns per call. Using ${mg.javaClass.simpleName} ")
        }

        return count
    }

    fun perft(game: ChessGame, depth: Int, trackBranches: Boolean = false): Int {
       // val game = game.clone()
        val maxDepth = depth
        val rootPos = game.toFen()

        return perftRecurse(game, depth, trackBranches, maxDepth, mutableListOf())
    }

    private fun perftRecurse(game: ChessGame, depth: Int, trackBranches: Boolean, maxDepth: Int, trace: MutableList<String>): Int {
        var count = 0
        if (depth <= 0) {
            return 1
        }

        for (move in game.getMoves(game.turn)) {
            val traceStep = "${abs(maxDepth - depth) + 1}. ${move.literal()}"
            try {
                trace.add(traceStep)
                game.playMove(move)
            } catch(e: Exception) {
                println(game.getBoard().textVisual())
                println(game.toFen())
                println("Error in perft at depth $depth: ${e.message}")
                println(e.stackTrace.joinToString("\n"))
                println("trace: ${trace.reversed().joinToString(", ")}")
                break
            }

            val fromThisBranch = perftRecurse(game, depth - 1, trackBranches, maxDepth, trace)
            count += fromThisBranch
            game.undoMove()
            trace.removeAt(trace.indexOf(traceStep))
            if (trackBranches && depth == maxDepth) {
                println("${move.literal()}: $fromThisBranch")
            }
        }

        return count
    }

    suspend fun perftParallel(
        root: ChessGame,
        depth: Int,
        trackBranches: Boolean = false
    ): Long = coroutineScope {

        val moves = root.getMoves(root.turn)
        val results = AtomicLong(0L)

        val jobs = moves.map { move ->
            async(Dispatchers.Default) {
                val gameCopy = root.clone()
                gameCopy.playMove(move)
                val count = perftRecurseSingleThread(gameCopy, depth - 1)

                if (trackBranches) {
                    println("${move.literal()}: $count")
                }

                results.addAndGet(count.toLong())
            }
        }

        jobs.awaitAll()
        return@coroutineScope results.get()
    }

    private fun perftRecurseSingleThread(
        game: ChessGame,
        depth: Int
    ): Int {
        if (depth == 0) return 1

        var count = 0
        for (move in game.getMoves(game.turn)) {
            game.playMove(move)
            count += perftRecurseSingleThread(game, depth - 1)
            game.undoMove()
        }
        return count
    }
//    fun perft(depth: Int, game: Game) {
//        val game = game.clone()
//       // perd
//
//    }
//
//    fun perft(depth: Int, position: FenString = FenString(), inferenceDepth: Int = -1): Int {
//
//        var count = 0
//        if (depth == 0) {
//            return 1
//        }
//
//        val turn = position.turn
//        val game: ChessGame = Game()
//        game.start(position)
//        val moves = game.getMoves(turn)
//
//        for (move in moves) {
//            val next = game.clone()
//            try {
//                next.playMove(move)
//            } catch (e: IllegalArgumentException) {
//                println(game.getBoard().textVisual())
//                println(game.toFen())
//                break
//            }
//
//            val value = perft(depth - 1, FenString(next.toFen()))
//
//             if (depth == inferenceDepth) {
//                 val fromThisMove = value
//                 println("${move.literal()}: $fromThisMove")
//             }
//
//            count += value
//
//        }
//        return count
//    }


}