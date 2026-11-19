package splendor.infrastructure.database

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager

class SqliteDatabase(
    val databasePath: Path = Paths.get("data", "splendor.db")
) {
    private val jdbcUrl: String

    init {
        Class.forName("org.sqlite.JDBC")
        val absolutePath = databasePath.toAbsolutePath()
        absolutePath.parent?.let(Files::createDirectories)
        jdbcUrl = "jdbc:sqlite:$absolutePath"
        initializeSchema()
    }

    fun <T> withConnection(block: (Connection) -> T): T =
        openConnection().use(block)

    fun <T> transaction(block: (Connection) -> T): T {
        openConnection().use { connection ->
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                return result
            } catch (exception: Throwable) {
                runCatching { connection.rollback() }
                throw exception
            }
        }
    }

    private fun openConnection(): Connection = DriverManager.getConnection(jdbcUrl).apply {
        createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("PRAGMA busy_timeout = 5000")
        }
    }

    private fun initializeSchema() {
        val schema = requireNotNull(javaClass.getResourceAsStream("/db/schema.sql")) {
            "SQLite schema resource /db/schema.sql is missing"
        }.bufferedReader().use { it.readText() }

        withConnection { connection ->
            connection.createStatement().use { statement ->
                schema.split(';')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .forEach(statement::execute)
            }
        }
    }
}
