package controller

import command.Command
import command.MakeMove
import command.PrintBoard
import model.board.Color
import model.board.Piece
import model.board.Piece.Type
import model.game.ChessGame
import model.game.Game
import model.misc.BetterMoves
import model.misc.BitBoards
import model.misc.Squares
import model.misc.from
import model.misc.square
import model.misc.to
import model.movement.BitBoardMoveGenerator
import view.GuiGame
import view.GuiMenu
import java.util.Stack
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.properties.Delegates
import kotlin.system.exitProcess

fun main() = Controller.go(gui = true)

object Controller : Features {
    var game: ChessGame = Game()
    var promotion: Piece.Type? = null
    private var history = Stack<ChessGame>() // for timed modes, you usually wont be undoing
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

    override fun startNewGame() {
        events.add {
            game = Game()
            GuiGame.currentState = GuiGame.States.BOARD_STATE
            GuiGame.viewBoard(game.getBoard())

            game.start()

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
            history.push(game.clone())
            val pro = promotion ?: Piece.Type.PAWN
            commandQueue.add(MakeMove(BetterMoves.encode(p, from, to, pro), game))
            promotion = null
        }
    }

    fun handleUndo() {
        events.add {
            if (!history.isEmpty()) {
                val previous = history.pop()
                game = previous
                GuiGame.currentState = GuiGame.States.BOARD_STATE
                GuiGame.viewBoard(previous.getBoard())

            }
        }
    }

    // gui stuff - - - -
    fun updateHighlights(square: square) {
        events.add {
            GuiGame.moveHighlights = game.getMoves(game.currentTurn()!!).filter { it.from() == square }.map { it.to()}.toSet()
//            GuiGame.checkSquare = if (mg.getCheckStatus()) {
//                BitBoards.getSetBitIndices(
//                    game.getBoard()
//                    .fetchPieceBitBoard(Piece.from(Type.KING, game.currentTurn()!!)))
//                    .first()
//            } else {
//                -1
//            }
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
}