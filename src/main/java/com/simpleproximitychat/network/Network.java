package com.simpleproximitychat.network;

import com.simpleproximitychat.SimpleProximityChat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {

    private static final String PROTOCOL = "1";

    // acceptMissingOr: los jugadores sin el mod pueden entrar igual (solo no verán iconos).
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SimpleProximityChat.MODID, "main"),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));

    private Network() {}

    public static void register() {
        CHANNEL.messageBuilder(RadioStatePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RadioStatePacket::encode)
                .decoder(RadioStatePacket::decode)
                .consumerMainThread(RadioStatePacket::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
