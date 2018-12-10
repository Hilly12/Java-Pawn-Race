import java.util.ArrayList;

class Evaluation {

    static int staticEval(Bitboard bb) {
        return (bb.getWhitePawns().size() - bb.getBlackPawns().size())
                + evaluatePawnChains(bb.getWhitePawns(), bb.getBlackPawns());
    }

    private static int evaluatePawnChains(ArrayList<Integer> whitePawns,
                                          ArrayList<Integer> blackPawns) {
        int[] pawnChainMaskWhite = new int[64];
        int[] pawnChainMaskBlack = new int[64];
        int pawnChainScore = 0;
        for (int pawn : whitePawns) {
            if (pawn % 8 == 7) {
                pawnChainMaskWhite[pawn] = pawnChainMaskWhite[pawn - 9] + 1;
            } else if (pawn % 8 == 0) {
                pawnChainMaskWhite[pawn] = pawnChainMaskWhite[pawn - 7] + 1;
            } else {
                pawnChainMaskWhite[pawn] = pawnChainMaskWhite[pawn - 9]
                        + pawnChainMaskWhite[pawn - 7] + 1;
            }
            pawnChainScore += pawnChainMaskWhite[pawn];
        }
        for (int pawn : blackPawns) {
            if (pawn % 8 == 7) {
                pawnChainMaskBlack[pawn] = pawnChainMaskBlack[pawn + 7] + 1;
            } else if (pawn % 8 == 0) {
                pawnChainMaskBlack[pawn] = pawnChainMaskBlack[pawn + 9] + 1;
            } else {
                pawnChainMaskBlack[pawn] = pawnChainMaskBlack[pawn + 7] +
                        pawnChainMaskBlack[pawn + 9] + 1;
            }
            pawnChainScore -= pawnChainMaskBlack[pawn];
        }
        return pawnChainScore;
    }
}
