package com.simpleproximitychat;

import com.mojang.brigadier.CommandDispatcher;
import com.simpleproximitychat.radio.RadioCompat;
import com.simpleproximitychat.radio.RadioNodes;
import com.simpleproximitychat.radio.Walkie;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * /simpleproximitychat debug — muestra qué calcula el mod para quien lo ejecuta:
 * distancia de voz, radios activas detectadas y cómo le llegaría un mensaje a cada jugador.
 */
@Mod.EventBusSubscriber(modid = SimpleProximityChat.MODID)
public final class DebugCommand {

    private DebugCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("simpleproximitychat")
                .then(Commands.literal("debug").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    run(player);
                    return 1;
                })));
    }

    private static void run(ServerPlayer me) {
        List<ServerPlayer> players = me.server.getPlayerList().getPlayers();
        double range = SpcVoicechatPlugin.voiceDistance();

        line(me, "=== Simple Proximity Chat debug ===", ChatFormatting.GOLD);
        line(me, String.format("Distancia de voz: %.0f (limpio hasta %.0f)", range, range * 0.75), ChatFormatting.GRAY);
        line(me, "Simple Radio: " + RadioCompat.isLoaded()
                + " | Curios: " + SimpleProximityChat.isLoaded("curios")
                + " | VibrativeVoice: " + SimpleProximityChat.isLoaded("vibrativevoice")
                + " | Beautified Chat: " + SimpleProximityChat.isLoaded("beautifiedchatserver"), ChatFormatting.GRAY);

        if (RadioCompat.isLoaded()) {
            line(me, "Radio entre dimensiones (crossDimensional de Simple Radio): "
                    + (RadioNodes.crossDimensional() ? "activada" : "desactivada"), ChatFormatting.GRAY);
            for (ServerPlayer p : players) {
                List<Walkie> ws = Walkie.activeOf(p);
                StringBuilder sb = new StringBuilder(p.getGameProfile().getName()).append(": ");
                if (ws.isEmpty()) {
                    sb.append("sin radio activa");
                } else {
                    for (Walkie w : ws) {
                        sb.append(String.format("[%s %s %s, tx %d rx %d umbral %d, escucha %d, altavoz %d] ",
                                w.type(), w.frequency(), w.am() ? "AM" : "FM",
                                w.transmissionPower(), w.receptionPower(), w.diminishThreshold(),
                                w.listeningRange(), w.speakingRange()));
                    }
                }
                line(me, sb.toString(), ChatFormatting.YELLOW);
            }
        }

        // Simula un mensaje tuyo
        ChatPlan plan = ChatRouter.plan(me, players);
        line(me, "Si escribes ahora:", ChatFormatting.GOLD);
        for (ServerPlayer p : players) {
            if (p == me) continue;
            Delivery d = plan.deliveries().get(p.getUUID());
            String how;
            if (d == null) how = "no le llega";
            else if (d.signed()) how = "le llega normal (cerca)";
            else how = String.format("%s, distorsión %.0f%%", d.radio() ? "por RADIO" : "local", d.intensity() * 100);
            String dist = p.level() == me.level() ? String.format("%.0f bloques", p.distanceTo(me)) : "otra dimensión";
            line(me, " - " + p.getGameProfile().getName() + " (" + dist + "): " + how, ChatFormatting.WHITE);
        }
        line(me, "Salidas de radio alcanzadas: " + plan.radioOutputs().size(), ChatFormatting.GRAY);
    }

    private static void line(ServerPlayer to, String text, ChatFormatting color) {
        to.sendSystemMessage(Component.literal(text).withStyle(color));
    }
}
