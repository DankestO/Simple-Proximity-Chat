package com.simpleproximitychat.radio;

import com.simpleproximitychat.SimpleProximityChat;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lee la config de servidor de Simple Radio (SimpleRadioLibrary.SERVER_CONFIG) por reflexión.
 *
 * Así no dependemos de sus clases para compilar, y siempre usamos los valores actuales
 * (si cambias la config de Simple Radio, el chat por radio cambia con ella).
 * Si algo falla (otra versión de Simple Radio), se usan sus valores por defecto de la 4.6.x.
 */
public final class SimpleRadioConfig {

    private static final String LIBRARY = "com.codinglitch.simpleradio.SimpleRadioLibrary";
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static volatile boolean warned;

    private SimpleRadioConfig() {}

    public static int getInt(String page, String key, int fallback) {
        Object value = read(page, key);
        return value instanceof Number n ? n.intValue() : fallback;
    }

    public static double getDouble(String page, String key, double fallback) {
        Object value = read(page, key);
        return value instanceof Number n ? n.doubleValue() : fallback;
    }

    public static boolean getBoolean(String page, String key, boolean fallback) {
        Object value = read(page, key);
        return value instanceof Boolean b ? b : fallback;
    }

    private static Object read(String page, String key) {
        try {
            Object root = field(Class.forName(LIBRARY), "SERVER_CONFIG").get(null);
            if (root == null) return null;
            Object pageObj = field(root.getClass(), page).get(root);
            if (pageObj == null) return null;
            return field(pageObj.getClass(), key).get(pageObj);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                SimpleProximityChat.LOGGER.warn("No se pudo leer la config de Simple Radio ({}.{}); usando valores por defecto", page, key, e);
            }
            return null;
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        String cacheKey = owner.getName() + '#' + name;
        Field f = FIELDS.get(cacheKey);
        if (f == null) {
            f = owner.getField(name);
            FIELDS.put(cacheKey, f);
        }
        return f;
    }
}
