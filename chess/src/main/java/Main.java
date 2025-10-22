import java.util.*;

public class Main {

    enum Color { WHITE, BLACK }
    enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

    static class Piece {
        PieceType type;
        Color color;
        Piece(PieceType type, Color color) { this.type = type; this.color = color; }
        char symbol() {
            char c = switch (type) {
                case KING -> 'K';
                case QUEEN -> 'Q';
                case ROOK -> 'R';
                case BISHOP -> 'B';
                case KNIGHT -> 'N';
                case PAWN -> 'P';
            };
            return color == Color.WHITE ? c : Character.toLowerCase(c);
        }
        @Override
        public String toString() { return String.valueOf(symbol()); }
    }

    // Simple container that stores zero-based from/to coordinates for a move.
    static class Move { int fr, fc, tr, tc; Move(int fr,int fc,int tr,int tc){this.fr=fr;this.fc=fc;this.tr=tr;this.tc=tc;} }

    static class Board {
        Piece[][] grid = new Piece[8][8];

        Board() { init(); }

        void init() {
            // Start by clearing the board before placing the initial setup.
            for (int r=0;r<8;r++) for (int c=0;c<8;c++) grid[r][c]=null;
            // pawns
            for (int c=0;c<8;c++) grid[6][c] = new Piece(PieceType.PAWN, Color.WHITE);
            for (int c=0;c<8;c++) grid[1][c] = new Piece(PieceType.PAWN, Color.BLACK);
            // rooks
            grid[7][0] = new Piece(PieceType.ROOK, Color.WHITE);
            grid[7][7] = new Piece(PieceType.ROOK, Color.WHITE);
            grid[0][0] = new Piece(PieceType.ROOK, Color.BLACK);
            grid[0][7] = new Piece(PieceType.ROOK, Color.BLACK);
            // knights
            grid[7][1] = new Piece(PieceType.KNIGHT, Color.WHITE);
            grid[7][6] = new Piece(PieceType.KNIGHT, Color.WHITE);
            grid[0][1] = new Piece(PieceType.KNIGHT, Color.BLACK);
            grid[0][6] = new Piece(PieceType.KNIGHT, Color.BLACK);
            // bishops
            grid[7][2] = new Piece(PieceType.BISHOP, Color.WHITE);
            grid[7][5] = new Piece(PieceType.BISHOP, Color.WHITE);
            grid[0][2] = new Piece(PieceType.BISHOP, Color.BLACK);
            grid[0][5] = new Piece(PieceType.BISHOP, Color.BLACK);
            // queens
            grid[7][3] = new Piece(PieceType.QUEEN, Color.WHITE);
            grid[0][3] = new Piece(PieceType.QUEEN, Color.BLACK);
            // kings
            grid[7][4] = new Piece(PieceType.KING, Color.WHITE);
            grid[0][4] = new Piece(PieceType.KING, Color.BLACK);
        }

        Board copy() {
            // Create a deep copy so simulations do not mutate the live position.
            Board b = new Board();
            b.grid = new Piece[8][8];
            for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
                Piece p = this.grid[r][c];
                if (p!=null) b.grid[r][c] = new Piece(p.type, p.color);
            }
            return b;
        }

        boolean inBounds(int r,int c){ return r>=0 && r<8 && c>=0 && c<8; }
        Piece at(int r,int c){ if(!inBounds(r,c)) return null; return grid[r][c]; }

        void applyMove(Move m) {
            // Move the piece and handle the single special rule we support (promotion).
            Piece p = grid[m.fr][m.fc];
            grid[m.tr][m.tc] = p;
            grid[m.fr][m.fc] = null;
            // promotion: pawn reaches last rank
            if (p != null && p.type == PieceType.PAWN) {
                if (p.color == Color.WHITE && m.tr == 0) grid[m.tr][m.tc] = new Piece(PieceType.QUEEN, Color.WHITE);
                if (p.color == Color.BLACK && m.tr == 7) grid[m.tr][m.tc] = new Piece(PieceType.QUEEN, Color.BLACK);
            }
        }

        // Return true if square (r,c) is attacked by color 'by'
        boolean isAttacked(int r, int c, Color by) {
            // Iterate all pieces of color 'by' and see if any can move to r,c (ignoring king safety)
            for (int rr=0; rr<8; rr++) for (int cc=0; cc<8; cc++) {
                Piece p = grid[rr][cc];
                if (p==null || p.color!=by) continue;
                if (canPieceAttack(rr,cc,r,c)) return true;
            }
            return false;
        }

        boolean canPieceAttack(int sr,int sc,int tr,int tc) {
            Piece p = at(sr,sc);
            if (p==null) return false;
            int dr = tr - sr, dc = tc - sc;
            switch (p.type) {
                case PAWN:
                    if (p.color == Color.WHITE) {
                        return (dr == -1 && Math.abs(dc) == 1);
                    } else {
                        return (dr == 1 && Math.abs(dc) == 1);
                    }
                case KNIGHT:
                    int adr = Math.abs(dr), adc = Math.abs(dc);
                    return (adr==2 && adc==1) || (adr==1 && adc==2);
                case BISHOP:
                    if (Math.abs(dr)!=Math.abs(dc)) return false;
                    return isClearPath(sr,sc,tr,tc);
                case ROOK:
                    if (dr!=0 && dc!=0) return false;
                    return isClearPath(sr,sc,tr,tc);
                case QUEEN:
                    if (dr==0 || dc==0 || Math.abs(dr)==Math.abs(dc)) return isClearPath(sr,sc,tr,tc);
                    return false;
                case KING:
                    return Math.max(Math.abs(dr),Math.abs(dc))==1;
                default: return false;
            }
        }

        boolean isClearPath(int sr,int sc,int tr,int tc) {
            // Step through every square between source and target and ensure nothing blocks the way.
            int dr = Integer.compare(tr, sr);
            int dc = Integer.compare(tc, sc);
            int r = sr+dr, c = sc+dc;
            while (r!=tr || c!=tc) {
                if (grid[r][c]!=null) return false;
                r += dr; c += dc;
            }
            return true;
        }

        // Generate pseudo-legal moves for a piece (not checking king safety)
        List<Move> generatePseudoMoves(int r,int c) {
            List<Move> moves = new ArrayList<>();
            Piece p = at(r,c);
            if (p==null) return moves;
            switch (p.type) {
                case PAWN:
                    int dir = p.color==Color.WHITE ? -1 : 1;
                    // forward one
                    int nr = r + dir;
                    if (inBounds(nr,c) && at(nr,c)==null) {
                        moves.add(new Move(r,c,nr,c));
                        // two squares from starting rank
                        int startRow = p.color==Color.WHITE ? 6 : 1;
                        int nr2 = r + 2*dir;
                        if (r==startRow && at(nr2,c)==null) moves.add(new Move(r,c,nr2,c));
                    }
                    // captures
                    for (int dc : new int[]{-1,1}) {
                        int cc = c+dc;
                        if (inBounds(nr,cc)) {
                            Piece t = at(nr,cc);
                            if (t!=null && t.color!=p.color) moves.add(new Move(r,c,nr,cc));
                        }
                    }
                    break;
                case KNIGHT:
                    int[] drs = {2,2,-2,-2,1,1,-1,-1};
                    int[] dcs = {1,-1,1,-1,2,-2,2,-2};
                    for (int i=0;i<8;i++) {
                        int rr = r+drs[i], cc = c+dcs[i];
                        if (!inBounds(rr,cc)) continue;
                        Piece t = at(rr,cc);
                        if (t==null || t.color!=p.color) moves.add(new Move(r,c,rr,cc));
                    }
                    break;
                case BISHOP:
                case ROOK:
                case QUEEN:
                    int[][] dirs;
                    if (p.type==PieceType.BISHOP) dirs = new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}};
                    else if (p.type==PieceType.ROOK) dirs = new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
                    else dirs = new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
                    for (int[] d : dirs) {
                        int rr=r+d[0], cc=c+d[1];
                        while (inBounds(rr,cc)) {
                            Piece t = at(rr,cc);
                            if (t==null) moves.add(new Move(r,c,rr,cc));
                            else { if (t.color!=p.color) moves.add(new Move(r,c,rr,cc)); break; }
                            rr += d[0]; cc += d[1];
                        }
                    }
                    break;
                case KING:
                    for (int dr2=-1; dr2<=1; dr2++) for (int dc2=-1; dc2<=1; dc2++) {
                        if (dr2==0 && dc2==0) continue;
                        int rr=r+dr2, cc=c+dc2;
                        if (!inBounds(rr,cc)) continue;
                        Piece t = at(rr,cc);
                        if (t==null || t.color!=p.color) moves.add(new Move(r,c,rr,cc));
                    }
                    break;
            }
            return moves;
        }

        // Generate legal moves for color (checks king safety)
        List<Move> generateLegalMoves(Color color) {
            // Try every pseudo-legal move and only keep the ones that leave the king safe.
            List<Move> legal = new ArrayList<>();
            for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
                Piece p = at(r,c);
                if (p==null || p.color!=color) continue;
                List<Move> pseudos = generatePseudoMoves(r,c);
                for (Move m : pseudos) {
                    Board copy = this.copy();
                    copy.applyMove(m);
                    if (!copy.isKingInCheck(color)) legal.add(m);
                }
            }
            return legal;
        }

        boolean isKingInCheck(Color color) {
            // find king
            int kr=-1,kc=-1;
            for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
                Piece p = at(r,c);
                if (p!=null && p.type==PieceType.KING && p.color==color) { kr=r; kc=c; }
            }
            if (kr==-1) return true; // no king -> treat as in check
            Color opp = (color==Color.WHITE?Color.BLACK:Color.WHITE);
            return isAttacked(kr,kc,opp);
        }

        void print() {
            System.out.println();
            for (int r=0;r<8;r++) {
                System.out.print(8-r + " ");
                for (int c=0;c<8;c++) {
                    Piece p = grid[r][c];
                    System.out.print((p==null?".":p.symbol()) + " ");
                }
                System.out.println();
            }
            System.out.println("  a b c d e f g h");
            System.out.println();
        }
    }

    // parse a square like e2 -> returns [row,col] or null
    static int[] parseSquare(String s) {
        s = s.trim();
        if (s.length()!=2) return null;
        char f = s.charAt(0);
        char r = s.charAt(1);
        int col = f - 'a';
        int row = '8' - r;
        if (col<0 || col>7 || row<0 || row>7) return null;
        return new int[]{row,col};
    }

    static Move parseMove(String input) {
        // Normalize the user input (e.g. "e2 e4") into coordinate indices.
        input = input.trim();
        input = input.replaceAll("\\s+", "");
        if (input.length()!=4) return null;
        String s1 = input.substring(0,2);
        String s2 = input.substring(2,4);
        int[] a = parseSquare(s1);
        int[] b = parseSquare(s2);
        if (a==null || b==null) return null;
        return new Move(a[0],a[1],b[0],b[1]);
    }

    public static void main(String[] args) {
        Board board = new Board();
        Scanner sc = new Scanner(System.in);
        Color turn = Color.WHITE;
        System.out.println("Simple Console Chess (no castling, no en-passant). Moves like: e2e4 or e2 e4. Type 'resign' to resign.");

        while (true) {
            board.print();
            System.out.println((turn==Color.WHITE?"White":"Black") + " to move.");

            List<Move> legal = board.generateLegalMoves(turn);
            if (legal.isEmpty()) {
                // When no legal moves exist, the game is over (either checkmate or stalemate).
                if (board.isKingInCheck(turn)) {
                    System.out.println("Checkmate! " + (turn==Color.WHITE?"Black":"White") + " wins.");
                } else {
                    System.out.println("Stalemate! It's a draw.");
                }
                break;
            }

            System.out.print("Enter move: ");
            String line = sc.nextLine();
            if (line==null) break;
            line = line.trim();
            if (line.equalsIgnoreCase("resign")) {
                System.out.println((turn==Color.WHITE?"White":"Black") + " resigns. " + (turn==Color.WHITE?"Black":"White") + " wins.");
                break;
            }
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) { System.out.println("Bye"); break; }
            if (line.equalsIgnoreCase("help")) {
                System.out.println("Enter moves like 'e2e4' or 'e2 e4'. No castling/en-passant. Pawn auto-promotes to queen.");
                continue;
            }

            Move m = parseMove(line);
            if (m==null) {
                System.out.println("Couldn't parse move. Use format e2e4 or e2 e4. Type 'help' for help.");
                continue;
            }
            Piece p = board.at(m.fr,m.fc);
            if (p==null) { System.out.println("No piece at source square."); continue; }
            if (p.color!=turn) { System.out.println("That piece isn't yours."); continue; }

            // check if move is in legal moves
            boolean found = false;
            for (Move lm : legal) if (lm.fr==m.fr && lm.fc==m.fc && lm.tr==m.tr && lm.tc==m.tc) { found=true; break; }
            if (!found) { System.out.println("Illegal move."); continue; }

            board.applyMove(m);
            // show check status
            Color opp = (turn==Color.WHITE?Color.BLACK:Color.WHITE);
            if (board.isKingInCheck(opp)) System.out.println("Check!");
            turn = opp;
        }

        sc.close();
    }
}

