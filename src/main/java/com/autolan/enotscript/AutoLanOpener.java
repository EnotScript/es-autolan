package com.autolan.enotscript;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoLanOpener {
    private static final Logger LOGGER = LogManager.getLogger();
    private boolean lanOpened = false;
    private boolean worldLoaded = false;

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean currentWorldLoaded = mc.level != null && mc.screen == null;

        // Если мир только загрузился — открываем LAN
        if (currentWorldLoaded && !worldLoaded) {
            IntegratedServer server = mc.getSingleplayerServer();
            if (server != null && !server.isPublished()) {
                LOGGER.info("📡 Открытие LAN-сервера при загрузке мира...");
                LanServerManager.openToLan(server);
                lanOpened = true;
            }
        }

        // Если мир выгружается — сбрасываем флаг
        if (!currentWorldLoaded && worldLoaded && lanOpened) {
            lanOpened = false;
            LOGGER.info("🌙 Выход из мира — флаг lanOpened сброшен");
        }

        worldLoaded = currentWorldLoaded;
    }
}
