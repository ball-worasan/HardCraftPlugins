package com.hardcraft.core;

import com.hardcraft.core.api.CoreApi;
import com.hardcraft.core.api.DefaultServiceRegistry;
import com.hardcraft.core.api.ServiceRegistry;
import com.hardcraft.core.command.HardCraftCommand;
import com.hardcraft.core.config.CoreConfig;
import com.hardcraft.core.config.CoreSettings;
import com.hardcraft.core.migration.MigrationRunner;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class HardCraftCore extends JavaPlugin implements CoreApi {
    private final DefaultServiceRegistry services = new DefaultServiceRegistry();
    private CoreConfig coreConfig;
    private MigrationRunner.State migrationState;

    @Override
    public void onEnable() {
        try {
            coreConfig = new CoreConfig(this);
            CoreSettings settings = coreConfig.load();
            Path database = getDataFolder().toPath().resolve(settings.databasePath()).normalize();
            migrationState = new MigrationRunner(getLogger()).run(settings.databaseEnabled(), database);
            services.clear();
            getServer().getServicesManager().unregisterAll(this);
            PluginCommand command = Objects.requireNonNull(getCommand("hardcraft"), "hardcraft command missing");
            HardCraftCommand handler = new HardCraftCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
            getServer().getServicesManager().register(CoreApi.class, this, this, ServicePriority.Normal);
            getLogger().info("HardCraftCore v" + getPluginMeta().getVersion()
                    + " enabled; API contract=" + CONTRACT_VERSION + "; migration=" + migrationState);
        } catch (RuntimeException exception) {
            getLogger().severe("HardCraftCore failed to start: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        services.clear();
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("HardCraftCore disabled.");
    }

    @Override
    public int contractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public ServiceRegistry services() {
        return services;
    }

    public CoreSettings reloadValidatedConfig() {
        return coreConfig.load();
    }

    public CoreSettings settings() {
        return coreConfig.settings();
    }

    public MigrationRunner.State migrationState() {
        return migrationState;
    }
}