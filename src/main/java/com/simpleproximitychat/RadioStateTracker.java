package com.simpleproximitychat;

import com.simpleproximitychat.network.Network;
import com.simpleproximitychat.network.RadioStatePacket;
import com.simpleproximitychat.radio.HudState;
import com.simpleproximitychat.radio.RadioCompat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cada medio segundo calcula el icono de radio de cada jugador y se lo envía solo si ha cambiado.
 */
@Mod.EventBusSubscriber(modid = SimpleProximityChat.MODID)
public final class RadioStateTracker {

    private static final int INTERVAL_TICKS = 10;
    private static final Map<UUID, HudState> LAST = new HashMap<>();
    private static int tick;

    private RadioStateTracker() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++tick % INTERVAL_TICKS != 0) return;
        if (!RadioCompat.isLoaded()) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        Map<UUID, HudState> states = RadioCompat.hudStates(players);
        for (ServerPlayer player : players) {
            HudState state = states.getOrDefault(player.getUUID(), HudState.NONE);
            if (LAST.put(player.getUUID(), state) != state) {
                Network.sendTo(player, new RadioStatePacket(state));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Fuerza reenvío tras cambiar de dimensión
        LAST.remove(event.getEntity().getUUID());
    }
}
