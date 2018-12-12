import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.*;
import java.security.Key;

public class JPanelBoard extends JPanel implements MouseListener, KeyListener {

    private static final long serialVersionUID = 1L;
    private static int squareSize;

    private int appW;
    private int appH;

    private Game game;
    private Board board;
    private boolean isWhiteComputer;
    private boolean isBlackComputer;

    private int selected;
    private boolean undo;

    private Timer gameTimer;


    JPanelBoard(JFrame F, int appW, int appH, Game game, Board board,
                boolean isWhiteComputer, boolean isBlackComputer) {
        super();
        F.setFocusable(true);
        F.addMouseListener(this);
        F.addKeyListener(this);
        F.setSize(appW, appH);
        this.appW = appW;
        this.appH = appH;
        this.game = game;
        this.board = board;
        this.isWhiteComputer = isWhiteComputer;
        this.isBlackComputer = isBlackComputer;
        squareSize = 50;
        selected = -1;
        undo = false;
        startGameTimer();
    }

    private void startGameTimer() {
        gameTimer = new Timer(100, e -> {
            if ((game.getCurrentPlayer() == 1 && isWhiteComputer)
                    || (game.getCurrentPlayer() == -1 && isBlackComputer)) {
                game.makeAIMove();
                if (game.getWinner() != 0) {
                    gameTimer.stop();
                }
            }
            if (game.getCurrentPlayer() == 1 && !isWhiteComputer
                    || game.getCurrentPlayer() == -1 && !isBlackComputer) {
                if (undo) {
                    game.undo();
                    undo = false;
                }
            }
            repaint();
        });
        gameTimer.start();
    }


    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, appW, appH);
        int i = 0;
        Color cbw = new Color(240, 217, 181);
        Color cbb = new Color(181, 136, 99);
        Color sel = new Color(98, 96, 71);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (63 - i == selected) {
                    g.setColor(sel);
                } else {
                    if (x % 2 == 0) {
                        if (y % 2 == 0) {
                            g.setColor(cbw);
                        } else {
                            g.setColor(cbb);
                        }
                    } else {
                        if (y % 2 == 0) {
                            g.setColor(cbb);
                        } else {
                            g.setColor(cbw);
                        }
                    }
                }
                g.fillRect(x * squareSize, y * squareSize, squareSize, squareSize);
                if (board.get(63 - i) == 1) {
                    g.drawImage(UI.whitePawn,
                            x * squareSize, y * squareSize, squareSize, squareSize, null);
                } else if (board.get(63 - i) == -1) {
                    g.drawImage(UI.blackPawn,
                            x * squareSize, y * squareSize, squareSize, squareSize, null);
                }
                i++;
            }
        }
        repaint();
    }

    public void mouseClicked(MouseEvent e) {
        int x = (e.getX() - 8) / squareSize;
        int y = (e.getY() - 32) / squareSize;
        int i = (7 - y) * 8 + (7 - x);

        if (!gameTimer.isRunning()) {
            return;
        }

        if ((game.getCurrentPlayer() == 1 && !isWhiteComputer)
                || (game.getCurrentPlayer() == -1 && !isBlackComputer)) {
            if (board.get(i) == game.getCurrentPlayer() || selected != -1) {
                if (selected != -1) {
                    String san = parseCoord(selected, i);
                    if (game.makeGUIMove(san)) {
                        if (game.getWinner() != 0) {
                            gameTimer.stop();
                        }
                        selected = -1;
                    } else {
                        System.out.println("Invalid Move!");
                        selected = -1;
                    }
                } else {
                    selected = i;
                }
            } else if (board.get(i) == 0) {
                selected = -1;
            }
        }
        repaint();
    }

    private String parseCoord(int from, int to) {
        int occupier = board.get(from);
        if (Math.abs(from - to) == 8 || Math.abs(from - to) == 16) {
            return parseInt(from) + "-" + parseInt(to);
        }
        if (board.get(to) == -occupier) {
            return parseInt(from) + "x" + parseInt(to);
        }
        return parseInt(from) + "x" + parseInt(to) + " ep";
    }

    private String parseInt(int index) {
        return "" + (char) (7 - (index % 8) + 'a') + (char) ((index / 8) + '1');
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_U) {
            undo = true;
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_U) {
            undo = false;
        }
    }

    public void keyTyped(KeyEvent e) {

    }

}
