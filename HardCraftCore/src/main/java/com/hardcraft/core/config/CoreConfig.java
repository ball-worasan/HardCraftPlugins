package com.hardcraft.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreConfig {
    private static final int CONFIG_VERSION = 1;
    private final JavaPlugin plugin;
    private CoreSettings settings;

    public CoreConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CoreSettings load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.getConfig().addDefault("database.path", "hardcraft.db");
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
        CoreSettings candidate = validate(plugin.getConfig().getValues(true));
        settings = candidate;
        return candidate;
    }

    public CoreSettings settings() {
        if (settings == null) {
            throw new IllegalStateException("Configuration has not been loaded");
        }
        return settings;
    }

    static CoreSettings validate(Map<String, Object> raw) {
        List<String> errors = new ArrayList<>();
        Integer version = intAt(raw, "config-version");
        if (version == null || version != CONFIG_VERSION) errors.add("config-version must be " + CONFIG_VERSION);
        if (!(at(raw, "debug") instanceof Boolean)) errors.add("debug must be true or false");
        if (!(at(raw, "database.enabled") instanceof Boolean)) errors.add("database.enabled must be true or false");
        Object path = at(raw, "database.path");
        if (!(path instanceof String text) || text.isBlank() || text.contains("..") || text.startsWith("/")
                || text.startsWith("\\")) {
            errors.add("database.path must be a non-empty relative path without '..'");
        }
        Integer poolSize = intAt(raw, "database.pool-size");
        if (poolSize == null) {
            errors.add("database.pool-size must be an integer");
        } else if (poolSize < 1 || poolSize > 32) {
            errors.add("database.pool-size must be between 1 and 32");
        }
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ", errors));
        return new CoreSettings((Boolean) at(raw, "debug"), (Boolean) at(raw, "database.enabled"),
                (String) path, poolSize);
    }

    private static Object at(Map<String, Object> raw, String path) {
        if (raw.containsKey(path)) return raw.get(path);
        Object current = raw;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(part);
        }
        return current;
    }

    private static Integer intAt(Map<String, Object> raw, String path) {
        return at(raw, path) instanceof Number number ? number.intValue() : null;
    }
}