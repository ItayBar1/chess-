import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChessGUI {
    private Main.Board board;
    private Main.Color turn;
    private final JButton[][] squares = new JButton[8][8];
    private final JFrame frame = new JFrame("Chess");
    private final JLabel status = new JLabel();
    private int selR = -1, selC = -1;
    private final Set<Integer> legalTargets = new HashSet<>(); // encode as r*8+c

    public ChessGUI() {
        board = new Main.Board();
        turn = Main.Color.WHITE;
        initUI();
        updateBoardUI();
    }

    private void initUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        boardPanel.setBorder(new LineBorder(Color.BLACK, 2));

        Font pieceFont = new Font(Font.MONOSPACED, Font.BOLD, 36);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton btn = new JButton();
                btn.setFont(pieceFont);
                btn.setFocusPainted(false);
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setOpaque(true);
                final int rr = r, cc = c;
                btn.addActionListener(e -> onSquareClicked(rr, cc));
                Color base = ((r + c) % 2 == 0) ? new Color(240, 217, 181) : new Color(181, 136, 99);
                btn.setBackground(base);
                squares[r][c] = btn;
                boardPanel.add(btn);
            }
        }

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        status.setText("Welcome — White to move");

        JButton newGame = new JButton("New Game");
        newGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        newGame.addActionListener(e -> {
            board = new Main.Board();
            turn = Main.Color.WHITE;
            selR = selC = -1;
            legalTargets.clear();
            updateBoardUI();
            updateStatus(null);
        });

        JButton resign = new JButton("Resign");
        resign.setAlignmentX(Component.CENTER_ALIGNMENT);
        resign.addActionListener(e -> {
            String winner = (turn == Main.Color.WHITE) ? "Black" : "White";
            JOptionPane.showMessageDialog(frame, (turn==Main.Color.WHITE?"White":"Black") + " resigns. " + winner + " wins.");
            board = new Main.Board();
            turn = Main.Color.WHITE;
            selR = selC = -1;
            legalTargets.clear();
            updateBoardUI();
            updateStatus(null);
        });

        right.add(status);
        right.add(Box.createVerticalStrut(10));
        right.add(newGame);
        right.add(Box.createVerticalStrut(5));
        right.add(resign);

        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(right, BorderLayout.EAST);

        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void onSquareClicked(int r, int c) {
        // If nothing selected: try to select a piece of current color
        Main.Piece p = board.at(r, c);
        if (selR == -1) {
            if (p == null) return;
            if (p.color != turn) {
                JOptionPane.showMessageDialog(frame, "That piece isn't yours.");
                return;
            }
            selectSquare(r, c);
            return;
        }

        // If clicked same square -> deselect
        if (selR == r && selC == c) {
            selR = selC = -1;
            legalTargets.clear();
            updateBoardUI();
            return;
        }

        int key = r * 8 + c;
        if (legalTargets.contains(key)) {
            Main.Move m = new Main.Move(selR, selC, r, c);
            board.applyMove(m);
            // switch turn
            turn = (turn == Main.Color.WHITE) ? Main.Color.BLACK : Main.Color.WHITE;
            selR = selC = -1;
            legalTargets.clear();
            updateBoardUI();
            updateStatus(m);
        } else {
            // if clicked on another of your pieces, change selection
            if (p != null && p.color == turn) {
                selectSquare(r, c);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    private void selectSquare(int r, int c) {
        selR = r; selC = c;
        legalTargets.clear();
        List<Main.Move> legal = board.generateLegalMoves(turn);
        for (Main.Move m : legal) {
            if (m.fr == r && m.fc == c) legalTargets.add(m.tr * 8 + m.tc);
        }
        updateBoardUI();
    }

    private void updateBoardUI() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Main.Piece p = board.at(r, c);
                JButton btn = squares[r][c];
                btn.setText(p == null ? "" : toUnicode(p));
                // background
                Color base = ((r + c) % 2 == 0) ? new Color(240, 217, 181) : new Color(181, 136, 99);
                btn.setBackground(base);
                // selection highlight
                if (r == selR && c == selC) {
                    btn.setBackground(Color.YELLOW);
                } else if (legalTargets.contains(r * 8 + c)) {
                    btn.setBackground(Color.GREEN);
                }
                btn.setBorder(new LineBorder(Color.BLACK));
            }
        }
    }

    private void updateStatus(Main.Move lastMove) {
        // Determine legal moves for current player
        List<Main.Move> legal = board.generateLegalMoves(turn);
        boolean inCheck = board.isKingInCheck(turn);
        if (legal.isEmpty()) {
            if (inCheck) {
                status.setText((turn==Main.Color.WHITE?"White":"Black") + " is checkmated. " + (turn==Main.Color.WHITE?"Black":"White") + " wins.");
                JOptionPane.showMessageDialog(frame, status.getText());
            } else {
                status.setText("Stalemate! It's a draw.");
                JOptionPane.showMessageDialog(frame, status.getText());
            }
        } else {
            String s = (turn==Main.Color.WHITE?"White":"Black") + " to move.";
            if (inCheck) s += " (in check)";
            status.setText(s);
        }
    }

    private String toUnicode(Main.Piece p) {
        if (p == null) return "";
        return switch (p.type) {
            case KING -> p.color == Main.Color.WHITE ? "♔" : "♚"; // ♔ ♚
            case QUEEN -> p.color == Main.Color.WHITE ? "♕" : "♛"; // ♕ ♛
            case ROOK -> p.color == Main.Color.WHITE ? "♖" : "♜"; // ♖ ♜
            case BISHOP -> p.color == Main.Color.WHITE ? "♗" : "♝"; // ♗ ♝
            case KNIGHT -> p.color == Main.Color.WHITE ? "♘" : "♞"; // ♘ ♞
            case PAWN -> p.color == Main.Color.WHITE ? "♙" : "♟"; // ♙ ♟
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}

