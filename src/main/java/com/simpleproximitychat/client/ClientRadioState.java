package com.simpleproximitychat.client;

import com.simpleproximitychat.radio.HudState;

/** Estado de radio recibido del servidor (solo cliente). */
public final class ClientRadioState {

    private static volatile HudState state = HudState.NONE;

    private ClientRadioState() {}

    public static HudState get() {
        return state;
    }

    public static void set(HudState newState) {
        state = newState == null ? HudState.NONE : newState;
    }
}
