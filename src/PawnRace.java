import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Scanner;

public class PawnRace extends JFrame {

    private static final long serialVersionUID = 1L;

    public static void main(String[] args) {
        Zobrist.GenerateRandomNumbers();
        Bitboard.initialize();
        UI.initialize();

        char whiteGap = getFileInput("white");
        char blackGap = getFileInput("black");
        Board board = new Board(whiteGap, blackGap);
        Game game = new Game(board, 5000);
        final boolean isBlackComputer = getAIBlackToPlay();
        final boolean isWhiteComputer = !isBlackComputer;

        JFrame F = new JFrame("Pawn Race");
        F.setResizable(false);

        JPanelBoard app = new JPanelBoard(F, 414, 436, game, board,
                isWhiteComputer, isBlackComputer);
        // UI gameUI = new UI();

        F.add(app, BorderLayout.CENTER);
        F.setVisible(true);
        F.toFront();

        F.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

//        while (game.getWinner() == 0) {
//            gameUI.update();
//            board.display();
//            game.makeMove(gameUI, blackToPlay ? isBlackComputer : isWhiteComputer);
//            blackToPlay = !blackToPlay;
//        }

//        if (game.getWinner() == Color.WHITE) {
//            System.out.println("White Wins");
//        } else if (game.getWinner() == Color.BLACK) {
//            System.out.println("Black Wins");
//        }
    }

    private static boolean getAIBlackToPlay() {
        Scanner input = new Scanner(System.in);
        int col = 0;
        do {
            System.out.print("Enter the AI's color: ");
            String in = input.nextLine().toLowerCase();
            switch (in) {
                case "white":
                    col = 1;
                    break;
                case "black":
                    col = -1;
                    break;
            }
        } while (col == 0);
        return col == Color.BLACK;
    }

    private static char getFileInput(String col) {
        Scanner input = new Scanner(System.in);
        char c = ' ';
        do {
            System.out.print("Enter the gap for " + col + ": ");
            String in = input.nextLine().toLowerCase();
            if (in.length() != 1) {
                continue;
            }
            c = in.charAt(0);
        } while (c < 'a' || 'h' < c);
        return c;
    }
}