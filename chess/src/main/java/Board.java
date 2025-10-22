import java.util.ArrayList;
import java.util.List;

public class Board {
    private final Piece[][] grid;
    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteKingsideRookMoved;
    private boolean whiteQueensideRookMoved;
    private boolean blackKingsideRookMoved;
    private boolean blackQueensideRookMoved;

    public Board() {
        this.grid = new Piece[8][8];
        init();
    }

    private Board(Board other) {
        this.grid = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = other.grid[r][c];
                if (piece != null) {
                    this.grid[r][c] = new Piece(piece.getType(), piece.getColor());
                }
            }
        }
        this.whiteKingMoved = other.whiteKingMoved;
        this.blackKingMoved = other.blackKingMoved;
        this.whiteKingsideRookMoved = other.whiteKingsideRookMoved;
        this.whiteQueensideRookMoved = other.whiteQueensideRookMoved;
        this.blackKingsideRookMoved = other.blackKingsideRookMoved;
        this.blackQueensideRookMoved = other.blackQueensideRookMoved;
    }

    public Board copy() {
        return new Board(this);
    }

    public void init() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                grid[r][c] = null;
            }
        }
        for (int c = 0; c < 8; c++) grid[6][c] = new Piece(PieceType.PAWN, PlayerColor.WHITE);
        for (int c = 0; c < 8; c++) grid[1][c] = new Piece(PieceType.PAWN, PlayerColor.BLACK);
        grid[7][0] = new Piece(PieceType.ROOK, PlayerColor.WHITE);
        grid[7][7] = new Piece(PieceType.ROOK, PlayerColor.WHITE);
        grid[0][0] = new Piece(PieceType.ROOK, PlayerColor.BLACK);
        grid[0][7] = new Piece(PieceType.ROOK, PlayerColor.BLACK);
        grid[7][1] = new Piece(PieceType.KNIGHT, PlayerColor.WHITE);
        grid[7][6] = new Piece(PieceType.KNIGHT, PlayerColor.WHITE);
        grid[0][1] = new Piece(PieceType.KNIGHT, PlayerColor.BLACK);
        grid[0][6] = new Piece(PieceType.KNIGHT, PlayerColor.BLACK);
        grid[7][2] = new Piece(PieceType.BISHOP, PlayerColor.WHITE);
        grid[7][5] = new Piece(PieceType.BISHOP, PlayerColor.WHITE);
        grid[0][2] = new Piece(PieceType.BISHOP, PlayerColor.BLACK);
        grid[0][5] = new Piece(PieceType.BISHOP, PlayerColor.BLACK);
        grid[7][3] = new Piece(PieceType.QUEEN, PlayerColor.WHITE);
        grid[0][3] = new Piece(PieceType.QUEEN, PlayerColor.BLACK);
        grid[7][4] = new Piece(PieceType.KING, PlayerColor.WHITE);
        grid[0][4] = new Piece(PieceType.KING, PlayerColor.BLACK);
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteKingsideRookMoved = false;
        whiteQueensideRookMoved = false;
        blackKingsideRookMoved = false;
        blackQueensideRookMoved = false;
    }

    public boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    public Piece at(int r, int c) {
        if (!inBounds(r, c)) return null;
        return grid[r][c];
    }

    public void applyMove(Move m) {
        Piece moving = grid[m.fr][m.fc];
        Piece captured = grid[m.tr][m.tc];
        if (captured != null && captured.getType() == PieceType.ROOK) {
            markRookMovedOrGone(captured.getColor(), m.tr, m.tc);
        }
        grid[m.fr][m.fc] = null;
        if (moving != null) {
            if (moving.getType() == PieceType.KING) {
                if (moving.getColor() == PlayerColor.WHITE) {
                    whiteKingMoved = true;
                } else {
                    blackKingMoved = true;
                }
                if (Math.abs(m.tc - m.fc) == 2) {
                    int row = m.tr;
                    if (m.tc > m.fc) {
                        Piece rook = grid[row][7];
                        grid[row][7] = null;
                        grid[row][5] = rook;
                        markRookMovedOrGone(moving.getColor(), row, 7);
                    } else {
                        Piece rook = grid[row][0];
                        grid[row][0] = null;
                        grid[row][3] = rook;
                        markRookMovedOrGone(moving.getColor(), row, 0);
                    }
                }
            } else if (moving.getType() == PieceType.ROOK) {
                markRookMovedOrGone(moving.getColor(), m.fr, m.fc);
            }
        }
        grid[m.tr][m.tc] = moving;
        if (moving != null && moving.getType() == PieceType.PAWN) {
            if (moving.getColor() == PlayerColor.WHITE && m.tr == 0) {
                grid[m.tr][m.tc] = new Piece(PieceType.QUEEN, PlayerColor.WHITE);
            }
            if (moving.getColor() == PlayerColor.BLACK && m.tr == 7) {
                grid[m.tr][m.tc] = new Piece(PieceType.QUEEN, PlayerColor.BLACK);
            }
        }
    }

    private void markRookMovedOrGone(PlayerColor color, int row, int col) {
        if (color == PlayerColor.WHITE && row == 7) {
            if (col == 0) whiteQueensideRookMoved = true;
            if (col == 7) whiteKingsideRookMoved = true;
        } else if (color == PlayerColor.BLACK && row == 0) {
            if (col == 0) blackQueensideRookMoved = true;
            if (col == 7) blackKingsideRookMoved = true;
        }
    }

    public boolean isAttacked(int r, int c, PlayerColor by) {
        for (int rr = 0; rr < 8; rr++) {
            for (int cc = 0; cc < 8; cc++) {
                Piece piece = grid[rr][cc];
                if (piece == null || piece.getColor() != by) continue;
                if (canPieceAttack(rr, cc, r, c)) return true;
            }
        }
        return false;
    }

    private boolean canPieceAttack(int sr, int sc, int tr, int tc) {
        Piece piece = at(sr, sc);
        if (piece == null) return false;
        int dr = tr - sr;
        int dc = tc - sc;
        return switch (piece.getType()) {
            case PAWN -> piece.getColor() == PlayerColor.WHITE ? (dr == -1 && Math.abs(dc) == 1)
                    : (dr == 1 && Math.abs(dc) == 1);
            case KNIGHT -> {
                int adr = Math.abs(dr);
                int adc = Math.abs(dc);
                yield (adr == 2 && adc == 1) || (adr == 1 && adc == 2);
            }
            case BISHOP -> Math.abs(dr) == Math.abs(dc) && isClearPath(sr, sc, tr, tc);
            case ROOK -> (dr == 0 || dc == 0) && isClearPath(sr, sc, tr, tc);
            case QUEEN -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && isClearPath(sr, sc, tr, tc);
            case KING -> Math.max(Math.abs(dr), Math.abs(dc)) == 1;
        };
    }

    private boolean isClearPath(int sr, int sc, int tr, int tc) {
        int dr = Integer.compare(tr, sr);
        int dc = Integer.compare(tc, sc);
        int r = sr + dr;
        int c = sc + dc;
        while (r != tr || c != tc) {
            if (grid[r][c] != null) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    public List<Move> generatePseudoMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        Piece piece = at(r, c);
        if (piece == null) return moves;
        switch (piece.getType()) {
            case PAWN -> {
                int dir = piece.getColor() == PlayerColor.WHITE ? -1 : 1;
                int nr = r + dir;
                if (inBounds(nr, c) && at(nr, c) == null) {
                    moves.add(new Move(r, c, nr, c));
                    int startRow = piece.getColor() == PlayerColor.WHITE ? 6 : 1;
                    int nr2 = r + 2 * dir;
                    if (r == startRow && at(nr2, c) == null) {
                        moves.add(new Move(r, c, nr2, c));
                    }
                }
                for (int dc : new int[]{-1, 1}) {
                    int cc = c + dc;
                    if (inBounds(nr, cc)) {
                        Piece target = at(nr, cc);
                        if (target != null && target.getColor() != piece.getColor()) {
                            moves.add(new Move(r, c, nr, cc));
                        }
                    }
                }
            }
            case KNIGHT -> {
                int[] drs = {2, 2, -2, -2, 1, 1, -1, -1};
                int[] dcs = {1, -1, 1, -1, 2, -2, 2, -2};
                for (int i = 0; i < 8; i++) {
                    int rr = r + drs[i];
                    int cc = c + dcs[i];
                    if (!inBounds(rr, cc)) continue;
                    Piece target = at(rr, cc);
                    if (target == null || target.getColor() != piece.getColor()) {
                        moves.add(new Move(r, c, rr, cc));
                    }
                }
            }
            case BISHOP, ROOK, QUEEN -> {
                int[][] dirs;
                if (piece.getType() == PieceType.BISHOP) {
                    dirs = new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                } else if (piece.getType() == PieceType.ROOK) {
                    dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                } else {
                    dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                }
                for (int[] d : dirs) {
                    int rr = r + d[0];
                    int cc = c + d[1];
                    while (inBounds(rr, cc)) {
                        Piece target = at(rr, cc);
                        if (target == null) {
                            moves.add(new Move(r, c, rr, cc));
                        } else {
                            if (target.getColor() != piece.getColor()) {
                                moves.add(new Move(r, c, rr, cc));
                            }
                            break;
                        }
                        rr += d[0];
                        cc += d[1];
                    }
                }
            }
            case KING -> {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int rr = r + dr;
                        int cc = c + dc;
                        if (!inBounds(rr, cc)) continue;
                        Piece target = at(rr, cc);
                        if (target == null || target.getColor() != piece.getColor()) {
                            moves.add(new Move(r, c, rr, cc));
                        }
                    }
                }
            }
        }
        return moves;
    }

    public List<Move> generateLegalMoves(PlayerColor color) {
        List<Move> legal = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = at(r, c);
                if (piece == null || piece.getColor() != color) continue;
                List<Move> pseudos = generatePseudoMoves(r, c);
                for (Move move : pseudos) {
                    Board copy = copy();
                    copy.applyMove(move);
                    if (!copy.isKingInCheck(color)) {
                        legal.add(move);
                    }
                }
            }
        }
        addCastlingMoves(color, legal);
        return legal;
    }

    private void addCastlingMoves(PlayerColor color, List<Move> legal) {
        int row = color == PlayerColor.WHITE ? 7 : 0;
        int kingCol = 4;
        Piece king = at(row, kingCol);
        if (king == null || king.getType() != PieceType.KING || king.getColor() != color) return;
        if (isKingInCheck(color)) return;
        PlayerColor opponent = color.opponent();
        if (!hasKingMoved(color)) {
            if (canCastleKingside(color, row, opponent)) {
                legal.add(new Move(row, kingCol, row, kingCol + 2));
            }
            if (canCastleQueenside(color, row, opponent)) {
                legal.add(new Move(row, kingCol, row, kingCol - 2));
            }
        }
    }

    private boolean hasKingMoved(PlayerColor color) {
        return color == PlayerColor.WHITE ? whiteKingMoved : blackKingMoved;
    }

    private boolean canCastleKingside(PlayerColor color, int row, PlayerColor opponent) {
        if (color == PlayerColor.WHITE) {
            if (whiteKingsideRookMoved) return false;
        } else {
            if (blackKingsideRookMoved) return false;
        }
        Piece rook = at(row, 7);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != color) return false;
        if (at(row, 5) != null || at(row, 6) != null) return false;
        if (isAttacked(row, 5, opponent) || isAttacked(row, 6, opponent)) return false;
        return true;
    }

    private boolean canCastleQueenside(PlayerColor color, int row, PlayerColor opponent) {
        if (color == PlayerColor.WHITE) {
            if (whiteQueensideRookMoved) return false;
        } else {
            if (blackQueensideRookMoved) return false;
        }
        Piece rook = at(row, 0);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != color) return false;
        if (at(row, 1) != null || at(row, 2) != null || at(row, 3) != null) return false;
        if (isAttacked(row, 3, opponent) || isAttacked(row, 2, opponent)) return false;
        return true;
    }

    public boolean isKingInCheck(PlayerColor color) {
        int kr = -1;
        int kc = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = at(r, c);
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    kr = r;
                    kc = c;
                }
            }
        }
        if (kr == -1) return true;
        return isAttacked(kr, kc, color.opponent());
    }

    public void print() {
        System.out.println();
        for (int r = 0; r < 8; r++) {
            System.out.print(8 - r + " ");
            for (int c = 0; c < 8; c++) {
                Piece piece = grid[r][c];
                System.out.print((piece == null ? "." : piece.symbol()) + " ");
            }
            System.out.println();
        }
        System.out.println("  a b c d e f g h");
        System.out.println();
    }

    public boolean isAutomaticDrawByInsufficientMaterial() {
        List<Piece> pieces = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = grid[r][c];
                if (piece != null) {
                    pieces.add(piece);
                }
            }
        }
        if (pieces.size() == 2) {
            return true;
        }
        if (pieces.size() == 3) {
            for (Piece piece : pieces) {
                if (piece.getType() == PieceType.BISHOP || piece.getType() == PieceType.KNIGHT) {
                    return true;
                }
            }
        }
        return false;
    }
}
