package com.hardcraft.core.config;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
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
        CoreSettings candidate = validate(plugin.getConfig());
        settings = candidate;
        return candidate;
    }

    public CoreSettings settings() {
        if (settings == null) {
            throw new IllegalStateException("Configuration has not been loaded");
        }
        return settings;
    }

    static CoreSettings validate(FileConfiguration config) {
        List<String> errors = new ArrayList<>();
        int version = config.getInt("config-version", -1);
        if (version != CONFIG_VERSION) errors.add("config-version must be " + CONFIG_VERSION);
        if (!config.isBoolean("debug")) errors.add("debug must be true or false");
        if (!config.isBoolean("database.enabled")) errors.add("database.enabled must be true or false");
        if (!config.isInt("database.pool-size")) errors.add("database.pool-size must be an integer");
        int poolSize = config.getInt("database.pool-size", -1);
        if (poolSize < 1 || poolSize > 32) errors.add("database.pool-size must be between 1 and 32");
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ", errors));
        return new CoreSettings(config.getBoolean("debug"), config.getBoolean("database.enabled"), poolSize);
    }
}