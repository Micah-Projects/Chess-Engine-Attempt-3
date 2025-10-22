package view

import model.board.Color
import model.board.Piece
import model.misc.square

interface Visualizer {

    fun toggleSquareNames(value: Boolean? = null)

    fun toggleOrientation(color: Color? = null): Color

    fun addHighlights(color: String, vararg squares: String)

    fun addHighlights(color: String, vararg squares: square)

    fun clearHighlights()
}