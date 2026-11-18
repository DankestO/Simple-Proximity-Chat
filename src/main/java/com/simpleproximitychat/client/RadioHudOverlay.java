package com.simpleproximitychat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simpleproximitychat.SimpleProximityChat;
import com.simpleproximitychat.SpcVoicechatPlugin;
import com.simpleproximitychat.radio.HudState;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.config.ConfigAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Dibuja el icono de radio justo encima del icono de micrófono de Simple Voice Chat,
 * usando su misma posición y escala (hud_icon_pos_x / hud_icon_pos_y / hud_icon_scale).
 */
public final class RadioHudOverlay {

    private static final ResourceLocation RADIO = tex("radio");
    private static final ResourceLocation MICROPHONE = tex("microphone");
    private static final ResourceLocation SPEAKER = tex("speaker");

    /** Separación entre nuestro icono y el de Voice Chat, en píxeles de icono. */
    private static final int STEP = 18;

    private RadioHudOverlay() {}

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(SimpleProximityChat.MODID, "textures/hud/" + name + ".png");
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        HudState state = ClientRadioState.get();
        if (state == HudState.NONE) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        // Mismos valores por defecto que Simple Voice Chat
        int posX = 16;
        int posY = -16;
        double scale = 1.0D;
        VoicechatClientApi api = SpcVoicechatPlugin.clientApi();
        if (api != null) {
            ConfigAccessor config = api.getClientConfig();
            if (config.getBoolean("hide_icons", false)) return;
            posX = config.getInt("hud_icon_pos_x", posX);
            posY = config.getInt("hud_icon_pos_y", posY);
            scale = config.getDouble("hud_icon_scale", scale);
        }

        ResourceLocation texture = switch (state) {
            case RADIO -> RADIO;
            case MICROPHONE -> MICROPHONE;
            case SPEAKER -> SPEAKER;
            default -> null;
        };
        if (texture == null) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        // Igual que Voice Chat: valores negativos = anclado al borde derecho/inferior
        if (posX < 0) pose.translate(width, 0, 0);
        if (posY < 0) pose.translate(0, height, 0);
        pose.translate(posX, posY, 0);
        pose.scale((float) scale, (float) scale, 1F);

        int x = posX < 0 ? -16 : 0;
        int y = posY < 0 ? -16 : 0;
        // Encima del micrófono; si está pegado arriba del todo, debajo para no salirse.
        boolean fitsAbove = posY < 0 || posY >= STEP * scale;
        y += fitsAbove ? -STEP : STEP;

        RenderSystem.enableBlend();
        graphics.blit(texture, x, y, 0F, 0F, 16, 16, 16, 16);
        RenderSystem.disableBlend();
        pose.popPose();
    }
}
