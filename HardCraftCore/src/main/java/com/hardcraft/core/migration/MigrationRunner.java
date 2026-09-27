package com.hardcraft.core.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public final class MigrationRunner {
    public enum State { NOT_CONFIGURED, APPLIED, UP_TO_DATE }

    private final Logger logger;

    public MigrationRunner(Logger logger) {
        this.logger = logger;
    }

    public State run(boolean databaseEnabled, Path database) {
        if (!databaseEnabled) {
            logger.info("Database disabled; migrations: NOT_CONFIGURED");
            return State.NOT_CONFIGURED;
        }
        try {
            Path parent = database.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            boolean applied;
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath())) {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON");
                    statement.execute("CREATE TABLE IF NOT EXISTS hardcraft_schema_version (version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
                    applied = version(statement) < 1;
                    if (applied) {
                        statement.executeUpdate("INSERT INTO hardcraft_schema_version(version, applied_at) VALUES (1, CURRENT_TIMESTAMP)");
                    }
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
            State state = applied ? State.APPLIED : State.UP_TO_DATE;
            logger.info("SQLite schema=1; migration=" + state);
            return state;
        } catch (Exception exception) {
            throw new IllegalStateException("SQLite migration failed for " + database.getFileName() + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static int version(Statement statement) throws SQLException {
        try (ResultSet rows = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM hardcraft_schema_version")) {
            return rows.next() ? rows.getInt(1) : 0;
        }
    }
}