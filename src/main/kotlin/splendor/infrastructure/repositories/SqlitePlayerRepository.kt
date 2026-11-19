package splendor.infrastructure.repositories

import splendor.application.repositories.PlayerRepository
import splendor.domain.identifiers.PlayerId
import splendor.domain.players.Player
import splendor.infrastructure.database.SqliteDatabase
import java.util.UUID

class SqlitePlayerRepository(
    private val database: SqliteDatabase
) : PlayerRepository {

    override fun save(player: Player) {
        database.transaction { connection ->
            connection.prepareStatement(
                "INSERT INTO players(id, nickname) VALUES(?, ?) " +
                    "ON CONFLICT(id) DO UPDATE SET nickname = excluded.nickname"
            ).use { statement ->
                statement.setString(1, player.id.value.toString())
                statement.setString(2, player.nickname)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO player_statistics(player_id,games_played,games_won,total_points) " +
                    "VALUES(?,0,0,0) ON CONFLICT(player_id) DO NOTHING"
            ).use { statement ->
                statement.setString(1, player.id.value.toString())
                statement.executeUpdate()
            }
        }
    }

    override fun findById(id: PlayerId): Player? = database.withConnection { connection ->
        connection.prepareStatement("SELECT id, nickname FROM players WHERE id = ?").use { statement ->
            statement.setString(1, id.value.toString())
            statement.executeQuery().use { result ->
                if (!result.next()) null
                else Player(PlayerId(UUID.fromString(result.getString("id"))), result.getString("nickname"))
            }
        }
    }

    override fun findAll(): List<Player> = database.withConnection { connection ->
        connection.prepareStatement("SELECT id, nickname FROM players ORDER BY rowid").use { statement ->
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(Player(PlayerId(UUID.fromString(result.getString("id"))), result.getString("nickname")))
                    }
                }
            }
        }
    }
}
