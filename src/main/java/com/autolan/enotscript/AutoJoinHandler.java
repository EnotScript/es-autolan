package com.autolan.enotscript;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
                    try {
                        autoJoinTriggered = true;
                        LOGGER.info("⏳ Автоматический вход в мир: {}", worldName);

                        mc.forceSetScreen(null);
                        mc.createWorldOpenFlows().openWorld(worldName, () -> {
                            LOGGER.info("Мир {} загружен", worldName);
                        });
                    } catch (Exception e) {
                        LOGGER.error("Ошибка загрузки мира '{}'", worldName, e);
                    }
                }
            }
        }
    }
}
