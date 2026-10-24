package com.relay.client;

import com.relay.RelayMod;
import com.relay.client.hud.HudState;
import com.relay.client.hud.InventoryTaskPanel;
import com.relay.client.hud.RelayHudRenderer;
import com.relay.feature.hud.RelayHudPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

public final class RelayClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 任何一步失败都只停用 HUD，绝不让客户端起不来
        try {
            ClientPlayNetworking.registerGlobalReceiver(RelayHudPayload.TYPE,
                    (payload, context) -> context.client().execute(() -> HudState.accept(payload)));
        } catch (Throwable t) {
            RelayMod.LOGGER.error("HUD 网络接收器注册失败，HUD 将被停用（游戏可正常启动）", t);
            return;
        }

        try {
            HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("relay", "hud"), new RelayHudRenderer());
        } catch (Throwable t) {
            RelayMod.LOGGER.error("HUD 绘制层注册失败，HUD 将被停用（游戏可正常启动）", t);
        }

        try {
            // 打开背包时，在背包左侧画详细任务面板（画在屏幕最上层）
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                if (screen instanceof AbstractContainerScreen<?>) {
                    HudState.containerOpen = true;
                    ScreenEvents.remove(screen).register(closed -> HudState.containerOpen = false);
                    ScreenEvents.afterForeground(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                            InventoryTaskPanel.render(graphics, s));
                } else {
                    HudState.containerOpen = false;
                }
            });
        } catch (Throwable t) {
            RelayMod.LOGGER.error("背包任务面板注册失败（其它功能不受影响）", t);
        }
    }
}
