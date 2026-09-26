package com.hardcraft.core.api;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DefaultServiceRegistry implements ServiceRegistry {
    private final ConcurrentMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");
        Object existing = services.putIfAbsent(type, type.cast(service));
        if (existing != null) {
            throw new IllegalStateException("Service already registered: " + type.getName());
        }
    }

    @Override
    public <T> Optional<T> find(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return Optional.ofNullable(type.cast(services.get(type)));
    }

    @Override
    public <T> void unregister(Class<T> type) {
        services.remove(Objects.requireNonNull(type, "type"));
    }

    @Override
    public void clear() {
        services.clear();
    }
}