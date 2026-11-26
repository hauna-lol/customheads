package dev.headswap.input;

import dev.headswap.config.ConfigManager;
import dev.headswap.exclusion.ExclusionManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Atajos opcionales (sin tecla asignada por defecto; se configuran en Controles > HeadSwap). */
public final class HeadSwapKeybinds {
    private static KeyBinding toggleHide;
    private static KeyBinding toggleSelf;

    private HeadSwapKeybinds() {}

    public static void register() {
        toggleHide = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.headswap.toggle_hide", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.headswap"));
        toggleSelf = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.headswap.toggle_self", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.headswap"));

        ClientTickEvents.END_CLIENT_TICK.register(HeadSwapKeybinds::onTick);
    }

    private static void onTick(MinecraftClient client) {
        while (toggleHide.wasPressed()) {
            var cfg = ConfigManager.get();
            cfg.hideHeads = !cfg.hideHeads;
            ConfigManager.save();
            actionBar(client, "Ocultar cabezas: " + (cfg.hideHeads ? "ACTIVADO" : "desactivado"));
        }
        while (toggleSelf.wasPressed()) {
            if (client.player == null) continue;
            boolean excluded = ExclusionManager.toggle(client.player.getUuid(), client.player.getGameProfile().getName());
            actionBar(client, "Reset Head (tu jugador): " + (excluded ? "ACTIVADO (cabeza original)" : "desactivado"));
        }
    }

    private static void actionBar(MinecraftClient client, String msg) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[HeadSwap] " + msg), true);
        }
    }
}
