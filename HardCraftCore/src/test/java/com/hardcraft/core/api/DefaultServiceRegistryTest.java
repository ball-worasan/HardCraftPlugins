package com.hardcraft.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DefaultServiceRegistryTest {
    @Test
    void reportsMissingServicesWithoutInventingValues() {
        assertTrue(new DefaultServiceRegistry().find(Runnable.class).isEmpty());
    }

    @Test
    void registersTypedServiceOnceAndCanClearIt() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        Runnable service = () -> {};
        registry.register(Runnable.class, service);
        assertEquals(service, registry.find(Runnable.class).orElseThrow());
        assertThrows(IllegalStateException.class, () -> registry.register(Runnable.class, service));
        registry.clear();
        assertTrue(registry.find(Runnable.class).isEmpty());
    }
}