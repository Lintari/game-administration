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

SQLite JDBC stores application data in `data/splendor.db`; the directory and database are created automatically on first launch. The database file is ignored by Git. SQLite is embedded, so no separate database server is required.

The schema is in `src/main/resources/db/schema.sql`. Its main tables are `players`, `games`, `game_players`, `cards`, `card_costs`, `nobles`, `noble_requirements`, `table_tokens`, `open_cards`, `deck_cards`, `participant_tokens`, `participant_cards`, `participant_nobles`, `turn_tokens`, `moves`, and `player_statistics`. Foreign keys connect game state and results to their players and game records.

Every game save writes the game state, participants, card/token/noble locations, and move history in one transaction. The same transaction rebuilds aggregate statistics from finished game results, so retrying a save does not increment totals twice. `StatisticsService.recalculate()` can also rebuild the aggregate table on demand.

To inspect the database in IntelliJ IDEA, open the Database tool window, add a data source, choose SQLite, and select `data/splendor.db`.
