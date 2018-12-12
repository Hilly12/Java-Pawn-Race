class Board {
    private int[] board;
    private int whitePieces;
    private int blackPieces;
    private long hash;

    Board(char whiteGap, char blackGap) {
        board = new int[64];

        for (int i = 0; i < 64; i++) {
            if (i / 8 == 1) {
                board[i] = Color.WHITE;
            } else if (i / 8 == 6) {
                board[i] = Color.BLACK;
            }
        }
        board[15 - (whiteGap - 'a')] = Color.NONE;
        board[55 - (blackGap - 'a')] = Color.NONE;

        // Turing Test (Black always wins if white plays c2-c3)
//        int[] n = {
//                0, 0, 0, 0, 0, 0, 0, 0,
//                0, 0, 0, 0, 0, 1, 0, 0,
//                0, 0, 0, 0, 0, 0, 1, 1,
//                0, 0, 0, 0, 0, 0, 0, 0,
//                0, 0, 0, 0, 0, -1, -1, -1,
//                0, 0, 0, 0, 0, 0, 0, 0,
//                0, 0, 0, 0, 0, 0, 0, 0,
//                0, 0, 0, 0, 0, 0, 0, 0
//        };
//        board = n.clone();

        whitePieces = 7;
        blackPieces = 7;

        hash = Zobrist.getZobristHash(board, false);
    }

    int get(int i) {
        return board[i];
    }

    int getWhitePieces() {
        return whitePieces;
    }

    int getBlackPieces() {
        return blackPieces;
    }

    long getHash() {
        return hash;
    }

    int getEnPassantColumn(Move lastMove) {
        if (lastMove == null || !lastMove.isDoublePush()) {
            return -1;
        }
        return 7 - (lastMove.getTo() % 8);
    }

    void applyMove(Move move) {
        int occupier = board[move.getFrom()];
        int col = (occupier + 1) / 2; // (white = 1 = 1, black = -1 = 0)

        board[move.getTo()] = occupier;
        hash ^= Zobrist.zArray[col][move.getTo()];

        board[move.getFrom()] = Color.NONE;
        hash ^= Zobrist.zArray[col][move.getFrom()];

        if (move.isEnPassantCapture()) {
            int ep = move.getTo() - occupier * 8;
            board[ep] = Color.NONE;
            hash ^= Zobrist.zArray[1 - col][ep];
            whitePieces -= 1 - col;
            blackPieces -= col;
        } else if (move.isCapture()) {
            hash ^= Zobrist.zArray[1 - col][move.getTo()];
            whitePieces -= 1 - col;
            blackPieces -= col;
        }

        hash ^= Zobrist.zBlackMove;
    }

    void unapplyMove(Move move) {
        int occupier = board[move.getTo()];
        int other = -occupier;
        int col = (occupier + 1) / 2;

        board[move.getTo()] = Color.NONE;
        hash ^= Zobrist.zArray[col][move.getTo()];

        board[move.getFrom()] = occupier;
        hash ^= Zobrist.zArray[col][move.getFrom()];

        if (move.isEnPassantCapture()) {
            int ep = move.getTo() - occupier * 8;
            board[ep] = other;
            hash ^= Zobrist.zArray[1 - col][ep];
            whitePieces += 1 - col;
            blackPieces += col;
        } else if (move.isCapture()) {
            board[move.getTo()] = other;
            hash ^= Zobrist.zArray[1 - col][move.getTo()];
            whitePieces += 1 - col;
            blackPieces += col;
        }

        hash ^= Zobrist.zBlackMove;
    }

    void display() {
        System.out.println(this);
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            if (i % 8 == 0) {
                out.append("  ---------------------------------\n");
                out.append((char) ('1' + (7 - (i / 8))));
                out.append(" ");
            }
            out.append("| ");
            if (board[63 - i] == Color.WHITE) {
                out.append("W");
            } else if (board[63 - i] == Color.BLACK) {
                out.append("B");
            } else {
                out.append(" ");
            }
            out.append(" ");
            if (i % 8 == 7) {
                out.append("|\n");
            }
        }
        out.append("  ---------------------------------\n");
        out.append("  ");
        for (int i = 0; i < 8; i++) {
            out.append("  ");
            out.append((char) ('a' + i));
            out.append(" ");
        }
        out.append(" \n\n");
        return out.toString();
    }
}
