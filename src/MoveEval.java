// If only you could pattern match in Java... :(
class MoveEval {
    Move move;
    int eval;
    boolean winningMove;

    MoveEval(Move move, int eval, boolean winningMove) {
        this.move = move;
        this.eval = eval;
        this.winningMove = winningMove;
    }
}