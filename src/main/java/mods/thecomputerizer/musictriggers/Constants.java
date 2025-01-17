package mods.thecomputerizer.musictriggers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Constants {
    public static final String MODID = "musictriggers";
    public static final ResourceLocation ICON_LOCATION = new ResourceLocation(MODID,"textures/logo.png");
    public static final Logger MAIN_LOG = LogManager.getLogger("Music Triggers");
    public static final File CONFIG_DIR = new File("config/MusicTriggers");
    public static final char[] BLACKLISTED_TABLE_CHARACTERS = new char[]{' ','.',')','(','[',']'};
    public static ResourceLocation res(String path) {
        return new ResourceLocation(MODID,path);
    }

    /**
     * In case I forget to or choose not remove some log spam, this will only ensure it only happens in dev
     */
    private static final boolean IS_DEV = false;

    public static boolean isDev() {
        return IS_DEV;
    }


    /**
     * Used for dev purposes only for easier debuging purposes
     */
    @Environment(EnvType.CLIENT)
    public static void debugError(String message, Object ... parameters) {
        if(IS_DEV && !Minecraft.getInstance().isPaused()) MAIN_LOG.error(message,parameters);
    }

    /**
     * Minecraft is a client only class so it cant be checked from the server without packets
     */
    public static void debugErrorServer(String message, Object ... parameters) {
        if(IS_DEV) MAIN_LOG.error(message,parameters);
    }
}
