package dev.headswap.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.headswap.config.ConfigManager;
import dev.headswap.config.HeadSwapConfig;
import dev.headswap.exclusion.ExclusionManager;
import dev.headswap.profile.HeadProfileResolver;
import dev.headswap.profile.HeadSource;
import dev.headswap.render.HeadRenderPolicy;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * /headswap ...   y   /resethead ...   (alias de /headswap reset ...)
 * Todo es local: no se envia nada al servidor.
 */
public final class HeadSwapCommands {

    private static final List<String> SKULL_TARGETS = List.of(
            "all", "player_head", "creeper_head", "skeleton_skull", "wither_skeleton_skull",
            "zombie_head", "piglin_head", "dragon_head");

    private static final SuggestionProvider<FabricClientCommandSource> TARGET_SUGGESTIONS =
            (ctx, builder) -> CommandSource.suggestMatching(SKULL_TARGETS, builder);

    private static final SuggestionProvider<FabricClientCommandSource> RULE_SUGGESTIONS =
            (ctx, builder) -> {
                List<String> l = new ArrayList<>();
                for (HeadSwapConfig.Replacement r : ConfigManager.get().replacements) l.add(r.target);
                return CommandSource.suggestMatching(l, builder);
            };

    private static final SuggestionProvider<FabricClientCommandSource> ONLINE_SUGGESTIONS =
            (ctx, builder) -> {
                List<String> l = new ArrayList<>();
                ClientPlayNetworkHandler h = ctx.getSource().getClient().getNetworkHandler();
                if (h != null) for (PlayerListEntry e : h.getPlayerList()) l.add(e.getProfile().getName());
                return CommandSource.suggestMatching(l, builder);
            };

    private static final SuggestionProvider<FabricClientCommandSource> EXCLUDED_SUGGESTIONS =
            (ctx, builder) -> {
                List<String> l = new ArrayList<>();
                for (HeadSwapConfig.ExcludedPlayer e : ExclusionManager.list()) {
                    if (e.name != null && !e.name.isBlank()) l.add(e.name);
                }
                return CommandSource.suggestMatching(l, builder);
            };

    private HeadSwapCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> root = dispatcher.register(
                literal("headswap")
                        .executes(ctx -> status(ctx))
                        .then(literal("status").executes(ctx -> status(ctx)))
                        .then(literal("reload").executes(ctx -> {
                            ConfigManager.load();
                            ExclusionManager.reload();
                            HeadProfileResolver.clearCache();
                            msg(ctx, "Configuracion recargada.");
                            return 1;
                        }))
                        .then(literal("enable").executes(ctx -> setEnabled(ctx, true)))
                        .then(literal("disable").executes(ctx -> setEnabled(ctx, false)))
                        .then(literal("hide")
                                .executes(ctx -> setHide(ctx, !ConfigManager.get().hideHeads))
                                .then(literal("on").executes(ctx -> setHide(ctx, true)))
                                .then(literal("off").executes(ctx -> setHide(ctx, false))))
                        .then(literal("set")
                                .then(argument("target", StringArgumentType.string())
                                        .suggests(TARGET_SUGGESTIONS)
                                        .then(argument("source", StringArgumentType.greedyString())
                                                .executes(HeadSwapCommands::setRule))))
                        .then(literal("remove")
                                .then(argument("target", StringArgumentType.string())
                                        .suggests(RULE_SUGGESTIONS)
                                        .executes(HeadSwapCommands::removeRule)))
                        .then(literal("list").executes(HeadSwapCommands::listRules))
                        .then(literal("reset")
                                .then(literal("self").executes(HeadSwapCommands::resetSelf))
                                .then(literal("add")
                                        .then(argument("player", StringArgumentType.string())
                                                .suggests(ONLINE_SUGGESTIONS)
                                                .executes(HeadSwapCommands::resetAdd)))
                                .then(literal("remove")
                                        .then(argument("player", StringArgumentType.string())
                                                .suggests(EXCLUDED_SUGGESTIONS)
                                                .executes(HeadSwapCommands::resetRemove)))
                                .then(literal("list").executes(HeadSwapCommands::resetList))
                                .then(literal("clear").executes(ctx -> {
                                    ExclusionManager.clear();
                                    msg(ctx, "Lista de excluidos vaciada.");
                                    return 1;
                                })))
        );
        // Alias: /resethead <self|add|remove|list|clear>
        dispatcher.register(literal("resethead").redirect(root.getChild("reset")));
    }

    // ---------------- general ----------------

    private static int status(CommandContext<FabricClientCommandSource> ctx) {
        HeadSwapConfig c = ConfigManager.get();
        msg(ctx, "Reemplazo: " + (c.enabled ? "ACTIVADO" : "desactivado")
                + " | Ocultar cabezas: " + (c.hideHeads ? "ACTIVADO" : "desactivado")
                + " | Reglas: " + c.replacements.size()
                + " | Excluidos: " + c.excludedPlayers.size());
        msg(ctx, "Comandos: /headswap <enable|disable|hide [on|off]|set|remove|list|reload|reset ...>");
        return 1;
    }

    private static int setEnabled(CommandContext<FabricClientCommandSource> ctx, boolean value) {
        ConfigManager.get().enabled = value;
        ConfigManager.save();
        msg(ctx, "Reemplazo de cabezas " + (value ? "ACTIVADO." : "desactivado."));
        return 1;
    }

    private static int setHide(CommandContext<FabricClientCommandSource> ctx, boolean value) {
        ConfigManager.get().hideHeads = value;
        ConfigManager.save();
        msg(ctx, "Ocultar todas las cabezas " + (value ? "ACTIVADO." : "desactivado."));
        return 1;
    }

    // ---------------- reglas ----------------

    private static int setRule(CommandContext<FabricClientCommandSource> ctx) {
        String target = HeadRenderPolicy.normalizeTarget(StringArgumentType.getString(ctx, "target"));
        String source = StringArgumentType.getString(ctx, "source").trim();
        if (HeadSource.parse(source).kind() == HeadSource.Kind.INVALID) {
            msg(ctx, "Source invalido. Usa un UUID de jugador, un hash de textura (64 hex) o una URL de textures.minecraft.net.");
            return 0;
        }
        List<HeadSwapConfig.Replacement> rules = ConfigManager.get().replacements;
        boolean updated = false;
        for (HeadSwapConfig.Replacement r : rules) {
            if (HeadRenderPolicy.normalizeTarget(r.target).equals(target)) {
                r.source = source;
                r.enabled = true;
                updated = true;
                break;
            }
        }
        if (!updated) rules.add(new HeadSwapConfig.Replacement(target, source, true));
        ConfigManager.save();
        msg(ctx, (updated ? "Regla actualizada: " : "Regla creada: ") + target + " -> " + source);
        return 1;
    }

    private static int removeRule(CommandContext<FabricClientCommandSource> ctx) {
        String target = HeadRenderPolicy.normalizeTarget(StringArgumentType.getString(ctx, "target"));
        boolean removed = ConfigManager.get().replacements
                .removeIf(r -> HeadRenderPolicy.normalizeTarget(r.target).equals(target));
        if (removed) ConfigManager.save();
        msg(ctx, removed ? "Regla eliminada: " + target : "No existe una regla para " + target);
        return removed ? 1 : 0;
    }

    private static int listRules(CommandContext<FabricClientCommandSource> ctx) {
        List<HeadSwapConfig.Replacement> rules = ConfigManager.get().replacements;
        if (rules.isEmpty()) {
            msg(ctx, "No hay reglas de reemplazo.");
            return 1;
        }
        for (HeadSwapConfig.Replacement r : rules) {
            msg(ctx, (r.enabled ? "[x] " : "[ ] ") + HeadRenderPolicy.normalizeTarget(r.target) + " -> " + r.source);
        }
        return 1;
    }

    // ---------------- Reset Head ----------------

    private static int resetSelf(CommandContext<FabricClientCommandSource> ctx) {
        var player = ctx.getSource().getPlayer();
        boolean excluded = ExclusionManager.toggle(player.getUuid(), player.getGameProfile().getName());
        msg(ctx, excluded ? "Reset Head ACTIVADO para ti: veras tu cabeza original."
                          : "Reset Head desactivado para ti: se vuelve a aplicar el reemplazo.");
        return 1;
    }

    private static int resetAdd(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "player");
        Optional<PlayerRef> ref = resolveOnlineOrUuid(ctx.getSource(), input);
        if (ref.isEmpty()) {
            msg(ctx, "No encuentro a '" + input + "' conectado. Usa su UUID para excluirlo sin que este en linea.");
            return 0;
        }
        boolean added = ExclusionManager.add(ref.get().uuid(), ref.get().name());
        msg(ctx, added ? "Excluido: " + ref.get().label() : ref.get().label() + " ya estaba excluido.");
        return 1;
    }

    private static int resetRemove(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "player");
        UUID id = null;
        String label = input;

        UUID parsed = HeadSource.tryParseUuid(input);
        if (parsed != null) {
            id = parsed;
        } else {
            Optional<HeadSwapConfig.ExcludedPlayer> saved = ExclusionManager.findByName(input);
            if (saved.isPresent()) id = HeadSource.tryParseUuid(saved.get().uuid);
            if (id == null) {
                Optional<PlayerRef> online = resolveOnlineOrUuid(ctx.getSource(), input);
                if (online.isPresent()) id = online.get().uuid();
            }
        }
        if (id == null || !ExclusionManager.remove(id)) {
            msg(ctx, "'" + label + "' no esta en la lista de excluidos.");
            return 0;
        }
        msg(ctx, "Reemplazo reactivado para " + label + ".");
        return 1;
    }

    private static int resetList(CommandContext<FabricClientCommandSource> ctx) {
        List<HeadSwapConfig.ExcludedPlayer> list = ExclusionManager.list();
        if (list.isEmpty()) {
            msg(ctx, "No hay jugadores excluidos.");
            return 1;
        }
        for (HeadSwapConfig.ExcludedPlayer e : list) {
            msg(ctx, (e.name == null || e.name.isBlank() ? "(sin nombre)" : e.name) + "  " + e.uuid);
        }
        return 1;
    }

    // ---------------- util ----------------

    private record PlayerRef(UUID uuid, String name) {
        String label() {
            return (name == null || name.isBlank()) ? uuid.toString() : name;
        }
    }

    private static Optional<PlayerRef> resolveOnlineOrUuid(FabricClientCommandSource source, String input) {
        ClientPlayNetworkHandler handler = source.getClient().getNetworkHandler();
        UUID uuid = HeadSource.tryParseUuid(input);
        if (uuid != null) {
            String name = "";
            if (handler != null) {
                PlayerListEntry entry = handler.getPlayerListEntry(uuid);
                if (entry != null) name = entry.getProfile().getName();
            }
            return Optional.of(new PlayerRef(uuid, name));
        }
        if (handler != null) {
            for (PlayerListEntry e : handler.getPlayerList()) {
                if (e.getProfile().getName().equalsIgnoreCase(input)) {
                    return Optional.of(new PlayerRef(e.getProfile().getId(), e.getProfile().getName()));
                }
            }
        }
        return Optional.empty();
    }

    private static void msg(CommandContext<FabricClientCommandSource> ctx, String text) {
        ctx.getSource().sendFeedback(Text.literal("[HeadSwap] " + text));
    }
}
