import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

class Game {

    private static final int EXACT = 0;
    private static final int LOWERBOUND = 1;
    private static final int UPPERBOUND = 2;
    private static final int INFINITY = 40000;

    // 65535 (+1) ~safe
    private static final int ttLastEntry = 0xfffff; // 1048575 (+1)

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
        initializeTranspositionTable();
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
//                System.out.println(minimax.move);
                if (minimax.winningMove) {
                    break;
                }
            }
            depth++;
        }
        System.out.println("Transpostion Table Hits = " + hits);
        System.out.println("Max depth = " + maxDepth);
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
        MoveEval winningMove = pushPawn(bb, validMoves, col);
        if (winningMove != null) {
//            System.out.println(winningMove.move);
            return winningMove;
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
        int bestInitialEval;
        int initialEval;
        Move bestMove = validMoves[0];
        if (col == Color.WHITE) {
            bestEval = -INFINITY;
            bestInitialEval = -INFINITY;
            for (Move move : validMoves) {
//                for (int i = 0; i < debug; i++) {
//                    System.out.print("\t");
//                }
//                System.out.println(move + ":");
                applyMove(move);
                initialEval = Evaluation.initialEval(new Bitboard(board),
                        board.getEnPassantColumn(move));
                eval = minimax(depth - 1, alpha, beta, -col, startTime, debug + 1).eval;
//                System.out.println(eval);
                unapplyMove(move);
                if (eval > bestEval || (eval == bestEval && initialEval > bestInitialEval)) {
                    bestEval = eval;
                    bestMove = move;
                    bestInitialEval = initialEval;
                }
                alpha = Math.max(alpha, bestEval);
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            bestEval = INFINITY;
            bestInitialEval = INFINITY;
            for (Move move : validMoves) {
//                for (int i = 0; i < debug; i++) {
//                    System.out.print("\t");
//                }
//                System.out.println(move + ":");
                applyMove(move);
                initialEval = Evaluation.initialEval(new Bitboard(board),
                        board.getEnPassantColumn(move));
                eval = minimax(depth - 1, alpha, beta, -col, startTime, debug + 1).eval;
//                System.out.println(eval);
                unapplyMove(move);
                if (eval < bestEval || (eval == bestEval && initialEval < bestInitialEval)) {
                    bestEval = eval;
                    bestMove = move;
                    bestInitialEval = initialEval;
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

    private MoveEval pushPawn(Bitboard bb, Move[] validMoves, int col) {
        boolean push = false;
        int index = 0;
        int pawn = 0;
        boolean confirmPush = true;
        if (col == Color.WHITE) {
            while (!push && index < bb.getWhitePawns().size()) {
                pawn = bb.getWhitePawns().get(index);
                push = bb.isWhitePassedPawn(pawn) || bb.isWhitePassedPawn(pawn + 8)
                        || (pawn / 8 >= 4 && bb.isWhiteCandidate(pawn)
                        && bb.blackAttackers(pawn + 8) <= bb.whiteAttackers(pawn + 8));
                index++;
            }
            if (push) {
                int wD = 7 - pawn / 8;
                for (int bp : bb.getBlackPawns()) {
                    int bD = bp / 8;
                    if (wD > bD) {
                        confirmPush = false;
                    }
                }
            }
        } else {
            while (!push && index < bb.getBlackPawns().size()) {
                pawn = bb.getBlackPawns().get(index);
                push = bb.isBlackPassedPawn(pawn) || bb.isBlackPassedPawn(pawn - 8)
                        || (pawn / 8 <= 3 && bb.isBlackCandidate(pawn)
                        && bb.whiteAttackers(pawn - 8) <= bb.blackAttackers(pawn - 8));
                index++;
            }
            if (push) {
                int bD = pawn / 8;
                for (int wp : bb.getWhitePawns()) {
                    int wD = 7 - (wp / 8);
                    if (bD > wD) {
                        confirmPush = false;
                    }
                }
            }
        }
        if (push && confirmPush) {
            for (Move move : validMoves) {
                if (move.getFrom() == pawn && !move.isCapture() && !move.isDoublePush()) {
                    return new MoveEval(move, INFINITY * col, true);
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

    private void initializeTranspositionTable() {
        transpositionTable = new HashEntry[ttLastEntry + 1];
//        Board fake = new Board('h', 'a');
//        Move[] whiteMoves1 = getAllValidMoves(new Bitboard(fake), 1, -1);
//        for (Move wm1 : whiteMoves1) {
//            fake.applyMove(wm1);
//            if (wm1.toString().equals("a2-a4")) {
//
//            } else {
//
//            }
//            fake.unapplyMove(wm1);
//        }
    }

    private void addEntry(Board brd, Move bestMove) {
        Bitboard bb = new Bitboard(brd);
        long hash = brd.getHash();
        transpositionTable[(int) (hash & ttLastEntry)] = new HashEntry(hash, bb.w, bb.b,
                INFINITY, INFINITY, bestMove);
    }
}
