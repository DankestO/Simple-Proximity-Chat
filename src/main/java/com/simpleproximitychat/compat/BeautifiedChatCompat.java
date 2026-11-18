package com.simpleproximitychat.compat;

import com.simpleproximitychat.SimpleProximityChat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Beautified Chat Server (dependencia opcional).
 *
 * Para los mensajes distorsionados o por radio, en lugar de esperar a que Beautified Chat los
 * intercepte, usamos directamente su función pública de formato
 * ({@code Util.getFormattedChatMessage}): así salen con su rango, hora y colores de nombre.
 * Se llama por reflexión, así que no hace falta para compilar.
 */
public final class BeautifiedChatCompat {

    private static final String UTIL = "com.serilum.beautifiedchatserver_common_forge.util.Util";

    private static boolean initialized;
    private static boolean available;
    private static Method format;
    private static Method timestamp;
    private static Method colour;
    private static Method coloured;

    private BeautifiedChatCompat() {}

    public static boolean isLoaded() {
        return SimpleProximityChat.isLoaded("beautifiedchatserver");
    }

    /**
     * Formatea un mensaje como lo haría Beautified Chat.
     *
     * @param keepColours true = respeta los colores que ya trae el texto (el ámbar de la radio);
     *                    false = le aplica el color de mensaje de Beautified Chat
     * @return el mensaje formateado, o null si no se puede (se usará otro método)
     */
    @Nullable
    public static MutableComponent format(ServerPlayer sender, MutableComponent body, boolean keepColours) {
        if (!init()) return null;
        try {
            MutableComponent message = body;
            if (!keepColours) {
                ChatFormatting c = (ChatFormatting) colour.invoke(null, "chatmessage");
                // Mismo paso que hace Beautified Chat: color de mensaje, respetando el resto de estilos
                message = (MutableComponent) coloured.invoke(null, (Component) body, c, true);
            }
            String time = (String) timestamp.invoke(null);
            return (MutableComponent) format.invoke(null, sender, message, time, false);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            available = false;
            SimpleProximityChat.LOGGER.warn("Beautified Chat Server no respondió como se esperaba; "
                    + "los mensajes a distancia/radio usarán el formato alternativo", e);
            return null;
        }
    }

    private static synchronized boolean init() {
        if (initialized) return available;
        initialized = true;
        if (!isLoaded()) return available = false;
        try {
            Class<?> util = Class.forName(UTIL);
            format = util.getMethod("getFormattedChatMessage", ServerPlayer.class, MutableComponent.class, String.class, boolean.class);
            timestamp = util.getMethod("getTimestamp");
            colour = util.getMethod("getColour", String.class);
            coloured = util.getMethod("getColouredComponent", Component.class, ChatFormatting.class, boolean.class);
            available = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            available = false;
            SimpleProximityChat.LOGGER.warn("Beautified Chat Server instalado pero con una API distinta; se ignora", e);
        }
        return available;
    }
}
