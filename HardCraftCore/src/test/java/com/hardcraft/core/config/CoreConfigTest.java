package com.hardcraft.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class CoreConfigTest {
    @Test
    void acceptsTheShippedDefaults() {
        CoreSettings settings = CoreConfig.validate(raw(1, false, true, "hardcraft.db", 4));
        assertEquals(false, settings.debug());
        assertEquals(true, settings.databaseEnabled());
        assertEquals("hardcraft.db", settings.databasePath());
        assertEquals(4, settings.databasePoolSize());
    }

    @Test
    void acceptsBukkitFlattenedValues() {
        CoreSettings settings = CoreConfig.validate(Map.of(
                "config-version", 1,
                "debug", false,
                "database.enabled", true,
                "database.path", "hardcraft.db",
                "database.pool-size", 4));

        assertEquals("hardcraft.db", settings.databasePath());
    }

    @Test
    void reportsEveryInvalidFieldAtOnce() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> CoreConfig.validate(raw(2, "yes", false, "../outside.db", 64)));
        String message = failure.getMessage();
        assertTrue(message.contains("config-version must be 1"), message);
        assertTrue(message.contains("debug must be true or false"), message);
        assertTrue(message.contains("database.path"), message);
        assertTrue(message.contains("database.pool-size must be between 1 and 32"), message);
    }

    @Test
    void rejectsMissingValuesInsteadOfDefaultingThem() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> CoreConfig.validate(Map.of()));
        String message = failure.getMessage();
        assertTrue(message.contains("config-version must be 1"), message);
        assertTrue(message.contains("debug must be true or false"), message);
        assertTrue(message.contains("database.pool-size must be an integer"), message);
    }

    private static Map<String, Object> raw(int version, Object debug, boolean databaseEnabled,
            Object path, Object poolSize) {
        Map<String, Object> database = new LinkedHashMap<>();
        database.put("enabled", databaseEnabled);
        database.put("path", path);
        database.put("pool-size", poolSize);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("config-version", version);
        values.put("debug", debug);
        values.put("database", database);
        return values;
    }
}