class HashEntry {
    long hash;
    long w;
    long b;
//    private boolean blackToPlay;
    int depth;
    int flag;
    int eval;
    Move bestMove;

    HashEntry() {

    }

    HashEntry(long hash, long w, long b, int depth, int eval, Move bestMove) {
        this.hash = hash;
        this.w = w;
        this.b = b;
        this.depth = depth;
        this.eval = eval;
        this.bestMove = bestMove;
    }

    boolean equals(Bitboard bb, long otherHash) {
        return bb.w == w && bb.b == b && otherHash == hash;
    }

//    HashEntry(Bitboard bb, int color) {
//        w = bb.w;
//        b = bb.b;
//        blackToPlay = color == Color.BLACK;
//    }

//    // White can't be in the bottom-most row hence we shift it down for the first few bits
//    // of the hash which will be used in indexing to be more variant (less rewriting)
//    long getHash() {
//        return ~(b << 8) ^ (w >> 8);
//    }

//    boolean equals(HashEntry other) {
//        return w == other.w && b == other.b && blackToPlay == other.blackToPlay;
//    }
}