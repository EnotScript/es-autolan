package com.autolan.enotscript;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

/**
 * Утилиты для форматирования локализованных сообщений Minecraft (Yarn mappings 1.21.1).
 */
public class MessageUtils {
    private static final ChatFormatting PREFIX_FORMATTING = ChatFormatting.GRAY;
    private static final ChatFormatting KEY_FORMATTING = ChatFormatting.GRAY;
    private static final ChatFormatting VALUE_FORMATTING = ChatFormatting.WHITE;
    private static final ChatFormatting NUMBER_FORMATTING = ChatFormatting.AQUA;

    public static MutableComponent keyValue(String keyTranslationKey, Component value) {
        return Component.translatable(keyTranslationKey).withStyle(KEY_FORMATTING)
                .append(Component.literal(": ").withStyle(KEY_FORMATTING))
                .append(((MutableComponent) value).withStyle(VALUE_FORMATTING));
    }

    public static MutableComponent keyValue(String keyTranslationKey, int number) {
        return keyValue(keyTranslationKey, Component.literal(String.valueOf(number)).withStyle(NUMBER_FORMATTING));
    }
}
