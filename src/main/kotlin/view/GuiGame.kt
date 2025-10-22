package view
import command.MakeMove
import command.PrintBoard
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
import model.board.Board
import model.board.ChessBoard
import model.board.Color
import model.board.MutableChessBoard
import model.board.Piece.*
import model.misc.Squares
import model.board.Piece
import model.board.ReadOnlyBoard
import model.game.ChessGame
import model.game.Game
import model.misc.Moves
import model.misc.square

import kotlin.jvm.javaClass
import kotlin.to


class GuiGame  {
    companion object : View {
        private const val SQUARE_DIMENSION = 67.5
        private var viewBoard = ReadOnlyBoard(Board()) // place holding

        override fun viewBoard(board: ChessBoard) {
            this.viewBoard = ReadOnlyBoard(board)
        }

        var highlights = listOf<Int>()
        var orientation = model.board.Color.WHITE
    }


    @FXML
    private lateinit var canvas: Canvas
    private lateinit var stage: Stage

    @FXML
    private lateinit var scene: Scene

    @FXML
    private lateinit var commandLine: TextField
    private lateinit var root: Parent
    private lateinit var paintBrush: GraphicsContext

    private var squarePositions = Array(8) { col -> Array(8) { row -> Pair(col * SQUARE_DIMENSION, row * SQUARE_DIMENSION) } }
    private var mouseX = -1.0
    private var mouseY = -1.0


    private var clickedSquare = -1


    val imageIndex = mapOf<Int, String>(
        0 to "Piece Images 2/White Pawn.png",
        1 to "Piece Images 2/White Knight.png",
        2 to "Piece Images 2/White Bishop.png",
        3 to "Piece Images 2/White Rook.png",
        4 to "Piece Images 2/White Queen.png",
        5 to "Piece Images 2/White King.png",
        6 to "Piece Images 2/Black Pawn.png",
        7 to "Piece Images 2/Black Knight.png",
        8 to "Piece Images 2/Black Bishop.png",
        9 to "Piece Images 2/Black Rook.png",
        10 to "Piece Images 2/Black Queen.png",
        11 to "Piece Images 2/Black King.png"

    )


    @FXML
    fun openMenu(e: ActionEvent) {
        // Game.Sessions.vacateAll()
        root = FXMLLoader.load(javaClass.getResource("/Menu3.fxml"))
        stage = ((e.source as Node).scene.window) as Stage
        scene = Scene(root)
        stage.scene = scene
        stage.show()
    }

    @FXML
    fun newGame() {
//        focusBoard = Board()
//        focusBoard.addPiece(Piece.random(), Squares.random())
//        focusBoard.addPiece(WHITE_PAWN, Squares.valueOf("e2"))
//        focusBoard.addPiece(WHITE_PAWN, Squares.valueOf("d2"))
//        focusBoard.addPiece(BLACK_PAWN, Squares.valueOf("d7"))
//        focusBoard.addPiece(BLACK_PAWN, Squares.valueOf("e7"))
     //   val gen: MoveGenerator = BitBoardMoveGenerator()
       // focusBoard.loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        //focusBoard.addPiece(Piece.random(), Squares.valueOf())
       // println(focusBoard.textVisual())
        Controller.startNewGame()
    }

    @FXML
    fun undoMove() {

    }

    @FXML
    fun parseCommand() {
        orientation == orientation.enemy
    }

    @Override
    fun initialize() {
        paintBrush = canvas.graphicsContext2D
        val refreshRate = object : AnimationTimer() {
            private var lastUpdate = 0L
            val secondNanos = 1_000_000_000
            val rate = secondNanos / 40
            override fun handle(now: Long) {
                if (now - lastUpdate >= rate) {
                    lastUpdate = now
                    refreshBoard()
                }
            }
        }
        refreshRate.start()
        events()

    }

    fun events() {
        canvas.setOnMousePressed { e ->
            mouseX = e.x - SQUARE_DIMENSION / 2
            mouseY = e.y - SQUARE_DIMENSION / 2

            clickedSquare = convertPairToIntSquare( adjustForOrientation(getSquareFromPixels(e.x, e.y)))
            Controller.setHighlights(clickedSquare)

        }

        canvas.setOnMouseDragged { e ->
            mouseX = e.x - 67.5 / 2
            mouseY = e.y - 67.5 / 2
        }

        canvas.setOnMouseReleased { e ->
            val endSquare = convertPairToIntSquare(adjustForOrientation(getSquareFromPixels(e.x, e.y)))
            Controller.clearHighlights()
            Controller.safeTryMove(clickedSquare, endSquare)
            clickedSquare = -1
        }
    }
    // for now

    private fun refreshBoard() {
        renderSquares()
        renderSquareVisuals()
        renderPieces()
    }

    private fun renderSquares() {
        for (rank in 0 until Squares.NUM_RANKS) {
            /// Squares.NUM_FILES - 1 downTo  0
            for (file in 0 until Squares.NUM_FILES) {
                val sq = Squares.fromFileRank(file, rank)

                // Set square colors
                paintBrush.fill = when ((file + rank) % 2) {
                    0 -> web("#D5AB6D")
                    else -> web("#E9D4B4")
                }
                drawSquare(file, rank)

                // draw move squares of piece
                if (sq in highlights) {
                    paintBrush.fill = web("#7DAFB5", 0.61)
                    drawSquare(file, rank) //
                }
            }
        }
    }

    fun drawSquare(file: Int, rank: Int) {
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

    private fun renderPieces() {
        for (square in Squares.range) {
            val s = orient(square)
            val x = Squares.fileOf(s) * SQUARE_DIMENSION
            val y = Squares.rankOf(s) * SQUARE_DIMENSION

            if (square != clickedSquare) {
                drawPieceAt(viewBoard.fetchPiece(square), x, y)
            }
        }
            drawPieceAt(viewBoard.fetchPiece(clickedSquare), mouseX, mouseY) // for layering

    }

    private fun renderSquareVisuals() {
        for (rank in 0 until Squares.NUM_RANKS) {
            for (file in 0 until Squares.NUM_FILES) {
               // val oriented = orient(s)
                paintBrush.fill = when ((file + rank) % 2) {
                    0 -> web("#D5AB6D")
                    else -> web("#E9D4B4")
                }
                paintBrush.font = Font(20.0)
                paintBrush.fillText(
                    Squares.asText(orient(Squares.fromFileRank(file, rank))),
                    (file * SQUARE_DIMENSION + 0.5 * SQUARE_DIMENSION) - 10.0,
                    (rank * SQUARE_DIMENSION + 0.5 * SQUARE_DIMENSION) + 10.0
                )
            }
        }

    }

    private fun drawPieceAt(piece: Piece, x: Double, y: Double) {
        if (!piece.isEmpty()) {
            val image = Image(imageIndex[piece.value])
            paintBrush.drawImage(
                image,
                x,
                y,
                SQUARE_DIMENSION,
                SQUARE_DIMENSION
            )
        }

    }

    private fun adjustForOrientation(coords: Pair<Int, Int>): Pair<Int, Int> {
        return if (orientation == Color.WHITE) Pair(coords.first, 7 - coords.second) else coords
    }

    private fun orient(square: square): square {
        val rank = Squares.rankOf(square)
        val file = Squares.fileOf(square)
       return if (orientation == Color.WHITE)  Squares.fromFileRank(file,  7 - rank) else square
    }

    private fun getSquareFromPixels(x: Double, y: Double): Pair<Int, Int> {
        val offset = 9
        squarePositions.forEach { row ->
            row.forEach {
                if (x >= it.first + offset && x < it.first + SQUARE_DIMENSION - offset &&
                    y >= it.second + offset && y < it.second + SQUARE_DIMENSION - offset
                ) {
                    return Pair(
                        (it.first / SQUARE_DIMENSION).toInt(),
                        (it.second / SQUARE_DIMENSION).toInt()
                    )
                }
            }
        }
        return Pair(9, 9)
    }

    private fun convertPairToIntSquare(square: Pair<Int, Int>): Int {
        val x = square.first
        val y = square.second
        if (x == 0 && y == 0) return 0
        return if (x in 0..8 && y in 0..8) (y * 8 + x) else -1
    }



}

