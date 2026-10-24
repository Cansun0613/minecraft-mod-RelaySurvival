package com.relay.api;

import java.util.UUID;

public record TurnInfo(int index, int total, UUID playerId, String playerName, int remaining, int turnSeconds) {

    public TurnInfo withRemaining(int seconds) {
        return new TurnInfo(index, total, playerId, playerName, seconds, turnSeconds);
    }
}
