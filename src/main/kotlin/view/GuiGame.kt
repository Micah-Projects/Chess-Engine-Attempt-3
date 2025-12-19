package view
import controller.Controller
import javafx.animation.AnimationTimer
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.*
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.paint.Color.*
import javafx.scene.text.Font
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import model.ai.agents.RandomMover
import model.board.Color
import model.utils.Squares
import model.board.Piece
import model.game.Game
import model.game.ReadOnlyChessGame
import model.matches.MatchMaker
import model.utils.FenString
import model.utils.square
import model.movement.BitBoardMoveGenerator
import model.movement.InlinedBitBoardMoveGenerator

import kotlin.jvm.javaClass


class GuiGame  {
    companion object : View {
        private const val SQUARE_DIMENSION = 67.5

        private val palette: Palette get() {
            return when (currentState) {
                States.BOARD_STATE -> Palettes.default
                States.PROMOTION_PROMPT -> Palettes.prompt
            }
        }
        private val centerSquares = listOf(
            Squares.valueOf("e4"),
            Squares.valueOf("d4"),
            Squares.valueOf("e5"),
            Squares.valueOf("d5")
        )

        private var viewGame: ReadOnlyChessGame = Game() // place holding
        private var viewMatch: MatchMaker.Match? = null
        private var mouseX = -1.0
        private var mouseY = -1.0

        private lateinit var paintBrush: GraphicsContext

        var moveHighlights = setOf<Int>()
        var checkSquare = -1
        var orientation = Color.WHITE
        var currentState: States = States.BOARD_STATE
        var promoteColor = Color.WHITE
        var clickedSquare = -1

        override fun viewGame(game: ReadOnlyChessGame) {
            this.viewGame = game
        }

        fun promptForPromotion(color: Color) {
            promoteColor = color
            currentState = States.PROMOTION_PROMPT
        }
        private interface Palette {
            val darkSquare: javafx.scene.paint.Color
            val lightSquare: javafx.scene.paint.Color
            val blueHighlight: javafx.scene.paint.Color
            val redHighlight: javafx.scene.paint.Color
        }
        private object Palettes {

            val default = object: Palette {
                override val darkSquare =  web("#D5AB6D")
                override val lightSquare = web("#E9D4B4")
                override val blueHighlight = web("#7DAFB5", 0.61)
                override val redHighlight = web("#d91c1c", 0.55)
            }

            val prompt = object : Palette {
                override val darkSquare =  web("#D5AB6D").darker()
                override val lightSquare = web("#E9D4B4").darker()
                override val blueHighlight = web("#7DAFB5", 0.61).darker()
                override val redHighlight = web("#d91c1c", 0.55).darker()
            }
        }
    }
    private val imageIndex = arrayOf(
        Image("Piece Images 2/White Pawn.png"),
        Image("Piece Images 2/White Knight.png"),
        Image("Piece Images 2/White Bishop.png"),
        Image("Piece Images 2/White Rook.png"),
        Image("Piece Images 2/White Queen.png"),
        Image("Piece Images 2/White King.png"),
        Image("Piece Images 2/Black Pawn.png"),
        Image("Piece Images 2/Black Knight.png"),
        Image("Piece Images 2/Black Bishop.png"),
        Image("Piece Images 2/Black Rook.png"),
        Image("Piece Images 2/Black Queen.png"),
        Image("Piece Images 2/Black King.png")
    )

    enum class States {
        BOARD_STATE,
        PROMOTION_PROMPT
    }

    @FXML
    private lateinit var canvas: Canvas
    @FXML
    private lateinit var commandLine: TextField

    @FXML
    fun openMenu(e: ActionEvent) {
        val root: Parent = FXMLLoader.load(javaClass.getResource("/Menu3.fxml"))
        val stage: Stage = ((e.source as Node).scene.window) as Stage
        val scene = Scene(root)
        stage.scene = scene
        Controller.resetMetadata()
        stage.show()
    }

    @FXML
    fun newGame() {
        Controller.startNewGame()
    }

    @FXML
    fun undoMove() {
        Controller.handleUndo()
    }

    @FXML
    fun parseCommand() {
        val text = commandLine.text
        //commandLine.clear()
        CoroutineScope(Dispatchers.Default).launch {
            if (text.contains("perft")) {
                val depth = text.split(" ")[1].toInt()
                println("[depth $depth] from here: ${Controller.perftParallel(
                    Controller.game,
                    depth,
                    trackBranches = text.split(" ").getOrNull(2) == "track"
                )}")
            }
            if (text.contains("bench")) {
                val fen = FenString(Controller.game.toFen())
                val rep = text.split(" ").getOrNull(1)?.toInt() ?: 1
                println("benching...")
                var bbmg = 0.0
                var ibbmg = 0.0
                repeat (rep) {
                    bbmg += Controller.benchGenSpeed(1_000_000_000, fen, BitBoardMoveGenerator(), printResult = false)
                    ibbmg += Controller.benchGenSpeed(1_000_000_000, fen, InlinedBitBoardMoveGenerator(), printResult = false)
                }

                val avgBBMG = bbmg / rep
                val avgIBBMG = ibbmg / rep

                val percent = String.format("%.3f",  ((avgIBBMG/avgBBMG) * 100) - 100)
                println("bbmg: $avgBBMG moves per rep")
                println("ibbmg: $avgIBBMG moves per rep")
                println("IBBMG runs at $percent% faster than BBMG")

            }
            if (text.contains("get fen")) {
                println("Fen: ${Controller.game.toFen()}")
            }
            if (text.contains("load fen")) {
                Controller.startGameWith(FenString(text.split("\"")[1]))
                println("Fen: ${Controller.game.toFen()}")
            }
            if (text.contains("count moves")) {
                println("total moves: ${Controller.game.getMoves(Controller.game.turn!!).size}")
            }
            if (text.contains("get hash")) {
                println(Controller.game.getBoard().hash)
                println(Controller.game.getBoard().hash.toString(2))
            }
            if (text.contains("get status")) {
                println(Controller.game.status)
              //  println(Controller.game.getBoard().hash.toString(2))
            }
            if (text.contains("stop matches")) {
               MatchMaker.stopMatches()
                //  println(Controller.game.getBoard().hash.toString(2))
            }
            if (text.contains("bm")) {
                val g = viewGame.clone()
                viewGame(g)
                Controller.game = g
                val timew: Long = 300L * Config.SECOND_NANOS
                val timeb: Long = 300L * Config.SECOND_NANOS
                val whiteBonus = 1_000_000L // 1 millis
                val blackBonus = 1_000_000L // 1 millis
                viewMatch = MatchMaker.Match(RandomMover(), RandomMover(), g, timew, timeb)
                viewMatch?.start()
            }
        }
    }

    @Override
    fun initialize() {
        paintBrush = canvas.graphicsContext2D
        val refreshRate = object : AnimationTimer() {
            private var lastUpdate = 0L
            override fun handle(now: Long) {
                if (now - lastUpdate >= Config.GUI_TICK_RATE) {
                    lastUpdate = now
                    refreshScreen()
                }
            }
        }
        refreshRate.start()
        registerEvents()

    }

    private fun registerEvents() {
        canvas.setOnMousePressed { e ->
            mouseX = e.x - SQUARE_DIMENSION / 2
            mouseY = e.y - SQUARE_DIMENSION / 2

            when (currentState) {
                States.BOARD_STATE -> {
                    clickedSquare = getSquareFromScreen(e.x, e.y)
                    Controller.updateHighlights(clickedSquare)
                }

                States.PROMOTION_PROMPT -> {

                }
            }
        }

        canvas.setOnMouseDragged { e ->
            if (currentState != States.PROMOTION_PROMPT) {
                mouseX = e.x - SQUARE_DIMENSION / 2
                mouseY = e.y - SQUARE_DIMENSION / 2
            }
        }

        canvas.setOnMouseReleased { e ->
            when (currentState) {

                States.BOARD_STATE -> {
                    val endSquare = getSquareFromScreen(e.x, e.y)
                    Controller.clearHighlights()
                    Controller.tryMove(clickedSquare, endSquare)
                }

                States.PROMOTION_PROMPT -> {
                    // orient to be angle-agnostic
                   // val s = orient(getSquareFromScreen(e.x, e.y))
                    val s = getSquareFromScreen(e.x, e.y)
                    if (s == -1) {
                        Controller.callBack { currentState = States.BOARD_STATE }
                    } else {
                        when {
                            Squares.asText(s) == "d5" -> Controller.callBack { promotion = Piece.Type.QUEEN }
                            Squares.asText(s) == "e5" -> Controller.callBack { promotion = Piece.Type.ROOK }
                            Squares.asText(s) == "e4" -> Controller.callBack { promotion = Piece.Type.KNIGHT }
                            Squares.asText(s) == "d4" -> Controller.callBack { promotion = Piece.Type.BISHOP }
                            else -> Controller.callBack { currentState = States.BOARD_STATE }
                        }
                    }
                }
            }
             // clickedSquare = -1
        }
    }
    private fun refreshScreen() {
        when (currentState) {
            States.BOARD_STATE -> refreshBoard()
            States.PROMOTION_PROMPT -> showPromotionIcons(promoteColor)
        }
    }
    private fun showPromotionIcons(color: Color) {
        refreshBoard()
        paintBrush.fill = palette.darkSquare.darker()
        for (i in centerSquares.indices) { //
            val square = centerSquares[i]
            drawSquare(square)
            drawPieceAt(Piece.from( Piece.Type.promotions[i], color), square)
        }
    }

    private fun refreshBoard() {
        renderSquares()
        renderSquareVisuals()
        renderPieces()
    }

    private fun drawSquare(file: Int, rank: Int) {
        drawSquare(Squares.fromFileRank(file, rank))
    }
    private fun drawSquare(square: square) {
        val oriented = orient(square)
        paintBrush.fillRect(
            Squares.fileOf(oriented) * SQUARE_DIMENSION,
            Squares.rankOf(oriented) * SQUARE_DIMENSION,
            SQUARE_DIMENSION,
            SQUARE_DIMENSION
        )
    }
    private fun renderSquares() {
        for (rank in 0 until Squares.NUM_RANKS) {
            for (file in 0 until Squares.NUM_FILES) {
                val sq = Squares.fromFileRank(file, rank)
                // Set square colors

                paintBrush.fill = when ((file + rank) % 2) {
                    0 -> palette.darkSquare
                    else -> palette.lightSquare
                }
                drawSquare(file, rank)

                // draw move squares of piece
                if (sq in moveHighlights) {
                    paintBrush.fill = palette.blueHighlight
                    drawSquare(file, rank) //
                }

                if (sq == checkSquare) {
                    paintBrush.fill = palette.redHighlight
                    drawSquare(file, rank) //
                }
            }
        }
    }
    private fun renderSquareVisuals() {
        for (rank in 0 until Squares.NUM_RANKS) {
            for (file in 0 until Squares.NUM_FILES) {
                val oriented = orient(Squares.fromFileRank(file, rank))
                paintBrush.fill = when ((Squares.fileOf(oriented) + Squares.rankOf(oriented)) % 2) {
                    0 -> palette.lightSquare
                    else -> palette.darkSquare
                }

                paintBrush.font = Font(20.0)
                paintBrush.fillText(
                    Squares.asText(oriented),
                    (file * SQUARE_DIMENSION + 0.5 * SQUARE_DIMENSION) - 10.0,
                    (rank * SQUARE_DIMENSION + 0.5 * SQUARE_DIMENSION) + 10.0
                )
            }
        }

    }

    private fun drawPieceAt(piece: Piece, square: square) {
        drawPieceAt(piece, Squares.fileOf(square), Squares.rankOf(square))
    }
    private fun drawPieceAt(piece: Piece, file: square, rank: square) {
        val (file, rank) = orient(file, rank)
        if (!piece.isEmpty()) {
            val image = imageIndex[piece.id]
            paintBrush.drawImage(
                image,
                file * SQUARE_DIMENSION,
                rank * SQUARE_DIMENSION,
                SQUARE_DIMENSION,
                SQUARE_DIMENSION
            )
        }
    }
    private fun drawPieceAtMouse(piece: Piece) {
        if (!piece.isEmpty()) {
            val image = imageIndex[piece.id]
            paintBrush.drawImage(
                image,
                mouseX,
                mouseY,
                SQUARE_DIMENSION,
                SQUARE_DIMENSION
            )
        }
    }
    private fun renderPieces() {
        for (square in Squares.range) {
            if (square != clickedSquare) {
                drawPieceAt(viewGame.getBoard().fetchPiece(square), square)
            }
        }
            drawPieceAtMouse(viewGame.getBoard().fetchPiece(clickedSquare)) // for layering

    }

    private fun orient(file: Int, rank: Int): Pair<Int, Int> {
        return if (orientation == Color.WHITE)  Pair(file,  7 - rank) else Pair(file, rank)
    }
    private fun orient(square: square): square {
        val rank = Squares.rankOf(square)
        val file = Squares.fileOf(square)
       return if (orientation == Color.WHITE)  Squares.fromFileRank(file,  7 - rank) else square
    }

    // lenience represents how exact the position has to be to return the square
    private fun getSquareFromScreen(x: Double, y: Double, lenience: Double = .825): square {
        val radius = lenience.coerceIn(0.0, 1.0) * (SQUARE_DIMENSION / 2)

        val file = (x / SQUARE_DIMENSION).toInt()
        val rank = (y / SQUARE_DIMENSION).toInt()

        val centerOfFile = (file * SQUARE_DIMENSION) + (SQUARE_DIMENSION / 2)
        val centerOfRank = (rank * SQUARE_DIMENSION) + (SQUARE_DIMENSION / 2)

        val left = centerOfFile - radius
        val right = centerOfFile + radius
        val top = centerOfRank - radius
        val bottom = centerOfRank + radius

        return if (x in left..right && y in top..bottom) orient(Squares.fromFileRank(file, rank)) else -1
    }



}

