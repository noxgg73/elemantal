package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncElementS2CPacket {
    private final String elementId;

    public SyncElementS2CPacket(String elementId) {
        this.elementId = elementId;
    }

    public SyncElementS2CPacket(FriendlyByteBuf buf) {
        this.elementId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(elementId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerElementProvider.PLAYER_ELEMENT)
                        .ifPresent(data -> data.setElement(ElementType.fromId(elementId)));
            }
        });
        return true;
    }
}
