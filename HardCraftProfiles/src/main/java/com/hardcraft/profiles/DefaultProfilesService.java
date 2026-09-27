package com.hardcraft.profiles;

import com.hardcraft.profiles.api.PlayerProfile;
import com.hardcraft.profiles.api.ProfilesApi;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DefaultProfilesService implements ProfilesApi, AutoCloseable {
    private final ProfileRepository repository;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform()
            .name("HardCraftProfiles-DB").factory());
    private final Map<UUID, Instant> sessions = new ConcurrentHashMap<>();

    public DefaultProfilesService(ProfileRepository repository) {
        this.repository = repository;
    }

    public CompletableFuture<PlayerProfile> joined(UUID uuid, String latestName, Instant at) {
        sessions.put(uuid, at);
        return submit(() -> repository.join(uuid, latestName, at));
    }

    public CompletableFuture<PlayerProfile> left(UUID uuid, Instant at) {
        Instant joinedAt = sessions.remove(uuid);
        if (joinedAt == null) return failed(new IllegalStateException("No active session: " + uuid));
        long seconds = Math.max(0, Duration.between(joinedAt, at).toSeconds());
        return submit(() -> repository.addPlayTime(uuid, seconds));
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> find(UUID uuid) {
        return submit(() -> repository.find(uuid));
    }

    @Override
    public CompletableFuture<PlayerProfile> setSetting(UUID uuid, String key, String value) {
        return submit(() -> repository.setSetting(uuid, key, value));
    }

    public void flushSessions(Instant at) {
        for (UUID uuid : Map.copyOf(sessions).keySet()) left(uuid, at);
    }

    private <T> CompletableFuture<T> submit(java.util.concurrent.Callable<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try { return operation.call(); }
            catch (RuntimeException exception) { throw exception; }
            catch (Exception exception) { throw new IllegalStateException(exception); }
        }, databaseExecutor);
    }

    private static <T> CompletableFuture<T> failed(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    @Override
    public void close() {
        databaseExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(15, TimeUnit.SECONDS)) databaseExecutor.shutdownNow();
        } catch (InterruptedException exception) {
            databaseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}