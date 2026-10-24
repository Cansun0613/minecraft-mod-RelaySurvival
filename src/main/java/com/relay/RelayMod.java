package com.relay;

import com.relay.api.RelayBootstrap;
import com.relay.core.RelayCommand;
import com.relay.core.RelayCore;
import com.relay.core.RelayRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public final class RelayMod implements ModInitializer {

    public static final String MOD_ID = "relay";
    public static final Logger LOGGER = LoggerFactory.getLogger("RelaySurvival");

    @Override
    public void onInitialize() {
        // 必须最先执行：网络通道要在任何客户端/服务端逻辑之前注册
        runBootstraps();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RelayRuntime runtime = new RelayRuntime(server);
            RelayCore.set(runtime);
            runtime.bootstrap();
            LOGGER.info("接力生存已就绪，使用 /relay start 开局");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            RelayCore.optional().ifPresent(RelayRuntime::shutdown);
            RelayCore.set(null);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> RelayCore.optional().ifPresent(RelayRuntime::tick));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                RelayCore.optional().ifPresent(rt -> rt.onJoin(handler.player)));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                RelayCore.optional().ifPresent(rt -> rt.onLeave(handler.player)));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            RelayCore.optional().ifPresent(rt -> {
                if (entity instanceof ServerPlayer player) {
                    rt.onPlayerDeath(player);
                } else if (entity instanceof EnderDragon) {
                    rt.onDragonKilled();
                }
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                RelayCommand.register(dispatcher));
    }

    private static void runBootstraps() {
        var iterator = ServiceLoader.load(RelayBootstrap.class, RelayMod.class.getClassLoader()).iterator();
        while (true) {
            RelayBootstrap bootstrap;
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                bootstrap = iterator.next();
            } catch (Throwable t) {
                LOGGER.error("跳过无法加载的早期初始化项", t);
                continue;
            }
            try {
                bootstrap.onInitialize();
                LOGGER.info("早期初始化 {} 完成", bootstrap.id());
            } catch (Throwable t) {
                LOGGER.error("早期初始化 {} 失败（已跳过）", bootstrap.id(), t);
            }
        }
    }
}
