import java.util.ArrayList;
import java.util.Collections;

class Bitboard {

    private static final long row1 = 0x00000000000000ffL;
    private static final long row2 = 0x000000000000ff00L;
    private static final long row4 = 0x00000000ff000000L;
    private static final long row5 = 0x000000ff00000000L;
    private static final long row7 = 0x00ff000000000000L;
    private static final long row8 = 0xff00000000000000L;
    private static final long notAFile = 0x7f7f7f7f7f7f7f7fL;
    private static final long notHFile = 0xfefefefefefefefeL;
//    private static final long whiteSquares = 0xaa55aa55aa55aa55L;
//    private static final long blackSquares = 0x55aa55aa55aa55aaL;

    private static final long[] columns = {
            0x8080808080808080L,
            0x4040404040404040L,
            0x2020202020202020L,
            0x1010101010101010L,
            0x0808080808080808L,
            0x0404040404040404L,
            0x0202020202020202L,
            0x0101010101010101L
    };

    private static int[] firstBit;
    private static long[] whiteFrontSpan;
    private static long[] blackBackSpan;
    private static long[] whiteNorth;
    private static long[] blackSouth;
    private static long[] blackAttacksTo;
    private static long[] whiteAttacksTo;

    long w;
    long b;

    private ArrayList<Integer> whitePawns;
    private ArrayList<Integer> blackPawns;

    static void initialize() {
        firstBit = new int[65536];
        firstBit[0] = -1;
        for (int i = 1; i < 65536; ++i) {
            int count = 0;
            int j = i;
            while ((j & 0x0001) == 0) {
                j = j >> 1;
                count++;
            }
            firstBit[i] = count;
        }

        whiteFrontSpan = new long[64];
        blackBackSpan = new long[64];
        whiteNorth = new long[64];
        blackSouth = new long[64];
        whiteAttacksTo = new long[64];
        blackAttacksTo = new long[64];

        int count = 63;
        long current;
        int bitstring;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (j == 0) {
                    bitstring = 0b11000000;
                } else if (j == 7) {
                    bitstring = 0b00000011;
                } else {
                    bitstring = 0b111 << (6 - j);
                }
                // White
                current = bitstring;
                for (int k = 0; k < i; k++) {
                    current = (current << 8) | bitstring;
                }
                for (int k = i; k < 8; k++) {
                    current = (current << 8);
                }
                whiteFrontSpan[count] = current;
                // Black
                current = 0;
                for (int k = i + 1; k < 8; k++) {
                    current = (current << 8) | bitstring;
                }
                blackBackSpan[count] = current;
                count--;
            }
        }
        count = 63;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                bitstring = 1 << (7 - j);
                // White
                current = bitstring;
                for (int k = 0; k < i; k++) {
                    current = (current << 8) | bitstring;
                }
                for (int k = i; k < 8; k++) {
                    current = (current << 8);
                }
                whiteNorth[count] = current;
                // Black
                current = 0;
                for (int k = i + 1; k < 8; k++) {
                    current = (current << 8) | bitstring;
                }
                blackSouth[count] = current;
                count--;
            }
        }
        count = 55;
        for (int i = 1; i < 7; i++) {
            for (int j = 0; j < 8; j++) {
                if (j == 0) {
                    whiteAttacksTo[count] = ultraLShiftOne(count - 9);
                    blackAttacksTo[count] = ultraLShiftOne(count + 7);
                } else if (j == 7) {
                    whiteAttacksTo[count] = ultraLShiftOne(count - 7);
                    blackAttacksTo[count] = ultraLShiftOne(count + 9);
                } else {
                    whiteAttacksTo[count] = ultraLShiftOne(count - 9) | ultraLShiftOne(count - 7);
                    blackAttacksTo[count] = ultraLShiftOne(count + 7) | ultraLShiftOne(count + 9);
                }
                count--;
            }
        }
    }

    Bitboard(Board board) {
        whitePawns = new ArrayList<>();
        blackPawns = new ArrayList<>();
        w = 0;
        b = 0;
        for (int i = 63; i > 0; i--) {
            if (board.get(i) == 1) {
                w |= 1;
                whitePawns.add(i);
            } else if (board.get(i) == -1) {
                b |= 1;
                blackPawns.add(i);
            }
            w <<= 1;
            b <<= 1;
        }
        Collections.reverse(blackPawns);
    }

    boolean getAIType(char wG, char bG) {
//        if (wG == 'h' && bG == 'a'
//        || wG == 'a' && bG == 'h') {
//            return false;
//        }
        return false;
    }

    long whiteSinglePush() {
        return ((w & ~row8) << 8) & ~(w | b);
    }

    long blackSinglePush() {
        return ((b & ~row1) >> 8) & ~(w | b);
    }

    long whiteDoublePush() {
        return ((((w & row2) << 8) & ~(w | b)) << 8) & ~(w | b);
    }

    long blackDoublePush() {
        return ((((b & row7) >> 8) & ~(w | b)) >> 8) & ~(w | b);
    }

    long whiteCaptureLeft() {
        return ((w & ~row8 & notAFile) << 9) & b;
    }

    long blackCaptureLeft() {
        return ((b & ~row1 & notAFile) >> 7) & w;
    }

    long whiteCaptureRight() {
        return ((w & ~row8 & notHFile) << 7) & b;
    }

    long blackCaptureRight() {
        return ((b & ~row1 & notHFile) >> 9) & w;
    }

    long whiteEnPassantLeft(int enPassantColumn) {
        return (((w & row5 & notAFile) << 1) & columns[enPassantColumn] & b) << 8;
    }

    long whiteEnPassantRight(int enPassantColumn) {
        return (((w & row5 & notHFile) >> 1) & columns[enPassantColumn] & b) << 8;
    }

    long blackEnPassantLeft(int enPassantColumn) {
        return (((b & row4 & notAFile) << 1) & columns[enPassantColumn] & w) >> 8;
    }

    long blackEnPassantRight(int enPassantColumn) {
        return (((b & row4 & notHFile) >> 1) & columns[enPassantColumn] & w) >> 8;
    }

    long whiteOnRank8() {
        return w & row8;
    }

    long blackOnRank1() {
        return b & row1;
    }

    boolean whiteDoublePawn(int col) {
        return Long.bitCount(columns[col] & w) != 1;
    }

    boolean blackDoublePawn(int col) {
        return Long.bitCount(columns[col] & b) != 1;
    }

    boolean whiteIsolated(int pawn) {
        return Long.bitCount(whiteFrontSpan[pawn - 8] & ~whiteNorth[pawn - 8] & w) == 0;
    }

    boolean blackIsolated(int pawn) {
        return Long.bitCount(blackBackSpan[pawn + 8] & ~blackSouth[pawn + 8] & b) == 0;
    }

    boolean isWhitePassedPawn(int pawn) {
        return (whiteFrontSpan[pawn] & b) == 0;
    }

    boolean isBlackPassedPawn(int pawn) {
        return (blackBackSpan[pawn] & w) == 0;
    }

    boolean isWhiteCandidate(int pawn) {
        return (whiteNorth[pawn] & (w | b)) == 0;
    }

    boolean isBlackCandidate(int pawn) {
        return (blackSouth[pawn] & (w | b)) == 0;
    }

    int whiteAttackers(int pawn) {
        return Long.bitCount(whiteAttacksTo[pawn] & w);
    }

    long wA(int pawn) {
        return whiteAttacksTo[pawn] & w;
    }

    int blackAttackers(int pawn) {
        return Long.bitCount(blackAttacksTo[pawn] & b);
    }

    ArrayList<Integer> getWhitePawns() {
        return whitePawns;
    }

    ArrayList<Integer> getBlackPawns() {
        return blackPawns;
    }

    static IntLong FirstBit(long n) {
        int fb;
        int x;
        int count = 0;
        do {
            x = (int) (n & 0xffff);
            fb = firstBit[x];
            if (fb < 0) {
                n >>= 16;
                count += 16;
            } else {
                break;
            }
        } while (true);
        return new IntLong(fb + count, (n >> fb) ^ 1);
    }

    static void printLong(long bbl) {
        System.out.println();
        for (int row = 0; row < 8; row++) {
            long r = (bbl >> (56 - row * 8)) & 255;
            System.out.println("---------------------------------");
            for (int col = 0; col < 8; col++) {
                System.out.print("| ");
                if (((r >> (7 - col)) & 1) == 0b1) {
                    System.out.print(1);
                } else {
                    System.out.print(" ");
                }
                System.out.print(" ");
            }
            System.out.println("|");
        }
        System.out.println("---------------------------------");
        System.out.println();
    }

    private static long ultraLShiftOne(int op) {
        long out = 1;
        while (op > 16) {
            out <<= 16;
            op -= 16;
        }
        return out << op;
    }
}
