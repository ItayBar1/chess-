import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChessGUI {
  private Main.Board board;
  private Main.Color turn;
  private final JButton[][] squares = new JButton[8][8];
  private final JFrame frame = new JFrame("Chess");
  private final JLabel status = new JLabel();
  private int selR = -1, selC = -1;
  private final Set<Integer> legalTargets = new HashSet<>(); // encode as r*8+c
  private final EnumMap<Main.Color, EnumMap<Main.PieceType, Icon>> pieceIcons =
      new EnumMap<>(Main.Color.class);
  private final ChessAI ai = new ChessAI();
  private ChessAI.Difficulty selectedDifficulty = ChessAI.Difficulty.MEDIUM;
  private JComboBox<ChessAI.Difficulty> difficultySelector;
  private boolean aiThinking;
  private boolean gameOver;
  private long gameId;

  private static final Color LIGHT_SQUARE = new Color(196, 210, 244);
  private static final Color DARK_SQUARE = new Color(68, 86, 122);
  private static final Color SELECT_COLOR = new Color(255, 196, 61);
  private static final Color TARGET_COLOR = new Color(124, 187, 143);
  private static final int ICON_BOUND = 64;
  private static final Color COORDINATE_COLOR = new Color(62, 74, 96);

  public ChessGUI() {
    board = new Main.Board();
    turn = Main.Color.WHITE;
    gameOver = false;
    aiThinking = false;
    gameId = 0L;
    loadPieceIcons();
    initUI();
    updateBoardUI();
    updateStatus(null);
  }

  private void loadPieceIcons() {
    // Preload all icons to avoid repeated disk access during rendering.
    for (Main.Color color : Main.Color.values()) {
      EnumMap<Main.PieceType, Icon> byType = new EnumMap<>(Main.PieceType.class);
      for (Main.PieceType type : Main.PieceType.values()) {
        String resourceName =
            "/icons/"
                + (color == Main.Color.WHITE ? "white" : "black")
                + "_"
                + type.name().toLowerCase()
                + ".png";
        byType.put(type, loadIcon(resourceName));
      }
      pieceIcons.put(color, byType);
    }
  }

  private Icon loadIcon(String resourcePath) {
    URL resource = getClass().getResource(resourcePath);
    if (resource == null) {
      throw new IllegalStateException("Missing resource: " + resourcePath);
    }
    ImageIcon original = new ImageIcon(resource);
    // Ensure every icon fits the same square by drawing it on a scaled canvas.
    int width = original.getIconWidth();
    int height = original.getIconHeight();
    if (width <= 0 || height <= 0) {
      return original;
    }
    double scale = Math.min((double) ICON_BOUND / width, (double) ICON_BOUND / height);
    int scaledW = (int) Math.round(width * scale);
    int scaledH = (int) Math.round(height * scale);
    BufferedImage canvas = new BufferedImage(ICON_BOUND, ICON_BOUND, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = canvas.createGraphics();
    g2.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int x = (ICON_BOUND - scaledW) / 2;
    int y = (ICON_BOUND - scaledH) / 2;
    g2.drawImage(original.getImage(), x, y, scaledW, scaledH, null);
    g2.dispose();
    return new ImageIcon(canvas);
  }

  private void initUI() {
    // Construct the application window and all interactive controls.
    installLookAndFeel();

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    GradientPanel root = new GradientPanel(new Color(29, 33, 48), new Color(9, 12, 24));
    root.setLayout(new BorderLayout(25, 25));
    root.setBorder(new EmptyBorder(30, 35, 30, 35));
    frame.setContentPane(root);

    JLabel title = new JLabel("Chess", SwingConstants.CENTER);
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

    JPanel boardWithLabels = new JPanel(new BorderLayout());
    boardWithLabels.setOpaque(false);
    boardWithLabels.add(boardPanel, BorderLayout.CENTER);

    JPanel filesTop = new JPanel(new GridLayout(1, 8));
    filesTop.setOpaque(false);
    filesTop.setBorder(new EmptyBorder(0, 8, 8, 8));
    for (int c = 0; c < 8; c++) {
      filesTop.add(createCoordinateLabel(String.valueOf((char) ('a' + c))));
    }
    boardWithLabels.add(filesTop, BorderLayout.NORTH);

    JPanel ranksLeft = new JPanel(new GridLayout(8, 1));
    ranksLeft.setOpaque(false);
    ranksLeft.setBorder(new EmptyBorder(8, 0, 8, 8));
    for (int r = 0; r < 8; r++) {
      ranksLeft.add(createCoordinateLabel(String.valueOf(8 - r)));
    }
    boardWithLabels.add(ranksLeft, BorderLayout.WEST);

    boardContainer.add(boardWithLabels);

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
    setStatusMessage("Welcome to Chess — prepare to play as White!");

    for (int r = 0; r < 8; r++) {
      for (int c = 0; c < 8; c++) {
        JButton btn = new JButton();
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setIconTextGap(0);
        final int rr = r, cc = c;
        btn.addActionListener(e -> onSquareClicked(rr, cc));
        Color base = ((r + c) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
        btn.setBackground(base);
        squares[r][c] = btn;
        boardPanel.add(btn);
      }
    }

    JLabel difficultyLabel = new JLabel("Difficulty");
    difficultyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    difficultyLabel.setForeground(new Color(205, 214, 255));
    difficultyLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

    difficultySelector = new JComboBox<>(ChessAI.Difficulty.values());
    difficultySelector.setAlignmentX(Component.CENTER_ALIGNMENT);
    difficultySelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
    difficultySelector.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
    difficultySelector.setForeground(new Color(32, 37, 54));
    difficultySelector.setBackground(new Color(210, 220, 255));
    difficultySelector.setSelectedItem(selectedDifficulty);
    difficultySelector.addActionListener(
        e -> {
          ChessAI.Difficulty choice =
              (ChessAI.Difficulty) difficultySelector.getSelectedItem();
          if (choice != null) {
            selectedDifficulty = choice;
            if (!gameOver) {
              updateStatus(null);
            }
          }
        });

    JButton newGame =
        createControlButton(
            "New Match",
            () -> {
              resetBoardState();
            });

    JButton resign =
        createControlButton(
            "Resign",
            () -> {
              if (gameOver) {
                return;
              }
              aiThinking = false;
              gameOver = true;
              JOptionPane.showMessageDialog(
                  frame, "You resign. The computer claims victory.");
              resetBoardState();
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
    sidePanel.add(Box.createVerticalStrut(18));
    sidePanel.add(difficultyLabel);
    sidePanel.add(Box.createVerticalStrut(8));
    sidePanel.add(difficultySelector);
    sidePanel.add(Box.createVerticalStrut(18));
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
    // Prefer Nimbus for a modern appearance while gracefully falling back to defaults.
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
    // Helper to build a styled button with hover/press feedback and an attached action.
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
    button.addChangeListener(
        e -> {
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
    // Start a fresh game and clear any UI highlights.
    board = new Main.Board();
    turn = Main.Color.WHITE;
    selR = selC = -1;
    legalTargets.clear();
    aiThinking = false;
    gameOver = false;
    gameId++;
    updateBoardUI();
    updateStatus(null);
  }

  private void setStatusMessage(String messageBody) {
    status.setText("<html><div style='text-align:center;'>" + messageBody + "</div></html>");
  }

  private void onSquareClicked(int r, int c) {
    // Human interaction entry point — only allow White to play and ignore clicks during AI turns.
    if (gameOver || aiThinking || turn != Main.Color.WHITE) {
      return;
    }

    // If nothing selected: try to select a piece of current color
    Main.Piece p = board.at(r, c);
    if (selR == -1) {
      if (p == null) return;
      if (p.color != turn) {
        Toolkit.getDefaultToolkit().beep();
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
      if (!gameOver && turn == Main.Color.BLACK) {
        performAIMove();
      }
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
    // Remember which square is active and cache the legal destinations for highlighting.
    selR = r;
    selC = c;
    legalTargets.clear();
    List<Main.Move> legal = board.generateLegalMoves(turn);
    for (Main.Move m : legal) {
      if (m.fr == r && m.fc == c) legalTargets.add(m.tr * 8 + m.tc);
    }
    updateBoardUI();
  }

  private void updateBoardUI() {
    // Redraw pieces and highlight currently selected squares/targets.
    for (int r = 0; r < 8; r++) {
      for (int c = 0; c < 8; c++) {
        Main.Piece p = board.at(r, c);
        JButton btn = squares[r][c];
        btn.setIcon(p == null ? null : getPieceIcon(p));
        btn.setText("");
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

  private Icon getPieceIcon(Main.Piece piece) {
    Map<Main.PieceType, Icon> byType = pieceIcons.get(piece.color);
    if (byType == null) return null;
    return byType.get(piece.type);
  }

  private JLabel createCoordinateLabel(String text) {
    JLabel label = new JLabel(text.toUpperCase(), SwingConstants.CENTER);
    label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
    label.setForeground(COORDINATE_COLOR);
    label.setOpaque(false);
    return label;
  }

  private void updateStatus(Main.Move lastMove) {
    // Compose a rich-text status panel reflecting the latest move and current situation.
    // Determine legal moves for current player
    List<Main.Move> legal = board.generateLegalMoves(turn);
    boolean inCheck = board.isKingInCheck(turn);
    StringBuilder message = new StringBuilder();
    String dialogMessage = null;
    if (lastMove != null) {
      message
          .append("<span style='font-size:12px;color:#c9d1ff;'>Last move: ")
          .append(formatMove(lastMove))
          .append("</span><br/>");
    }
    if (legal.isEmpty()) {
      gameOver = true;
      aiThinking = false;
      if (inCheck) {
        boolean playerToMove = (turn == Main.Color.WHITE);
        String loser = playerToMove ? "You" : "Computer";
        String winner = playerToMove ? "Computer" : "You";
        String loserVerb = playerToMove ? "are" : "is";
        String winnerVerb = winner.equals("You") ? "win" : "wins";
        dialogMessage = loser + " " + loserVerb + " checkmated. " + winner + " " + winnerVerb + ".";
        message
            .append("<span style='font-size:15px;font-weight:bold;color:#ff7b7b;'>")
            .append(loser)
            .append(" ")
            .append(loserVerb)
            .append(" checkmated.</span><br/>")
            .append("<span style='color:#9fffd7;'>")
            .append(winner)
            .append(winner.equals("You") ? " claim victory!" : " celebrates victory!")
            .append("</span>");
      } else {
        dialogMessage = "Stalemate! The duel ends in a draw.";
        message
            .append(
                "<span style='font-size:15px;font-weight:bold;color:#ffd369;'>Stalemate.</span><br/>")
            .append("<span style='color:#d6e0ff;'>The battle ends peacefully.</span>");
      }
    } else {
      if (turn == Main.Color.WHITE) {
        message
            .append("<span style='font-size:18px;font-weight:bold;'>Your move</span>")
            .append("<br/><span style='color:#bcd0ff;'>Plan your attack against the computer.</span>");
      } else {
        message
            .append("<span style='font-size:18px;font-weight:bold;'>Computer thinking…</span>")
            .append("<br/><span style='color:#bcd0ff;'>Analyzing the best reply.</span>");
      }
      if (inCheck) {
        String warning =
            (turn == Main.Color.WHITE)
                ? "Your king is in check!"
                : "Computer king is in check!";
        message.append("<br/><span style='color:#ff9f7b;'>").append(warning).append("</span>");
      }
      message
          .append("<br/><span style='color:#a9b8e8;'>Difficulty: ")
          .append(selectedDifficulty.displayName())
          .append("</span>");
    }
    setStatusMessage(message.toString());
    if (dialogMessage != null) {
      JOptionPane.showMessageDialog(frame, dialogMessage);
    }
  }

  private String formatMove(Main.Move move) {
    Main.Piece piece = board.at(move.tr, move.tc);
    String pieceName = "";
    if (piece != null) {
      pieceName =
          switch (piece.type) {
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

  private void performAIMove() {
    // Run the AI move selection asynchronously so the UI stays responsive.
    if (aiThinking || gameOver || turn != Main.Color.BLACK) {
      return;
    }
    aiThinking = true;
    final long currentGameId = gameId;
    final Main.Board snapshot = board.copy();

    SwingWorker<Main.Move, Void> worker =
        new SwingWorker<>() {
          @Override
          protected Main.Move doInBackground() {
            return ai.chooseMove(snapshot, Main.Color.BLACK, selectedDifficulty);
          }

          @Override
          protected void done() {
            aiThinking = false;
            if (currentGameId != gameId || gameOver || turn != Main.Color.BLACK) {
              return;
            }
            try {
              Main.Move aiMove = get();
              if (aiMove == null) {
                updateStatus(null);
                return;
              }
              board.applyMove(aiMove);
              turn = Main.Color.WHITE;
              selR = selC = -1;
              legalTargets.clear();
              updateBoardUI();
              updateStatus(aiMove);
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(
                  frame, "Computer move failed: " + ex.getMessage());
            }
          }
        };
    worker.execute();
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
