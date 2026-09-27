package com.hardcraft.items.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public interface ItemsApi {
    Optional<ItemDefinition> definition(String id);
    ItemStack create(String id, Map<String, String> metadata, UUID owner);
    ItemData read(ItemStack item);
    boolean canTransfer(ItemStack item, UUID destinationOwner);
}