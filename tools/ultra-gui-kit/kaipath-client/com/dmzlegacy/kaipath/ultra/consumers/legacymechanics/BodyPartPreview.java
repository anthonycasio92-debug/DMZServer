package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;

/**
 * Attaches {@code playerModel.parts.bodyParts} to the preview entity.
 *
 * <p>{@link #begin} runs just before the entity is drawn. {@link FormPartsPreviewMixin}
 * asks {@link #meshesFor} while DMZUltra's FormParts layer builds the look, and unions
 * those meshes into the design so {@code part.<id>} bones render on that entity.
 * A missing DMZUltra install, or an id the catalog does not know, leaves the base
 * entity alone.
 */
public final class BodyPartPreview {
    private static final ThreadLocal<Active> ACTIVE = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, Set<String>> LOADED = new ConcurrentHashMap<>();

    private BodyPartPreview() {}

    private record Active(UUID playerId, Set<String> meshes) {}

    /** Remember the pieces for this draw. Safe to call when parts are absent. */
    public static void begin(LivingEntity entity, JsonObject parts) {
        end();
        try {
            if (entity == null || parts == null || !parts.has("bodyParts") || !parts.get("bodyParts").isJsonArray()) {
                return;
            }
            List<BodyPartWire.Model> models =
                    BodyPartWire.resolve(
                            BodyPartWire.parse(parts.getAsJsonArray("bodyParts")), BodyPartPreview::piecesFor);
            if (models.isEmpty()) {
                return;
            }
            LinkedHashSet<String> meshes = new LinkedHashSet<>();
            boolean aliasTail = false;
            boolean directTail = false;
            for (BodyPartWire.Model model : models) {
                if (model == null || model.mesh() == null || model.mesh().isBlank()) {
                    continue;
                }
                if (BodyPartWire.SAIYAN_TAIL_MESH.equals(model.mesh())) {
                    if (model.alias()) {
                        aliasTail = true;
                    } else {
                        directTail = true;
                    }
                }
                meshes.add(model.mesh());
            }
            // The live player's own race layer already draws the Saiyan tail. Don't stack
            // tail_fur on top of it. A preview without that character still gets the mesh.
            if (aliasTail && !directTail && raceTailAlreadyShown(entity)) {
                meshes.remove(BodyPartWire.SAIYAN_TAIL_MESH);
            }
            if (meshes.isEmpty()) {
                return;
            }
            ACTIVE.set(new Active(entityUuid(entity), Set.copyOf(meshes)));
        } catch (Throwable ignored) {
            end();
        }
    }

    public static void end() {
        ACTIVE.remove();
    }

    /**
     * Meshes to union into DMZUltra's look for this player. Empty when no preview is
     * drawing, or the player is not the preview subject.
     */
    public static Set<String> meshesFor(Object player) {
        Active active = ACTIVE.get();
        if (active == null || active.meshes == null || active.meshes.isEmpty()) {
            return Set.of();
        }
        UUID want = active.playerId;
        UUID got = entityUuid(player);
        if (want != null && got != null && !want.equals(got)) {
            return Set.of();
        }
        return active.meshes;
    }

    /**
     * Copy {@code look} (or build one) so its design includes {@code extra} piece ids.
     * Returns the original look when DMZUltra's types are missing.
     */
    public static Object mergeLook(Object look, Set<String> extra) {
        if (extra == null || extra.isEmpty()) {
            return look;
        }
        try {
            Class<?> designC = Class.forName("com.dmzultra.client.forms.FormParts$Design");
            Class<?> lookC = Class.forName("com.dmzultra.client.forms.FormParts$Look");
            LinkedHashSet<String> pieces = new LinkedHashSet<>();
            int own = 0xFFFFFF;
            int trim = 0xE9C46A;
            int horn = 0xEFE6CF;
            int light = 0xFFFFFF;
            float front = 0f;
            float back = 0f;
            int skin = 0xFFFFFF;
            int second = 0xFFFFFF;
            int hair = 0x202424;
            if (look != null && lookC.isInstance(look)) {
                Object design = lookC.getMethod("design").invoke(look);
                if (design != null) {
                    Object existing = designC.getMethod("parts").invoke(design);
                    if (existing instanceof Set<?> set) {
                        for (Object piece : set) {
                            if (piece != null) {
                                pieces.add(String.valueOf(piece));
                            }
                        }
                    }
                    own = integer(design, "own", own);
                    trim = integer(design, "trim", trim);
                    horn = integer(design, "horn", horn);
                    light = integer(design, "light", light);
                    front = decimal(design, "front", front);
                    back = decimal(design, "back", back);
                }
                skin = integer(look, "skin", skin);
                second = integer(look, "second", second);
                hair = integer(look, "hair", hair);
            }
            pieces.addAll(extra);
            Object design =
                    designC.getConstructor(
                                    Set.class, int.class, int.class, int.class, int.class, float.class, float.class)
                            .newInstance(pieces, own, trim, horn, light, front, back);
            return lookC.getConstructor(designC, int.class, int.class, int.class)
                    .newInstance(design, skin, second, hair);
        } catch (Throwable ignored) {
            return look;
        }
    }

    static Set<String> piecesFor(String source) {
        if (source == null || source.isBlank()) {
            return Set.of();
        }
        if (BodyPartWire.SOURCE_DMZULTRA.equals(source)) {
            if (!dmzUltraInstalled()) {
                return Set.of();
            }
            Set<String> live = readPieces(source);
            return live.isEmpty() ? BodyPartWire.DMZULTRA_PIECES : live;
        }
        return readPieces(source);
    }

    private static boolean dmzUltraInstalled() {
        try {
            Class.forName("com.dmzultra.client.forms.FormParts", false, BodyPartPreview.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Set<String> readPieces(String source) {
        Set<String> cached = LOADED.get(source);
        if (cached != null) {
            return cached;
        }
        Set<String> loaded = tryLoadPieces(source);
        if (!loaded.isEmpty()) {
            LOADED.put(source, loaded);
        }
        return loaded;
    }

    /** {@code assets/<source>/form_parts/forms.json} → {@code pieces} array. Empty on any miss. */
    private static Set<String> tryLoadPieces(String source) {
        InputStream in = null;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("m_91087_").invoke(null);
            if (mc == null) {
                return Set.of();
            }
            Object manager = null;
            for (String name : new String[] {"m_91098_", "getResourceManager"}) {
                try {
                    manager = mc.getClass().getMethod(name).invoke(mc);
                    if (manager != null) {
                        break;
                    }
                } catch (Throwable ignored) {
                    // next accessor
                }
            }
            if (manager == null) {
                return Set.of();
            }
            Class<?> rl = Class.forName("net.minecraft.resources.ResourceLocation");
            Object location =
                    rl.getMethod("fromNamespaceAndPath", String.class, String.class)
                            .invoke(null, source, "form_parts/forms.json");
            Object resource = null;
            for (Method method : manager.getClass().getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(location)) {
                    Object found = method.invoke(manager, location);
                    if (found instanceof java.util.Optional<?> opt) {
                        resource = opt.orElse(null);
                    } else {
                        resource = found;
                    }
                    if (resource != null) {
                        break;
                    }
                }
            }
            if (resource == null) {
                return Set.of();
            }
            for (String name : new String[] {"m_215507_", "open"}) {
                try {
                    Object opened = resource.getClass().getMethod(name).invoke(resource);
                    if (opened instanceof InputStream stream) {
                        in = stream;
                        break;
                    }
                } catch (Throwable ignored) {
                    // next opener
                }
            }
            if (in == null) {
                return Set.of();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("pieces") || !root.get("pieces").isJsonArray()) {
                    return Set.of();
                }
                LinkedHashSet<String> pieces = new LinkedHashSet<>();
                JsonArray arr = root.getAsJsonArray("pieces");
                for (JsonElement el : arr) {
                    if (el != null && el.isJsonPrimitive()) {
                        String id = el.getAsString().trim().toLowerCase(java.util.Locale.ROOT);
                        if (!id.isEmpty()) {
                            pieces.add(id);
                        }
                    }
                }
                return pieces.isEmpty() ? Set.of() : Set.copyOf(pieces);
            }
        } catch (Throwable ignored) {
            return Set.of();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                    // already consumed or closed by the reader
                }
            }
        }
    }

    private static boolean raceTailAlreadyShown(Object entity) {
        try {
            Class<?> capClass = Class.forName("com.dragonminez.common.stats.StatsCapability");
            Object cap = capClass.getField("INSTANCE").get(null);
            Class<?> provider = Class.forName("com.dragonminez.common.stats.StatsProvider");
            Class<?> capability = Class.forName("net.minecraftforge.common.capabilities.Capability");
            Class<?> entityType = Class.forName("net.minecraft.world.entity.Entity");
            Object lazy = provider.getMethod("get", capability, entityType).invoke(null, cap, entity);
            if (lazy == null) {
                return false;
            }
            Object stats = null;
            try {
                Object resolved = lazy.getClass().getMethod("resolve").invoke(lazy);
                if (resolved instanceof java.util.Optional<?> opt) {
                    stats = opt.orElse(null);
                }
            } catch (Throwable ignored) {
                stats = lazy.getClass().getMethod("orElse", Object.class).invoke(lazy, new Object[] {null});
            }
            if (stats == null) {
                return false;
            }
            Object character = stats.getClass().getMethod("getCharacter").invoke(stats);
            if (character == null) {
                return false;
            }
            Object flag = character.getClass().getMethod("isHasSaiyanTail").invoke(character);
            return flag instanceof Boolean on && on;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static UUID entityUuid(Object entity) {
        if (entity == null) {
            return null;
        }
        for (String name : new String[] {"m_20148_", "getUUID"}) {
            try {
                Object id = entity.getClass().getMethod(name).invoke(entity);
                if (id instanceof UUID uuid) {
                    return uuid;
                }
            } catch (Throwable ignored) {
                // next accessor
            }
        }
        return null;
    }

    private static int integer(Object target, String method, int fallback) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
            // keep fallback
        }
        return fallback;
    }

    private static float decimal(Object target, String method, float fallback) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable ignored) {
            // keep fallback
        }
        return fallback;
    }
}
