package model.ai.agents

import model.board.Color
import model.game.ReadOnlyChessGame
import model.matches.MatchMaker
import model.movement.Move

abstract class Player {
    abstract fun prepareContext(game: ReadOnlyChessGame, team: Color, tc: Long?) // passes the agent some data to process before the start
    abstract fun search(listener: MatchMaker.Listener, game: ReadOnlyChessGame) // blocks the co-routine


    fun beginSearch(listener: MatchMaker.Listener, game: ReadOnlyChessGame) {
        search(listener, game)
        listener.finalizeMove()
    }

}