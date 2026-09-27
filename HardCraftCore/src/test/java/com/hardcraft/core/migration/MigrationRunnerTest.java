package com.hardcraft.core.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MigrationRunnerTest {
    @Test
    void skipsAnAlreadyAppliedMigration(@TempDir Path directory) throws Exception {
        Path database = directory.resolve("restart.db");
        MigrationRunner runner = new MigrationRunner(Logger.getAnonymousLogger());

        assertEquals(MigrationRunner.State.APPLIED, runner.run(true, database));
        assertEquals(MigrationRunner.State.UP_TO_DATE, runner.run(true, database));

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT COUNT(*) FROM hardcraft_schema_version WHERE version = 1")) {
            assertEquals(1, rows.getInt(1));
        }
    }

    @Test
    void migratesBacksUpAndRestoresOutsideTheServer(@TempDir Path directory) throws Exception {
        Path live = directory.resolve("live.db");
        assertEquals(MigrationRunner.State.APPLIED,
                new MigrationRunner(Logger.getAnonymousLogger()).run(true, live));

        Path backup = directory.resolve("backup.db");
        Files.copy(live, backup);
        Files.delete(live);
        Path restored = directory.resolve("restore-test.db");
        Files.copy(backup, restored);

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + restored);
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT MAX(version) FROM hardcraft_schema_version")) {
            assertEquals(1, rows.getInt(1));
        }
    }
}