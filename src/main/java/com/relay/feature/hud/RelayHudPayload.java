package com.relay.feature.hud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务器 → 客户端 的 HUD 状态同步包。
 * 只有 HUD 功能存在时才会发送；客户端没装/没启用时不会报错。
 */
public record RelayHudPayload(
        boolean running,
        int index,
        int total,
        int remaining,
        int turnSeconds,
        String playerName,
        String playerId,
        String category,
        float scale,
        List<TaskLine> tasks
) implements CustomPacketPayload {

    public record TaskLine(
            String category,
            String tier,
            String title,
            String description,
            int progress,
            int target,
            int secondsLeft,
            int timeLimit,
            int successSeconds,
            int failSeconds,
            String state,
            boolean timed
    ) {
    }

    public static final Identifier ID = Identifier.fromNamespaceAndPath("relay", "hud_v2");

    public static final CustomPacketPayload.Type<RelayHudPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RelayHudPayload> CODEC = StreamCodec.of(
            (buf, payload) -> payload.write(buf),
            RelayHudPayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(running);
        buf.writeVarInt(index);
        buf.writeVarInt(total);
        buf.writeVarInt(remaining);
        buf.writeVarInt(turnSeconds);
        buf.writeUtf(playerName == null ? "" : playerName, 64);
        buf.writeUtf(playerId == null ? "" : playerId, 40);
        buf.writeUtf(category == null ? "" : category, 32);
        buf.writeFloat(scale);
        buf.writeVarInt(tasks.size());
        for (TaskLine line : tasks) {
            buf.writeUtf(line.category(), 32);
            buf.writeUtf(line.tier() == null ? "C" : line.tier(), 4);
            buf.writeUtf(line.title(), 128);
            buf.writeUtf(line.description() == null ? "" : line.description(), 512);
            buf.writeVarInt(line.progress());
            buf.writeVarInt(line.target());
            buf.writeVarInt(line.secondsLeft());
            buf.writeVarInt(line.timeLimit());
            buf.writeVarInt(line.successSeconds());
            buf.writeVarInt(line.failSeconds());
            buf.writeUtf(line.state() == null ? "ACTIVE" : line.state(), 8);
            buf.writeBoolean(line.timed());
        }
    }

    private static RelayHudPayload read(RegistryFriendlyByteBuf buf) {
        boolean running = buf.readBoolean();
        int index = buf.readVarInt();
        int total = buf.readVarInt();
        int remaining = buf.readVarInt();
        int turnSeconds = buf.readVarInt();
        String name = buf.readUtf(64);
        String playerId = buf.readUtf(40);
        String category = buf.readUtf(32);
        float scale = buf.readFloat();
        int count = buf.readVarInt();
        List<TaskLine> tasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tasks.add(new TaskLine(
                    buf.readUtf(32),
                    buf.readUtf(4),
                    buf.readUtf(128),
                    buf.readUtf(512),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readUtf(8),
                    buf.readBoolean()));
        }
        return new RelayHudPayload(running, index, total, remaining, turnSeconds, name, playerId, category, scale, tasks);
    }
}
