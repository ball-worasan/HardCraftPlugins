package com.hardcraft.core.config;

public record CoreSettings(boolean debug, boolean databaseEnabled, String databasePath, int databasePoolSize) {}