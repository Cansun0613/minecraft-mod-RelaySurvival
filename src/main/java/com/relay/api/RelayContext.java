package com.relay.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * 功能模块能看到的全部能力。核心只暴露接口，不暴露实现类。
 */
public interface RelayContext extends RelayServices {

    RelayConfig config();

    RelayTimer timer();

    RelayTurns turns();

    RelaySnapshots snapshots();

    RelayMessenger messenger();

    RelayEvents events();

    MinecraftServer server();

    default Optional<ServerPlayer> player(java.util.UUID id) {
        return Optional.ofNullable(server().getPlayerList().getPlayer(id));
    }

    default String nameOf(java.util.UUID id) {
        return player(id).map(p -> p.getGameProfile().name()).orElse(id.toString().substring(0, 8));
    }
}
