package com.hardcraft.items.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record ItemData(ItemStatus status, String id, int dataVersion, Map<String, String> metadata,
                       Optional<UUID> owner, Optional<ItemDefinition> definition) {
    public ItemData { metadata = Map.copyOf(metadata); }
}