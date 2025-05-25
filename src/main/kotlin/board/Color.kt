package board

/**
 * The colors for two chess players.
 */
enum class Color(private val c: Int) {
    WHITE(0),
    BLACK(1);


    private val enemy: Color by lazy {
        when (this) {
            WHITE -> BLACK
            BLACK -> WHITE
        }
    }

    /**
     * Returns this color represented as an Int
     */
    fun get(): Int = c


    /**
     * Returns the enemy of the given [Color]
     */
    fun enemy(): Color = enemy
}