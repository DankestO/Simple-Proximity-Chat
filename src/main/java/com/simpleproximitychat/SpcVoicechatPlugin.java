package com.simpleproximitychat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

/**
 * Plugin de Simple Voice Chat. Solo lo usamos para leer:
 *  - en el servidor: la distancia de voz (alcance del chat local),
 *  - en el cliente: la posición/escala del icono de micrófono (para dibujar el nuestro encima).
 */
@ForgeVoicechatPlugin
public class SpcVoicechatPlugin implements VoicechatPlugin {

    /** Distancia por defecto de Simple Voice Chat, por si aún no se ha inicializado. */
    public static final double DEFAULT_DISTANCE = 48.0D;

    private static volatile VoicechatApi api;
    private static volatile VoicechatClientApi clientApi;

    @Override
    public String getPluginId() {
        return SimpleProximityChat.MODID;
    }

    @Override
    public void initialize(VoicechatApi voicechatApi) {
        api = voicechatApi;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatConnectionEvent.class, event -> clientApi = event.getVoicechat());
    }

    /** Distancia de voz configurada en Simple Voice Chat (voice_chat_distance). */
    public static double voiceDistance() {
        VoicechatApi a = api;
        if (a == null) {
            return DEFAULT_DISTANCE;
        }
        double d = a.getVoiceChatDistance();
        return d > 0 ? d : DEFAULT_DISTANCE;
    }

    /** API de cliente de Voice Chat, o null si aún no se ha conectado. */
    public static VoicechatClientApi clientApi() {
        return clientApi;
    }
}
