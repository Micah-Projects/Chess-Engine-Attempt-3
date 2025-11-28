package model.movement

class MoveBuffer : ReadOnlyMoveBuffer {
    override var pointer: Int = 0
    override fun get(index: Int): Move {
        return if (index < pointer) buffer[index] else throw IndexOutOfBoundsException()
    }

    private val buffer = IntArray(218) { 0 }
    private val resultList: MutableList<Int> = mutableListOf()

    var inPlaceOps: Boolean = false

    fun add(move: Move) {
        buffer[pointer++] = move
    }

    fun clear() {
        pointer = 0
    }

    override fun getList(copy: Boolean): List<Move> {
        filter { true }
        return if (copy) resultList else resultList.toList()
    }

    override fun forEach(action: (Move) -> Unit) {
        var i = 0
        while (i < pointer) {
            action(buffer[i++])
        }
    }

    override fun filter(criterion: (Move) -> Boolean): List<Move> {
        resultList.clear()
        var i = 0
        while (i < pointer) {
            val move = buffer[i++]
            if (criterion(move)) {
                resultList.add(move)
            }
        }
        return if (inPlaceOps) resultList else resultList.toList()
    }


    override fun <R : Comparable<R>> arrangeBy(ascending: Boolean, criterion: ((Move) -> R?)): List<Move> {
        filter { true } // just gets the move list
        if (ascending) resultList.sortBy(criterion) else resultList.sortByDescending(criterion)
        return if (inPlaceOps) resultList else resultList.toList()
    }

}