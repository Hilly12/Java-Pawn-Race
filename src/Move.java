class Move {
    private int from;
    private int to;
    private boolean isCapture;
    private boolean isEnPassantCapture;


    Move(int from, int to, boolean isCapture, boolean isEnPassantCapture) {
        this.from = from;
        this.to = to;
        this.isCapture = isCapture;
        this.isEnPassantCapture = isEnPassantCapture;
    }

    int getFrom() {
        return from;
    }

    int getTo() {
        return to;
    }

    boolean isCapture() {
        return isCapture;
    }

    boolean isEnPassantCapture() {
        return isEnPassantCapture;
    }

    boolean isDoublePush() {
        return Math.abs(from - to) == 16;
    }

    public String toString() {
        String f = (char) ((7 - (from % 8)) + 'a') + "" + (char) ((from / 8) + '1');
        String t = (char) ((7 - (to % 8)) + 'a') + "" + (char) ((to / 8) + '1');
        return f + (isCapture ? 'x' : '-') + t + (isEnPassantCapture ? " ep" : "");
    }
}
