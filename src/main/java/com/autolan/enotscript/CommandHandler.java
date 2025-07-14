package com.autolan.enotscript;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.GameType;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CommandHandler {

    /**
     * SuggestionProvider для имен миров — подсказывает названия директорий из папки saves
     */
    public static final SuggestionProvider<CommandSourceStack> WORLD_NAME_SUGGESTIONS = (context, builder) -> {
        Minecraft mc = Minecraft.getInstance();
        File savesFolder = new File(mc.gameDirectory, "saves");
        if (savesFolder.exists() && savesFolder.isDirectory()) {
            String prefix = builder.getRemaining().toLowerCase();
            for (File file : Objects.requireNonNull(savesFolder.listFiles())) {
                if (file.isDirectory() && file.getName().toLowerCase().startsWith(prefix)) {
                    builder.suggest(file.getName());
                }
            }
        }
        return builder.buildFuture();
    };

    /**
     * Регистрация всех подкоманд /autolan
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> baseCommand = Commands.literal("autolan")
                .requires(source -> source.hasPermission(2));

        // /autolan status
        baseCommand.then(Commands.literal("status")
                .executes(context -> {
                    sendStatusMessage(context.getSource());
                    return 1;
                }));

        // /autolan toggle
        baseCommand.then(Commands.literal("toggle")
                .executes(context -> {
                    boolean newValue = !AutoLan.CONFIG.enabled.get();
                    setConfig(() -> AutoLan.CONFIG.enabled.set(newValue));
                    context.getSource().sendSuccess(
                            () -> Component.literal("AutoLan " + (newValue ? "включён" : "выключен"))
                                    .withStyle(newValue ? Style.EMPTY.withColor(0x22AA22) : Style.EMPTY.withColor(0xAA0000)),
                            true
                    );
                    return 1;
                }));

        // /autolan port <число>
        baseCommand.then(Commands.literal("port")
                .then(Commands.argument("port", IntegerArgumentType.integer(1024, 49151))
                        .executes(context -> {
                            int newPort = IntegerArgumentType.getInteger(context, "port");
                            setConfig(() -> AutoLan.CONFIG.port.set(newPort));
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Порт изменён на: " + newPort),
                                    true
                            );
                            return 1;
                        })));

        // /autolan gametype <режим>
        baseCommand.then(Commands.literal("gametype")
                .then(Commands.argument("type", StringArgumentType.word())
                        // Подсказки из списка доступных режимов игры
                        .suggests((ctx, builder) -> {
                            for (GameType gt : GameType.values()) {
                                builder.suggest(gt.getName());
                            }
                            return builder.buildFuture();
                        })
                        // Логика выполнения — проверка и применение режима
                        .executes(context -> {
                            String typeName = StringArgumentType.getString(context, "type").toLowerCase();

                            GameType foundType = null;
                            for (GameType gt : GameType.values()) {
                                if (gt.getName().equalsIgnoreCase(typeName)) {
                                    foundType = gt;
                                    break;
                                }
                            }

                            if (foundType == null) {
                                context.getSource().sendFailure(Component.literal(
                                        "Недопустимый режим игры! Допустимые значения: survival, creative, adventure, spectator"
                                ).withStyle(Style.EMPTY.withColor(0xAA0000)));
                                return 0;
                            }

                            final GameType newType = foundType;
                            setConfig(() -> AutoLan.CONFIG.gameType.set(newType));

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Режим игры изменён на: " + newType.getName()),
                                    true
                            );
                            return 1;
                        })));

        // /autolan cheats
        baseCommand.then(Commands.literal("cheats")
                .executes(context -> {
                    boolean newValue = !AutoLan.CONFIG.allowCheats.get();
                    setConfig(() -> AutoLan.CONFIG.allowCheats.set(newValue));
                    context.getSource().sendSuccess(
                            () -> Component.literal("Читы " + (newValue ? "разрешены" : "запрещены")),
                            true
                    );
                    return 1;
                }));

        // /autolan maxplayers <число>
        baseCommand.then(Commands.literal("maxplayers")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                        .executes(context -> {
                            int newCount = IntegerArgumentType.getInteger(context, "count");
                            setConfig(() -> AutoLan.CONFIG.maxPlayers.set(newCount));

                            var server = Minecraft.getInstance().getSingleplayerServer();
                            if (server != null && server.isPublished()) {
                                LanServerManager.setMaxPlayers(server, newCount);
                                LanServerManager.updateServerMOTD(server);
                                LanServerManager.syncMaxPlayers(server);
                            }

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Максимум игроков установлен: " + newCount),
                                    true
                            );
                            return 1;
                        })));

        // /autolan motd <текст>
        baseCommand.then(Commands.literal("motd")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> {
                            String newMotd = StringArgumentType.getString(context, "text");
                            setConfig(() -> AutoLan.CONFIG.motd.set(newMotd));

                            var server = Minecraft.getInstance().getSingleplayerServer();
                            if (server != null && server.isPublished()) {
                                LanServerManager.updateServerMOTD(server);
                            }

                            context.getSource().sendSuccess(
                                    () -> Component.literal("MOTD обновлён: " + newMotd),
                                    true
                            );
                            return 1;
                        })));

        // /autolan reload
        baseCommand.then(Commands.literal("reload")
                .executes(context -> {
                    AutoLan.CONFIG_SPEC.save();
                    context.getSource().sendSuccess(
                            () -> Component.literal("Конфигурация сохранена"),
                            true
                    );
                    return 1;
                }));

        // /autolan autojoinDelaySeconds <секунды>
        baseCommand.then(Commands.literal("autojoinDelaySeconds")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 60))
                        .executes(context -> {
                            int newDelay = IntegerArgumentType.getInteger(context, "seconds");
                            setConfig(() -> AutoLan.CONFIG.autojoinDelaySeconds.set(newDelay));
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Задержка автозагрузки установлена: " + newDelay + " секунд"),
                                    true
                            );
                            return 1;
                        })));

        // /autolan autojoinWorldName <имя мира>
        baseCommand.then(Commands.literal("autojoinWorldName")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(WORLD_NAME_SUGGESTIONS)
                        .executes(context -> {
                            String newWorldName = StringArgumentType.getString(context, "name");
                            setConfig(() -> AutoLan.CONFIG.autojoinWorldName.set(newWorldName));
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Имя мира для автозагрузки установлено: " + newWorldName),
                                    true
                            );
                            return 1;
                        })));

        dispatcher.register(baseCommand);
    }

    /**
     * Выводит текущий статус всех параметров AutoLan
     */

    private static MutableComponent statusLine(String key, String value) {
        return Component.translatable(key).withStyle(style -> style.withColor(0x55FF55))
                .append(Component.literal(": ").withStyle(style -> style.withColor(0xAAAAAA)))
                .append(Component.literal(value).withStyle(style -> style.withColor(0xFFFFFF)))
                .append(Component.literal("\n"));
    }
    private static void sendStatusMessage(CommandSourceStack source) {
        source.sendSuccess(() -> Component.empty()
                        .append(Component.translatable("autolan.status.header").withStyle(style -> style.withColor(0xFFD700)))
                        .append(Component.literal("\n"))

                        .append(statusLine("autolan.status.enabled", String.valueOf(AutoLan.CONFIG.enabled.get())))
                        .append(statusLine("autolan.status.port", String.valueOf(AutoLan.CONFIG.port.get())))
                        .append(statusLine("autolan.status.gametype", AutoLan.CONFIG.gameType.get().getName()))
                        .append(statusLine("autolan.status.cheats", String.valueOf(AutoLan.CONFIG.allowCheats.get())))
                        .append(statusLine("autolan.status.maxplayers", String.valueOf(AutoLan.CONFIG.maxPlayers.get())))
                        .append(statusLine("autolan.status.motd", AutoLan.CONFIG.motd.get()))
                        .append(statusLine("autolan.status.world", AutoLan.CONFIG.autojoinWorldName.get()))
                        .append(statusLine("autolan.status.delay", AutoLan.CONFIG.autojoinDelaySeconds.get() + " сек"))
                , false);
    }



    /**
     * Упрощённый метод для изменения конфига с сохранением
     */
    private static void setConfig(Runnable setter) {
        setter.run();
        AutoLan.CONFIG_SPEC.save();
    }
}
