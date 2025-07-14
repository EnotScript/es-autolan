package com.autolan.enotscript;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Управление LAN-сервером:
 * - открытие сервера в LAN
 * - обновление лимита игроков
 * - обновление MOTD
 * - синхронизация данных с клиентами
 */
public class LanServerManager {
    private static final Logger LOGGER = LogManager.getLogger("AutoLan");

    /**
     * Публикует одиночный мир в LAN с текущими настройками.
     */
    public static void openToLan(IntegratedServer server) {
        if (!AutoLan.CONFIG.enabled.get()) return;

        boolean success = server.publishServer(
                AutoLan.CONFIG.gameType.get(),
                AutoLan.CONFIG.allowCheats.get(),
                AutoLan.CONFIG.port.get()
        );

        if (!success) {
            LOGGER.error("❌ Failed to publish server to LAN");
            return;
        }

        int port = server.getPort();
        setMaxPlayers(server, AutoLan.CONFIG.maxPlayers.get());
        updateServerMOTD(server);
        syncMaxPlayers(server);

        LOGGER.info("✅ Server published on port {}", port);

        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("autolan.lan.started")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(String.valueOf(port)).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(", "))
                            .append(Component.translatable("autolan.lan.maxplayers")
                                    .withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(String.valueOf(AutoLan.CONFIG.maxPlayers.get())).withStyle(ChatFormatting.AQUA))
                            ),
                    false
            );
        }
    }

    /**
     * Устанавливает лимит игроков через рефлексию.
     */
    public static void setMaxPlayers(IntegratedServer server, int newMax) {
        try {
            Object playerList = server.getPlayerList();
            Class<?> superClass = playerList.getClass().getSuperclass();

            for (Field field : superClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getType() == int.class) {
                    int value = field.getInt(playerList);
                    if (value == 8 || value == AutoLan.CONFIG.maxPlayers.get()) {
                        field.setInt(playerList, newMax);
                        LOGGER.info("✅ Max players set to {} via field '{}'", newMax, field.getName());
                        return;
                    }
                }
            }

            LOGGER.warn("⚠️ Could not find maxPlayers field in PlayerList superclass");
        } catch (Exception e) {
            LOGGER.error("❌ Failed to set maxPlayers: {}", e.getMessage());
        }
    }

    /**
     * Синхронизирует ограничение игроков с клиентами через пакет границы мира.
     */
    public static void syncMaxPlayers(IntegratedServer server) {
        try {
            WorldBorder border = server.overworld().getWorldBorder();
            ClientboundInitializeBorderPacket packet = new ClientboundInitializeBorderPacket(border);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(packet);
            }

            LOGGER.debug("🔄 Synced maxPlayers to all clients");
        } catch (Exception e) {
            LOGGER.error("❌ Failed to sync maxPlayers: {}", e.getMessage());
        }
    }

    /**
     * Обновляет MOTD одиночного мира через рефлексию.
     */
    public static void updateServerMOTD(IntegratedServer server) {
        try {
            String newMotd = AutoLan.CONFIG.motd.get();
            Class<?> current = server.getClass();

            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getType() == String.class) {
                        String value = (String) field.get(server);
                        if (value != null && !value.isEmpty()) {
                            field.set(server, newMotd);
                            LOGGER.info("✅ MOTD updated to '{}' via field '{}'", newMotd, field.getName());
                            return;
                        }
                    }
                }
                current = current.getSuperclass();
            }

            LOGGER.warn("⚠️ MOTD field not found in class hierarchy");
        } catch (Exception e) {
            LOGGER.error("❌ Failed to update MOTD: {}", e.getMessage());
        }
    }
}
