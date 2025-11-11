package controller

import model.misc.Squares
import model.misc.square

interface Features {
    fun tryMove(from: square, to: square)
    fun startNewGame()
    fun printBoard()
    fun pollEvents()

}