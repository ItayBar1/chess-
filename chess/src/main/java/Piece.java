public class Piece {
    private final PieceType type;
    private final PlayerColor color;

    public Piece(PieceType type, PlayerColor color) {
        this.type = type;
        this.color = color;
    }

    public PieceType getType() {
        return type;
    }

    public PlayerColor getColor() {
        return color;
    }

    public char symbol() {
        char c = switch (type) {
            case KING -> 'K';
            case QUEEN -> 'Q';
            case ROOK -> 'R';
            case BISHOP -> 'B';
            case KNIGHT -> 'N';
            case PAWN -> 'P';
        };
        return color == PlayerColor.WHITE ? c : Character.toLowerCase(c);
    }

    @Override
    public String toString() {
        return String.valueOf(symbol());
    }
}
