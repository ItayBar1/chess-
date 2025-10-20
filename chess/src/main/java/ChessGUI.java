import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

    private static final Color LIGHT_SQUARE = new Color(237, 229, 211);
    private static final Color DARK_SQUARE = new Color(90, 108, 126);
    private static final Color SELECT_COLOR = new Color(255, 196, 61);
    private static final Color TARGET_COLOR = new Color(124, 187, 143);
    private static final Font PIECE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 38);

    public ChessGUI() {
        board = new Main.Board();
        turn = Main.Color.WHITE;
        initUI();
        updateBoardUI();
    }

    private void initUI() {
        installLookAndFeel();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        GradientPanel root = new GradientPanel(new Color(29, 33, 48), new Color(9, 12, 24));
        root.setLayout(new BorderLayout(25, 25));
        root.setBorder(new EmptyBorder(30, 35, 30, 35));
        frame.setContentPane(root);

        JLabel title = new JLabel("Aurora Chess", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 36));
        title.setForeground(new Color(238, 242, 255));
        title.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.add(title, BorderLayout.NORTH);

        RoundedPanel boardContainer = new RoundedPanel(28, new Color(255, 255, 255, 210));
        boardContainer.setLayout(new GridBagLayout());
        boardContainer.setOpaque(false);
        boardContainer.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        boardPanel.setOpaque(false);
        boardPanel.setPreferredSize(new Dimension(520, 520));
        boardContainer.add(boardPanel);

        RoundedPanel sidePanel = new RoundedPanel(20, new Color(35, 43, 63, 230));
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setOpaque(false);
        sidePanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        sidePanel.setPreferredSize(new Dimension(230, 0));

        JLabel subtitle = new JLabel("Plan. Calculate. Conquer.");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(new Color(168, 181, 214));
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        status.setForeground(new Color(235, 240, 255));
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        status.setBorder(new EmptyBorder(18, 12, 18, 12));
        setStatusMessage("Welcome to Aurora Chess<br/>White to move");

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton btn = new JButton();
                btn.setFont(PIECE_FONT);
                btn.setFocusPainted(false);
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setForeground(new Color(45, 58, 76));
                final int rr = r, cc = c;
                btn.addActionListener(e -> onSquareClicked(rr, cc));
                Color base = ((r + c) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
                btn.setBackground(base);
                squares[r][c] = btn;
                boardPanel.add(btn);
            }
        }

        JButton newGame = createControlButton("New Match", () -> {
            resetBoardState();
            setStatusMessage("New match — White to move");
        });

        JButton resign = createControlButton("Resign", () -> {
            String resigning = (turn == Main.Color.WHITE) ? "White" : "Black";
            String winner = (turn == Main.Color.WHITE) ? "Black" : "White";
            JOptionPane.showMessageDialog(frame, resigning + " resigns. " + winner + " claims victory.");
            resetBoardState();
            setStatusMessage("Fresh board — White to move");
        });

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(new Color(66, 78, 105));

        JLabel tipTitle = new JLabel("Creative Tip");
        tipTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        tipTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        tipTitle.setForeground(new Color(205, 214, 255));

        JLabel tipBody = new JLabel("Visualize three moves ahead.");
        tipBody.setAlignmentX(Component.CENTER_ALIGNMENT);
        tipBody.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tipBody.setForeground(new Color(176, 192, 226));
        tipBody.setBorder(new EmptyBorder(8, 12, 0, 12));

        sidePanel.add(subtitle);
        sidePanel.add(Box.createVerticalStrut(15));
        sidePanel.add(status);
        sidePanel.add(Box.createVerticalStrut(15));
        sidePanel.add(newGame);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(resign);
        sidePanel.add(Box.createVerticalStrut(18));
        sidePanel.add(separator);
        sidePanel.add(Box.createVerticalStrut(18));
        sidePanel.add(tipTitle);
        sidePanel.add(tipBody);
        sidePanel.add(Box.createVerticalGlue());

        root.add(boardContainer, BorderLayout.CENTER);
        root.add(sidePanel, BorderLayout.EAST);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception ignored) {
            // default look and feel will be used
        }
    }

    private JButton createControlButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(93, 121, 237));
        button.setBorder(new EmptyBorder(12, 18, 12, 18));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.addActionListener(e -> action.run());
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.addChangeListener(e -> {
            ButtonModel model = button.getModel();
            if (model.isPressed()) {
                button.setBackground(new Color(74, 100, 214));
            } else if (model.isRollover()) {
                button.setBackground(new Color(111, 142, 255));
            } else {
                button.setBackground(new Color(93, 121, 237));
            }
        });
        return button;
    }

    private void resetBoardState() {
        board = new Main.Board();
        turn = Main.Color.WHITE;
        selR = selC = -1;
        legalTargets.clear();
        updateBoardUI();
    }

    private void setStatusMessage(String messageBody) {
        status.setText("<html><div style='text-align:center;'>" + messageBody + "</div></html>");
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
                btn.setForeground(p != null && p.color == Main.Color.WHITE ? new Color(240, 242, 255) : new Color(34, 45, 64));
                // background
                Color base = ((r + c) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
                btn.setBackground(base);
                // selection highlight
                if (r == selR && c == selC) {
                    btn.setBackground(SELECT_COLOR);
                } else if (legalTargets.contains(r * 8 + c)) {
                    btn.setBackground(TARGET_COLOR);
                }
                btn.setBorder(new LineBorder(new Color(255, 255, 255, 70)));
            }
        }
    }

    private void updateStatus(Main.Move lastMove) {
        // Determine legal moves for current player
        List<Main.Move> legal = board.generateLegalMoves(turn);
        boolean inCheck = board.isKingInCheck(turn);
        StringBuilder message = new StringBuilder();
        String dialogMessage = null;
        if (lastMove != null) {
            message.append("<span style='font-size:12px;color:#c9d1ff;'>Last move: ")
                    .append(formatMove(lastMove))
                    .append("</span><br/>");
        }
        if (legal.isEmpty()) {
            if (inCheck) {
                String loser = (turn==Main.Color.WHITE?"White":"Black");
                String winner = (turn==Main.Color.WHITE?"Black":"White");
                dialogMessage = loser + " is checkmated. " + winner + " wins.";
                message.append("<span style='font-size:15px;font-weight:bold;color:#ff7b7b;'>")
                        .append(loser)
                        .append(" is checkmated.</span><br/>")
                        .append("<span style='color:#9fffd7;'>")
                        .append(winner)
                        .append(" triumphs!</span>");
            } else {
                dialogMessage = "Stalemate! It's a draw.";
                message.append("<span style='font-size:15px;font-weight:bold;color:#ffd369;'>Stalemate.</span><br/>")
                        .append("<span style='color:#d6e0ff;'>It's a draw.</span>");
            }
        } else {
            message.append("<span style='font-size:18px;font-weight:bold;'>")
                    .append(turn==Main.Color.WHITE?"White":"Black")
                    .append(" to move</span>");
            if (inCheck) {
                message.append("<br/><span style='color:#ff9f7b;'>King in check!</span>");
            } else {
                message.append("<br/><span style='color:#bcd0ff;'>Stay sharp and craft your strategy.</span>");
            }
        }
        setStatusMessage(message.toString());
        if (dialogMessage != null) {
            JOptionPane.showMessageDialog(frame, dialogMessage);
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

    private String formatMove(Main.Move move) {
        Main.Piece piece = board.at(move.tr, move.tc);
        String pieceName = "";
        if (piece != null) {
            pieceName = switch (piece.type) {
                case KING -> "K";
                case QUEEN -> "Q";
                case ROOK -> "R";
                case BISHOP -> "B";
                case KNIGHT -> "N";
                case PAWN -> "";
            };
        }
        String from = toSquareName(move.fr, move.fc);
        String to = toSquareName(move.tr, move.tc);
        return (pieceName.isEmpty() ? "" : pieceName + " ") + from + " → " + to;
    }

    private String toSquareName(int r, int c) {
        char file = (char) ('a' + c);
        int rank = 8 - r;
        return "" + file + rank;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }

    private static class GradientPanel extends JPanel {
        private final Color start;
        private final Color end;

        GradientPanel(Color start, Color end) {
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color backgroundColor;

        RoundedPanel(int arc, Color backgroundColor) {
            this.arc = arc;
            this.backgroundColor = backgroundColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}

