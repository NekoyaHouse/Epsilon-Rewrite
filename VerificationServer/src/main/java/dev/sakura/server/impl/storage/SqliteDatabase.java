package dev.sakura.server.impl.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqliteDatabase {
    private final String jdbcUrl;

    public SqliteDatabase(Path dbFile) {
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();
    }

    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
        return connection;
    }

    public void initSchema() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "username TEXT PRIMARY KEY," +
                            "password_salt TEXT NOT NULL," +
                            "password_hash TEXT NOT NULL," +
                            "hwid TEXT," +
                            "qq_json TEXT NOT NULL," +
                            "phone TEXT NOT NULL," +
                            "prefix TEXT NOT NULL," +
                            "max_cloud_configs INTEGER NOT NULL DEFAULT 3," +
                            "expire_at INTEGER NOT NULL," +
                            "online INTEGER NOT NULL," +
                            "banned INTEGER NOT NULL," +
                            "created_at INTEGER NOT NULL," +
                            "updated_at INTEGER NOT NULL" +
                            ")"
            );

            try {
                statement.execute("ALTER TABLE users ADD COLUMN prefix TEXT NOT NULL DEFAULT ''");
            } catch (SQLException ignored) {
            }

            try {
                statement.execute("ALTER TABLE users ADD COLUMN max_cloud_configs INTEGER NOT NULL DEFAULT 3");
            } catch (SQLException ignored) {
            }

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS cards (" +
                            "card_key TEXT PRIMARY KEY," +
                            "grp TEXT NOT NULL," +
                            "duration_ms INTEGER NOT NULL," +
                            "created_at INTEGER NOT NULL," +
                            "used_by TEXT," +
                            "used_at INTEGER" +
                            ")"
            );
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cards_group ON cards(grp)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_users_online ON users(online)");

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS user_qq (" +
                            "username TEXT NOT NULL," +
                            "qq TEXT NOT NULL," +
                            "PRIMARY KEY(username, qq)," +
                            "FOREIGN KEY(username) REFERENCES users(username) ON DELETE CASCADE" +
                            ")"
            );
            statement.execute("CREATE INDEX IF NOT EXISTS idx_user_qq_qq ON user_qq(qq)");

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS cloud_configs (" +
                            "owner_username TEXT NOT NULL," +
                            "config_name TEXT NOT NULL," +
                            "content TEXT NOT NULL," +
                            "created_at INTEGER NOT NULL," +
                            "updated_at INTEGER NOT NULL," +
                            "PRIMARY KEY(owner_username, config_name)," +
                            "FOREIGN KEY(owner_username) REFERENCES users(username) ON DELETE CASCADE" +
                            ")"
            );
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cloud_configs_owner ON cloud_configs(owner_username)");
        }
    }

    public int resetAllOnline() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            return statement.executeUpdate("UPDATE users SET online = 0 WHERE online != 0");
        }
    }
}

