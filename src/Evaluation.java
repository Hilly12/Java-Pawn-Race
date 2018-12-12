import java.util.ArrayList;

class Evaluation {

    private static int[] linearMap = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            32, 30, 20, 32, 32, 20, 30, 32,
            31, 30, 10, 30, 30, 10, 30, 31,
            15, 15, 0, 15, 15, 0, 15, 15,
            -5, 5, 10, 0, 0, 10, 5, -5,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    static int staticEval(Bitboard bb) {
        return (bb.getWhitePawns().size() - bb.getBlackPawns().size()) * 100;
        /* evaluatePawnStructure(bb, bb.getWhitePawns(), bb.getBlackPawns());*/
    }

    static int initialEval(Bitboard bb, int enPassantColumn) {
        int eval = 0;
        for (int pawn : bb.getWhitePawns()) {
            if (enPassantColumn != -1) {
                if (bb.blackEnPassantLeft(enPassantColumn) > 0
                        || bb.blackEnPassantRight(enPassantColumn) > 0) {
                    continue;
                }
            }
            if (bb.blackAttackers(pawn) <= bb.whiteAttackers(pawn) && !bb.whiteIsolated(pawn)) {
                eval += 200 + linearMap[63 - pawn];
                eval += bb.whiteAttackers(pawn);
                if (bb.isWhiteCandidate(pawn)) {
                    eval += pawn / 8;
                }
            }
        }
        for (int pawn : bb.getBlackPawns()) {
            if (enPassantColumn != -1) {
                if (bb.whiteEnPassantLeft(enPassantColumn) > 0
                        || bb.whiteEnPassantRight(enPassantColumn) > 0) {
                    continue;
                }
            }
            if (bb.whiteAttackers(pawn) <= bb.blackAttackers(pawn) && !bb.blackIsolated(pawn)) {
                eval -= 200 + linearMap[pawn];
                eval -= bb.blackAttackers(pawn);
                if (bb.isBlackCandidate(pawn)) {
                    eval -= 7 - (pawn / 8);
                }
            }
        }
        return eval;
    }

    private static int evaluatePawnStructure(Bitboard bb,
                                             ArrayList<Integer> whitePawns,
                                             ArrayList<Integer> blackPawns) {
        int[] pawnChainMaskWhite = new int[64];
        int[] pawnChainMaskBlack = new int[64];
        int pawnChainScore = 0;
        int enPassantScore = 0;
        int column;
        int pawn;
        for (int i = whitePawns.size() - 1; i >= 0; i--) {
            pawn = whitePawns.get(i);
            if (pawn / 8 == 4) {
                enPassantScore++;
            }
            column = 7 - (pawn % 8);
            if (column == 0) { // A
                pawnChainMaskWhite[pawn] = pawnChainMaskWhite[pawn - 9] + 1;
            } else if (column == 7) { // H
                pawnChainMaskWhite[pawn] = pawnChainMaskWhite[pawn - 7] + 1;
            } else {
                pawnChainMaskWhite[pawn] = pawnChainMaskWhite[pawn - 9]
                        + pawnChainMaskWhite[pawn - 7] + 1;
            }
            pawnChainScore += pawnChainMaskWhite[pawn];
        }
        for (int i = blackPawns.size() - 1; i >= 0; i--) {
            pawn = blackPawns.get(i);
            column = 7 - (pawn % 8);
            if (pawn / 8 == 3) {
                enPassantScore--;
            }
            if (column == 0) {
                pawnChainMaskBlack[pawn] = pawnChainMaskBlack[pawn + 7] + 1;
            } else if (column == 7) {
                pawnChainMaskBlack[pawn] = pawnChainMaskBlack[pawn + 9] + 1;
            } else {
                pawnChainMaskBlack[pawn] = pawnChainMaskBlack[pawn + 7] +
                        pawnChainMaskBlack[pawn + 9] + 1;
            }
            pawnChainScore -= pawnChainMaskBlack[pawn];
        }
        return pawnChainScore + enPassantScore * 25;
    }
}
