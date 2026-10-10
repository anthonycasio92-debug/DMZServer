package com.dbzlegacy.adaptivedifficulty.config.editor;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Staff config editor. Rows come from public fields on the live config objects,
 * so a new field shows up without a new button.
 * Player record files (rivalry, sparring, rival progression) stay out of this
 * editor. Those maps are live player data, not settings.
 */
public final class ConfigEditor {
    /** Scroll lines stay under the CNPC 64-character trim. */
    private static final int LINE_MAX = 60;
    private static final int LIST_CAP = 200;

    private ConfigEditor() {}

    public enum Kind {
        BOOLEAN, NUMBER, TEXT, ENUM, GROUP, LIST, MAP, OTHER
    }

    public record Root(String id, String title, String fileName, Supplier<Object> target, Runnable persist) {}

    public record Entry(String line, String path, Kind kind) {}

    public enum AddMode {
        NONE, VALUE, KEY_AND_VALUE
    }

    public static List<Root> roots() {
        List<Root> roots = new ArrayList<>();
        roots.add(new Root(
                "difficulty",
                "Legacy Mechanics",
                "legacymechanics.json",
                DifficultyConfig::get,
                () -> {
                    DifficultyConfig.sanitizeLive();
                    DifficultyConfig.save();
                }));
        roots.add(new Root(
                "character",
                "Character services",
                "character-services.json",
                CharacterServicesConfig::get,
                () -> {
                    if (!CharacterServicesConfig.save()) {
                        throw new IllegalStateException("character-services save failed");
                    }
                }));
        roots.add(new Root(
                "saga",
                "Saga reset",
                "saga-reset.json",
                SagaResetConfig::get,
                () -> {
                    if (!SagaResetConfig.save()) {
                        throw new IllegalStateException("saga-reset save failed");
                    }
                }));
        return roots;
    }

    public static Root root(String id) {
        if (id == null) {
            return null;
        }
        for (Root root : roots()) {
            if (root.id().equals(id)) {
                return root;
            }
        }
        return null;
    }

    public static List<Entry> children(String rootId, String path) {
        Located located = locate(rootId, path == null ? "" : path);
        if (located == null || located.value == null) {
            return List.of();
        }
        Object value = located.value;
        if (value instanceof Map<?, ?> map) {
            return mapEntries(path, map, valueType(located.field, 1));
        }
        if (value instanceof List<?> list) {
            return listEntries(path, list, valueType(located.field, 0));
        }
        if (isGroup(value.getClass())) {
            return fieldEntries(path, value);
        }
        return List.of();
    }

    public static int hiddenCount(String rootId, String path) {
        Located located = locate(rootId, path == null ? "" : path);
        if (located == null || located.value == null) {
            return 0;
        }
        int size = 0;
        if (located.value instanceof Map<?, ?> map) {
            size = map.size();
        } else if (located.value instanceof List<?> list) {
            size = list.size();
        }
        return Math.max(0, size - LIST_CAP);
    }

    public static Kind kindOf(String rootId, String path) {
        Located located = locate(rootId, path == null ? "" : path);
        if (located == null) {
            return Kind.OTHER;
        }
        if (located.value == null && located.field != null) {
            return kindForType(rawClass(located.field.getGenericType()));
        }
        return kindOfValue(located.value, located.field);
    }

    public static AddMode addMode(String rootId, String path) {
        Kind kind = kindOf(rootId, path);
        if (kind == Kind.MAP) {
            return AddMode.KEY_AND_VALUE;
        }
        if (kind == Kind.LIST) {
            return AddMode.VALUE;
        }
        return AddMode.NONE;
    }

    public static String currentText(String rootId, String path) {
        Located located = locate(rootId, path);
        if (located == null || located.value == null) {
            return "";
        }
        return String.valueOf(located.value);
    }

    public static List<String> editLines(String rootId, String path) {
        Located located = locate(rootId, path);
        List<String> lines = new ArrayList<>();
        if (located == null) {
            lines.add("§7That setting is not on this config.");
            return lines;
        }
        lines.add("§7Current §f" + shortText(located.value));
        if (located.field != null && located.field.getType().isEnum()) {
            Object[] constants = located.field.getType().getEnumConstants();
            StringBuilder names = new StringBuilder();
            for (Object constant : constants) {
                if (names.length() > 80) {
                    names.append(" …");
                    break;
                }
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(constant);
            }
            lines.add("§7Use one of §f" + names);
        } else {
            lines.add("§7Saving writes this config file.");
        }
        return lines;
    }

    /** @return a player-facing error, or null when the value was saved */
    public static String apply(String rootId, String path, String raw) {
        Root root = root(rootId);
        if (root == null) {
            return "That config is not editable.";
        }
        Located located = locate(rootId, path);
        if (located == null || located.owner == null) {
            return "That setting is not on this config.";
        }
        if (isBlocked(located.field)) {
            return "The admin permission stays in the config file.";
        }
        try {
            if (located.key != null) {
                int typeIndex = located.container instanceof List<?> ? 0 : 1;
                Object coerced = coerce(valueType(located.field, typeIndex), raw);
                if (located.container instanceof Map<?, ?>) {
                    mutableMap(located.field, located.owner).put(located.key, coerced);
                } else if (located.container instanceof List<?>) {
                    List<Object> list = mutableList(located.field, located.owner);
                    if (!replaceListValue(list, located.field, located.key, coerced)) {
                        return "That entry is no longer in the list.";
                    }
                } else {
                    return "That entry can't be edited here.";
                }
            } else if (located.field != null) {
                if ("*".equals(raw == null ? "" : raw.trim())
                        && located.field.getName().toLowerCase(Locale.ROOT).contains("permission")) {
                    return "A permission node can't be *.";
                }
                located.field.set(located.owner, coerce(located.field.getGenericType(), raw));
            } else {
                return "That setting can't be edited here.";
            }
        } catch (NumberFormatException e) {
            return "That needs to be a number.";
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            return message == null || message.isBlank() ? "That value didn't work." : message;
        } catch (ReflectiveOperationException e) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] config edit failed {}.{}: {}",
                    AdaptiveDifficultyMod.MOD_ID, rootId, path, e.toString());
            return "That setting couldn't be changed.";
        }
        return persist(root);
    }

    /** @return a player-facing error, or null when the boolean was flipped and saved */
    public static String toggle(String rootId, String path) {
        Located located = locate(rootId, path);
        if (located == null || located.value == null) {
            return "That setting is not on this config.";
        }
        if (!(located.value instanceof Boolean current)) {
            return "That setting is not a switch.";
        }
        return apply(rootId, path, Boolean.toString(!current));
    }

    /** @return a player-facing error, or null when the entry was removed and saved */
    public static String remove(String rootId, String path) {
        Root root = root(rootId);
        if (root == null) {
            return "That config is not editable.";
        }
        Located located = locate(rootId, path);
        if (located == null || located.key == null || located.field == null || located.owner == null) {
            return "Select an entry to remove.";
        }
        try {
            if (located.container instanceof Map<?, ?>) {
                Map<String, Object> map = mutableMap(located.field, located.owner);
                if (!map.containsKey(located.key)) {
                    return "That entry is already gone.";
                }
                map.remove(located.key);
            } else if (located.container instanceof List<?>) {
                List<Object> list = mutableList(located.field, located.owner);
                if (!removeListValue(list, located.field, located.key)) {
                    return "That entry is already gone.";
                }
            } else {
                return "That entry can't be removed here.";
            }
        } catch (ReflectiveOperationException e) {
            return "That entry couldn't be removed.";
        }
        return persist(root);
    }

    /** @return a player-facing error, or null when the entry was added and saved */
    public static String add(String rootId, String path, String key, String raw) {
        Root root = root(rootId);
        if (root == null) {
            return "That config is not editable.";
        }
        Located located = locate(rootId, path);
        if (located == null || located.field == null || located.owner == null) {
            return "That list is not on this config.";
        }
        try {
            if (located.value instanceof Map<?, ?> || kindForType(rawClass(located.field.getGenericType())) == Kind.MAP) {
                if (key == null || key.isBlank()) {
                    return "Type a name for the new entry.";
                }
                Map<String, Object> map = mutableMap(located.field, located.owner);
                map.put(key.trim(), coerce(valueType(located.field, 1), raw));
            } else if (located.value instanceof List<?>
                    || kindForType(rawClass(located.field.getGenericType())) == Kind.LIST) {
                List<Object> list = mutableList(located.field, located.owner);
                list.add(coerce(valueType(located.field, 0), raw));
            } else {
                return "Entries can't be added here.";
            }
        } catch (NumberFormatException e) {
            return "That needs to be a number.";
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            return message == null || message.isBlank() ? "That value didn't work." : message;
        } catch (ReflectiveOperationException e) {
            return "That entry couldn't be added.";
        }
        return persist(root);
    }

    public static String parentPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.endsWith("]")) {
            int bracket = path.lastIndexOf('[');
            return bracket <= 0 ? "" : path.substring(0, bracket);
        }
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(0, dot);
    }

    private static String persist(Root root) {
        try {
            root.persist().run();
            return null;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] config save failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID, root.id(), t.toString());
            return "The file could not be saved.";
        }
    }

    private static List<Entry> fieldEntries(String path, Object value) {
        List<Entry> entries = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        for (Field field : value.getClass().getFields()) {
            if (skipField(field)) {
                continue;
            }
            fields.add(field);
        }
        fields.sort(Comparator
                .comparingInt((Field field) -> rank(kindForType(field.getType())))
                .thenComparing(Field::getName, String.CASE_INSENSITIVE_ORDER));
        for (Field field : fields) {
            Object child;
            try {
                child = field.get(value);
            } catch (ReflectiveOperationException e) {
                continue;
            }
            String childPath = joinField(path, field.getName());
            Kind kind = kindOfValue(child, field);
            entries.add(new Entry(line(field.getName(), summary(child, kind)), childPath, kind));
        }
        return entries;
    }

    private static List<Entry> mapEntries(String path, Map<?, ?> map, Type valueType) {
        List<String> keys = new ArrayList<>();
        for (Object key : map.keySet()) {
            if (key != null) {
                keys.add(String.valueOf(key));
            }
        }
        keys.sort(String.CASE_INSENSITIVE_ORDER);
        if (keys.size() > LIST_CAP) {
            keys = keys.subList(0, LIST_CAP);
        }
        List<Entry> entries = new ArrayList<>();
        for (String key : keys) {
            Object child = map.get(key);
            Kind kind = child == null ? kindForType(rawClass(valueType)) : kindOfValue(child, null);
            entries.add(new Entry(line(key, summary(child, kind)), joinKey(path, key), kind));
        }
        return entries;
    }

    private static List<Entry> listEntries(String path, List<?> list, Type valueType) {
        int limit = Math.min(list.size(), LIST_CAP);
        List<Entry> entries = new ArrayList<>();
        boolean strings = rawClass(valueType) == String.class;
        for (int i = 0; i < limit; i++) {
            Object child = list.get(i);
            String key = strings ? String.valueOf(child) : Integer.toString(i);
            Kind kind = child == null ? kindForType(rawClass(valueType)) : kindOfValue(child, null);
            String label = strings ? key : (i + 1) + "";
            entries.add(new Entry(line(label, strings ? "entry" : summary(child, kind)), joinKey(path, key), kind));
        }
        return entries;
    }

    private static String line(String name, String summary) {
        String label = name == null ? "" : name;
        String detail = summary == null ? "" : summary;
        String text = "§f" + label + " §8· §7" + detail;
        if (text.length() <= LINE_MAX) {
            return text;
        }
        int keep = Math.max(8, LINE_MAX - 1);
        return text.substring(0, keep) + "…";
    }

    private static String summary(Object value, Kind kind) {
        if (kind == Kind.GROUP) {
            return "group";
        }
        if (kind == Kind.LIST) {
            int size = value instanceof Collection<?> collection ? collection.size() : 0;
            return size == 1 ? "1 entry" : size + " entries";
        }
        if (kind == Kind.MAP) {
            int size = value instanceof Map<?, ?> map ? map.size() : 0;
            return size == 1 ? "1 entry" : size + " entries";
        }
        if (value instanceof Boolean on) {
            return on ? "ON" : "OFF";
        }
        return shortText(value);
    }

    private static String shortText(Object value) {
        if (value == null) {
            return "empty";
        }
        String text = String.valueOf(value).replace('\n', ' ');
        if (text.length() > 18) {
            return text.substring(0, 17) + "…";
        }
        return text;
    }

    private static int rank(Kind kind) {
        return switch (kind) {
            case GROUP, MAP, LIST -> 0;
            case BOOLEAN -> 1;
            case NUMBER -> 2;
            case TEXT, ENUM -> 3;
            default -> 4;
        };
    }

    private static Kind kindOfValue(Object value, Field field) {
        if (value instanceof Boolean) {
            return Kind.BOOLEAN;
        }
        if (value instanceof Number) {
            return Kind.NUMBER;
        }
        if (value instanceof String) {
            return Kind.TEXT;
        }
        if (value instanceof Enum<?>) {
            return Kind.ENUM;
        }
        if (value instanceof Map<?, ?>) {
            return Kind.MAP;
        }
        if (value instanceof List<?>) {
            return Kind.LIST;
        }
        if (value != null && isGroup(value.getClass())) {
            return Kind.GROUP;
        }
        if (field != null) {
            return kindForType(field.getType());
        }
        return Kind.OTHER;
    }

    private static Kind kindForType(Class<?> type) {
        if (type == null) {
            return Kind.OTHER;
        }
        if (type == boolean.class || type == Boolean.class) {
            return Kind.BOOLEAN;
        }
        if (isNumber(type)) {
            return Kind.NUMBER;
        }
        if (type == String.class) {
            return Kind.TEXT;
        }
        if (type.isEnum()) {
            return Kind.ENUM;
        }
        if (Map.class.isAssignableFrom(type)) {
            return Kind.MAP;
        }
        if (List.class.isAssignableFrom(type)) {
            return Kind.LIST;
        }
        if (isGroup(type)) {
            return Kind.GROUP;
        }
        return Kind.OTHER;
    }

    private static boolean isNumber(Class<?> type) {
        return type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class;
    }

    private static boolean isGroup(Class<?> type) {
        if (type == null || type.isEnum() || type.isPrimitive() || type.isArray()) {
            return false;
        }
        Package pkg = type.getPackage();
        return pkg != null && pkg.getName().startsWith("com.dbzlegacy.adaptivedifficulty");
    }

    private static boolean skipField(Field field) {
        int mod = field.getModifiers();
        if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || field.isSynthetic()) {
            return true;
        }
        return isBlocked(field);
    }

    private static boolean isBlocked(Field field) {
        if (field == null) {
            return false;
        }
        String name = field.getName();
        return "adminPermission".equals(name) || name.contains("Migrated");
    }

    private static Object coerce(Type type, String raw) {
        String text = raw == null ? "" : raw.trim();
        Class<?> rawClass = rawClass(type);
        if (rawClass == String.class || rawClass == null || rawClass == Object.class) {
            return raw == null ? "" : raw;
        }
        if (rawClass == boolean.class || rawClass == Boolean.class) {
            if ("true".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text) || "off".equalsIgnoreCase(text)) {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Use on or off.");
        }
        if (rawClass.isEnum()) {
            for (Object constant : rawClass.getEnumConstants()) {
                if (constant.toString().equalsIgnoreCase(text)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("That is not one of the allowed names.");
        }
        if (text.isEmpty()) {
            throw new NumberFormatException("empty");
        }
        String number = text.replace(",", "");
        try {
            if (rawClass == int.class || rawClass == Integer.class) {
                return Integer.valueOf(number);
            }
            if (rawClass == long.class || rawClass == Long.class) {
                return Long.valueOf(number);
            }
            if (rawClass == double.class || rawClass == Double.class) {
                double value = Double.parseDouble(number);
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    throw new IllegalArgumentException("That number doesn't work here.");
                }
                return value;
            }
            if (rawClass == float.class || rawClass == Float.class) {
                float value = Float.parseFloat(number);
                if (Float.isNaN(value) || Float.isInfinite(value)) {
                    throw new IllegalArgumentException("That number doesn't work here.");
                }
                return value;
            }
            if (rawClass == short.class || rawClass == Short.class) {
                return Short.valueOf(number);
            }
            if (rawClass == byte.class || rawClass == Byte.class) {
                return Byte.valueOf(number);
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(number);
        }
        throw new IllegalArgumentException("That value can't be edited here.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutableMap(Field field, Object owner) throws ReflectiveOperationException {
        Object current = field.get(owner);
        if (current instanceof LinkedHashMap<?, ?> || current instanceof java.util.HashMap<?, ?>
                || current instanceof java.util.concurrent.ConcurrentHashMap<?, ?>) {
            return (Map<String, Object>) current;
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        if (current instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    copy.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        field.set(owner, copy);
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> mutableList(Field field, Object owner) throws ReflectiveOperationException {
        Object current = field.get(owner);
        if (current instanceof ArrayList<?>) {
            return (List<Object>) current;
        }
        List<Object> copy = new ArrayList<>();
        if (current instanceof Collection<?> collection) {
            copy.addAll(collection);
        }
        field.set(owner, copy);
        return copy;
    }

    private static boolean replaceListValue(List<Object> list, Field field, String key, Object coerced) {
        if (rawClass(valueType(field, 0)) == String.class) {
            for (int i = 0; i < list.size(); i++) {
                if (key.equals(String.valueOf(list.get(i)))) {
                    list.set(i, coerced);
                    return true;
                }
            }
            return false;
        }
        int index = Integer.parseInt(key);
        if (index < 0 || index >= list.size()) {
            return false;
        }
        list.set(index, coerced);
        return true;
    }

    private static boolean removeListValue(List<Object> list, Field field, String key) {
        if (rawClass(valueType(field, 0)) == String.class) {
            for (int i = 0; i < list.size(); i++) {
                if (key.equals(String.valueOf(list.get(i)))) {
                    list.remove(i);
                    return true;
                }
            }
            return false;
        }
        int index = Integer.parseInt(key);
        if (index < 0 || index >= list.size()) {
            return false;
        }
        list.remove(index);
        return true;
    }

    private static Type valueType(Field field, int index) {
        if (field == null) {
            return String.class;
        }
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments().length > index) {
            return parameterized.getActualTypeArguments()[index];
        }
        return String.class;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> clazz) {
            return clazz;
        }
        return null;
    }

    private static String joinField(String path, String name) {
        if (path == null || path.isEmpty()) {
            return name;
        }
        return path + "." + name;
    }

    private static String joinKey(String path, String key) {
        return path + "[" + escape(key) + "]";
    }

    private static String escape(String key) {
        return key.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]");
    }

    private static Located locate(String rootId, String path) {
        Root root = root(rootId);
        if (root == null) {
            return null;
        }
        Object current = root.target().get();
        if (current == null) {
            return null;
        }
        if (path == null || path.isEmpty()) {
            return new Located(null, null, null, null, current);
        }
        Object owner = null;
        Object container = null;
        Field field = null;
        String key = null;
        int i = 0;
        while (i < path.length()) {
            if (path.charAt(i) == '.') {
                i++;
            }
            if (i >= path.length()) {
                break;
            }
            if (path.charAt(i) == '[') {
                StringBuilder parsed = new StringBuilder();
                i++;
                while (i < path.length()) {
                    char c = path.charAt(i);
                    if (c == '\\' && i + 1 < path.length()) {
                        parsed.append(path.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (c == ']') {
                        i++;
                        break;
                    }
                    parsed.append(c);
                    i++;
                }
                key = parsed.toString();
                container = current;
                if (current instanceof Map<?, ?> map) {
                    current = map.get(key);
                } else if (current instanceof List<?> list) {
                    current = findListValue(list, key);
                } else {
                    return null;
                }
                continue;
            }
            int start = i;
            while (i < path.length() && path.charAt(i) != '.' && path.charAt(i) != '[') {
                i++;
            }
            String name = path.substring(start, i);
            if (current == null) {
                return null;
            }
            field = findField(current.getClass(), name);
            if (field == null || isBlocked(field)) {
                return null;
            }
            owner = current;
            container = null;
            key = null;
            try {
                current = field.get(current);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return new Located(field, owner, container, key, current);
    }

    private static Object findListValue(List<?> list, String key) {
        for (Object value : list) {
            if (key.equals(String.valueOf(value))) {
                return value;
            }
        }
        try {
            int index = Integer.parseInt(key);
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
        } catch (NumberFormatException ignored) {
            // A string entry that is not an index.
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            try {
                Field field = cursor.getField(name);
                if (!skipField(field)) {
                    return field;
                }
                return null;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    /** {@code owner} holds {@code field}. {@code container} is the map or list when {@code key} is set. */
    private record Located(Field field, Object owner, Object container, String key, Object value) {}
}
