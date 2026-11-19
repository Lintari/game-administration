# Splendor Manager

Kotlin application for managing Splendor players and games. The same application and domain services are used by both the existing console interface and the Swing desktop interface.

## Run

```bash
./gradlew run       # console UI
./gradlew runGui    # Swing GUI
./gradlew clean build
```

The GUI opens on the Swing Event Dispatch Thread. It provides player registration and lookup, game creation, a playable board, player statistics, and completed-game history. Game actions pass through `GameService` to the existing domain rules and validation.

## Architecture

See [docs/architecture.puml](docs/architecture.puml) for the layer diagram. GUI screens use Application DTOs and services. The Entry Point assembles repositories, services, content provider, and UI implementations. Console UI remains available.

## Data storage

The configured player and game repositories are in-memory. Players, games, moves, and statistics are lost when the process exits; the GUI does not provide persistent storage. A minimal persistence extension would add SQLite-backed implementations of `PlayerRepository` and `GameRepository` in Infrastructure, with schema/mapping for players and complete game state (participants, table, decks, tokens, cards, phase, and move history). The service and presentation APIs can remain unchanged. This storage extension has not been implemented.
