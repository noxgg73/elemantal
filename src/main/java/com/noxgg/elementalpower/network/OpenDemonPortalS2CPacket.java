package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.screen.DemonPortalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDemonPortalS2CPacket {
    public OpenDemonPortalS2CPacket() {}
    public OpenDemonPortalS2CPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new DemonPortalScreen());
        });
        return true;
    }
}
