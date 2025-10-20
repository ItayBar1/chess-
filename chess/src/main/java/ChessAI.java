import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ChessAI {
  public enum Difficulty {
    EASY(1, "Easy"),
    MEDIUM(2, "Medium"),
    HARD(3, "Hard");

    private final int depth;
    private final String label;

    Difficulty(int depth, String label) {
      this.depth = depth;
      this.label = label;
    }

    public int searchDepth() {
      return depth;
    }

    public String displayName() {
      return label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private static final double KING_VALUE = 1000.0;
  private static final double QUEEN_VALUE = 9.0;
  private static final double ROOK_VALUE = 5.0;
  private static final double BISHOP_VALUE = 3.25;
  private static final double KNIGHT_VALUE = 3.0;
  private static final double PAWN_VALUE = 1.0;

  private final Random random = new Random();

  public Main.Move chooseMove(Main.Board board, Main.Color aiColor, Difficulty difficulty) {
    List<Main.Move> legal = board.generateLegalMoves(aiColor);
    if (legal.isEmpty()) {
      return null;
    }
    Collections.shuffle(legal, random);

    double bestScore = Double.NEGATIVE_INFINITY;
    Main.Move bestMove = legal.getFirst();
    int depth = difficulty.searchDepth();

    for (Main.Move move : legal) {
      Main.Board copy = board.copy();
      copy.applyMove(move);
      double score =
          alphaBeta(
              copy,
              depth - 1,
              Double.NEGATIVE_INFINITY,
              Double.POSITIVE_INFINITY,
              opposite(aiColor),
              aiColor);
      if (score > bestScore + 1e-6) {
        bestScore = score;
        bestMove = move;
      } else if (Math.abs(score - bestScore) <= 1e-6 && random.nextBoolean()) {
        bestMove = move;
      }
    }
    return bestMove;
  }

  private double alphaBeta(
      Main.Board board,
      int depth,
      double alpha,
      double beta,
      Main.Color turn,
      Main.Color perspective) {
    if (depth < 0) {
      return evaluateBoard(board, perspective);
    }

    List<Main.Move> legal = board.generateLegalMoves(turn);
    if (legal.isEmpty()) {
      if (board.isKingInCheck(turn)) {
        return turn == perspective ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
      }
      return 0.0; // stalemate
    }

    Collections.shuffle(legal, random);
    if (turn == perspective) {
      double value = Double.NEGATIVE_INFINITY;
      for (Main.Move move : legal) {
        Main.Board copy = board.copy();
        copy.applyMove(move);
        value =
            Math.max(
                value,
                alphaBeta(copy, depth - 1, alpha, beta, opposite(turn), perspective));
        alpha = Math.max(alpha, value);
        if (alpha >= beta) {
          break;
        }
      }
      return value;
    } else {
      double value = Double.POSITIVE_INFINITY;
      for (Main.Move move : legal) {
        Main.Board copy = board.copy();
        copy.applyMove(move);
        value =
            Math.min(
                value,
                alphaBeta(copy, depth - 1, alpha, beta, opposite(turn), perspective));
        beta = Math.min(beta, value);
        if (beta <= alpha) {
          break;
        }
      }
      return value;
    }
  }

  private double evaluateBoard(Main.Board board, Main.Color perspective) {
    double score = 0.0;
    for (int r = 0; r < 8; r++) {
      for (int c = 0; c < 8; c++) {
        Main.Piece piece = board.at(r, c);
        if (piece == null) continue;
        double value = switch (piece.type) {
          case KING -> KING_VALUE;
          case QUEEN -> QUEEN_VALUE;
          case ROOK -> ROOK_VALUE;
          case BISHOP -> BISHOP_VALUE;
          case KNIGHT -> KNIGHT_VALUE;
          case PAWN -> PAWN_VALUE;
        };
        score += piece.color == perspective ? value : -value;
      }
    }

    int mobility = board.generateLegalMoves(perspective).size();
    int oppMobility = board.generateLegalMoves(opposite(perspective)).size();
    score += 0.05 * (mobility - oppMobility);

    if (board.isKingInCheck(perspective)) {
      score -= 0.5;
    }
    if (board.isKingInCheck(opposite(perspective))) {
      score += 0.5;
    }
    return score;
  }

  private Main.Color opposite(Main.Color color) {
    return color == Main.Color.WHITE ? Main.Color.BLACK : Main.Color.WHITE;
  }
}
