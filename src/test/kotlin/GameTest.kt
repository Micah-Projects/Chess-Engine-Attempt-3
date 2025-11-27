import model.game.Game
import model.game.GameStatus
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

class GameTest {
    private var game = Game()

    @BeforeEach
    fun setup() {
        game = Game()
    }

    @Test
    fun emptyGameReturnsEquivalentFenAsStartPosition() {
        val expected = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        assertEquals(expected, game.toFen())
    }



    @Test
    fun playUntil50MoveRule() {
        game.start()
        while (!game.isOver()) {
            game.playMove(game.getMoves(game.currentTurn()).random())
            println(game.getBoard().textVisual())
        }
        println(game.getPlies())
        println(game.getHalfMoveClock())
        assertEquals(GameStatus.DRAW_FIFTY_MOVES, game.getStatus())
    }

    @Test
    fun testGameStatuses() {
        assertEquals(GameStatus.UNSTARTED, game.getStatus())
        game.start()
        assertEquals(GameStatus.ONGOING, game.getStatus())
        // expand this test later
    }

}