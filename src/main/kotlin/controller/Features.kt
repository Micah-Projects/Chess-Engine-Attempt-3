package controller

import model.utils.square

interface Features {
    fun tryMove(from: square, to: square)
    fun startNewGame()
    fun printBoard()
    fun pollEvents()

}