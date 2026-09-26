package com.hardcraft.core.api;

import java.util.Optional;

public interface ServiceRegistry {
    <T> void register(Class<T> type, T service);

    <T> Optional<T> find(Class<T> type);

    <T> void unregister(Class<T> type);

    void clear();
}