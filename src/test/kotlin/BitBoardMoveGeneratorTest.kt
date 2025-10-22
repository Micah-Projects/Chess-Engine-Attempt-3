import model.misc.BitBoards
import model.misc.Squares
import model.misc.square

class BitBoardMoveGeneratorTest {
//    var blockerMask = BitBoards.EMPTY_BB
//    val square = Squares.random()
//    val blockers = mutableListOf<square>()
//    val validSquares = getSetIndices(rookMasks[square])
//
//
//
//    for (i in 0..Random.nextInt(1, validSquares.size)) {
//        var pick = validSquares.random()
//        while(pick in blockers) {
//            pick = validSquares.random()
//        }
//        blockers.add(pick)
//    }
//
//    println("rook on ${Squares.asText(square)} move mask with empty board:")
//    BitBoards.print(rookMasks[square])
//    val text = StringBuilder()
//    for (blocker in blockers) {
//        blockerMask = BitBoards.addBit(blockerMask, blocker)
//        text.append(Squares.asText(blocker) + ", ")
//    }
//
//
//    println("rook on ${Squares.asText(square)} move mask with blockers on $text:")
//    BitBoards.print(rookAttackMap[square][indexFromOccupancy(square, rookMasks[square] and blockerMask)])

}