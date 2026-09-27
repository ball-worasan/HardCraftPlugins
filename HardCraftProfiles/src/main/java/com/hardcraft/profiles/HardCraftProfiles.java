package com.hardcraft.profiles;

import com.hardcraft.core.api.CoreApi;
import com.hardcraft.profiles.api.ProfilesApi;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class HardCraftProfiles extends JavaPlugin implements Listener {
    private CoreApi core;
    private DefaultProfilesService profiles;

    @Override
    public void onEnable() {
        try {
            RegisteredServiceProvider<CoreApi> registration = getServer().getServicesManager().getRegistration(CoreApi.class);
            core = Objects.requireNonNull(registration, "HardCraftCore API service is unavailable").getProvider();
            if (core.contractVersion() != CoreApi.CONTRACT_VERSION) {
                throw new IllegalStateException("HardCraftCore API contract must be 1, got " + core.contractVersion());
            }
            JavaPlugin corePlugin = (JavaPlugin) getServer().getPluginManager().getPlugin("HardCraftCore");
            Path database = Objects.requireNonNull(corePlugin, "HardCraftCore plugin is unavailable")
                    .getDataFolder().toPath().resolve("hardcraft.db");
            ProfileRepository repository = new ProfileRepository(database);
            repository.migrate();
            profiles = new DefaultProfilesService(repository);
            core.services().register(ProfilesApi.class, profiles);
            getServer().getPluginManager().registerEvents(this, this);
            for (Player player : getServer().getOnlinePlayers()) load(player);
            getLogger().info("HardCraftProfiles v" + getPluginMeta().getVersion()
                    + " enabled; Core API=1; profiles schema=1");
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "HardCraftProfiles failed to start: " + exception.getMessage(), exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { load(event.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profiles.left(event.getPlayer().getUniqueId(), Instant.now()).exceptionally(failure -> {
            logFailure(failure);
            return null;
        });
    }

    private void load(Player player) {
        profiles.joined(player.getUniqueId(), player.getName(), Instant.now()).exceptionally(failure -> {
            logFailure(failure);
            return null;
        });
    }

    private void logFailure(Throwable failure) {
        getLogger().log(Level.SEVERE, "Profile persistence failed", failure);
    }

    @Override
    public void onDisable() {
        if (core != null) core.services().unregister(ProfilesApi.class);
        if (profiles != null) {
            profiles.flushSessions(Instant.now());
            profiles.close();
        }
    }
}