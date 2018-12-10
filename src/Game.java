import java.util.ArrayList;

class Game {

    private static final int EXACT = 0;
    private static final int LOWERBOUND = 1;
    private static final int UPPERBOUND = 2;
    private static final int INFINITY = 40000;
    private static final int ttLastEntry = 0xffff; // 65535 (+1)

    private Board board;
    private ArrayList<Move> playedMoves;
    private int currentPlayer;
    private int index;
    private int threshold;
    private boolean stalemate;

    private HashEntry[] transpositionTable;

    // Debugging
    private int maxDepth;
    private int hits;

    Game(Board board, int threshold) {
        this.board = board;
        this.threshold = threshold;
        playedMoves = new ArrayList<>();
        currentPlayer = Color.WHITE;
        index = -1;
        stalemate = false;
        transpositionTable = new HashEntry[ttLastEntry + 1];
    }

    int getCurrentPlayer() {
        return currentPlayer;
    }

    private Move getLastMove() {
        return (index >= 0) ? playedMoves.get(index) : null;
    }

    private void applyMove(Move move) {
        board.applyMove(move);
        playedMoves.add(move);
        currentPlayer = -currentPlayer;
        index++;
    }

    private void unapplyMove(Move move) {
        board.unapplyMove(move);
        playedMoves.remove(index);
        currentPlayer = -currentPlayer;
        index--;
    }

    void makeMove(UI gameUI, boolean isComputerPlayer) {
        Bitboard bb = new Bitboard(board);
        Move move;
        Move[] validMoves = getAllValidMoves(bb, currentPlayer,
                board.getEnPassantColumn(getLastMove()));
        if (validMoves.length == 0) {
            System.out.println("Stalemate");
            stalemate = true;
            return;
        }
        if (isComputerPlayer) {
            System.out.println("Thinking...");
            move = AI(validMoves);
        } else {
            move = gameUI.getTextInput(validMoves);
        }
        System.out.println(move);
        applyMove(move);
    }

    void makeAIMove() {
        Bitboard bb = new Bitboard(board);
        Move[] validMoves = getAllValidMoves(bb, currentPlayer,
                board.getEnPassantColumn(getLastMove()));
        if (validMoves.length == 0) {
            stalemate = true;
            return;
        }

        System.out.println("Thinking...");
        Move move = AI(validMoves);
        System.out.println(move);
        applyMove(move);
    }

    boolean makeGUIMove(String san) {
        Bitboard bb = new Bitboard(board);
        Move[] validMoves = getAllValidMoves(bb, currentPlayer,
                board.getEnPassantColumn(getLastMove()));
        if (validMoves.length == 0) {
            stalemate = true;
            return true;
        }
        for (Move move : validMoves) {
            if (move.toString().equals(san)) {
                System.out.println(move);
                applyMove(move);
                return true;
            }
        }
        return false;
    }

    int getWinner() {
        if (stalemate) {
            System.out.println("Stalemate");
            return Color.DRAWFLAG;
        }
        for (int i = 0; i < 8; i++) {
            if (board.get(56 + i) == Color.WHITE) {
                System.out.println("White Wins");
                return Color.WHITE;
            } else if (board.get(i) == Color.BLACK) {
                System.out.println("Black Wins");
                return Color.BLACK;
            }
        }
        return Color.NONE;
    }

    // 'AI' *cough* *cough*
    private Move AI(Move[] validMoves) {
        long startTime = System.currentTimeMillis();
        int depth = 6;
        Move bestMove = validMoves[0];
        MoveEval minimax;
        maxDepth = 0;
        hits = 0;
        while (!runOutOfTime(startTime) && depth <= 100) {
            minimax = minimax(depth - 1, -INFINITY, INFINITY, currentPlayer, startTime, 0);
            if (minimax.move != null) {
                bestMove = minimax.move;
                if (minimax.winningMove) {
                    break;
                }
            }
            depth++;
        }
        System.out.println("Transpostion Table Hits = " + hits);
        System.out.println("Max depth = " + maxDepth);
        System.out.println();
        return bestMove;
    }

    private MoveEval minimax(int depth, int alpha, int beta, int col, long startTime, int debug) {
        Bitboard bb = new Bitboard(board);
        int alphaOrig = alpha;
        int betaOrig = beta;

        if (debug > maxDepth) {
            maxDepth = debug;
        }

        // This takes a few milliseconds so we can ignore the redundancy in the initial call
        Move[] validMoves = getAllValidMoves(bb, col,
                board.getEnPassantColumn(getLastMove()));


        if (board.getWhitePieces() == 0 || bb.blackOnRank1() > 0) {
            return new MoveEval(null, -INFINITY, false);
        }
        if (board.getBlackPieces() == 0 || bb.whiteOnRank8() > 0) {
            return new MoveEval(null, INFINITY, false);
        }

        // Stalemate
        if (validMoves.length == 0) {
            return new MoveEval(null, 0, false);
        }

        // Winning Move
        Move winningMove = pushPawn(bb, validMoves, col);
        if (winningMove != null) {
            // System.out.println(winningMove);
            return new MoveEval(winningMove, INFINITY * col, true);
        }

        // Terminal Node
        if (depth == 0 || runOutOfTime(startTime)) {
            return new MoveEval(null, Evaluation.staticEval(bb), false);
        }

        // Transposition Table
        long hash = board.getHash();
        HashEntry ttEntry = transpositionTable[(int) (hash & ttLastEntry)];
        if (ttEntry != null && ttEntry.equals(bb, hash) && ttEntry.depth > depth) {
            hits++;
            if (isValidMove(ttEntry.bestMove, validMoves)) {
                switch (ttEntry.flag) {
                    case LOWERBOUND:
                        alpha = Math.max(alpha, ttEntry.eval);
                        break;
                    case UPPERBOUND:
                        beta = Math.min(beta, ttEntry.eval);
                        break;
                    case EXACT:
                        return new MoveEval(ttEntry.bestMove, ttEntry.eval, false);
                }
                if (alpha >= beta) {
                    return new MoveEval(ttEntry.bestMove, ttEntry.eval, false);
                }
            }
        }

        int bestEval;
        int eval;
        Move bestMove = validMoves[0];
        if (col == Color.WHITE) {
            bestEval = -INFINITY;
            for (Move move : validMoves) {
//                for (int i = 0; i < debug; i++) {
//                    System.out.print("\t");
//                }
//                System.out.println(move + ":");
                applyMove(move);
                eval = minimax(depth - 1, alpha, beta, -col, startTime, debug + 1).eval;
//                System.out.println(eval);
                unapplyMove(move);
                if (eval > bestEval) {
                    bestEval = eval;
                    bestMove = move;
                }
                alpha = Math.max(alpha, bestEval);
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            bestEval = INFINITY;
            for (Move move : validMoves) {
//                for (int i = 0; i < debug; i++) {
//                    System.out.print("\t");
//                }
//                System.out.println(move + ":");
                applyMove(move);
                eval = minimax(depth - 1, alpha, beta, -col, startTime, debug + 1).eval;
//                System.out.println(eval);
                unapplyMove(move);
                if (eval < bestEval) {
                    bestEval = eval;
                    bestMove = move;
                }
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }
            }
        }

        ttEntry = new HashEntry();
        ttEntry.hash = hash;
        ttEntry.eval = bestEval;
        ttEntry.w = bb.w;
        ttEntry.b = bb.b;
        if (bestEval <= alphaOrig) {
            ttEntry.flag = UPPERBOUND;
        } else if (bestEval >= betaOrig) {
            ttEntry.flag = LOWERBOUND;
        } else {
            ttEntry.flag = EXACT;
        }
        ttEntry.depth = depth;
        ttEntry.bestMove = bestMove;
        transpositionTable[(int) (hash & ttLastEntry)] = ttEntry;
        return new MoveEval(bestMove, bestEval, false);
    }

    private Move pushPawn(Bitboard bb, Move[] validMoves, int col) {
        boolean isPassedPawn = false;
        int index = 0;
        int pawn = 0;
        int file;
        boolean push = true;
        if (col == Color.WHITE) {
            while (!isPassedPawn) {
                isPassedPawn = true;
                pawn = bb.getWhitePawns().get(index);
                file = pawn % 8;
                if (file == 7) { // A File
                    for (int row = 1 + pawn / 8; row < 8; row++) {
                        int i = row * 8 + file;
                        if (board.get(i) != Color.NONE
                                || board.get(i - 1) != Color.NONE) {
                            isPassedPawn = false;
                            break;
                        }
                    }
                } else if (file == 0) { // H File
                    for (int row = 1 + pawn / 8; row < 8; row++) {
                        int i = row * 8 + file;
                        if (board.get(i) != Color.NONE
                                || board.get(i + 1) != Color.NONE) {
                            isPassedPawn = false;
                            break;
                        }
                    }
                } else {
                    for (int row = 1 + pawn / 8; row < 8; row++) {
                        int i = row * 8 + file;
                        if (board.get(i) != Color.NONE
                                || board.get(i + 1) != Color.NONE
                                || board.get(i - 1) != Color.NONE) {
                            isPassedPawn = false;
                            break;
                        }
                    }
                }
                index++;
                if (index == bb.getWhitePawns().size()) {
                    break;
                }
            }
            if (isPassedPawn) {
                int wD = 7 - pawn / 8;
                for (int bp : bb.getBlackPawns()) {
                    int bD = bp / 8;
                    if (wD > bD) {
                        push = false;
                    }
                }
            }
        } else {
            while (!isPassedPawn) {
                isPassedPawn = true;
                pawn = bb.getBlackPawns().get(index);
                file = pawn % 8;
                if (file == 7) { // A File
                    for (int row = (pawn / 8) - 1; row >= 0; row--) {
                        int i = row * 8 + file;
                        if (board.get(i) != Color.NONE
                                || board.get(i - 1) != Color.NONE) {
                            isPassedPawn = false;
                            break;
                        }
                    }
                } else if (file == 0) { // H File
                    for (int row = (pawn / 8) - 1; row >= 0; row--) {
                        int i = row * 8 + file;
                        if (board.get(i) != Color.NONE
                                || board.get(i + 1) != Color.NONE) {
                            isPassedPawn = false;
                            break;
                        }
                    }
                } else {
                    for (int row = (pawn / 8) - 1; row >= 0; row--) {
                        int i = row * 8 + file;
                        if (board.get(i) != Color.NONE
                                || board.get(i + 1) != Color.NONE
                                || board.get(i - 1) != Color.NONE) {
                            isPassedPawn = false;
                            break;
                        }
                    }
                }
                index++;
                if (index == bb.getBlackPawns().size()) {
                    break;
                }
            }
            if (isPassedPawn) {
                int bD = pawn / 8;
                for (int wp : bb.getWhitePawns()) {
                    int wD = 7 - (wp / 8);
                    if (bD > wD) {
                        push = false;
                    }
                }
            }
        }
        if (isPassedPawn && push) {
            for (Move move : validMoves) {
                if (move.getFrom() == pawn && !move.isCapture() && !move.isDoublePush()) {
                    return move;
                }
            }
        }
        return null;
    }


    // Move Generation
    private static Move[] getAllValidMoves(Bitboard bb, int color, int enPassantColumn) {
        ArrayList<Move> moves = new ArrayList<>();
        if (color == Color.WHITE) {
            long whiteSinglePush = bb.whiteSinglePush();
            long whiteDoublePush = bb.whiteDoublePush();
            long whiteCaptureL = bb.whiteCaptureLeft();
            long whiteCaptureR = bb.whiteCaptureRight();
            int i = 0;
            IntLong fb;
            if (enPassantColumn >= 0) {
                long whiteEnPassantL = bb.whiteEnPassantLeft(enPassantColumn);
                long whiteEnPassantR = bb.whiteEnPassantRight(enPassantColumn);
                if (whiteEnPassantL > 0) {
                    fb = Bitboard.FirstBit(whiteEnPassantL);
                    moves.add(new Move(fb.i - 9, fb.i, true, true));
                }
                if (whiteEnPassantR > 0) {
                    fb = Bitboard.FirstBit(whiteEnPassantR);
                    moves.add(new Move(fb.i - 7, fb.i, true, true));
                }
            }
            while (whiteCaptureL > 0) {
                fb = Bitboard.FirstBit(whiteCaptureL);
                i = fb.i + i;
                whiteCaptureL = fb.l;
                moves.add(new Move(i - 9, i, true, false));
            }
            i = 0;
            while (whiteCaptureR > 0) {
                fb = Bitboard.FirstBit(whiteCaptureR);
                i = fb.i + i;
                whiteCaptureR = fb.l;
                moves.add(new Move(i - 7, i, true, false));
            }
            i = 0;
            while (whiteDoublePush > 0) {
                fb = Bitboard.FirstBit(whiteDoublePush);
                i = fb.i + i;
                whiteDoublePush = fb.l;
                moves.add(new Move(i - 16, i, false, false));
            }
            i = 0;
            // When msb is 2 ^ 64
            if (whiteSinglePush < 0) {
                moves.add(new Move(55, 63, false, false));
                whiteSinglePush &=
                        0b0111111111111111111111111111111111111111111111111111111111111111L;
            }
            while (whiteSinglePush > 0) {
                fb = Bitboard.FirstBit(whiteSinglePush);
                i = fb.i + i;
                whiteSinglePush = fb.l;
                moves.add(new Move(i - 8, i, false, false));
            }
        } else {
            long blackSinglePush = bb.blackSinglePush();
            long blackDoublePush = bb.blackDoublePush();
            long blackCaptureL = bb.blackCaptureLeft();
            long blackCaptureR = bb.blackCaptureRight();
            int i = 0;
            IntLong fb;
            if (enPassantColumn >= 0) {
                long blackEnPassantL = bb.blackEnPassantLeft(enPassantColumn);
                long blackEnPassantR = bb.blackEnPassantRight(enPassantColumn);
                if (blackEnPassantL > 0) {
                    fb = Bitboard.FirstBit(blackEnPassantL);
                    moves.add(new Move(fb.i + 7, fb.i, true, true));
                }
                if (blackEnPassantR > 0) {
                    fb = Bitboard.FirstBit(blackEnPassantR);
                    moves.add(new Move(fb.i + 9, fb.i, true, true));
                }
            }
            while (blackCaptureL > 0) {
                fb = Bitboard.FirstBit(blackCaptureL);
                i = fb.i + i;
                blackCaptureL = fb.l;
                moves.add(new Move(i + 7, i, true, false));
            }
            i = 0;
            while (blackCaptureR > 0) {
                fb = Bitboard.FirstBit(blackCaptureR);
                i = fb.i + i;
                blackCaptureR = fb.l;
                moves.add(new Move(i + 9, i, true, false));
            }
            i = 0;
            while (blackDoublePush > 0) {
                fb = Bitboard.FirstBit(blackDoublePush);
                i = fb.i + i;
                blackDoublePush = fb.l;
                moves.add(new Move(i + 16, i, false, false));
            }
            i = 0;
            while (blackSinglePush > 0) {
                fb = Bitboard.FirstBit(blackSinglePush);
                i = fb.i + i;
                blackSinglePush = fb.l;
                moves.add(new Move(i + 8, i, false, false));
            }
        }
        Move[] m = new Move[moves.size()];
        moves.toArray(m);
        return m;
    }

    private boolean runOutOfTime(long startTime) {
        return System.currentTimeMillis() - startTime > threshold;
    }

    private boolean isValidMove(Move move, Move[] validMoves) {
        String san = move.toString();
        for (Move vm : validMoves) {
            if (san.equals(vm.toString())) {
                return true;
            }
        }
        return false;
    }
}
