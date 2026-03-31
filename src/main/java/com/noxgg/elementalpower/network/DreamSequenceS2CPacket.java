package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.screen.DreamOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DreamSequenceS2CPacket {
    private final int phase;
    private final String message;

    public DreamSequenceS2CPacket(int phase, String message) {
        this.phase = phase;
        this.message = message;
    }

    public DreamSequenceS2CPacket(FriendlyByteBuf buf) {
        this.phase = buf.readInt();
        this.message = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(phase);
        buf.writeUtf(message);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DreamOverlay.setPhase(phase, message);
        });
        return true;
    }
}
