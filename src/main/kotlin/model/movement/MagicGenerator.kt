package model.movement

import model.board.Piece
import model.misc.BitBoard
import model.misc.BitBoards.binaryFill
import model.misc.Squares
import model.misc.square
import java.io.File
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.system.exitProcess


object MagicGenerator {
val mgSliders = 2
    private val filePath = "src/main/kotlin/model/movement/MagicKeys.txt"
    private var hasCache = false
    private var currentCache: Array<Array<ULong>> = Array(0) { Array(0) { 0uL } }

    fun getMagicKeys() : Array<Array<ULong>> {
        if (hasCache) {
            return currentCache
        }
        currentCache = readMagics()
        hasCache = true
        return currentCache
    }

    private fun readMagics(): Array<Array<ULong>> {
        val file = File(filePath)
        require(file.exists()) {
            "Cannot retrieve magic keys. Must call genMagics() first."
        }
        val keys = Array<Array<ULong>>(mgSliders) { idx ->
            val magicLines = file.readLines().filter { it.split(" ")[0] == idx.toString() }
            Array<ULong>(ULong.SIZE_BITS) { square ->
                magicLines[square].split(" ")[1].toULong()
            }
        }
        return keys
    }

    fun clearCache() {
        hasCache = false
    }

    fun genMagics() {
        try {
            val file = File(filePath)
            if (file.exists()) {
                return
            } else {
                val sliderMasks = Array<Array<ULong>>(mgSliders) { idx ->
                    val piece = Piece.sliders[idx]
                    Array(Squares.COUNT) { square ->
                        RayCrawler.crawlExcludeEdge(square, RayCrawler.getRays(piece))
                    }
                }
                val sliderBlockerSets = Array<Array<Array<ULong>>>(mgSliders) { idx ->
                    Array(Squares.COUNT) { square ->
                        binaryFill(sliderMasks[idx][square])
                    }
                }
                val sliderMagics = Array<Array<ULong>>(mgSliders) { idx ->
                    val piece = Piece.sliders[idx] // will be rook or bishop
                    Array(Squares.COUNT) { square ->
                        var found = false
                        var magic: ULong = 0uL
                        val blockersForSquare = sliderBlockerSets[idx][square] // the possible combinations from that square
                        val neededBits =  sliderMasks[idx][square].countOneBits() //log(blockersForSquare.size.toDouble(),2.0) // bit count to squeeze indices into
                        val shiftFactor = ULong.SIZE_BITS - neededBits.toInt()

                        while (!found) {
                            val foundHashes = HashSet<Int>()
                            magic = Random.nextULong() and Random.nextULong() and Random.nextULong()

                            for (blockerConfig in blockersForSquare) {
                                val occupancy = blockerConfig and sliderMasks[idx][square]
                                val hashValue = ((magic * occupancy) shr shiftFactor).toUInt().toInt()
                                if (hashValue !in foundHashes) {
                                    // magic is good so far
                                    foundHashes.add(hashValue)
                                } else {
                                    // collision, bad magic
                                    break
                                }
                            }

                            if (foundHashes.size == blockersForSquare.size) {
                                //  println("magic: $magic found for ${Squares.asText(square)}!")
                                found = true
                            }

                        }

                        magic
                    }
                }
                file.createNewFile()
                for ((sliderIdx, magics) in sliderMagics.withIndex()) {
                    for (magic in magics) {
                        file.appendText("$sliderIdx $magic\n")
                    }
                }
            }
        } catch (e: Exception) {
            println("An error occurred when generating magic keys: ${e.message}")
            exitProcess(1)
        }
    }
}