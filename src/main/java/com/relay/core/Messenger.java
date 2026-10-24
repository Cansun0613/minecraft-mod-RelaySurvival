package com.relay.core;

import com.relay.api.RelayMessenger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

public final class Messenger implements RelayMessenger {

    private final MinecraftServer server;

    public Messenger(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void chat(Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    @Override
    public void actionBar(Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendOverlayMessage(message);
        }
    }

    @Override
    public void title(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            titleTo(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    @Override
    public void sound(SoundEvent sound, float volume, float pitch) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            soundTo(player, sound, volume, pitch);
        }
    }

    @Override
    public void chatTo(UUID playerId, Component message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }

    @Override
    public void titleTo(UUID playerId, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            titleTo(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    private void titleTo(ServerPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        var connection = player.connection;
        connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        if (subtitle != null) {
            connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
        connection.send(new ClientboundSetTitleTextPacket(title));
    }

    @Override
    public void soundTo(UUID playerId, SoundEvent sound, float volume, float pitch) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            soundTo(player, sound, volume, pitch);
        }
    }

    private void soundTo(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.MASTER, volume, pitch);
    }
}
