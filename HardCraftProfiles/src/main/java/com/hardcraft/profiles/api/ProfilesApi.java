package com.hardcraft.profiles.api;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfilesApi {
    CompletableFuture<Optional<PlayerProfile>> find(UUID uuid);

    CompletableFuture<PlayerProfile> setSetting(UUID uuid, String key, String value);
}