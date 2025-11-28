package model.movement

interface ReadOnlyMoveBuffer {
    val pointer: Int
    operator fun get(index: Int): Move

    fun forEach(action: (Move) -> Unit)

    fun getList(copy: Boolean = true): List<Move>

    fun filter(criterion: (Move) -> Boolean): List<Move>

    fun <R : Comparable<R>> arrangeBy(ascending: Boolean = false, criterion: ((Move) -> R?)): List<Move>

}