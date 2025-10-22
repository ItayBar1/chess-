public enum PlayerColor {
    WHITE,
    BLACK;

    public PlayerColor opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
