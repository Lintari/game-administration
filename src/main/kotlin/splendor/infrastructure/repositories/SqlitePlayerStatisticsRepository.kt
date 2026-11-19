package splendor.infrastructure.repositories

import splendor.application.repositories.PlayerStatisticsRecord
import splendor.application.repositories.PlayerStatisticsRepository
import splendor.domain.identifiers.PlayerId
import splendor.infrastructure.database.SqliteDatabase
import java.util.UUID

class SqlitePlayerStatisticsRepository(
    private val database: SqliteDatabase
) : PlayerStatisticsRepository {
    override fun findAll(): List<PlayerStatisticsRecord> = database.withConnection { connection ->
        connection.prepareStatement("SELECT player_id,games_played,games_won,total_points FROM player_statistics ORDER BY player_id").use { statement ->
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) add(
                        PlayerStatisticsRecord(
                            playerId = PlayerId(UUID.fromString(result.getString("player_id"))),
                            gamesPlayed = result.getInt("games_played"),
                            gamesWon = result.getInt("games_won"),
                            totalPoints = result.getInt("total_points")
                        )
                    )
                }
            }
        }
    }

    override fun findByPlayerId(playerId: PlayerId): PlayerStatisticsRecord? = database.withConnection { connection ->
        connection.prepareStatement("SELECT games_played,games_won,total_points FROM player_statistics WHERE player_id=?").use { statement ->
            statement.setString(1, playerId.value.toString())
            statement.executeQuery().use { result ->
                if (!result.next()) null else PlayerStatisticsRecord(
                    playerId = playerId,
                    gamesPlayed = result.getInt("games_played"),
                    gamesWon = result.getInt("games_won"),
                    totalPoints = result.getInt("total_points")
                )
            }
        }
    }

    override fun recalculate() {
        database.transaction { connection ->
            connection.createStatement().use { it.executeUpdate("DELETE FROM player_statistics") }
            connection.createStatement().use { statement ->
                statement.executeUpdate("""
                    INSERT INTO player_statistics(player_id,games_played,games_won,total_points)
                    SELECT p.id,
                           COALESCE(SUM(CASE WHEN g.status='FINISHED' THEN 1 ELSE 0 END),0),
                           COALESCE(SUM(CASE WHEN g.status='FINISHED' AND gp.is_winner=1 THEN 1 ELSE 0 END),0),
                           COALESCE(SUM(CASE WHEN g.status='FINISHED' THEN gp.final_score ELSE 0 END),0)
                    FROM players p
                    LEFT JOIN game_players gp ON gp.player_id=p.id
                    LEFT JOIN games g ON g.id=gp.game_id
                    GROUP BY p.id
                """.trimIndent())
            }
        }
    }
}
