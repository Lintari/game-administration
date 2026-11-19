PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS players (
    id TEXT PRIMARY KEY,
    nickname TEXT NOT NULL CHECK(length(trim(nickname)) > 0)
);

CREATE TABLE IF NOT EXISTS games (
    id TEXT PRIMARY KEY,
    status TEXT NOT NULL,
    started_at TEXT,
    finished_at TEXT,
    current_index INTEGER NOT NULL DEFAULT 0,
    final_round_player_id TEXT,
    turn_player_id TEXT,
    turn_phase TEXT,
    winner_id TEXT,
    FOREIGN KEY(final_round_player_id) REFERENCES players(id) ON DELETE SET NULL,
    FOREIGN KEY(turn_player_id) REFERENCES players(id) ON DELETE SET NULL,
    FOREIGN KEY(winner_id) REFERENCES players(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS game_players (
    game_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    player_order INTEGER NOT NULL,
    nickname_at_start TEXT NOT NULL,
    final_score INTEGER,
    placement INTEGER,
    is_winner INTEGER NOT NULL DEFAULT 0 CHECK(is_winner IN (0, 1)),
    PRIMARY KEY(game_id, player_id),
    UNIQUE(game_id, player_order),
    FOREIGN KEY(game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS cards (
    id TEXT PRIMARY KEY,
    level TEXT NOT NULL,
    bonus TEXT NOT NULL,
    points INTEGER NOT NULL CHECK(points >= 0)
);

CREATE TABLE IF NOT EXISTS card_costs (
    card_id TEXT NOT NULL,
    color TEXT NOT NULL,
    amount INTEGER NOT NULL CHECK(amount >= 0),
    PRIMARY KEY(card_id, color),
    FOREIGN KEY(card_id) REFERENCES cards(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS nobles (
    id TEXT PRIMARY KEY,
    points INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS noble_requirements (
    noble_id TEXT NOT NULL,
    color TEXT NOT NULL,
    amount INTEGER NOT NULL CHECK(amount >= 0),
    PRIMARY KEY(noble_id, color),
    FOREIGN KEY(noble_id) REFERENCES nobles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS table_tokens (
    game_id TEXT NOT NULL,
    color TEXT NOT NULL,
    amount INTEGER NOT NULL CHECK(amount >= 0),
    PRIMARY KEY(game_id, color),
    FOREIGN KEY(game_id) REFERENCES games(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS open_cards (
    game_id TEXT NOT NULL,
    level TEXT NOT NULL,
    position INTEGER NOT NULL,
    card_id TEXT NOT NULL,
    PRIMARY KEY(game_id, level, position),
    UNIQUE(game_id, card_id),
    FOREIGN KEY(game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY(card_id) REFERENCES cards(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS deck_cards (
    game_id TEXT NOT NULL,
    level TEXT NOT NULL,
    position INTEGER NOT NULL,
    card_id TEXT NOT NULL,
    PRIMARY KEY(game_id, level, position),
    UNIQUE(game_id, card_id),
    FOREIGN KEY(game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY(card_id) REFERENCES cards(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS game_nobles (
    game_id TEXT NOT NULL,
    position INTEGER NOT NULL,
    noble_id TEXT NOT NULL,
    PRIMARY KEY(game_id, position),
    UNIQUE(game_id, noble_id),
    FOREIGN KEY(game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY(noble_id) REFERENCES nobles(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS participant_tokens (
    game_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    color TEXT NOT NULL,
    amount INTEGER NOT NULL CHECK(amount >= 0),
    PRIMARY KEY(game_id, player_id, color),
    FOREIGN KEY(game_id, player_id) REFERENCES game_players(game_id, player_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS participant_cards (
    game_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    card_id TEXT NOT NULL,
    card_status TEXT NOT NULL CHECK(card_status IN ('PURCHASED', 'RESERVED')),
    position INTEGER NOT NULL,
    PRIMARY KEY(game_id, player_id, card_id),
    UNIQUE(game_id, player_id, card_status, position),
    FOREIGN KEY(game_id, player_id) REFERENCES game_players(game_id, player_id) ON DELETE CASCADE,
    FOREIGN KEY(card_id) REFERENCES cards(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS participant_nobles (
    game_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    noble_id TEXT NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY(game_id, player_id, noble_id),
    FOREIGN KEY(game_id, player_id) REFERENCES game_players(game_id, player_id) ON DELETE CASCADE,
    FOREIGN KEY(noble_id) REFERENCES nobles(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS turn_tokens (
    game_id TEXT NOT NULL,
    color TEXT NOT NULL,
    amount INTEGER NOT NULL CHECK(amount >= 0),
    PRIMARY KEY(game_id, color),
    FOREIGN KEY(game_id) REFERENCES games(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS moves (
    game_id TEXT NOT NULL,
    move_number INTEGER NOT NULL,
    player_id TEXT NOT NULL,
    move_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    description TEXT NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY(game_id, move_number),
    FOREIGN KEY(game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY(game_id, player_id) REFERENCES game_players(game_id, player_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS player_statistics (
    player_id TEXT PRIMARY KEY,
    games_played INTEGER NOT NULL DEFAULT 0,
    games_won INTEGER NOT NULL DEFAULT 0,
    total_points INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_game_players_player ON game_players(player_id);
CREATE INDEX IF NOT EXISTS idx_games_status ON games(status);
CREATE INDEX IF NOT EXISTS idx_moves_game ON moves(game_id, move_number);
