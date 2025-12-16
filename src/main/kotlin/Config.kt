const val DEBUG = false
object Config {
    const val MAGIC_KEYS_FILE_PATH = "src/main/kotlin/model/utils/MagicKeys.txt"
    const val MAX_MATCHES = 100

    const val SECOND_NANOS = 1_000_000_000
    const val SECOND_MILLIS = 1_000L

    const val ENGINE_TPS: Int = 48
    const val GUI_TPS: Int = 40

    const val ENGINE_TICK_RATE: Long = SECOND_MILLIS / ENGINE_TPS
    const val GUI_TICK_RATE = SECOND_NANOS / GUI_TPS

    const val DELAY_TIME_NANOS = 0//1 * SECOND_NANOS
    const val DELAY_TIME_MILLIS = 1 * SECOND_MILLIS
}