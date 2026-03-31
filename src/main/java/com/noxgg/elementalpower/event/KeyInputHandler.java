package com.noxgg.elementalpower.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.UseElementPowerC2SPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class KeyInputHandler {
    public static final String KEY_CATEGORY = "key.category.elementalpower";
    public static final String KEY_USE_POWER = "key.elementalpower.use_power";

    public static final KeyMapping USE_POWER_KEY = new KeyMapping(
            KEY_USE_POWER,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );

    @Mod.EventBusSubscriber(modid = ElementalPowerMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(USE_POWER_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = ElementalPowerMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (USE_POWER_KEY.consumeClick()) {
                if (Minecraft.getInstance().player != null) {
                    ModMessages.sendToServer(new UseElementPowerC2SPacket());
                }
            }
        }
    }
}
