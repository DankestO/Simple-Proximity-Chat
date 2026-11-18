package com.simpleproximitychat.network;

import com.simpleproximitychat.client.ClientRadioState;
import com.simpleproximitychat.radio.HudState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: qué icono de radio mostrar. */
public record RadioStatePacket(HudState state) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(state.ordinal());
    }

    public static RadioStatePacket decode(FriendlyByteBuf buf) {
        return new RadioStatePacket(HudState.byId(buf.readByte()));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRadioState.set(state));
        ctx.get().setPacketHandled(true);
    }
}
