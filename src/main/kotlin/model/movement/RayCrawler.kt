package model.movement


import model.board.Piece
import model.board.Piece.Type
import model.misc.BitBoard
import model.misc.BitBoards
import model.misc.Squares
import model.misc.square
import kotlin.math.sign

/**
 * Ray crawling helper object for move generation.
 * Simply takes arguments for a ray cast and returns a corresponding move BitBoard.
 */
object RayCrawler {
    const val MAX_DISTANCE = 7
    const val DIRECTIONAL_VECTORS = 4
    const val DIRECTION_COUNT = 8

    val diagonals = listOf(-9, -7, 7, 9)
    val horizontals = listOf( -1, 1,)
    val verticals = listOf(-8, 8)
    val downDiagonal = listOf(-7, 7)
    val upDiagonal = listOf(-9, 9)

    val knights = listOf(-17, -15, -10,-6, 6, 10, 15, 17)
    val queens = verticals + horizontals + diagonals
    val rooks = verticals + horizontals
    val pawnAttacks = listOf(7, 9)

    val directionalVectors = listOf(verticals, horizontals, upDiagonal, downDiagonal)


    fun directionIndexOf(direction: List<Int>): Int {
        require(direction.size != 2) {"not a valid ray crawler direction. Directions must be of size 2"}
        return directionalVectors.indexOf(direction)
    }

    data class RayData(val origin: square, val current: square, val next: square, val direction: Int, val distance: Int)

    /**
     * Crawls a ray with instructions given by specified parameters.
     * @param from The origin position of the ray.
     * @param directions A list of directions that the crawler will traverse in.
     * @param accumulateIf A condition at which the crawler should count its current position.
     * @param stopDirectionIf A condition that when met, the crawler will terminate its current direction.
     * @param stopAllIf A condition that when met, the crawler will terminate its process.
     * @param maxDist How far the crawler is allowed to go in any direction.
     */
    inline fun crawlRays(
        from: square,
        directions: List<Int>,
        accumulateIf: (RayData.() -> Boolean) = { true },
        stopDirectionIf: (RayData.() -> Boolean) = { false },
        stopAllIf: (RayData.() -> Boolean) = { false },
        maxDist: Int = MAX_DISTANCE,
        includeStart: Boolean = false,
        onStep: (RayData.()-> Unit) = {}
    ): ULong {
        var result = 0uL
        var stopAll = false
        for (direction in directions) {
            if (stopAll) break
            for (distance in 0 .. maxDist) {
                val current = from + (direction * distance)
                val next = from + (direction * (distance + 1))
                if (!Squares.isInBounds(current)) {
                    break
                }

                val data = RayData(from, current, next, direction, distance)
                onStep(data)
                val willWrap = willWrap(current, next, direction)
                val stopThisDirection = stopDirectionIf(data)
                stopAll = stopAllIf(data)

                if (accumulateIf(data) && (from != current || includeStart)) {
                    result = BitBoards.addBit(result, current)
                }

                if (stopAll || stopThisDirection || willWrap) {
                    break
                }
            }
        }
        return result
    }

    /**
     * Attempts to draw a straight or diagonal line from point A to point B. if a line cant be found, it returns
     * an empty [BitBoard]. The line includes both the start and end positions
     */
    fun lineTo(from: square, to: square, includeStart: Boolean = true, includeEnd: Boolean = true): BitBoard {
        val direction = getDirection(from, to)
        if (direction !in queens) return 0uL
        return crawlRays(from, listOf(direction), includeStart = includeStart, stopDirectionIf = {
            current == to && includeEnd || !includeEnd && next == to
           // next == tox
        })
    }


    fun getDirection(from: square, to: square): Int {
        val sign = (to - from).sign
        val lesserRank =  Squares.rankOf(from) < Squares.rankOf(to)
        val lesserFile =  Squares.fileOf(from) < Squares.fileOf(to)
        val greaterRank = Squares.rankOf(from) > Squares.rankOf(to)
        val greaterFile = Squares.fileOf(from) > Squares.fileOf(to)
        val sameRank = Squares.isOnSameRank(from, to)
        val sameFile = Squares.isOnSameFile(from, to)
        val onDiag = Squares.isOnDiagonal(from, to)
        return when {
            lesserRank && lesserFile && onDiag -> 9
            greaterFile && greaterRank && onDiag -> -9
            greaterRank && lesserFile && onDiag -> -7
            greaterFile && lesserRank && onDiag -> 7
            sameRank && !sameFile -> 1 * sign
            sameFile && !sameRank -> 8 * sign
            else -> 0
        }
    }

    fun leap(from: square, directions: List<Int>, maxDist: Int = 1): ULong {
        return crawlRays(from, directions, maxDist = maxDist)
    }

    fun crawlUntilBlockers(blockers: ULong, startFrom: square, directions: List<Int>, includeStart: Boolean, maxDist: Int = MAX_DISTANCE): ULong {
        return crawlRays(startFrom,  directions,
            accumulateIf = { true },
            stopDirectionIf = { BitBoards.hasBit(blockers, this.current) },
            maxDist = maxDist,
            includeStart = includeStart
        )
    }

    fun crawlExcludeEdge(from: square, directions: List<Int>): ULong {
        return crawlRays(from, directions, stopDirectionIf = {
            !Squares.isInBounds(next) ||
                    direction in horizontals && Squares.isOnSideEdge(next) ||
                    direction in verticals && Squares.isOnVerticalEdge(next) ||
                    direction in diagonals && Squares.isOnEdge(next)
        },
            includeStart = false)
    }

    fun getRays(piece: Piece): List<Int> {
        return when {
            piece.isRook() -> rooks
            piece.isQueen() -> queens
            piece.isBishop() -> diagonals
            piece.isKnight() -> knights
         //   piece.isKing() -> RayCrawler
            else -> listOf()
        }
    }
    fun getRays(pieceType: Type): List<Int> {
        return when (pieceType) {
            Type.ROOK -> rooks
            Type.QUEEN -> queens
            Type.BISHOP -> diagonals
            Type.KNIGHT -> knights
            //   piece.isKing() -> RayCrawler
            else -> listOf()
        }
    }

    fun willWrap(current: square, next: square, direction: Int): Boolean {
        if (!Squares.isInBounds(current) || !Squares.isInBounds(next)) return false
            if (Squares.fileDist(current, next) > 2) {
                return true
            }
        return false
    }
}