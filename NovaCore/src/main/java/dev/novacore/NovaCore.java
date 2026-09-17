package dev.novacore;

import dev.novacore.command.NovaCoreCommand;
import dev.novacore.listener.PlayerListener;
import dev.novacore.manager.ConfigManager;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class NovaCore extends JavaPlugin {
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        NovaCoreCommand commandHandler = new NovaCoreCommand(this, configManager);
        PluginCommand command = Objects.requireNonNull(
                getCommand("novacore"), "Command 'novacore' is missing from plugin.yml");
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(new PlayerListener(configManager), this);
        getLogger().info("NovaCore v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NovaCore disabled.");
    }
}