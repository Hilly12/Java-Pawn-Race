import java.security.*;


// Turns out using Zobrist hashing is not too much better
// than storing 2 longs along with a boolean which would result
// in a 0 probability of collisions
// Using Zobrist, however would result in a greater maximum depth searched...
// Both options have their advantages and disadvantages
// I have chosen to use a combination of both for safety
class Zobrist {
    static long zArray[][] = new long[2][64];
    static long zBlackMove;

    private static long random64() {
        SecureRandom random = new SecureRandom();
        return random.nextLong();
    }

    static void GenerateRandomNumbers() {
        for (int color = 0; color < 2; color++) {
            for (int i = 0; i < 64; i++) {
                zArray[color][i] = random64();
            }

        }
        zBlackMove = random64();
    }

    static long getZobristHash(int[] board) {
        long hash = 0;
        for (int i = 0; i < 64; i++) {
            if (board[i] == 1) {
                hash ^= zArray[0][i];
            } else if (board[i] == -1) {
                hash ^= zArray[1][i];
            }
        }
        return hash;
    }
}