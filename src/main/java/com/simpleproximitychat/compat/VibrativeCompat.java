package com.simpleproximitychat.compat;

import com.simpleproximitychat.SimpleProximityChat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * VibrativeVoice (dependencia opcional): el texto también genera vibraciones de sculk/Warden.
 *
 * Se usa su API pública ({@code VibrativeVoiceApi.INSTANCE.trySendVibration}) por reflexión,
 * así que no hace falta para compilar. Esa llamada ya respeta su cooldown y sus restricciones.
 * Siempre se manda una vibración débil (WEAK): escribir hace poco ruido.
 */
public final class VibrativeCompat {

    private static final String API = "com.codinglitch.vibrativevoice.VibrativeVoiceApi";

    private static boolean initialized;
    private static boolean available;
    private static Class<?> apiClass;
    private static Method atEntity;
    private static Method atBlock;
    private static Object weak;

    private VibrativeCompat() {}

    /** Vibración en una entidad (quien escribe, o el dueño de un walkie que recibe). */
    public static void vibrate(Entity entity) {
        if (!init()) return;
        invoke(atEntity, entity.getUUID(), entity, entity.level(), weak);
    }

    /** Vibración en un bloque (un Altavoz o Radio que recibe). */
    public static void vibrate(Level level, BlockPos pos) {
        if (!init()) return;
        UUID id = UUID.nameUUIDFromBytes(("spc:" + level.dimension().location() + ':' + pos.asLong())
                .getBytes(StandardCharsets.UTF_8));
        invoke(atBlock, id, pos, level, weak);
    }

    private static void invoke(Method method, Object... args) {
        try {
            Object api = apiClass.getField("INSTANCE").get(null);
            if (api != null) method.invoke(api, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            SimpleProximityChat.LOGGER.warn("VibrativeVoice no respondió como se esperaba; se desactivan las vibraciones por texto", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized boolean init() {
        if (initialized) return available;
        initialized = true;
        if (!SimpleProximityChat.isLoaded("vibrativevoice")) return available = false;
        try {
            Class<?> api = Class.forName(API);
            Class<? extends Enum> type = (Class<? extends Enum>) Class.forName(API + "$VibrationType");
            weak = Enum.valueOf(type, "WEAK");
            atEntity = api.getMethod("trySendVibration", UUID.class, Entity.class, Level.class, type);
            atBlock = api.getMethod("trySendVibration", UUID.class, BlockPos.class, Level.class, type);
            apiClass = api;
            available = true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            SimpleProximityChat.LOGGER.warn("VibrativeVoice instalado pero con una API distinta; se ignora", e);
        }
        return available;
    }
}
