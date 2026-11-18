package com.simpleproximitychat.radio;

import com.simpleproximitychat.SimpleProximityChat;
import com.simpleproximitychat.compat.CuriosCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Un walkie (o transceptor) de Simple Radio que un jugador tiene "activo":
 * en una mano o en un hueco de Curios. En el inventario normal no cuenta.
 *
 * Se identifica por ID de objeto y se lee su sintonía del NBT, igual que hace Simple Radio
 * (claves "frequency" y "modulation" = "FM"/"AM").
 */
public record Walkie(ServerPlayer holder, Type type, String frequency, boolean am) {

    public enum Type {
        WALKIE_TALKIE("walkie_talkie"),
        TRANSCEIVER("transceiver");

        /** Página de la config de servidor de Simple Radio. */
        public final String configPage;

        Type(String configPage) {
            this.configPage = configPage;
        }
    }

    // --- Valores de la config de Simple Radio (con sus valores por defecto de 4.6.x) ---

    public int transmissionPower() {
        boolean walkie = type == Type.WALKIE_TALKIE;
        return am
                ? SimpleRadioConfig.getInt(type.configPage, "transmissionPowerAM", walkie ? 900 : 1800)
                : SimpleRadioConfig.getInt(type.configPage, "transmissionPowerFM", walkie ? 500 : 1000);
    }

    public int diminishThreshold() {
        boolean walkie = type == Type.WALKIE_TALKIE;
        return am
                ? SimpleRadioConfig.getInt(type.configPage, "diminishThresholdAM", walkie ? 200 : 300)
                : SimpleRadioConfig.getInt(type.configPage, "diminishThresholdFM", walkie ? 100 : 200);
    }

    public int receptionPower() {
        return SimpleRadioConfig.getInt(type.configPage, "receptionPower", type == Type.WALKIE_TALKIE ? 100 : 200);
    }

    public double transmissionDiminishment() {
        return SimpleRadioConfig.getDouble(type.configPage, "transmissionDiminishment", 1.0D);
    }

    /** Este walkie como emisor, en la posición de su dueño. */
    public RadioNodes.Source asSource() {
        return new RadioNodes.Source(holder, holder.level(), holder.position(), frequency, am,
                transmissionPower(), diminishThreshold(), transmissionDiminishment());
    }

    /** Este walkie como receptor: lo leen su dueño y quien esté en su speakingRange. */
    public RadioNodes.Sink asSink() {
        RadioNodes.Output output = new RadioNodes.Output(holder.level(), holder.position(), speakingRange(), holder, null);
        return new RadioNodes.Sink(holder, holder.level(), holder.position(), frequency, am, receptionPower(), List.of(output));
    }

    /** Distancia a la que el walkie capta lo que se dice a su alrededor. */
    public int listeningRange() {
        return SimpleRadioConfig.getInt(type.configPage, "listeningRange", 4);
    }

    /** Distancia a la que se oye/lee lo que recibe el walkie. */
    public int speakingRange() {
        return SimpleRadioConfig.getInt(type.configPage, "speakingRange", 4);
    }

    public boolean sameChannel(Walkie other) {
        return am == other.am && frequency.equals(other.frequency);
    }

    // --- Detección ---

    /** Walkies activos de un jugador: manos + Curios. */
    public static List<Walkie> activeOf(ServerPlayer player) {
        List<Walkie> out = new ArrayList<>(2);
        add(out, player, player.getMainHandItem());
        add(out, player, player.getOffhandItem());
        if (SimpleProximityChat.isLoaded("curios")) {
            for (ItemStack stack : CuriosCompat.findStacks(player, Walkie::isRadioItem)) {
                add(out, player, stack);
            }
        }
        return out;
    }

    private static void add(List<Walkie> out, ServerPlayer player, ItemStack stack) {
        Type type = typeOf(stack);
        if (type == null) return;
        if (!SimpleRadioConfig.getBoolean(type.configPage, "enabled", true)) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("frequency")) return; // sin sintonizar todavía
        String frequency = tag.getString("frequency");
        if (frequency.isEmpty()) return;
        boolean am = "AM".equalsIgnoreCase(tag.getString("modulation"));

        Walkie walkie = new Walkie(player, type, frequency, am);
        // Evita duplicados si lleva dos walkies iguales
        for (Walkie w : out) {
            if (w.type == type && w.sameChannel(walkie)) return;
        }
        out.add(walkie);
    }

    public static boolean isRadioItem(ItemStack stack) {
        return typeOf(stack) != null;
    }

    private static Type typeOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !"simpleradio".equals(id.getNamespace())) return null;
        return switch (id.getPath()) {
            case "walkie_talkie", "spuddie_talkie" -> Type.WALKIE_TALKIE;
            case "transceiver" -> Type.TRANSCEIVER;
            default -> null;
        };
    }
}
