package mods.thecomputerizer.musictriggers;

import mods.thecomputerizer.musictriggers.client.EventsClient;
import mods.thecomputerizer.musictriggers.client.audio.ChannelManager;
import mods.thecomputerizer.musictriggers.util.CustomTick;
import mods.thecomputerizer.musictriggers.util.events.AdvancementEvent;
import mods.thecomputerizer.musictriggers.util.events.LivingDamageEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class MusicTriggersClient implements ClientModInitializer {

    public static KeyBinding GUI;

    @Override
    public void onInitializeClient() {
        setUpClientEvents();
        GUI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.musictriggers.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.musictriggers"
        ));
    }

    private static void setUpClientEvents() {

        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> EventsClient.onDisconnect());

        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            //EventsClient.imageCards(matrixStack);
            EventsClient.debugInfo(matrixStack);
            EventsClient.renderBoss();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (GUI.wasPressed()) EventsClient.onKeyInput();
            ChannelManager.tickChannels();
            EventsClient.onTick();
        });

        LivingDamageEvent.EVENT.register(((entity, damageSource) -> {
            //EventsClient.onDamage(entity, damageSource);
            return ActionResult.PASS;
        }));

        AdvancementEvent.EVENT.register(((advancement) -> {
            EventsClient.onAdvancement(advancement);
            return ActionResult.PASS;
        }));

        CustomTick.setUp();
    }
}