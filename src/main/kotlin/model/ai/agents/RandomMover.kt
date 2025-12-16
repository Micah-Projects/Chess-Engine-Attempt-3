package model.ai.agents

import model.board.Color
import model.game.ReadOnlyChessGame
import model.matches.MatchMaker
import model.movement.Move

class RandomMover : Player() {
    lateinit var color: Color
    override fun prepareContext(game: ReadOnlyChessGame, team: Color, tc: Long?) {
        color = team
    }

    override fun search(listener: MatchMaker.Listener, game: ReadOnlyChessGame) {
        val moves = game.getMoves(color)
        if (moves.isNotEmpty()) {
            listener.take(moves.random())
            listener.finalizeMove()
        }
    }
}