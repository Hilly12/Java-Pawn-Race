import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Scanner;

class UI {

    private static final String IMAGE_PATH = "./images/";
    static BufferedImage whitePawn;
    static BufferedImage blackPawn;

    static void initialize() {
        File fileWP = new File(IMAGE_PATH + "wp.png");
        File fileBP = new File(IMAGE_PATH + "bp.png");
        try {
            whitePawn = ImageIO.read(fileWP);
            blackPawn = ImageIO.read(fileBP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    UI() {
    }

    Move getTextInput(Move[] validMoves) {
        Scanner input = new Scanner(System.in);
        boolean validInput = false;
        Move move = validMoves[0];
        while (!validInput) {
            System.out.print("Enter a move : ");
            String m = input.nextLine();
            for (Move vm : validMoves) {
                if (vm.toString().equals(m)) {
                    move = vm;
                    validInput = true;
                }
            }
        }
        return move;
    }

    Move getGUIInput(Move[] validMoves) {
        Scanner input = new Scanner(System.in);
        boolean validInput = false;
        Move move = validMoves[0];
        while (!validInput) {
            String m = input.next();
            for (Move vm : validMoves) {
                if (vm.toString().equals(m)) {
                    move = vm;
                    validInput = true;
                }
            }
        }
        return move;
    }
}