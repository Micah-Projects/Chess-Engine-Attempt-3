import model.utils.Squares
import org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.*

class SquaresUtilityTest {
    @Test
    fun test() {
        assertTrue { Squares.asText(0) == "a1" }
        assertTrue { Squares.asText(1) == "b1" }
    }
}