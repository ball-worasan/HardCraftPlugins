package com.hardcraft.profiles;

import com.hardcraft.profiles.api.PlayerProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ProfileRepository {
    private final String url;

    public ProfileRepository(Path database) {
        this.url = "jdbc:sqlite:" + database.toAbsolutePath();
    }

    public void migrate() {
        try {
            Path path = Path.of(url.substring("jdbc:sqlite:".length()));
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            try (Connection connection = open()) {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS hardcraft_profiles_schema_version (version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
                    statement.execute("CREATE TABLE IF NOT EXISTS hardcraft_profiles (uuid TEXT PRIMARY KEY, latest_name TEXT NOT NULL, first_joined_at INTEGER NOT NULL, last_joined_at INTEGER NOT NULL, play_time_seconds INTEGER NOT NULL DEFAULT 0 CHECK(play_time_seconds >= 0))");
                    statement.execute("CREATE TABLE IF NOT EXISTS hardcraft_profile_settings (uuid TEXT NOT NULL, setting_key TEXT NOT NULL, setting_value TEXT NOT NULL, PRIMARY KEY(uuid, setting_key), FOREIGN KEY(uuid) REFERENCES hardcraft_profiles(uuid) ON DELETE CASCADE)");
                    statement.executeUpdate("INSERT OR IGNORE INTO hardcraft_profiles_schema_version(version, applied_at) VALUES (1, CURRENT_TIMESTAMP)");
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Profiles migration failed: " + exception.getMessage(), exception);
        }
    }

    public PlayerProfile join(UUID uuid, String name, Instant joinedAt) {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO hardcraft_profiles(uuid, latest_name, first_joined_at, last_joined_at, play_time_seconds) VALUES(?,?,?,?,0) ON CONFLICT(uuid) DO UPDATE SET latest_name=excluded.latest_name,last_joined_at=excluded.last_joined_at")) {
            statement.setString(1, uuid.toString()); statement.setString(2, name);
            statement.setLong(3, joinedAt.toEpochMilli()); statement.setLong(4, joinedAt.toEpochMilli());
            statement.executeUpdate();
            return find(uuid).orElseThrow();
        } catch (SQLException exception) { throw failure("join", exception); }
    }

    public PlayerProfile addPlayTime(UUID uuid, long seconds) {
        if (seconds < 0) throw new IllegalArgumentException("seconds must not be negative");
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE hardcraft_profiles SET play_time_seconds=play_time_seconds+? WHERE uuid=?")) {
            statement.setLong(1, seconds); statement.setString(2, uuid.toString());
            if (statement.executeUpdate() != 1) throw new IllegalStateException("Unknown profile: " + uuid);
            return find(uuid).orElseThrow();
        } catch (SQLException exception) { throw failure("play-time update", exception); }
    }

    public Optional<PlayerProfile> find(UUID uuid) {
        try (Connection connection = open(); PreparedStatement profile = connection.prepareStatement(
                "SELECT latest_name,first_joined_at,last_joined_at,play_time_seconds FROM hardcraft_profiles WHERE uuid=?")) {
            profile.setString(1, uuid.toString());
            try (ResultSet row = profile.executeQuery()) {
                if (!row.next()) return Optional.empty();
                Map<String, String> settings = settings(connection, uuid);
                return Optional.of(new PlayerProfile(uuid, row.getString(1), Instant.ofEpochMilli(row.getLong(2)),
                        Instant.ofEpochMilli(row.getLong(3)), row.getLong(4), settings));
            }
        } catch (SQLException exception) { throw failure("lookup", exception); }
    }

    public PlayerProfile setSetting(UUID uuid, String key, String value) {
        if (key == null || key.isBlank() || key.length() > 64) throw new IllegalArgumentException("setting key must contain 1-64 characters");
        if (value == null || value.length() > 1024) throw new IllegalArgumentException("setting value must contain at most 1024 characters");
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO hardcraft_profile_settings(uuid,setting_key,setting_value) VALUES(?,?,?) ON CONFLICT(uuid,setting_key) DO UPDATE SET setting_value=excluded.setting_value")) {
            statement.setString(1, uuid.toString()); statement.setString(2, key); statement.setString(3, value);
            statement.executeUpdate();
            return find(uuid).orElseThrow();
        } catch (SQLException exception) { throw failure("setting update", exception); }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys=ON"); }
        return connection;
    }

    private static Map<String, String> settings(Connection connection, UUID uuid) throws SQLException {
        Map<String, String> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT setting_key,setting_value FROM hardcraft_profile_settings WHERE uuid=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) values.put(rows.getString(1), rows.getString(2)); }
        }
        return values;
    }

    private static IllegalStateException failure(String operation, SQLException exception) {
        return new IllegalStateException("Profiles " + operation + " failed: " + exception.getMessage(), exception);
    }
}