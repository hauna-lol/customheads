package dev.headswap.profile;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Interpreta el texto "source" de una regla: UUID, hash de textura o URL de textura. */
public record HeadSource(Kind kind, String value) {

    public enum Kind { UUID, TEXTURE_URL, INVALID }

    private static final Pattern UUID_DASHED =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern UUID_PLAIN = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern TEXTURE_HASH = Pattern.compile("^[0-9a-fA-F]{40,64}$");

    public static HeadSource parse(String raw) {
        if (raw == null) return new HeadSource(Kind.INVALID, "");
        String s = raw.trim();
        UUID uuid = tryParseUuid(s);
        if (uuid != null) return new HeadSource(Kind.UUID, uuid.toString());
        if (TEXTURE_HASH.matcher(s).matches()) {
            return new HeadSource(Kind.TEXTURE_URL,
                    "http://textures.minecraft.net/textures/" + s.toLowerCase(Locale.ROOT));
        }
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return new HeadSource(Kind.TEXTURE_URL, s);
        }
        return new HeadSource(Kind.INVALID, s);
    }

    /** @return el UUID si el texto es un UUID (con o sin guiones); si no, null. */
    public static UUID tryParseUuid(String s) {
        if (s == null) return null;
        s = s.trim();
        try {
            if (UUID_DASHED.matcher(s).matches()) {
                return UUID.fromString(s);
            }
            if (UUID_PLAIN.matcher(s).matches()) {
                return UUID.fromString(s.replaceFirst(
                        "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                        "$1-$2-$3-$4-$5"));
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }
}
