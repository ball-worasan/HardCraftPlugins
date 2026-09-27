package com.hardcraft.economy;

import com.hardcraft.core.api.CoreApi;
import com.hardcraft.economy.api.EconomyApi;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class HardCraftEconomy extends JavaPlugin {
    private CoreApi core;
    private DefaultEconomyService economy;

    @Override public void onEnable(){
        try {
            RegisteredServiceProvider<CoreApi> registration=getServer().getServicesManager().getRegistration(CoreApi.class);
            core=Objects.requireNonNull(registration,"HardCraftCore API service is unavailable").getProvider();
            if(core.contractVersion()!=CoreApi.CONTRACT_VERSION)throw new IllegalStateException("HardCraftCore API contract must be 1, got "+core.contractVersion());
            JavaPlugin corePlugin=(JavaPlugin)getServer().getPluginManager().getPlugin("HardCraftCore");
            Path database=Objects.requireNonNull(corePlugin,"HardCraftCore plugin is unavailable").getDataFolder().toPath().resolve("hardcraft.db");
            EconomyRepository repository=new EconomyRepository(database); repository.migrate();
            economy=new DefaultEconomyService(repository); core.services().register(EconomyApi.class,economy);
            getLogger().info("HardCraftEconomy v"+getPluginMeta().getVersion()+" enabled; Core API=1; economy schema=1");
            new StagingProbe(this,economy).run();
        } catch(RuntimeException failure){getLogger().log(Level.SEVERE,"HardCraftEconomy failed to start: "+failure.getMessage(),failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(core!=null)core.services().unregister(EconomyApi.class);if(economy!=null)economy.close();}
}