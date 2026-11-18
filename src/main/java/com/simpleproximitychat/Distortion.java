package com.simpleproximitychat;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Distorsión de texto: sustituye palabras al azar por guiones bajos de la misma longitud,
 * como si se perdieran por la distancia o la estática de la radio.
 */
public final class Distortion {

    private Distortion() {}

    /**
     * @param text      texto original
     * @param intensity 0 = limpio, 1 = casi ilegible
     */
    public static String apply(String text, float intensity) {
        if (intensity <= 0f || text.isEmpty()) {
            return text;
        }
        float i = Math.min(1f, intensity);
        // Al empezar a degradarse ya se pierde alguna palabra; al límite se pierden casi todas.
        float lossChance = 0.15f + 0.75f * i;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        String[] words = text.split(" ", -1);
        StringBuilder out = new StringBuilder(text.length());
        for (int w = 0; w < words.length; w++) {
            if (w > 0) {
                out.append(' ');
            }
            String word = words[w];
            if (!word.isEmpty() && random.nextFloat() < lossChance) {
                out.append("_".repeat(word.length()));
            } else {
                out.append(word);
            }
        }
        return out.toString();
    }
}
