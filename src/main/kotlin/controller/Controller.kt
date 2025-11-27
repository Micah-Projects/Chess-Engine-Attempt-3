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
import kotlin.math.abs
import kotlin.properties.Delegates
import kotlin.system.exitProcess

fun main() = Controller.go(gui = true)

object Controller : Features {
    var game: ChessGame = Game()
    var promotion: Piece.Type? = null
    private var tempFrom: Int = -1
    private var tempTo: Int = -1
    private var events = ConcurrentLinkedQueue<(Controller.() -> Unit)>()
    private var commandQueue = ConcurrentLinkedQueue<Command>()
    var guiRate by Delegates.notNull<Int>()

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
    // - - - - - - - - - - - - - - - functions to be called from other threads

    //** called from the gui thread
    fun callBack(callBack: (Controller.() -> Unit)) {
        // println("added callback")
        events.add(callBack)
    }

    fun receiveCommand(command: Command) {
        commandQueue.add(command)
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

//            CoroutineScope(Dispatchers.Default).launch {
//                while (!game.isOver()) {
//                    events.add {
//                        game.playMove(game.getMoves(game.currentTurn()).random())
//                        println(game.getBoard().textVisual())
//                    }
//                    delay(1000)
//                }
//            }

//            game.playMove(BetterMoves.encode(
//                Piece.WHITE_PAWN,
//                Squares.valueOf("d2"),
//                Squares.valueOf("d4"))
//            )
//            game.playMove(BetterMoves.encode(
//                Piece.BLACK_PAWN,
//                Squares.valueOf("d7"),
//                Squares.valueOf("d5"))
//            )
//
//            val mg = BitBoardMoveGenerator()
//            repeat(100) {
//
//                var count = 0
//                var now = 0L
//                var i = 0L
//                val limit = 1_000_000_000L
//                while (now < limit) {
//                    val start = System.nanoTime()
//                   // i++
//                    mg.generateMoves(game.getBoard(), Color.BLACK)
//                    val end = System.nanoTime()
//                    val time = end - start
//                    count++
//                    now += time
//                }
//
//              //  println("incs: $i")
//                println("moves generated: $count times in 1 second with ${String.format("%.6f", limit / count.toDouble() )}ns per call ")
//            }





        }
    }

    override fun tryMove(from: square, to: square) {
        events.add {
            if (validMoveCriteria(from, to)) {
                makeMove(from, to)

                // GuiGame.orientation = game.currentTurn()!!
            }
            GuiGame.clickedSquare = -1
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
            commandQueue.add(MakeMove(Moves.encode(p, from, to, pro), game))
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

    // gui stuff - - - -
    fun updateHighlights(square: square) {
        events.add {
            GuiGame.moveHighlights = game.getMoves(game.turn!!).filter { it.from() == square }.map { it.to()}.toSet()
        }
    }

    fun clearHighlights() {
        events.add {
            GuiGame.moveHighlights = setOf()
        }
    }

    fun fetchGuiRate(): Int {
        return guiRate
    }


    // - - - - - - - - - - - - - - -
    // entry to the program
    fun go(gui: Boolean = true, pollRate: Int = 40) {
        guiRate = pollRate
        if (pollRate < 1) {
            println("cannot have a poll-rate less than 1")
            exitProcess(1)
        }

        // testPerft5()
        val secondNanos = 1_000_000_000
        val rate = secondNanos / (pollRate * 2)
        if (gui) {
            Thread {
                javafx.application.Application.launch(GuiMenu::class.java)
            }.start()
        }


        var then = System.nanoTime()
        var now: Long
        while (true) {
            now = System.nanoTime()
            if (now - then > rate) {
                pollEvents()
                flushCommands()
                then = now
            }

            // add code to refresh the screen from the controller // maybe not
        }
    }
    fun nonsense() {

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
                // clone or copy your game
                val gameCopy = root.clone() // You must implement this properly

                // play root move
                gameCopy.playMove(move)

                // run normal single-thread perft
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


    fun testPerft5() {
        // println("nodes at depth: $5: ${perft(5)}")
        for (i in 0..5) {
         //   println("nodes at depth: $i: ${perft(i)}")
        }

    }

}