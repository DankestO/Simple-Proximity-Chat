package com.simpleproximitychat;

import com.simpleproximitychat.compat.BeautifiedChatCompat;
import com.simpleproximitychat.compat.VibrativeCompat;
import com.simpleproximitychat.radio.RadioCompat;
import com.simpleproximitychat.radio.RadioNetwork;
import com.simpleproximitychat.radio.RadioNodes;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Decide a quién y cómo le llega cada mensaje de chat de un jugador.
 *
 * No cancela nada: el mensaje sigue el recorrido normal de vanilla (MCA, consola,
 * Simple Radio y otros mods lo siguen viendo). Solo se elige a qué jugadores se les envía.
 */
public final class ChatRouter {

    /** A partir de este porcentaje de la distancia de voz el texto empieza a degradarse. */
    private static final double CLEAR_FRACTION = 0.75D;

    /** Color de los mensajes recibidos por radio (el ámbar de los iconos). */
    private static final TextColor RADIO_COLOR = TextColor.fromRgb(0xFFAA3C);

    private ChatRouter() {}

    /** Calcula, para cada jugador conectado, cómo debe recibir el mensaje de {@code sender}. */
    public static ChatPlan plan(ServerPlayer sender, List<ServerPlayer> players) {
        Map<UUID, Delivery> plan = new HashMap<>();
        double range = SpcVoicechatPlugin.voiceDistance();
        double clear = range * CLEAR_FRACTION;

        // 1) Proximidad, con el alcance de Simple Voice Chat
        Map<UUID, Float> local = new HashMap<>();
        for (ServerPlayer receiver : players) {
            if (receiver == sender) {
                plan.put(receiver.getUUID(), Delivery.ORIGINAL);
                continue;
            }
            if (receiver.level() != sender.level()) {
                continue;
            }
            double d = receiver.distanceTo(sender);
            if (d <= clear) {
                plan.put(receiver.getUUID(), Delivery.ORIGINAL);
            } else if (d <= range) {
                local.put(receiver.getUUID(), (float) ((d - clear) / (range - clear)));
            }
        }

        // 2) Radio (Simple Radio): walkies y torres
        RadioNetwork.Result radio = RadioCompat.send(sender, players);

        // 3) Para cada jugador, la mejor señal de las dos
        for (ServerPlayer receiver : players) {
            UUID id = receiver.getUUID();
            if (plan.containsKey(id)) {
                continue;
            }
            Float l = local.get(id);
            Float r = radio.intensities().get(id);
            if (r != null && (l == null || r <= l)) {
                plan.put(id, Delivery.ofRadio(r));
            } else if (l != null) {
                plan.put(id, Delivery.ofLocal(l));
            }
        }
        return new ChatPlan(plan, radio.reached());
    }

    /**
     * Envía las versiones de texto (distorsionadas o por radio) a quienes no reciben el
     * mensaje original, y genera las vibraciones. Se llama después de que vanilla haya
     * repartido el chat.
     *
     * Con Beautified Chat Server instalado se formatean con su propia función (rangos, hora,
     * colores de nombre). Si eso falla, se envían como chat de jugador con el texto cambiado para
     * que él los intercepte. Sin él, como mensaje de sistema con el formato vanilla.
     */
    public static void deliver(ServerPlayer sender, PlayerChatMessage message, ChatPlan plan,
                               List<ServerPlayer> players, Predicate<ServerPlayer> shouldFilter, ChatType.Bound bound) {
        String text = message.decoratedContent().getString();
        boolean decorator = BeautifiedChatCompat.isLoaded();
        for (ServerPlayer receiver : players) {
            Delivery delivery = plan.deliveries().get(receiver.getUUID());
            if (delivery == null || delivery.signed()) {
                continue;
            }
            String shown = Distortion.apply(text, delivery.intensity());
            MutableComponent body = Component.literal(shown);
            if (delivery.radio()) {
                body = body.withStyle(style -> style.withColor(RADIO_COLOR));
            }
            MutableComponent formatted = decorator
                    ? BeautifiedChatCompat.format(sender, body, delivery.radio()) // la radio conserva su ámbar
                    : null;
            if (formatted != null) {
                receiver.sendSystemMessage(formatted);
            } else if (decorator) {
                // Alternativa: mismo mensaje firmado con el contenido cambiado; el decorador lo intercepta
                OutgoingChatMessage outgoing = OutgoingChatMessage.create(message.withUnsignedContent(body));
                receiver.sendChatMessage(outgoing, shouldFilter.test(receiver), bound);
            } else {
                // Mismo formato que el chat de vanilla: <Nombre> mensaje
                receiver.sendSystemMessage(Component.translatable("chat.type.text", sender.getDisplayName(), body));
            }
        }

        // VibrativeVoice: quien escribe hace "ruido", y también cada walkie/altavoz que lo reproduce
        VibrativeCompat.vibrate(sender);
        Set<Object> done = new HashSet<>();
        for (RadioNodes.Output out : plan.radioOutputs()) {
            if (out.entity() != null) {
                if (done.add(out.entity().getUUID())) VibrativeCompat.vibrate(out.entity());
            } else if (out.block() != null) {
                if (done.add(out.block())) VibrativeCompat.vibrate(out.level(), out.block());
            }
        }
    }
}
