package com.simpleproximitychat;

/**
 * Cómo le llega un mensaje a un jugador concreto.
 *
 * @param signed    true = se entrega el mensaje original de vanilla (firmado, sin tocar)
 * @param radio     true = le llega por radio (walkie o altavoz)
 * @param intensity distorsión del texto (0 = limpio), solo si no es "signed"
 */
public record Delivery(boolean signed, boolean radio, float intensity) {

    public static final Delivery ORIGINAL = new Delivery(true, false, 0f);

    public static Delivery ofLocal(float intensity) {
        return new Delivery(false, false, intensity);
    }

    public static Delivery ofRadio(float intensity) {
        return new Delivery(false, true, intensity);
    }
}
