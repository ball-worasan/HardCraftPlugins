package com.hardcraft.items;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

final class MetadataCodec {
    private static final int MAX_ENTRIES = 64;
    private static final int MAX_TEXT = 1024;

    byte[] encode(Map<String, String> values) {
        if (values.size() > MAX_ENTRIES) throw new IllegalArgumentException("metadata has too many entries");
        try {
            var bytes = new ByteArrayOutputStream();
            try (var output = new DataOutputStream(bytes)) {
                output.writeInt(values.size());
                for (var entry : new TreeMap<>(values).entrySet()) {
                    validate(entry.getKey(), "metadata key");
                    validate(entry.getValue(), "metadata value");
                    output.writeUTF(entry.getKey());
                    output.writeUTF(entry.getValue());
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }

    Map<String, String> decode(byte[] bytes) throws IOException {
        try (var input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int size = input.readInt();
            if (size < 0 || size > MAX_ENTRIES) throw new IOException("invalid metadata entry count");
            Map<String, String> values = new TreeMap<>();
            for (int i = 0; i < size; i++) {
                String key = input.readUTF();
                String value = input.readUTF();
                validate(key, "metadata key"); validate(value, "metadata value");
                if (values.put(key, value) != null) throw new IOException("duplicate metadata key");
            }
            if (input.available() != 0) throw new IOException("trailing metadata bytes");
            return values;
        } catch (IllegalArgumentException invalid) { throw new IOException(invalid.getMessage(), invalid); }
    }

    private static void validate(String value, String label) {
        if (value == null || value.isEmpty() || value.length() > MAX_TEXT) throw new IllegalArgumentException(label + " is invalid");
    }
}