package com.hardcraft.items;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetadataCodecTest {
    private final MetadataCodec codec = new MetadataCodec();

    @Test void roundTripsMetadataDeterministically() throws Exception {
        Map<String, String> first = new LinkedHashMap<>(); first.put("charge", "7"); first.put("origin", "quest");
        Map<String, String> second = new LinkedHashMap<>(); second.put("origin", "quest"); second.put("charge", "7");
        assertArrayEquals(codec.encode(first), codec.encode(second));
        assertEquals(first, codec.decode(codec.encode(first)));
    }

    @Test void rejectsCorruptMetadata() {
        assertThrows(IOException.class, () -> codec.decode(new byte[] {0, 0, 0, 65}));
        byte[] valid = codec.encode(Map.of("key", "value"));
        byte[] trailing = java.util.Arrays.copyOf(valid, valid.length + 1);
        assertThrows(IOException.class, () -> codec.decode(trailing));
    }

    @Test void enforcesMetadataBoundary() {
        Map<String, String> excessive = new java.util.HashMap<>();
        for (int i = 0; i < 65; i++) excessive.put("k" + i, "v");
        assertThrows(IllegalArgumentException.class, () -> codec.encode(excessive));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(Map.of("", "v")));
    }
}