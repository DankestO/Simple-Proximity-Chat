package com.simpleproximitychat.mixin;

import com.simpleproximitychat.ChatPlan;
import com.simpleproximitychat.ChatRouter;
import com.simpleproximitychat.Delivery;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

/**
 * Filtra los destinatarios del chat de jugadores, en lugar de cancelar el evento.
 *
 * Vanilla, en PlayerList#broadcastChatMessage, primero escribe el mensaje en la consola y
 * después llama a ServerPlayer#sendChatMessage para cada jugador. Solo intervenimos en ese
 * envío: así MCA, la consola, Simple Radio (frequency ...) y el resto de mods lo siguen viendo.
 *
 * Los comandos /say, /me, /msg usan otras sobrecargas y no se ven afectados.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    private static final String SPC_TARGET =
            "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V";

    @Unique
    @Nullable
    private ChatPlan spc$plan;

    @Inject(method = SPC_TARGET, at = @At("HEAD"))
    private void spc$computePlan(PlayerChatMessage message, Predicate<ServerPlayer> shouldFilter,
                                 @Nullable ServerPlayer sender, ChatType.Bound bound, CallbackInfo ci) {
        // Sin remitente (consola, comandos...) = comportamiento vanilla, a todos.
        this.spc$plan = sender == null ? null : ChatRouter.plan(sender, ((PlayerList) (Object) this).getPlayers());
    }

    @Redirect(method = SPC_TARGET, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"))
    private void spc$filterRecipient(ServerPlayer receiver, OutgoingChatMessage outgoing, boolean filtered, ChatType.Bound bound) {
        ChatPlan plan = this.spc$plan;
        if (plan == null) {
            receiver.sendChatMessage(outgoing, filtered, bound);
            return;
        }
        Delivery delivery = plan.deliveries().get(receiver.getUUID());
        if (delivery != null && delivery.signed()) {
            receiver.sendChatMessage(outgoing, filtered, bound);
        }
        // Si no: o no le llega, o le llega una versión distorsionada / por radio al final.
    }

    @Inject(method = SPC_TARGET, at = @At("RETURN"))
    private void spc$deliverText(PlayerChatMessage message, Predicate<ServerPlayer> shouldFilter,
                                 @Nullable ServerPlayer sender, ChatType.Bound bound, CallbackInfo ci) {
        ChatPlan plan = this.spc$plan;
        this.spc$plan = null;
        if (plan != null && sender != null) {
            ChatRouter.deliver(sender, message, plan, ((PlayerList) (Object) this).getPlayers(), shouldFilter, bound);
        }
    }
}
