package dev.headswap.config;

import java.util.ArrayList;
import java.util.List;

/** Modelo de datos del archivo config/headswap.json. */
public final class HeadSwapConfig {

    /** Interruptor general del REEMPLAZO de cabezas (no afecta a hideHeads). */
    public boolean enabled = true;

    /** Oculta todas las cabezas de equipamiento (skulls) renderizadas sobre jugadores. */
    public boolean hideHeads = false;

    /** Si es true, los jugadores excluidos (Reset Head) conservan su cabeza original aunque hideHeads este activo. */
    public boolean hideIgnoresExcluded = false;

    /** Reglas de reemplazo. Se usa la primera regla activa con target exacto; si no hay, la regla "*". */
    public List<Replacement> replacements = new ArrayList<>();

    /** Jugadores excluidos del reemplazo (Reset Head), identificados por UUID. */
    public List<ExcludedPlayer> excludedPlayers = new ArrayList<>();

    public static final class Replacement {
        /** Id del item de cabeza (p. ej. "minecraft:creeper_head") o "*" para cualquier cabeza. */
        public String target = "*";
        /** UUID de jugador, hash de textura (64 hex) o URL de textura (textures.minecraft.net). */
        public String source = "";
        public boolean enabled = true;

        public Replacement() {}

        public Replacement(String target, String source, boolean enabled) {
            this.target = target;
            this.source = source;
            this.enabled = enabled;
        }
    }

    public static final class ExcludedPlayer {
        public String uuid = "";
        /** Solo informativo (para listar); la identidad real es el UUID. */
        public String name = "";

        public ExcludedPlayer() {}

        public ExcludedPlayer(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    public static HeadSwapConfig createDefault() {
        HeadSwapConfig c = new HeadSwapConfig();
        // Ejemplo: toda cabeza -> skin de Notch. Cambialo por el UUID que quieras.
        c.replacements.add(new Replacement("*", "069a79f4-44e9-4726-a5be-fca90e38aaf5", true));
        return c;
    }
}
