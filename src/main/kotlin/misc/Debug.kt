package misc
object Debug {
    var active = true
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

    private var Areas = init()

    private fun init(): MutableMap<Area, MutableList<String>> {
        return mutableMapOf<Area, MutableList<String>>(
            Area.BOARD to mutableListOf(),
            Area.COLOR to mutableListOf(),
            Area.PIECE to mutableListOf(),
            Area.FEN_STRINGS to mutableListOf(),
            Area.PARSER to mutableListOf(),
            Area.GUI to mutableListOf(),
            Area.GAME to mutableListOf(),
            Area.ALL to mutableListOf(),
        )
    }


    fun log(area: Area = Area.ALL, message: () -> String) {
        if (active) {
            Areas[area]?.add(message())
            if (area != Area.ALL) Areas[Area.ALL]?.add(message())
        }
    }

    fun getLogs(area: Area = Area.ALL): String {
        var report = ""
        if (area == Area.ALL) {
            for (a in Areas) if (a.key != Area.ALL) report += prepareLogs(a.key, a.value)
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

    fun clear(area: Area = Area.ALL) {
        if (area == Area.ALL) Areas = init() else Areas[area] = mutableListOf<String>()
    }




}