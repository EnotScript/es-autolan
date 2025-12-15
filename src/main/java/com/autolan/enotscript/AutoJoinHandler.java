package com.autolan.enotscript;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class AutoJoinHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private boolean autoJoinTriggered = false;
    private int ticksSinceStart = 0;

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (!AutoLan.CONFIG.enabled.get()) return;
        Minecraft mc = Minecraft.getInstance();

        if ((mc.screen != null) && (mc.level == null) && !autoJoinTriggered) {
            ticksSinceStart++;
            int targetTicks = AutoLan.CONFIG.autojoinDelaySeconds.get() * 20;

            if (ticksSinceStart >= targetTicks) {
                String worldName = AutoLan.CONFIG.autojoinWorldName.get().trim();
                if (!worldName.isEmpty()) {
                    File worldFolder = new File(mc.gameDirectory, "saves" + File.separator + worldName);
                    if (!worldFolder.exists() || !worldFolder.isDirectory()) {
                        LOGGER.warn("Автовход: мир '{}' не найден", worldName);
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.translatable("autolan.autojoin.failed_no_world", worldName).withStyle(style -> style.withColor(0xFF5555)), false);
                        }
                        // Reset counters so we'll try again later if value changes
                        ticksSinceStart = 0;
                        return;
                    }

                    try {
                        autoJoinTriggered = true;
                        LOGGER.info("⏳ Автоматический вход в мир: {}", worldName);
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.translatable("autolan.autojoin.starting", worldName).withStyle(style -> style.withColor(0x55FF55)), false);
                        }

                        mc.forceSetScreen(null);
                        mc.createWorldOpenFlows().openWorld(worldName, () -> {
                            LOGGER.info("Мир {} загружен", worldName);
                        });
                    } catch (Exception e) {
                        LOGGER.error("Ошибка загрузки мира '{}'", worldName, e);
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.literal("Failed to open world: " + worldName).withStyle(style -> style.withColor(0xFF5555)), false);
                        }
                        autoJoinTriggered = false;
                        ticksSinceStart = 0;
                    }
                }
            }
        }
    }
}
