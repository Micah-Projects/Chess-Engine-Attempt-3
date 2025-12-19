package model.matches


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.board.Color
import model.game.ChessGame
import model.game.Game
import model.game.GameStatus
import model.game.ReadOnlyChessGame
import model.ai.agents.Player
import model.movement.Move
import model.utils.FenString
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.random.Random

/*
The Match Maker component is capable of hosting games between two players. It's responsible for running the game,
tracking it's state, and once over, yields a result that contains some statistical data about the game which took place.
Some of this data includes:
    the winner if applicable, number of moves, end status, player colors, and other useful data.
The match maker class will be used to host games between two bodies. These bodies may be any of:
    -> user with gui
    -> user with terminal
    -> bot/engine
These games may be timed. The matchmaker can run batches of games simultaneously, or sequentially. these batches, once
complete, yield a result, where statistical data such as winner/loser body, color, quantity of each end status
result are all stored. Games can be started from a constant position, or a sequence of positions, and have specific timers for each player.
the match maker runs games on its own thread, and
 */
object MatchMaker {
    private var init = false
    private val tasks = ConcurrentLinkedQueue<() -> Unit>()
    private val matchSchedule = mutableListOf<Match>()
    private val staleMatches = mutableListOf<Match>()
    private var lastUpdateTime: Long = 0
    private var sessionKey: Long = Random.nextLong()

    fun initMatchThread() {
        if (init) return
        init = true
        thread(name = "MatchThread", isDaemon = true) {
            matchLoop()
        }
    }

    fun stopMatches() {
        val oldKey = sessionKey
        do { sessionKey = Random.nextLong() } while (sessionKey == oldKey) // account for the small chance it's the same
    }

    fun stopMatch(match: Match) {
        tasks.add {
            staleMatches.add(match)
        }
    }

    private fun matchLoop() {
        while (true) {
            lastUpdateTime = System.nanoTime() + Config.DELAY_TIME_NANOS
            while (tasks.isNotEmpty()) tasks.poll()?.invoke() // run all pending tasks
            updateMatches()
           // Thread.sleep(Config.ENGINE_TICK_RATE)
        }
    }

    private fun updateMatches() {
        val now = System.nanoTime()
        val dt = (now - lastUpdateTime)
        for (match in matchSchedule) {
            if (match.activeListener.timed) {
                match.activeListener.timeRemaining -= dt
            }
            // println("${match.activeListener.timeRemaining}, ${match.getGame().turn}")
            match.update()

            if (isInvalidKey(match.sessionKey)) {
                staleMatches.add(match)
            }
        }
        matchSchedule.removeAll(staleMatches)
        staleMatches.clear()
    }

    private fun isValidKey(sessionKey: Long): Boolean = sessionKey == this.sessionKey
    private fun isInvalidKey(sessionKey: Long): Boolean = sessionKey != this.sessionKey

    interface Listener {
        fun getTime(): Long

        fun hasTime(): Boolean

        fun popMove(): Move?

        fun take(move: Move)

        fun isAvailable(): Boolean

        fun finalizeMove()
    }

    class MoveListener(initialTime: Long? = null) : Listener {
        private var move: Move? = null
        var available = false
        val timed = initialTime != null && initialTime > 0
        var timeRemaining = initialTime ?: -1
        private var readyToPop = false

        // - - - - - - - - - called from player thread - - - - -
        override fun hasTime(): Boolean {
            return timeRemaining > 0 || !timed
        }

        override fun popMove(): Move? {
            val m = move
            move = null
            readyToPop = false
            return m
        }

        override fun take(move: Move) {
            if (!hasTime() || !available) return
            this.move = move
        }

        /**
         * call this method when you're ready to finalize your selection
         */
        override fun finalizeMove() {
            available = false
            readyToPop = true
        }

        override fun isAvailable(): Boolean = available

        fun isReadyToPop() = readyToPop

        override fun getTime(): Long {
            return timeRemaining
        }

    }
    /*
        run game and timers on one thread, run engine search on another thread
     */

    // p1 will always be white, p2 will always be black. when initializing, keep this order in mind
    class Match(val white: Player, val black: Player, private val game: ChessGame, val t1: Long?, val t2: Long?, val b1: Long = 0, val b2: Long = 0) {
        private val lw = MoveListener(t1)
        private val lb = MoveListener(t2)
        private var started = false
        val sessionKey = MatchMaker.sessionKey // use the current global key

        var activeListener: MoveListener = MoveListener(); private set

        var currentTurn = game.turn
        var ongoing = true
        fun getGame(): ReadOnlyChessGame = game

        fun update() {
            if (activeListener.isReadyToPop()) {
                val move = activeListener.popMove() // do a check to see if the listener is okay to pop first
                if (move != null) {
                    game.playMove(move)
                    val bonus = if (activeListener == lw) b1 else b2
                    activeListener.timeRemaining += bonus
                    val wtr = String.format("%.3f", lw.timeRemaining / Config.SECOND_NANOS.toDouble())
                    val btr = String.format("%.3f", lb.timeRemaining / Config.SECOND_NANOS.toDouble())
                        //print(game.toFen() + "\r")
                    print("Wtr: $wtr btr: $btr\n")
                    switchTurn()
                    }
            } else {
                if (game.isOngoing() && activeListener.timed && !activeListener.hasTime()) when (game.turn) {
                    Color.WHITE ->  game.end(GameStatus.WIN_BLACK_TIMEOUT)
                    Color.BLACK ->  game.end(GameStatus.WIN_WHITE_TIMEOUT)
                }

                //println("game ended")
                // terminate game via timeout
            }

        }

        private fun switchTurn() {
            activeListener.available = false // may lead to stall bugs?
            activeListener = if (game.turn == Color.WHITE) lw else lb
            activeListener.available = true
        }

        // adds this match into the update loop, ie, it begins execution. It can only be called once.
        fun start() {
            if (started) return
            started = true
            tasks.add {
                activeListener = if (game.turn == Color.WHITE) lw else lb
                white.prepareContext(game, Color.WHITE, t1)
                black.prepareContext(game, Color.BLACK, t2)
                switchTurn()
                game.start()
                matchSchedule.add(this) // schedules the match to begin ticking, i.e, updating moves, timers, etc

                // begin the players

                    // abstract this into a block which abstracts the coroutine + while loop.

                withRunning(game) {
                   if (activeListener.isAvailable()) (if (game.turn == Color.WHITE) white else black).beginSearch(activeListener, game)
                    if (!game.isOngoing()) {
                        println(game.status)
                    }
                }
            }
        }


        private inline fun withRunning(game: ChessGame, crossinline block: () -> Unit) {
            CoroutineScope(Dispatchers.Default).launch {
                while (game.isOngoing() && isValidKey(sessionKey)) {
                    block()
                    delay(Config.DELAY_TIME_MILLIS)
                }
                println("match ended")
            }
        }
    }



    // - - - called from outer threads, should remain responsive. - - -
    fun match(p1: Player, p2: Player, fen: FenString, t1: Long, t2: Long) {
        tasks.add {
            val m = Match(p1, p2, Game().start(fen), t1, t2)
            m.start()
        }
    }

    fun match(p1: Player, p2: Player, game: ChessGame, t1: Long, t2: Long) {
        tasks.add {
            val m = Match(p1, p2, game, t1, t2)
            m.start()
        }
    }

    fun bulkMatch() {

    }


}