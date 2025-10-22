package controller

import model.misc.Squares
import model.misc.square

interface Features {
    fun safeTryMove(from: square, to: square)
    fun makeMove(from: square, to: square)
    fun startNewGame()
    fun printBoard()


}