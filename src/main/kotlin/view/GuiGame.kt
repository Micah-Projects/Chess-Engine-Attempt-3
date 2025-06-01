package view

import command.Move
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
import model.board.Piece.*
import model.misc.Squares
import model.board.Piece
import model.misc.Moves
import model.misc.square

import kotlin.jvm.javaClass
import kotlin.to


class GuiGame : View{
    companion object {
        private const val SQUARE_DIMENSION = 67.5
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

    private var squarePositions = (Array(8) { Array(8) { Pair(0.0, 0.0) } })
    private var mouseX = -1.0
    private var mouseY = -1.0

    private var orientation = WHITE
    private var focusBoard: ChessBoard = Board()
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
        focusBoard = Board()
        focusBoard.addPiece(Piece.random(), 27)
        focusBoard.addPiece(Piece.random(), 27)
        focusBoard.addPiece(Piece.random(), 27)
        println(focusBoard.textVisual())
    }

    @FXML
    fun undoMove() {

    }

    @FXML
    fun parseCommand() {

    }

    @Override
    fun initialize() {
        setSquarePositions()
        paintBrush = canvas.graphicsContext2D
        val refreshRate = object : AnimationTimer() {
            private var lastUpdate = 0L
            override fun handle(now: Long) {
                if (now - lastUpdate >= 20_000_000) {
                    lastUpdate = now
                    refreshBoard()
                }
            }
        }
        refreshRate.start()

        canvas.setOnMousePressed { e ->
            mouseX = e.x - SQUARE_DIMENSION / 2
            mouseY = e.y - SQUARE_DIMENSION / 2
            clickedSquare = convertPairToIntSquare(adjustForOrientation(getSquareFromPixels(e.x, e.y)))

        }

        canvas.setOnMouseDragged { e ->
            mouseX = e.x - 67.5 / 2
            mouseY = e.y - 67.5 / 2
        }

        canvas.setOnMouseReleased { e ->
            val endSquare = convertPairToIntSquare(adjustForOrientation(getSquareFromPixels(e.x, e.y)))

            if (validMoveCriteria(clickedSquare, endSquare)) {
                val move = Moves.encode(clickedSquare, endSquare)
                Runner.receiveCommand(Move(move, focusBoard))
            }
            clickedSquare = -1

        }
    }

    private fun validMoveCriteria(start: square, end: square): Boolean =
        start in focusBoard.boardSquares
            && end in focusBoard.boardSquares
            && end != start
            && focusBoard.fetchPiece(clickedSquare) != EMPTY

    private fun setSquarePositions() {
        squarePositions.forEachIndexed { row, array ->
            array.indices.forEach { col ->
                squarePositions[row][col] = Pair(col * SQUARE_DIMENSION, row * SQUARE_DIMENSION)
            }
        }
    }

    private fun refreshBoard() {
        renderSquares()
        renderSquareVisuals()
        renderPieces()
    }

    private fun renderSquares() {
        for (s in 0..63) {
            val square = Squares.asCoord(s)
            val col = Squares.fileOf(s)
            val row = 7 - Squares.rankOf(s)

            // Set square colors
            paintBrush.fill = when ((col + row) % 2) {
                0 -> web("#D5AB6D")
                else -> web("#E9D4B4")
            }
            drawSquare(square)
        }
    }

    private fun drawSquare(position: Pair<Int, Int>) {
        val adjusted = adjustForOrientation(position)
        paintBrush.fillRect(
            adjusted.first * SQUARE_DIMENSION,
            adjusted.second * SQUARE_DIMENSION,
            SQUARE_DIMENSION,
            SQUARE_DIMENSION
        )
    }

    private fun renderPieces() {
        for (square in 0.until(64)) {
            val position = adjustForOrientation(Squares.asCoord(square)) //
            val x = scaleNumberToScreen(7 - position.first)
            val y = scaleNumberToScreen(position.second)

            if (square != clickedSquare) {
                drawPieceAt(focusBoard.fetchPiece(square), x, y)
            }
        }
        drawPieceAt(focusBoard.fetchPiece(clickedSquare), mouseX, mouseY) // for layering

    }

    private fun renderSquareVisuals() {
        for (s in focusBoard.boardSquares) {
            val row = Squares.rankOf(s)
            val col = 7 - Squares.fileOf(s)
            paintBrush.fill = when ((col + row) % 2) {
                0 -> web("#D5AB6D")
                else -> web("#E9D4B4")
            }
            paintBrush.font = Font(20.0)
            val newpos = adjustForOrientation(Pair(col, row))
            paintBrush.fillText(
                Squares.asText(s),
                (newpos.first * SQUARE_DIMENSION + 0.5 * SQUARE_DIMENSION) - 10.0,
                ((newpos.second * SQUARE_DIMENSION + 0.5 * SQUARE_DIMENSION)) + 10.0
            )
        }

    }

    private fun drawPieceAt(piece: Piece, x: Double, y: Double) {
        if (!piece.isEmpty()) {
            val image = Image(imageIndex[piece.get()])
            paintBrush.drawImage(
                image,
                x,
                y,
                SQUARE_DIMENSION,
                SQUARE_DIMENSION
            )
        }

    }

    private fun scaleNumberToScreen(number: Int): Double {
        return number.toDouble() * SQUARE_DIMENSION
    }

    private fun adjustForOrientation(coords: Pair<Int, Int>): Pair<Int, Int> {
        return if (orientation == WHITE) Pair(7 - coords.first, 7 - coords.second) else coords
    }

    private fun getSquareFromPixels(x: Double, y: Double): Pair<Int, Int> {
        val offset = 9
        squarePositions.forEach { row ->
            row.forEach {
                if (x >= it.first + offset && x < it.first + SQUARE_DIMENSION - offset &&
                    y >= it.second + offset && y < it.second + SQUARE_DIMENSION - offset
                ) {
                    return Pair(
                        (7 - it.first / SQUARE_DIMENSION).toInt(),
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

    override fun viewBoard(board: ChessBoard) {
        this.focusBoard = board
    }

}

