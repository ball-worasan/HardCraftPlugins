package com.hardcraft.profiles.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PlayerProfile(UUID uuid, String latestName, Instant firstJoinedAt, Instant lastJoinedAt,
        long playTimeSeconds, Map<String, String> settings) {
    public PlayerProfile {
        settings = Map.copyOf(settings);
    }
}