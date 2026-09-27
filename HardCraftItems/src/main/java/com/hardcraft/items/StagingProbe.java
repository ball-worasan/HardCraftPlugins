package com.hardcraft.items;

import com.hardcraft.items.api.ItemData;
import com.hardcraft.items.api.ItemStatus;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

final class StagingProbe {
    private static final String MARKER = "hardcraft-items-staging-probe";
    private final JavaPlugin plugin;
    private final DefaultItemsService items;

    StagingProbe(JavaPlugin plugin, DefaultItemsService items) { this.plugin = plugin; this.items = items; }

    void run() {
        if (!Boolean.getBoolean("hardcraft.items.stagingProbe")) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            World world = Bukkit.getWorlds().getFirst();
            Location location = new Location(world, 0, world.getMaxHeight() - 2, 0);
            verifySafetyRules();
            if (location.getBlock().getType() == Material.CHEST) {
                ItemData data = items.read(((Chest) location.getBlock().getState()).getBlockInventory().getItem(4));
                if (data.status() != ItemStatus.KNOWN || !data.id().equals("hardcraft:wayfinder")
                        || data.dataVersion() != 1 || !data.metadata().equals(Map.of("probe", MARKER, "charge", "7"))) {
                    throw new IllegalStateException("staging restart probe failed: " + data);
                }
                location.getBlock().setType(Material.AIR);
                world.save();
                plugin.getLogger().info("STAGING_PROBE PASS: item ID/version/metadata survived slot move, chest storage, restart");
                Bukkit.shutdown();
                return;
            }
            Inventory inventory = Bukkit.createInventory(null, 9);
            ItemStack created = items.create("hardcraft:wayfinder", Map.of("probe", MARKER, "charge", "7"), null);
            inventory.setItem(0, created);
            inventory.setItem(8, inventory.getItem(0));
            inventory.setItem(0, null);
            location.getBlock().setType(Material.CHEST);
            ((Chest) location.getBlock().getState()).getBlockInventory().setItem(4, inventory.getItem(8));
            world.save();
            plugin.getLogger().info("STAGING_PROBE PREPARED: moved slot and stored item in chest; restart required");
            Bukkit.shutdown();
        });
    }

    private void verifySafetyRules() {
        ItemStack first = items.create("hardcraft:wayfinder", Map.of("probe", MARKER), null);
        ItemStack second = items.create("hardcraft:wayfinder", Map.of("probe", MARKER), null);
        if (first.isSimilar(second)) throw new IllegalStateException("non-stackable item instances stack");
        ItemStack unknown = first.clone();
        unknown.editMeta(meta -> meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, "hardcraft:removed"));
        if (items.read(unknown).status() != ItemStatus.UNKNOWN_ID || unknown.getType() != first.getType()) throw new IllegalStateException("unknown ID rule failed");
        ItemStack future = first.clone();
        future.editMeta(meta -> meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "data_version"), PersistentDataType.INTEGER, 2));
        if (items.read(future).status() != ItemStatus.UNSUPPORTED_VERSION) throw new IllegalStateException("version rule failed");
        UUID owner = UUID.randomUUID();
        ItemStack bound = items.create("hardcraft:soul_token", Map.of("probe", MARKER), owner);
        if (!items.canTransfer(bound, owner) || items.canTransfer(bound, UUID.randomUUID())) throw new IllegalStateException("owner transfer rule failed");
        plugin.getLogger().info("STAGING_PROBE PASS: unknown ID, unsupported version, non-stackable, owner transfer rules");
    }
}