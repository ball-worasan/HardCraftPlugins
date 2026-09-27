package com.hardcraft.items;

import com.hardcraft.items.api.ItemData;
import com.hardcraft.items.api.ItemDefinition;
import com.hardcraft.items.api.ItemRarity;
import com.hardcraft.items.api.ItemStatus;
import com.hardcraft.items.api.ItemsApi;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

final class DefaultItemsService implements ItemsApi {
    private static final Pattern ID = Pattern.compile("[a-z0-9._-]+:[a-z0-9/._-]+");
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();
    private final MetadataCodec codec = new MetadataCodec();
    private final NamespacedKey idKey;
    private final NamespacedKey versionKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey metadataKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey instanceKey;

    DefaultItemsService(JavaPlugin plugin) {
        idKey = new NamespacedKey(plugin, "item_id");
        versionKey = new NamespacedKey(plugin, "data_version");
        rarityKey = new NamespacedKey(plugin, "rarity");
        metadataKey = new NamespacedKey(plugin, "metadata");
        ownerKey = new NamespacedKey(plugin, "owner");
        instanceKey = new NamespacedKey(plugin, "instance_id");
        register(new ItemDefinition("hardcraft:wayfinder", Material.COMPASS, "Wayfinder", ItemRarity.RARE, 1, false, true));
        register(new ItemDefinition("hardcraft:soul_token", Material.ECHO_SHARD, "Soul Token", ItemRarity.EPIC, 1, false, false));
        register(new ItemDefinition("hardcraft:ember", Material.BLAZE_POWDER, "Ember", ItemRarity.UNCOMMON, 1, true, true));
    }

    void register(ItemDefinition definition) {
        if (!ID.matcher(definition.id()).matches()) throw new IllegalArgumentException("Invalid namespaced item ID: " + definition.id());
        if (definitions.putIfAbsent(definition.id(), definition) != null) throw new IllegalArgumentException("Duplicate item ID: " + definition.id());
    }

    @Override public Optional<ItemDefinition> definition(String id) { return Optional.ofNullable(definitions.get(id)); }

    @Override public ItemStack create(String id, Map<String, String> metadata, UUID owner) {
        ItemDefinition definition = definition(id).orElseThrow(() -> new IllegalArgumentException("Unknown item ID: " + id));
        if (!definition.transferable() && owner == null) throw new IllegalArgumentException("Non-transferable item requires owner");
        ItemStack item = new ItemStack(definition.material());
        item.editMeta(meta -> {
            meta.displayName(Component.text(definition.displayName()));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(idKey, PersistentDataType.STRING, definition.id());
            pdc.set(versionKey, PersistentDataType.INTEGER, definition.dataVersion());
            pdc.set(rarityKey, PersistentDataType.STRING, definition.rarity().name());
            pdc.set(metadataKey, PersistentDataType.BYTE_ARRAY, codec.encode(metadata));
            if (owner != null) pdc.set(ownerKey, PersistentDataType.STRING, owner.toString());
            if (!definition.stackable()) pdc.set(instanceKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        });
        return item;
    }

    @Override public ItemData read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return empty(ItemStatus.NOT_CUSTOM);
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(idKey, PersistentDataType.STRING);
        if (id == null) return empty(ItemStatus.NOT_CUSTOM);
        Integer version = pdc.get(versionKey, PersistentDataType.INTEGER);
        byte[] encoded = pdc.get(metadataKey, PersistentDataType.BYTE_ARRAY);
        String ownerText = pdc.get(ownerKey, PersistentDataType.STRING);
        if (!ID.matcher(id).matches() || version == null || version < 1 || encoded == null) return malformed(id, version);
        Map<String, String> metadata;
        Optional<UUID> owner;
        try {
            metadata = codec.decode(encoded);
            owner = ownerText == null ? Optional.empty() : Optional.of(UUID.fromString(ownerText));
        } catch (IOException | IllegalArgumentException failure) { return malformed(id, version); }
        ItemDefinition definition = definitions.get(id);
        if (definition == null) return new ItemData(ItemStatus.UNKNOWN_ID, id, version, metadata, owner, Optional.empty());
        if (version != definition.dataVersion()) return new ItemData(ItemStatus.UNSUPPORTED_VERSION, id, version, metadata, owner, Optional.of(definition));
        return new ItemData(ItemStatus.KNOWN, id, version, metadata, owner, Optional.of(definition));
    }

    @Override public boolean canTransfer(ItemStack item, UUID destinationOwner) {
        ItemData data = read(item);
        if (data.status() != ItemStatus.KNOWN) return data.status() == ItemStatus.NOT_CUSTOM;
        return data.definition().orElseThrow().transferable() || data.owner().filter(destinationOwner::equals).isPresent();
    }

    private ItemData empty(ItemStatus status) { return new ItemData(status, "", 0, Map.of(), Optional.empty(), Optional.empty()); }
    private ItemData malformed(String id, Integer version) { return new ItemData(ItemStatus.MALFORMED, id, version == null ? 0 : version, Map.of(), Optional.empty(), Optional.empty()); }
}