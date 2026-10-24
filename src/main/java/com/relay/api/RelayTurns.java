package com.relay.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelayTurns {

    boolean running();

    int index();

    int total();

    Optional<UUID> current();

    Optional<UUID> next();

    List<UUID> order();

    TurnInfo info();

    /** 立刻交接给下一棒；已经是最后一棒则结束整局。 */
    void handoff(TurnEndReason reason);

    void skip();

    void start(List<UUID> order);

    void stop(boolean victory);
}
