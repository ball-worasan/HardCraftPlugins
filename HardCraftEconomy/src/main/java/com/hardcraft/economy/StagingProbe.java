package com.hardcraft.economy;

import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

final class StagingProbe {
    private static final UUID SOURCE=UUID.fromString("ec000000-0000-0000-0000-000000000001");
    private static final UUID DESTINATION=UUID.fromString("ec000000-0000-0000-0000-000000000002");
    private final JavaPlugin plugin; private final DefaultEconomyService economy;
    StagingProbe(JavaPlugin plugin,DefaultEconomyService economy){this.plugin=plugin;this.economy=economy;}
    void run(){
        if(!Boolean.getBoolean("hardcraft.economy.stagingProbe"))return;
        var marker=plugin.getDataFolder().toPath().resolve("staging-probe-prepared");
        CompletableFuture<Void> probe;
        if(Files.exists(marker)){
            probe=economy.balance(SOURCE).thenCombine(economy.balance(DESTINATION),(from,to)->{
                if(from!=65||to!=25)throw new IllegalStateException("restart balances differ: "+from+","+to);
                return null;
            }).thenCompose(ignored->economy.transfer(SOURCE,DESTINATION,25,"staging-transfer"))
              .thenCompose(replay->{if(!replay.replayed()||replay.sourceBalance()!=65||replay.destinationBalance()!=25)throw new IllegalStateException("retry changed result");return economy.ledger(SOURCE);})
              .thenAccept(entries->{if(entries.size()!=3)throw new IllegalStateException("ledger duplicated after restart");try{Files.delete(marker);}catch(Exception failure){throw new IllegalStateException(failure);}plugin.getLogger().info("STAGING_PROBE PASS: balances, ledger and idempotency survived restart");});
        }else{
            probe=economy.earn(SOURCE,100,"staging-earn").thenCompose(ignored->economy.spend(SOURCE,10,"staging-spend"))
              .thenCompose(ignored->economy.transfer(SOURCE,DESTINATION,25,"staging-transfer"))
              .thenAccept(ignored->{try{Files.createDirectories(marker.getParent());Files.createFile(marker);}catch(Exception failure){throw new IllegalStateException(failure);}plugin.getLogger().info("STAGING_PROBE PREPARED: earn, spend, transfer committed; restart required");});
        }
        probe.whenComplete((ignored,failure)->Bukkit.getScheduler().runTask(plugin,()->{if(failure!=null)plugin.getLogger().log(java.util.logging.Level.SEVERE,"STAGING_PROBE FAIL",failure);Bukkit.shutdown();}));
    }
}