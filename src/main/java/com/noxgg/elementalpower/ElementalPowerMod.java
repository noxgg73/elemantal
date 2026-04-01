package com.noxgg.elementalpower;

import com.mojang.logging.LogUtils;
import com.noxgg.elementalpower.block.ModBlocks;
import com.noxgg.elementalpower.item.ModItems;
import com.noxgg.elementalpower.item.ModCreativeTabs;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.sound.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ElementalPowerMod.MOD_ID)
public class ElementalPowerMod {
    public static final String MOD_ID = "elementalpower";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ElementalPowerMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModSounds.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModMessages::register);
        LOGGER.info("Elemental Power Mod initialized!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Elemental Power - Server starting!");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Elemental Power - Client setup!");
        }
    }
}
