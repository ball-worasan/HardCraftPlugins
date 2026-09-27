package com.hardcraft.items.api;

import java.util.Objects;
import org.bukkit.Material;

public record ItemDefinition(
        String id, Material material, String displayName, ItemRarity rarity,
        int dataVersion, boolean stackable, boolean transferable) {
    public ItemDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(rarity, "rarity");
        if (dataVersion < 1) throw new IllegalArgumentException("dataVersion must be positive");
    }
}