package com.autolan.enotscript;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod("autolan")
public class AutoLan {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final Config CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    static {
        var specPair = new ModConfigSpec.Builder().configure(Config::new);
        CONFIG = specPair.getLeft();
        CONFIG_SPEC = specPair.getRight();
    }

    public AutoLan(ModContainer modContainer) {
        LOGGER.info("AutoLan mod initializing...");
        modContainer.registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new AutoJoinHandler());
        NeoForge.EVENT_BUS.register(new AutoLanOpener());

    }


    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandHandler.registerCommands(event.getDispatcher());
    }
}
