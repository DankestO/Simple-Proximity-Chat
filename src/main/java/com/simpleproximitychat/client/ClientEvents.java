package com.simpleproximitychat.client;

import com.simpleproximitychat.SimpleProximityChat;
import com.simpleproximitychat.radio.HudState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ClientEvents {

    private ClientEvents() {}

    @Mod.EventBusSubscriber(modid = SimpleProximityChat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("radio_state", RadioHudOverlay::render);
        }
    }

    @Mod.EventBusSubscriber(modid = SimpleProximityChat.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientRadioState.set(HudState.NONE);
        }
    }
}
