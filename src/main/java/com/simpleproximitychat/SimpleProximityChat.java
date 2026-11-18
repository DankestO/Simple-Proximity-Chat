package com.simpleproximitychat;

import com.mojang.logging.LogUtils;
import com.simpleproximitychat.network.Network;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

@Mod(SimpleProximityChat.MODID)
public class SimpleProximityChat {

    public static final String MODID = "simpleproximitychat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimpleProximityChat() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
        LOGGER.info("Simple Proximity Chat cargado (Simple Radio: {}, Curios: {})",
                isLoaded("simpleradio"), isLoaded("curios"));
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
