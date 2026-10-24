package com.relay.client.hud;

import com.relay.feature.hud.RelayHudPayload;

import java.util.List;

public final class HudState {

    public static volatile boolean running;
    public static volatile int index;
    public static volatile int total;
    public static volatile int remaining;
    public static volatile int turnSeconds;
    public static volatile String playerName = "";
    public static volatile String playerId = "";
    public static volatile String category = "";
    public static volatile List<RelayHudPayload.TaskLine> tasks = List.of();
    public static volatile float panelScale = 0.75f;
    /** 是否有容器界面打开（打开时由 InventoryTaskPanel 接管显示）。 */
    public static volatile boolean containerOpen;

    private HudState() {
    }

    public static void accept(RelayHudPayload payload) {
        running = payload.running();
        index = payload.index();
        total = payload.total();
        remaining = payload.remaining();
        turnSeconds = payload.turnSeconds();
        playerName = payload.playerName();
        playerId = payload.playerId();
        category = payload.category();
        tasks = payload.tasks();
        panelScale = Math.max(0.5f, Math.min(1.5f, payload.scale()));
    }

    public static float scale() {
        return panelScale;
    }

    public static String clock(int seconds) {
        int s = Math.max(0, seconds);
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
