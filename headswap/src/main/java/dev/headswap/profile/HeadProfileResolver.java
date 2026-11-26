package dev.headswap.profile;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import dev.headswap.HeadSwapClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Convierte un "source" (UUID / hash / URL) en un ItemStack PLAYER_HEAD local con SkullOwner.
 * Ese ItemStack solo se usa para renderizar: nunca se envia al servidor ni toca el inventario real.
 * Las consultas de UUID a Mojang se hacen en un hilo aparte y se cachean.
 */
public final class HeadProfileResolver {

    private static final long RETRY_AFTER_MS = 120_000L;

    private static final Map<String, ItemStack> READY = new ConcurrentHashMap<>();
    private static final Map<String, Long> RETRY_AT = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HeadSwap-ProfileLoader");
        t.setDaemon(true);
        return t;
    });

    private HeadProfileResolver() {}

    public static void clearCache() {
        READY.clear();
        RETRY_AT.clear();
        WARNED.clear();
    }

    /**
     * @return el ItemStack de cabeza listo para renderizar, o null si aun se esta descargando / es invalido
     *         (en ese caso el llamador debe mostrar la cabeza original).
     */
    public static ItemStack resolve(String rawSource) {
        if (rawSource == null) return null;
        String key = rawSource.trim();
        if (key.isEmpty()) return null;

        ItemStack ready = READY.get(key);
        if (ready != null) return ready;

        HeadSource source = HeadSource.parse(key);
        switch (source.kind()) {
            case TEXTURE_URL -> {
                ItemStack stack = buildStack(profileFromTextureUrl(source.value()));
                READY.put(key, stack);
                return stack;
            }
            case UUID -> {
                startUuidLoad(key, UUID.fromString(source.value()));
                return null;
            }
            default -> {
                if (WARNED.add(key)) {
                    HeadSwapClient.LOGGER.warn("Source invalido en la configuracion: '{}' (usa UUID, hash de textura o URL)", key);
                }
                return null;
            }
        }
    }

    private static void startUuidLoad(String key, UUID uuid) {
        Long retryAt = RETRY_AT.get(key);
        if (retryAt != null && System.currentTimeMillis() < retryAt) return;
        if (!LOADING.add(key)) return;

        MinecraftSessionService sessionService = MinecraftClient.getInstance().getSessionService();
        EXECUTOR.execute(() -> {
            try {
                GameProfile filled = sessionService.fillProfileProperties(new GameProfile(uuid, null), true);
                if (filled != null && filled.getProperties().containsKey("textures")) {
                    READY.put(key, buildStack(filled));
                } else {
                    RETRY_AT.put(key, System.currentTimeMillis() + RETRY_AFTER_MS);
                    HeadSwapClient.LOGGER.warn("No se obtuvo textura para el UUID {} (invalido o limite de peticiones). Reintento en 2 min.", uuid);
                }
            } catch (Throwable t) {
                RETRY_AT.put(key, System.currentTimeMillis() + RETRY_AFTER_MS);
                HeadSwapClient.LOGGER.warn("Error al consultar el perfil {}: {}", uuid, t.toString());
            } finally {
                LOADING.remove(key);
            }
        });
    }

    private static GameProfile profileFromTextureUrl(String url) {
        JsonObject skin = new JsonObject();
        skin.addProperty("url", url);
        JsonObject textures = new JsonObject();
        textures.add("SKIN", skin);
        JsonObject root = new JsonObject();
        root.add("textures", textures);
        String encoded = Base64.getEncoder().encodeToString(root.toString().getBytes(StandardCharsets.UTF_8));

        UUID id = UUID.nameUUIDFromBytes(("headswap:" + url).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(id, null);
        profile.getProperties().put("textures", new Property("textures", encoded));
        return profile;
    }

    private static ItemStack buildStack(GameProfile profile) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        NbtCompound owner = new NbtCompound();
        NbtHelper.writeGameProfile(owner, profile);
        stack.setSubNbt("SkullOwner", owner);
        return stack;
    }
}
