package com.relay.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

import java.util.UUID;

/**
 * 对"发消息"的抽象：聊天、动作栏、标题、音效。
 * 功能模块不直接碰 ServerPlayer 的网络连接。
 */
public interface RelayMessenger {

    void chat(Component message);

    void actionBar(Component message);

    void title(Component title, Component subtitle, int fadeIn, int stay, int fadeOut);

    void sound(SoundEvent sound, float volume, float pitch);

    void chatTo(UUID player, Component message);

    void titleTo(UUID player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut);

    void soundTo(UUID player, SoundEvent sound, float volume, float pitch);
}
