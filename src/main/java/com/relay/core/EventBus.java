package com.relay.core;

import com.relay.api.RelayEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class EventBus implements RelayEvents {

    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    @Override
    public <T> void subscribe(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list == null) {
            return;
        }
        for (Consumer<?> consumer : List.copyOf(list)) {
            try {
                ((Consumer<T>) consumer).accept(event);
            } catch (Throwable t) {
                com.relay.RelayMod.LOGGER.error("event listener failed for {}", event.getClass().getSimpleName(), t);
            }
        }
    }
}
