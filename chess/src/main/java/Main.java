import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            ChessGame game = new ChessGame(scanner);
            game.play();
        }
    }
}
