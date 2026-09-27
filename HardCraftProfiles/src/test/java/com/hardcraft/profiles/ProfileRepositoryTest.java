package com.hardcraft.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ProfileRepositoryTest {
    @Test
    void repeatedJoinAndRenameKeepOneUuidProfile(@TempDir Path directory) {
        ProfileRepository repository = repository(directory);
        UUID uuid = UUID.randomUUID();
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        repository.join(uuid, "OldName", first);
        repository.join(uuid, "NewName", first.plusSeconds(60));

        var profile = repository.find(uuid).orElseThrow();
        assertEquals("NewName", profile.latestName());
        assertEquals(first, profile.firstJoinedAt());
        assertEquals(first.plusSeconds(60), profile.lastJoinedAt());
    }

    @Test
    void restartPreservesPlayTimeAndSettings(@TempDir Path directory) {
        Path database = directory.resolve("hardcraft.db");
        UUID uuid = UUID.randomUUID();
        ProfileRepository firstProcess = new ProfileRepository(database);
        firstProcess.migrate();
        firstProcess.join(uuid, "Player", Instant.EPOCH);
        firstProcess.addPlayTime(uuid, 42);
        firstProcess.setSetting(uuid, "chat.enabled", "false");

        ProfileRepository restarted = new ProfileRepository(database);
        restarted.migrate();
        var profile = restarted.find(uuid).orElseThrow();
        assertEquals(42, profile.playTimeSeconds());
        assertEquals("false", profile.settings().get("chat.enabled"));
    }

    @Test
    void distinctJavaAndFloodgateUuidsNeverCollide(@TempDir Path directory) {
        ProfileRepository repository = repository(directory);
        UUID javaUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID floodgateUuid = UUID.fromString("00000000-0000-0000-0009-01f64f65c7c3");
        repository.join(javaUuid, "Alex", Instant.EPOCH);
        repository.join(floodgateUuid, ".Alex", Instant.EPOCH);

        assertNotEquals(repository.find(javaUuid).orElseThrow().uuid(), repository.find(floodgateUuid).orElseThrow().uuid());
        assertEquals("Alex", repository.find(javaUuid).orElseThrow().latestName());
        assertEquals(".Alex", repository.find(floodgateUuid).orElseThrow().latestName());
    }

    private static ProfileRepository repository(Path directory) {
        ProfileRepository repository = new ProfileRepository(directory.resolve("hardcraft.db"));
        repository.migrate();
        return repository;
    }
}