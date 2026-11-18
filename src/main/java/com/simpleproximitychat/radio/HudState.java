package com.simpleproximitychat.radio;

/** Qué icono ve el jugador encima del micrófono de Voice Chat. */
public enum HudState {
    /** Ni transmite ni recibe por radio. */
    NONE,
    /** Transmite y recibe (walkie en la mano/Curios, o junto a alguien que lo tiene). */
    RADIO,
    /** Solo transmite (p. ej. junto a un Micrófono de torre encendido). */
    MICROPHONE,
    /** Solo recibe (p. ej. junto a un Altavoz o Radio de bloque). */
    SPEAKER;

    public static HudState of(boolean transmit, boolean receive) {
        if (transmit && receive) return RADIO;
        if (transmit) return MICROPHONE;
        if (receive) return SPEAKER;
        return NONE;
    }

    public static HudState byId(int id) {
        HudState[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
