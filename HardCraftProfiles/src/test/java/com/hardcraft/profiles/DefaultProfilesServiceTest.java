package com.hardcraft.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DefaultProfilesServiceTest {
    @Test
    void orderedJoinQuitAndRestartAccumulateTime(@TempDir Path directory) {
        Path database = directory.resolve("hardcraft.db");
        ProfileRepository repository = new ProfileRepository(database);
        repository.migrate();
        UUID uuid = UUID.randomUUID();
        try (DefaultProfilesService service = new DefaultProfilesService(repository)) {
            service.joined(uuid, "Player", Instant.EPOCH);
            service.left(uuid, Instant.EPOCH.plusSeconds(20)).join();
        }
        try (DefaultProfilesService restarted = new DefaultProfilesService(new ProfileRepository(database))) {
            restarted.joined(uuid, "Player", Instant.EPOCH.plusSeconds(100)).join();
            restarted.left(uuid, Instant.EPOCH.plusSeconds(130)).join();
            assertEquals(50, restarted.find(uuid).join().orElseThrow().playTimeSeconds());
        }
    }
}