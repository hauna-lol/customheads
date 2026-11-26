package dev.headswap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.headswap.HeadSwapClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carga/guarda config/headswap.json de forma local. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile HeadSwapConfig config = HeadSwapConfig.createDefault();

    private ConfigManager() {}

    public static HeadSwapConfig get() {
        return config;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("headswap.json");
    }

    public static synchronized void load() {
        Path p = path();
        HeadSwapConfig loaded = null;
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(r, HeadSwapConfig.class);
            } catch (Exception e) {
                HeadSwapClient.LOGGER.error("No se pudo leer {} (se usa la configuracion por defecto en memoria)", p, e);
            }
        }
        if (loaded == null) {
            loaded = HeadSwapConfig.createDefault();
        }
        if (loaded.replacements == null) loaded.replacements = new java.util.ArrayList<>();
        if (loaded.excludedPlayers == null) loaded.excludedPlayers = new java.util.ArrayList<>();
        loaded.replacements.removeIf(r -> r == null || r.target == null);
        loaded.excludedPlayers.removeIf(e -> e == null || e.uuid == null);
        config = loaded;
        save();
    }

    public static synchronized void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(config, w);
            }
        } catch (IOException e) {
            HeadSwapClient.LOGGER.error("No se pudo guardar {}", p, e);
        }
    }
}
