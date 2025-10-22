import java.util.List;
import java.util.Scanner;

public class ChessGame {
    private final Board board;
    private final Scanner scanner;
    private PlayerColor turn;

    public ChessGame(Scanner scanner) {
        this.board = new Board();
        this.scanner = scanner;
        this.turn = PlayerColor.WHITE;
    }

    public void play() {
        System.out.println("Simple Console Chess (no en-passant). Moves like: e2e4 or e2 e4. Castle by moving the king two squares. Type 'resign' to resign.");
        while (true) {
            board.print();
            if (board.isAutomaticDrawByInsufficientMaterial()) {
                System.out.println("Draw by insufficient material.");
                break;
            }
            System.out.println((turn == PlayerColor.WHITE ? "White" : "Black") + " to move.");
            List<Move> legal = board.generateLegalMoves(turn);
            if (legal.isEmpty()) {
                if (board.isKingInCheck(turn)) {
                    System.out.println("Checkmate! " + (turn == PlayerColor.WHITE ? "Black" : "White") + " wins.");
                } else {
                    System.out.println("Stalemate! It's a draw.");
                }
                break;
            }
            System.out.print("Enter move: ");
            String line = scanner.nextLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.equalsIgnoreCase("resign")) {
                System.out.println((turn == PlayerColor.WHITE ? "White" : "Black") + " resigns. " + (turn == PlayerColor.WHITE ? "Black" : "White") + " wins.");
                break;
            }
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                System.out.println("Bye");
                break;
            }
            if (line.equalsIgnoreCase("help")) {
                System.out.println("Enter moves like 'e2e4' or 'e2 e4'. No en-passant. Castle by moving the king two squares. Pawn auto-promotes to queen.");
                continue;
            }
            Move move = parseMove(line);
            if (move == null) {
                System.out.println("Couldn't parse move. Use format e2e4 or e2 e4. Type 'help' for help.");
                continue;
            }
            Piece piece = board.at(move.fr, move.fc);
            if (piece == null) {
                System.out.println("No piece at source square.");
                continue;
            }
            if (piece.getColor() != turn) {
                System.out.println("That piece isn't yours.");
                continue;
            }
            boolean found = false;
            for (Move legalMove : legal) {
                if (legalMove.fr == move.fr && legalMove.fc == move.fc && legalMove.tr == move.tr && legalMove.tc == move.tc) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Illegal move.");
                continue;
            }
            board.applyMove(move);
            PlayerColor opponent = turn.opponent();
            if (board.isKingInCheck(opponent)) {
                System.out.println("Check!");
            }
            turn = opponent;
        }
    }

    private static Move parseMove(String input) {
        input = input.trim().replaceAll("\\s+", "");
        if (input.length() != 4) return null;
        int[] from = parseSquare(input.substring(0, 2));
        int[] to = parseSquare(input.substring(2, 4));
        if (from == null || to == null) return null;
        return new Move(from[0], from[1], to[0], to[1]);
    }

    private static int[] parseSquare(String s) {
        if (s.length() != 2) return null;
        char file = s.charAt(0);
        char rank = s.charAt(1);
        int col = file - 'a';
        int row = '8' - rank;
        if (col < 0 || col > 7 || row < 0 || row > 7) return null;
        return new int[]{row, col};
    }
}
