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
}
