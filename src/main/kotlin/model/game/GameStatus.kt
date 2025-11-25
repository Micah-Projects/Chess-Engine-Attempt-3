package model.game

import model.board.Color

enum class GameStatus(val id: Int) { // can add other things like repetition, insufficient material, etc later
    UNSTARTED(-1),
    UNKNOWN(-1),

    ONGOING(0),
    STALEMATE(1),

    DRAW_REPETITION(2),
    DRAW_INSUFFICIENT(2),
    DRAW_FIFTY_MOVES(2),

    WIN_WHITE_CHECKMATE(3),
    WIN_BLACK_CHECKMATE(3),

    WIN_WHITE_TIMEOUT(4),
    WIN_BLACK_TIMEOUT(4);

}