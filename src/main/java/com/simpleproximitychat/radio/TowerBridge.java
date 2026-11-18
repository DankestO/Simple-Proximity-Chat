package com.simpleproximitychat.radio;

import com.codinglitch.simpleradio.central.AuditoryBlockEntity;
import com.codinglitch.simpleradio.central.FrequencingType;
import com.codinglitch.simpleradio.central.Frequency;
import com.codinglitch.simpleradio.central.Wiring;
import com.codinglitch.simpleradio.central.WorldlyPosition;
import com.codinglitch.simpleradio.core.registry.blocks.MicrophoneBlockEntity;
import com.codinglitch.simpleradio.radio.RadioManager;
import com.codinglitch.simpleradio.routers.Listener;
import com.codinglitch.simpleradio.routers.Receiver;
import com.codinglitch.simpleradio.routers.Router;
import com.codinglitch.simpleradio.routers.Speaker;
import com.codinglitch.simpleradio.routers.Transmitter;
import com.simpleproximitychat.SimpleProximityChat;
import com.simpleproximitychat.radio.RadioNodes.Output;
import com.simpleproximitychat.radio.RadioNodes.Sink;
import com.simpleproximitychat.radio.RadioNodes.Source;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Torres de radio de Simple Radio: Micrófono → (cables) → Transmisor ··· Receptor → (cables) → Altavoz.
 *
 * Usa las clases internas de Simple Radio (no son una API oficial). Por eso todo está aquí
 * aislado: solo se carga si Simple Radio está instalado, y cualquier error se registra una vez
 * y se ignora, para que el resto del mod siga funcionando.
 *
 * Los walkies (routers con dueño) se ignoran aquí: los gestiona {@link Walkie} con la regla
 * de "en la mano o en Curios".
 */
final class TowerBridge {

    private static final int MAX_HOPS = 32;
    private static boolean warned;

    private TowerBridge() {}

    // ---------------------------------------------------------------------------------------
    // Emisión: micrófonos encendidos cerca del que escribe → sus transmisores
    // ---------------------------------------------------------------------------------------

    static List<Source> sourcesFor(ServerPlayer sender) {
        try {
            List<Source> out = new ArrayList<>();
            for (Router router : RadioManager.getInstance().getRouters()) {
                if (!(router instanceof Listener mic) || !isBlockRouter(router)) continue;
                if (!isActiveMicInRange(mic, sender)) continue;
                for (Transmitter tx : collect(router, Transmitter.class)) {
                    Source src = toSource(tx);
                    if (src != null) out.add(src);
                }
            }
            return out;
        } catch (RuntimeException | LinkageError e) {
            warn(e);
            return List.of();
        }
    }

    private static Source toSource(Transmitter tx) {
        Frequency frequency = tx.getFrequency();
        WorldlyPosition loc = tx.getLocation();
        if (frequency == null || loc == null || loc.level == null) return null;
        Frequency.Modulation mod = frequency.getModulation();
        FrequencingType type = tx.getFrequencingType();
        double threshold = type != null ? type.getDiminishThreshold(mod) : 200;
        double dim = type != null ? type.transmissionDiminishment : 1.0;
        return new Source(null, loc.level, vec(loc), frequency.getFrequency(),
                mod == Frequency.Modulation.AMPLITUDE, tx.getPower(mod), threshold, dim);
    }

    // ---------------------------------------------------------------------------------------
    // Recepción: receptores/radios de bloque → sus altavoces
    // ---------------------------------------------------------------------------------------

    static List<Sink> sinks() {
        try {
            List<Sink> out = new ArrayList<>();
            for (Frequency frequency : RadioManager.getInstance().frequencies().get()) {
                boolean am = frequency.getModulation() == Frequency.Modulation.AMPLITUDE;
                for (Receiver rx : new ArrayList<>(frequency.getReceivers())) {
                    if (!isBlockRouter(rx)) continue;
                    WorldlyPosition loc = rx.getLocation();
                    if (loc == null || loc.level == null) continue;

                    List<Output> outputs = new ArrayList<>();
                    for (Speaker speaker : speakersOf(rx)) {
                        WorldlyPosition sl = speaker.getLocation();
                        if (sl == null || sl.level == null) continue;
                        outputs.add(new Output(sl.level, vec(sl), speaker.getRange(), null, sl.blockPos()));
                    }
                    if (outputs.isEmpty()) continue;
                    out.add(new Sink(null, loc.level, vec(loc), frequency.getFrequency(), am, rx.getPower(), outputs));
                }
            }
            return out;
        } catch (RuntimeException | LinkageError e) {
            warn(e);
            return List.of();
        }
    }

    private static List<Speaker> speakersOf(Receiver rx) {
        List<Speaker> speakers = new ArrayList<>(collect(rx, Speaker.class));
        // La Radio de bloque es receptor + altavoz en el mismo bloque
        BlockEntity be = blockEntity(rx.getLocation());
        if (be instanceof AuditoryBlockEntity auditory && auditory.speaker != null && !speakers.contains(auditory.speaker)) {
            speakers.add(auditory.speaker);
        }
        return speakers;
    }

    // ---------------------------------------------------------------------------------------
    // Iconos
    // ---------------------------------------------------------------------------------------

    /** ¿Está el jugador dentro del alcance de un Micrófono de torre encendido? */
    static boolean nearActiveMic(ServerPlayer player) {
        try {
            for (Router router : RadioManager.getInstance().getRouters()) {
                if (router instanceof Listener mic && isBlockRouter(router) && isActiveMicInRange(mic, player)) {
                    return true;
                }
            }
        } catch (RuntimeException | LinkageError e) {
            warn(e);
        }
        return false;
    }

    /** ¿Está el jugador dentro del alcance de un Altavoz o Radio de bloque? */
    static boolean nearSpeaker(ServerPlayer player) {
        try {
            for (Router router : RadioManager.getInstance().getRouters()) {
                if (!(router instanceof Speaker speaker) || !isBlockRouter(router)) continue;
                WorldlyPosition loc = router.getLocation();
                if (loc == null || loc.level != player.level()) continue;
                if (vec(loc).distanceTo(player.position()) <= speaker.getRange()) return true;
            }
        } catch (RuntimeException | LinkageError e) {
            warn(e);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------------------------

    private static boolean isActiveMicInRange(Listener mic, ServerPlayer player) {
        WorldlyPosition loc = mic.getLocation();
        if (loc == null || loc.level != player.level()) return false;
        if (!(blockEntity(loc) instanceof MicrophoneBlockEntity be) || !be.isListening()) return false;
        return vec(loc).distanceTo(player.position()) <= mic.getRange();
    }

    /** Router de un bloque (no de un objeto que lleva alguien) y del lado servidor. */
    private static boolean isBlockRouter(Router router) {
        return router.getOwner() == null && !Boolean.TRUE.equals(router.isClientSide()) && router.isValid();
    }

    /**
     * Recorre las conexiones (routers enlazados y cables) desde {@code start} y devuelve los
     * routers de bloque del tipo pedido, sin atravesarlos.
     */
    private static <T extends Router> List<T> collect(Router start, Class<T> type) {
        List<T> found = new ArrayList<>();
        Set<Router> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Router> queue = new ArrayDeque<>();
        ArrayDeque<Integer> depth = new ArrayDeque<>();
        queue.add(start);
        depth.add(0);
        visited.add(start);

        while (!queue.isEmpty()) {
            Router current = queue.poll();
            int d = depth.poll();
            if (current != start && type.isInstance(current)) {
                if (isBlockRouter(current)) found.add(type.cast(current));
                continue; // no seguimos más allá del destino
            }
            if (d >= MAX_HOPS) continue;

            List<Router> next = new ArrayList<>();
            List<Router> linked = current.getRouters();
            if (linked != null) next.addAll(linked);
            List<Wiring> wires = current.getWires();
            if (wires != null) {
                for (Wiring wire : new ArrayList<>(wires)) {
                    if (wire == null || !wire.isValid()) continue;
                    Router from = wire.getFromRouter();
                    Router to = wire.getToRouter();
                    if (from != null) next.add(from);
                    if (to != null) next.add(to);
                }
            }
            for (Router n : next) {
                if (n != null && visited.add(n)) {
                    queue.add(n);
                    depth.add(d + 1);
                }
            }
        }
        return found;
    }

    private static BlockEntity blockEntity(WorldlyPosition loc) {
        if (loc == null || loc.level == null) return null;
        return loc.level.getBlockEntity(loc.blockPos());
    }

    private static Vec3 vec(WorldlyPosition loc) {
        return new Vec3(loc.x, loc.y, loc.z);
    }

    private static void warn(Throwable e) {
        if (!warned) {
            warned = true;
            SimpleProximityChat.LOGGER.warn("Error leyendo las torres de Simple Radio; se ignoran las torres. "
                    + "¿Versión de Simple Radio distinta de 4.6.x?", e);
        }
    }
}
