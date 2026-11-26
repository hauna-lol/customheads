package dev.headswap.exclusion;

import dev.headswap.config.ConfigManager;
import dev.headswap.config.HeadSwapConfig.ExcludedPlayer;
import dev.headswap.profile.HeadSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Lista de jugadores excluidos del reemplazo (Reset Head), por UUID y persistida en la config. */
public final class ExclusionManager {
    private static final Set<UUID> IDS = ConcurrentHashMap.newKeySet();

    private ExclusionManager() {}

    public static void reload() {
        IDS.clear();
        for (ExcludedPlayer e : ConfigManager.get().excludedPlayers) {
            UUID id = HeadSource.tryParseUuid(e.uuid);
            if (id != null) IDS.add(id);
        }
    }

    public static boolean isExcluded(UUID id) {
        return id != null && IDS.contains(id);
    }

    public static boolean add(UUID id, String name) {
        if (!IDS.add(id)) return false;
        ConfigManager.get().excludedPlayers.add(new ExcludedPlayer(id.toString(), name == null ? "" : name));
        ConfigManager.save();
        return true;
    }

    public static boolean remove(UUID id) {
        if (!IDS.remove(id)) return false;
        ConfigManager.get().excludedPlayers.removeIf(e -> id.equals(HeadSource.tryParseUuid(e.uuid)));
        ConfigManager.save();
        return true;
    }

    /** @return true si ahora esta excluido. */
    public static boolean toggle(UUID id, String name) {
        if (isExcluded(id)) {
            remove(id);
            return false;
        }
        add(id, name);
        return true;
    }

    public static void clear() {
        IDS.clear();
        ConfigManager.get().excludedPlayers.clear();
        ConfigManager.save();
    }

    public static List<ExcludedPlayer> list() {
        return new ArrayList<>(ConfigManager.get().excludedPlayers);
    }

    /** Busca en la lista guardada por nombre (para poder quitar jugadores que no estan conectados). */
    public static Optional<ExcludedPlayer> findByName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return ConfigManager.get().excludedPlayers.stream()
                .filter(e -> e.name != null && e.name.toLowerCase(Locale.ROOT).equals(n))
                .findFirst();
    }
}
