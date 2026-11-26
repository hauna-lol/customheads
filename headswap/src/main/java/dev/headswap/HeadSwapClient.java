package dev.headswap;

import dev.headswap.command.HeadSwapCommands;
import dev.headswap.config.ConfigManager;
import dev.headswap.exclusion.ExclusionManager;
import dev.headswap.input.HeadSwapKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeadSwapClient implements ClientModInitializer {
    public static final String MOD_ID = "headswap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        ExclusionManager.reload();

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> HeadSwapCommands.register(dispatcher));
        HeadSwapKeybinds.register();

        LOGGER.info("HeadSwap cargado (solo cliente, solo visual).");
    }
}
