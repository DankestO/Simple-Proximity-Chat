package com.simpleproximitychat.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Piezas de la red de radio, sin depender de las clases de Simple Radio.
 * Tanto los walkies como las torres se traducen a estas piezas.
 */
public final class RadioNodes {

    private RadioNodes() {}

    /**
     * Punto que emite: un walkie activo o un Transmisor de torre.
     *
     * @param holder    jugador dueño del walkie, o null si es un bloque
     * @param txPower   potencia de transmisión (incluye antena, como en Simple Radio)
     * @param threshold diminishThreshold: por debajo de esta potencia sobrante empieza la estática
     * @param dim       transmissionDiminishment: cuánta potencia se pierde por bloque
     */
    public record Source(@Nullable ServerPlayer holder, Level level, Vec3 pos,
                         String frequency, boolean am,
                         double txPower, double threshold, double dim) {
        public boolean sameChannel(Sink sink) {
            return am == sink.am() && frequency.equals(sink.frequency());
        }
    }

    /**
     * Punto que recibe: un walkie activo o un Receptor/Radio de bloque.
     *
     * @param rxPower potencia de recepción (incluye antena)
     * @param outputs dónde se "oye" lo recibido (dueño del walkie, altavoces...)
     */
    public record Sink(@Nullable ServerPlayer holder, Level level, Vec3 pos,
                       String frequency, boolean am, double rxPower, List<Output> outputs) {}

    /**
     * Sitio donde se reproduce lo recibido. Lo leen los jugadores a {@code range} bloques
     * (y siempre {@code entity}, el dueño del walkie, si lo hay).
     */
    public record Output(Level level, Vec3 pos, double range,
                         @Nullable ServerPlayer entity, @Nullable BlockPos block) {}

    /**
     * Calidad de la señal según la fórmula de Simple Radio:
     * llega si txPower + rxPower >= distancia * dim; la potencia sobrante por debajo de
     * diminishThreshold se convierte en estática.
     *
     * Entre dimensiones (config de Simple Radio, sección "frequency"):
     *  - solo si crossDimensional = true;
     *  - la potencia pierde dimensionalFMInterference / dimensionalAMInterference;
     *  - la distancia se mide con las coordenadas escaladas de cada dimensión (Nether x8) y se
     *    divide entre |escalaA - escalaB| * dimensionalScaling. Si las dos dimensiones tienen la
     *    misma escala (p. ej. mundo normal y End) esa distancia es infinita, igual que en Simple Radio.
     *
     * @return 0 = limpio, (0,1] = con estática, -1 = no llega
     */
    public static float signal(Source src, Sink sink) {
        double tx = src.txPower();
        double d;
        DimensionType from = src.level().dimensionType();
        DimensionType to = sink.level().dimensionType();
        if (from == to) {
            d = src.pos().distanceTo(sink.pos());
        } else {
            if (!crossDimensional()) return -1f;
            tx -= src.am()
                    ? SimpleRadioConfig.getDouble("frequency", "dimensionalAMInterference", 50.0D)
                    : SimpleRadioConfig.getDouble("frequency", "dimensionalFMInterference", 75.0D);
            double scaleFrom = from.coordinateScale();
            double scaleTo = to.coordinateScale();
            double factor = Math.abs(scaleFrom - scaleTo)
                    * SimpleRadioConfig.getDouble("frequency", "dimensionalScaling", 0.75D);
            if (factor <= 0) return -1f;
            d = scaled(src.pos(), scaleFrom).distanceTo(scaled(sink.pos(), scaleTo)) / factor;
        }
        double remaining = tx + sink.rxPower() - d * src.dim();
        if (remaining < 0) return -1f;
        if (src.threshold() <= 0) return 0f;
        double clean = Math.min(1.0, remaining / src.threshold());
        return (float) (1.0 - clean);
    }

    /** Opción crossDimensional de Simple Radio (desactivada por defecto). */
    public static boolean crossDimensional() {
        return SimpleRadioConfig.getBoolean("frequency", "crossDimensional", false);
    }

    private static Vec3 scaled(Vec3 pos, double scale) {
        return new Vec3(pos.x * scale, pos.y, pos.z * scale);
    }
}
