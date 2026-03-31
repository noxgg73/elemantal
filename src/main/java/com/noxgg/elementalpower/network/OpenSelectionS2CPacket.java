package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.screen.ElementSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenSelectionS2CPacket {
    public OpenSelectionS2CPacket() {}

    public OpenSelectionS2CPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new ElementSelectionScreen());
        });
        return true;
    }
}
