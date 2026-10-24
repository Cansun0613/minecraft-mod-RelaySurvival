package com.relay.core;

import java.util.Optional;

public final class RelayCore {

    private static volatile RelayRuntime runtime;

    private RelayCore() {
    }

    public static void set(RelayRuntime value) {
        runtime = value;
    }

    public static RelayRuntime get() {
        RelayRuntime current = runtime;
        if (current == null) {
            throw new IllegalStateException("Relay runtime 尚未初始化");
        }
        return current;
    }

    public static Optional<RelayRuntime> optional() {
        return Optional.ofNullable(runtime);
    }
}
