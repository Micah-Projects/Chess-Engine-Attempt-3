package misc

import java.util.concurrent.ConcurrentHashMap

/**
 * Simple Debug object for lazily storing debug messages.
 */
object Debug {
    private var active = true

    /**
     * Identifier for different Debug categories.
     */
    enum class Area {
        BOARD,
        COLOR,
        PIECE,
        FEN_STRINGS,
        PARSER,
        GUI,
        GAME,
        ALL;
    }

    private var Areas: ConcurrentHashMap<Area, MutableList<String>> = init()

    private fun init(): ConcurrentHashMap<Area, MutableList<String>> {
        val map = ConcurrentHashMap<Area, MutableList<String>>()
        Area.entries.forEach { map[it] = mutableListOf<String>() }
        return map
    }

    /**
     * Allows the debugger to begin auditing logs.
     */
    fun start() {
        active = true
    }

    /**
     * Stops the debugger from adding new logs.
     */
    fun stop() {
        active = false
    }

    /**
     * Writes a [message] into the log specified by [area].
     * @param area The area which the log will be written to.
     * @param probability The Chance that a call will add a log. This is useful for reducing redundant logging.
     * @param message The string to be logged.
     * @return The logged message.
     */
    fun log(area: Area = Area.ALL , probability: Double = 1.0, message: () -> String): String {
        val m: String
        if (active && Math.random() <= probability) {
            m = message()
            Areas.computeIfAbsent(area) { mutableListOf() }.add(m)

            if (area != Area.ALL) Areas.computeIfAbsent(area) { mutableListOf() }.add(m)
        } else {
           m = if (!active) "Debug is Inactive" else ""
        }
        return m
    }

    /**
     * Times a code block.
     * @param title The label of this timed session.
     * @param block The code which is timed.
     * @return A string containing the title and elapsed duration in microseconds.
     */
    inline fun <T> time(title: String = "", block: () -> T): String {
        val start = System.nanoTime()
        block()
        val stop = System.nanoTime()
        val timeMicros =  (stop - start) / 1_000.0
        return if (title == "") "$timeMicros µs"  else "$title: $timeMicros µs"
    }

    /**
     * Clears the logs specified by [area]. Clears all the logs in the debugger by default.
     * @param area The area which Debug will clear the logs from.
     */
    fun clear(area: Area = Area.ALL) {
        if (area == Area.ALL) Areas = init() else Areas[area] = mutableListOf<String>()
    }

    /**
     * Returns the accumulated logs specified by [area]. Returns all logs by default.
     * @param area The area which Debug will return logs from.
     */
    fun getLogs(area: Area = Area.ALL): String {
        var report = ""
        if (area == Area.ALL) {
            for (a in Areas) if (a.key != Area.ALL) {
                report += prepareLogs(a.key, a.value)
            }
        } else {
            report += prepareLogs(area, Areas[area]!!)
        }
        return report
    }

    private fun prepareLogs(area: Area, log: List<String>): String {
        var report = StringBuilder()
        report.append("- - - - - - Showing logs for: $area - - - - - - \n")
        for (m in log) {
            report.append(m + "\n")
        }
        if (log.isEmpty()) report.append("No logs for this category.\n")
        return report.toString()
    }


}