package com.hardcraft.items;

import com.hardcraft.core.api.CoreApi;
import com.hardcraft.items.api.ItemsApi;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class HardCraftItems extends JavaPlugin {
    private CoreApi core;
    @Override public void onEnable() {
        try {
            RegisteredServiceProvider<CoreApi> registration = getServer().getServicesManager().getRegistration(CoreApi.class);
            core = Objects.requireNonNull(registration, "HardCraftCore API service is unavailable").getProvider();
            if (core.contractVersion() != 1) throw new IllegalStateException("HardCraftCore API contract must be 1, got " + core.contractVersion());
            DefaultItemsService items = new DefaultItemsService(this);
            core.services().register(ItemsApi.class, items);
            getServer().getPluginManager().registerEvents(new ItemRules(items), this);
            getLogger().info("HardCraftItems v" + getPluginMeta().getVersion() + " enabled; Core API=1; item data version=1");
            new StagingProbe(this, items).run();
        } catch (RuntimeException failure) {
            getLogger().log(Level.SEVERE, "HardCraftItems failed to start: " + failure.getMessage(), failure);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    @Override public void onDisable() { if (core != null) core.services().unregister(ItemsApi.class); }
}