package com.simpleproximitychat.radio;

import com.simpleproximitychat.radio.RadioNodes.Output;
import com.simpleproximitychat.radio.RadioNodes.Sink;
import com.simpleproximitychat.radio.RadioNodes.Source;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recorrido del texto por radio, imitando cómo Simple Radio mueve la voz.
 *
 *  Emiten:
 *   - el walkie activo (mano/Curios) del que escribe,
 *   - el walkie activo de otro jugador si el que escribe está en su listeningRange,
 *   - los Transmisores conectados a un Micrófono de torre encendido junto al que escribe.
 *  Reciben (misma frecuencia y modulación):
 *   - walkies activos → los lee su dueño y quien esté en su speakingRange,
 *   - Receptores/Radios de bloque → lo leen quienes estén en el alcance de sus Altavoces.
 *  Señal: fórmula de Simple Radio (ver {@link RadioNodes#signal}).
 *
 * Walkies y torres se mezclan: un walkie llega a una torre y una torre a un walkie.
 * Entre dimensiones solo llega si Simple Radio tiene crossDimensional activado.
 */
public final class RadioNetwork {

    private RadioNetwork() {}

    /** Resultado de enviar un mensaje por radio. */
    public record Result(Map<UUID, Float> intensities, List<Output> reached) {
        public static final Result EMPTY = new Result(Map.of(), List.of());
    }

    public static Result compute(ServerPlayer sender, List<ServerPlayer> players) {
        Map<ServerPlayer, List<Walkie>> active = activeWalkies(players);

        // 1) Emisores
        List<Source> sources = new ArrayList<>();
        for (Walkie w : active.getOrDefault(sender, List.of())) {
            sources.add(w.asSource());
        }
        for (Map.Entry<ServerPlayer, List<Walkie>> e : active.entrySet()) {
            ServerPlayer holder = e.getKey();
            if (holder == sender || holder.level() != sender.level()) continue;
            double d = holder.distanceTo(sender);
            for (Walkie w : e.getValue()) {
                if (d <= w.listeningRange()) sources.add(w.asSource());
            }
        }
        sources.addAll(TowerBridge.sourcesFor(sender));
        if (sources.isEmpty()) return Result.EMPTY;

        // 2) Receptores
        List<Sink> sinks = new ArrayList<>();
        for (List<Walkie> ws : active.values()) {
            for (Walkie w : ws) sinks.add(w.asSink());
        }
        sinks.addAll(TowerBridge.sinks());

        // 3) Señal de cada emisor a cada receptor; nos quedamos con la mejor
        Map<UUID, Float> result = new HashMap<>();
        List<Output> reached = new ArrayList<>();
        for (Sink sink : sinks) {
            float best = Float.MAX_VALUE;
            for (Source src : sources) {
                if (src.holder() != null && src.holder() == sink.holder()) continue; // su propio walkie
                if (!src.sameChannel(sink)) continue; // otra dimensión: lo decide RadioNodes.signal
                float s = RadioNodes.signal(src, sink);
                if (s >= 0f && s < best) best = s;
            }
            if (best == Float.MAX_VALUE) continue;

            for (Output out : sink.outputs()) {
                reached.add(out);
                for (ServerPlayer listener : players) {
                    if (listener == sender || listener.level() != out.level()) continue;
                    if (listener == out.entity() || listener.position().distanceTo(out.pos()) <= out.range()) {
                        result.merge(listener.getUUID(), best, Math::min);
                    }
                }
            }
        }
        return new Result(result, reached);
    }

    /**
     * Icono: transmite si tiene walkie activo, está en el listeningRange de uno ajeno o junto a un
     * Micrófono encendido; recibe si tiene walkie activo, está en el speakingRange de uno ajeno o
     * junto a un Altavoz/Radio.
     */
    public static HudState hudState(ServerPlayer player, Map<ServerPlayer, List<Walkie>> active) {
        boolean own = active.containsKey(player);
        boolean transmit = own;
        boolean receive = own;
        if (!own) {
            for (Map.Entry<ServerPlayer, List<Walkie>> e : active.entrySet()) {
                ServerPlayer holder = e.getKey();
                if (holder == player || holder.level() != player.level()) continue;
                double d = holder.distanceTo(player);
                for (Walkie w : e.getValue()) {
                    if (d <= w.listeningRange()) transmit = true;
                    if (d <= w.speakingRange()) receive = true;
                }
                if (transmit && receive) break;
            }
        }
        if (!transmit) transmit = TowerBridge.nearActiveMic(player);
        if (!receive) receive = TowerBridge.nearSpeaker(player);
        return HudState.of(transmit, receive);
    }

    public static Map<ServerPlayer, List<Walkie>> activeWalkies(List<ServerPlayer> players) {
        Map<ServerPlayer, List<Walkie>> active = new HashMap<>();
        for (ServerPlayer p : players) {
            List<Walkie> ws = Walkie.activeOf(p);
            if (!ws.isEmpty()) active.put(p, ws);
        }
        return active;
    }
}
