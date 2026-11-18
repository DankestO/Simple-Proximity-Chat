package com.simpleproximitychat.radio;

import com.simpleproximitychat.SimpleProximityChat;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Punto de entrada a todo lo relacionado con Simple Radio (dependencia opcional).
 * Si Simple Radio no está instalado, no hace nada y el mod queda como chat por proximidad.
 */
public final class RadioCompat {

    private RadioCompat() {}

    public static boolean isLoaded() {
        return SimpleProximityChat.isLoaded("simpleradio");
    }

    public static RadioNetwork.Result send(ServerPlayer sender, List<ServerPlayer> players) {
        if (!isLoaded()) return RadioNetwork.Result.EMPTY;
        return RadioNetwork.compute(sender, players);
    }

    public static Map<UUID, HudState> hudStates(List<ServerPlayer> players) {
        Map<UUID, HudState> states = new HashMap<>();
        if (!isLoaded()) return states;
        Map<ServerPlayer, List<Walkie>> active = RadioNetwork.activeWalkies(players);
        for (ServerPlayer p : players) {
            states.put(p.getUUID(), RadioNetwork.hudState(p, active));
        }
        return states;
    }
}
