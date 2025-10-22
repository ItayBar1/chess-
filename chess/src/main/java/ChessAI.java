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

  public Move chooseMove(Board board, PlayerColor aiPlayerColor, Difficulty difficulty) {
    List<Move> legal = board.generateLegalMoves(aiPlayerColor);
    if (legal.isEmpty()) {
      return null;
    }
    Collections.shuffle(legal, random);

    double bestScore = Double.NEGATIVE_INFINITY;
    Move bestMove = legal.getFirst();
    int depth = difficulty.searchDepth();

    for (Move move : legal) {
      Board copy = board.copy();
      copy.applyMove(move);
      double score =
          alphaBeta(
              copy,
              depth - 1,
              Double.NEGATIVE_INFINITY,
              Double.POSITIVE_INFINITY,
              aiPlayerColor.opponent(),
              aiPlayerColor);
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
      Board board,
      int depth,
      double alpha,
      double beta,
      PlayerColor turn,
      PlayerColor perspective) {
    if (depth < 0) {
      return evaluateBoard(board, perspective);
    }

    List<Move> legal = board.generateLegalMoves(turn);
    if (legal.isEmpty()) {
      if (board.isKingInCheck(turn)) {
        return turn == perspective ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
      }
      return 0.0;
    }

    Collections.shuffle(legal, random);
    if (turn == perspective) {
      double value = Double.NEGATIVE_INFINITY;
      for (Move move : legal) {
        Board copy = board.copy();
        copy.applyMove(move);
        value =
            Math.max(
                value,
                alphaBeta(copy, depth - 1, alpha, beta, turn.opponent(), perspective));
        alpha = Math.max(alpha, value);
        if (alpha >= beta) {
          break;
        }
      }
      return value;
    } else {
      double value = Double.POSITIVE_INFINITY;
      for (Move move : legal) {
        Board copy = board.copy();
        copy.applyMove(move);
        value =
            Math.min(
                value,
                alphaBeta(copy, depth - 1, alpha, beta, turn.opponent(), perspective));
        beta = Math.min(beta, value);
        if (beta <= alpha) {
          break;
        }
      }
      return value;
    }
  }

  private double evaluateBoard(Board board, PlayerColor perspective) {
    double score = 0.0;
    for (int r = 0; r < 8; r++) {
      for (int c = 0; c < 8; c++) {
        Piece piece = board.at(r, c);
        if (piece == null) continue;
        double value = switch (piece.getType()) {
          case KING -> KING_VALUE;
          case QUEEN -> QUEEN_VALUE;
          case ROOK -> ROOK_VALUE;
          case BISHOP -> BISHOP_VALUE;
          case KNIGHT -> KNIGHT_VALUE;
          case PAWN -> PAWN_VALUE;
        };
        score += piece.getColor() == perspective ? value : -value;
      }
    }

    int mobility = board.generateLegalMoves(perspective).size();
    int oppMobility = board.generateLegalMoves(perspective.opponent()).size();
    score += 0.05 * (mobility - oppMobility);

    if (board.isKingInCheck(perspective)) {
      score -= 0.5;
    }
    if (board.isKingInCheck(perspective.opponent())) {
      score += 0.5;
    }
    return score;
  }
}
