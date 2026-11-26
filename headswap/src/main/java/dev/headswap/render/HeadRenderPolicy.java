package dev.headswap.render;

import dev.headswap.config.ConfigManager;
import dev.headswap.config.HeadSwapConfig;
import dev.headswap.exclusion.ExclusionManager;
import dev.headswap.profile.HeadProfileResolver;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.Locale;

/**
 * Decide QUE ItemStack de cabeza se dibuja para un jugador. Solo afecta al renderizado local:
 * el equipamiento real del jugador no se toca nunca.
 */
public final class HeadRenderPolicy {

    private HeadRenderPolicy() {}

    public static boolean isSkull(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof AbstractSkullBlock;
    }

    /**
     * @param original lo que el jugador lleva realmente en la ranura de cabeza
     * @return lo que se debe dibujar: el original, ItemStack.EMPTY (ocultar) o la cabeza de reemplazo
     */
    public static ItemStack apply(PlayerEntity player, ItemStack original) {
        // Solo cabezas de equipamiento (skulls). Cascos, calabazas, etc. no se tocan.
        if (!isSkull(original)) return original;

        HeadSwapConfig config = ConfigManager.get();
        boolean excluded = ExclusionManager.isExcluded(player.getUuid());

        // 1) Hide Heads (independiente de la lista de excluidos, salvo que se pida lo contrario)
        if (config.hideHeads && !(excluded && config.hideIgnoresExcluded)) {
            return ItemStack.EMPTY;
        }

        // 2) Reset Head: jugador excluido -> cabeza original
        if (excluded || !config.enabled) {
            return original;
        }

        // 3) Reemplazo
        HeadSwapConfig.Replacement rule = findRule(config, original);
        if (rule == null) return original;

        ItemStack replacement = HeadProfileResolver.resolve(rule.source);
        return replacement != null ? replacement : original; // mientras carga, se ve la original
    }

    private static HeadSwapConfig.Replacement findRule(HeadSwapConfig config, ItemStack original) {
        String id = Registries.ITEM.getId(original.getItem()).toString();
        HeadSwapConfig.Replacement wildcard = null;
        for (HeadSwapConfig.Replacement r : config.replacements) {
            if (!r.enabled || r.source == null || r.source.isBlank()) continue;
            String target = normalizeTarget(r.target);
            if (target.equals(id)) return r;
            if (wildcard == null && target.equals("*")) wildcard = r;
        }
        return wildcard;
    }

    public static String normalizeTarget(String target) {
        if (target == null) return "*";
        String t = target.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || t.equals("all") || t.equals("any")) return "*";
        if (t.equals("*")) return t;
        return t.contains(":") ? t : "minecraft:" + t;
    }
}
