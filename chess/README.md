# Chess (Java)

This project provides two ways to enjoy a simplified game of chess written in Java:

* **Console mode** (`Main`): a lightweight command-line interface that accepts algebraic square
  coordinates (e.g., `e2e4`).
* **Graphical mode** (`ChessGUI`): a polished Swing UI with high-resolution piece artwork and an
  integrated computer opponent.

The code purposefully focuses on readability and approachability. Complex chess rules such as
castling, en-passant captures, and under-promotions are not implemented so that the overall
structure of move generation remains easy to follow.

## Features

- Full legal move validation for standard piece movement, checks, checkmates, and stalemates.
- Automatic pawn promotion to a queen when reaching the final rank.
- Minimax AI with alpha-beta pruning and selectable difficulty levels.
- Rich Swing interface with coordinate labels, move highlights, and contextual status messages.
- Clean console interface for quick games or integration with other tools.

## Project Layout

```
chess
├── src/main/java
│   ├── Main.java        # Console experience and core board logic
│   ├── ChessGUI.java    # Swing user interface and player interaction logic
│   └── ChessAI.java     # Alpha-beta chess engine used by the GUI
├── src/main/resources
│   └── icons/           # 64×64 PNG sprites for each piece
├── build.gradle.kts     # Gradle build definition
└── settings.gradle.kts
```

### Core Classes

- **`Main.Board`** – Maintains the 8×8 board representation, move generation, and legality checks.
  The GUI and AI reuse this class to ensure consistent rules.
- **`Main`** – Contains the console entry point and helper utilities for parsing and validating
  text-based moves.
- **`ChessGUI`** – Builds the Swing interface, handles user interaction, and orchestrates games
  between the human player (white) and the AI opponent (black).
- **`ChessAI`** – Implements a depth-limited alpha-beta search with a lightweight evaluation
  function based on material, mobility, and check pressure.

## Getting Started

### Prerequisites

- Java 17 or later
- Gradle (optional). The included Gradle wrapper (`./gradlew`) can bootstrap the required
  Gradle version automatically.

### Building the Project

```bash
./gradlew build
```

The Gradle build compiles both the console and GUI applications and runs any associated tests.

### Running the Console Version

Compile the project and then launch the console application directly from the build output:

```bash
./gradlew build
java -cp build/classes/java/main Main
```

Example commands:

- `e2e4` – Move the white pawn from e2 to e4
- `help` – Display usage guidance
- `resign` – End the game immediately

### Running the GUI Version

After compiling (for example by running `./gradlew build`), start the Swing interface with:

```bash
java -cp build/classes/java/main ChessGUI
```

The GUI launches with the human player controlling the white pieces. Choose the desired difficulty
from the side panel and play by clicking source and destination squares. Highlighted targets show
all legal destinations for the selected piece. The computer plays automatically after white moves.

## AI Difficulty Levels

The `ChessAI.Difficulty` enum exposes three presets:

| Difficulty | Depth | Description |
|------------|-------|-------------|
| Easy       | 1     | Evaluates only the immediate position; favors quick responses. |
| Medium     | 2     | Looks one move ahead for both sides; balanced challenge. |
| Hard       | 3     | Searches two full plies ahead, making more tactical decisions. |

Higher depths dramatically increase the number of explored positions. The Swing implementation
keeps the UI responsive by evaluating computer moves on a background thread.

## Customization Tips

- **Board logic** – Extend `Main.Board` to add advanced rules such as castling, en-passant, or
  draw detection. Ensure `generateLegalMoves` returns only moves that keep the king safe.
- **Evaluation function** – Adjust the weighting constants in `ChessAI` to change the AI’s playing
  style. Mobility and check bonuses can be tuned to emphasize different strategies.
- **Piece artwork** – Replace the PNG files inside `src/main/resources/icons/` with your preferred
  sprites. The loader automatically scales images to fit each square.

## Troubleshooting

- If the GUI does not start, confirm that your environment supports Swing (most desktop JVMs do).
- Missing icons trigger a descriptive `IllegalStateException`. Verify that the resource folder is
  available on the runtime classpath (Gradle handles this automatically).
- The console application expects algebraic squares (`a1`..`h8`). Input outside this format is
  rejected with an explanatory message.

## License

This project is provided as-is for educational purposes. Feel free to adapt the code to fit your
own chess-related experiments or demonstrations.
