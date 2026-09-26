package com.hardcraft.core.migration;

import java.util.logging.Logger;

public final class MigrationRunner {
    public enum State { NOT_CONFIGURED, APPLIED }

    private final Logger logger;

    public MigrationRunner(Logger logger) {
        this.logger = logger;
    }

    public State run(boolean databaseEnabled) {
        if (databaseEnabled) {
            throw new IllegalStateException(
                    "database.enabled=true but no database backend is configured; disable it or configure a supported backend");
        }
        logger.info("Database disabled; migrations: NOT_CONFIGURED");
        return State.NOT_CONFIGURED;
    }
}