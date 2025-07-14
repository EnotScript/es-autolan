package com.autolan.enotscript;

import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public final ModConfigSpec.BooleanValue enabled;
    public final ModConfigSpec.IntValue port;
    public final ModConfigSpec.EnumValue<GameType> gameType;
    public final ModConfigSpec.BooleanValue allowCheats;
    public final ModConfigSpec.IntValue maxPlayers;
    public final ModConfigSpec.ConfigValue<String> motd;
    public final ModConfigSpec.ConfigValue<String> autojoinWorldName;
    public final ModConfigSpec.IntValue autojoinDelaySeconds;

    public Config(ModConfigSpec.Builder builder) {
        builder.push("general");
        enabled = builder.comment("Enable automatic opening of the world in LAN")
                .define("enabled", true);
        port = builder.comment("Port for the LAN server")
                .defineInRange("port", 25565, 1024, 49151);
        gameType = builder.comment("Game mode")
                .defineEnum("gameType", GameType.SURVIVAL);
        allowCheats = builder.comment("Allow cheats")
                .define("allowCheats", false);
        maxPlayers = builder.comment("Maximum number of players")
                .defineInRange("maxPlayers", 8, 1, 100);
        motd = builder.comment("Server description (MOTD)")
                .define("motd", "Welcome to the AutoLAN world!");
        autojoinWorldName = builder.comment("Name of the world for automatic joining (folder in saves)")
                .define("autojoinWorldName", "");
        autojoinDelaySeconds = builder.comment("Delay before auto-joining the world (in seconds)")
                .defineInRange("autojoinDelaySeconds", 15, 0, 300);
        builder.pop();
    }
}
