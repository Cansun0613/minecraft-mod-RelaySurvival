package com.relay.api;

import java.util.List;
import java.util.UUID;

public record SessionInfo(List<UUID> order, int turnSeconds) {
}
