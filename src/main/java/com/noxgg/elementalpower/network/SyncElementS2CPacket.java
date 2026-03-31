package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncElementS2CPacket {
    private final String elementId;
    private final int level;
    private final int xp;
    private final int souls;

    public SyncElementS2CPacket(String elementId, int level, int xp, int souls) {
        this.elementId = elementId;
        this.level = level;
        this.xp = xp;
        this.souls = souls;
    }

    // Keep backwards-compatible constructor
    public SyncElementS2CPacket(String elementId) {
        this(elementId, 1, 0, 0);
    }

    public SyncElementS2CPacket(FriendlyByteBuf buf) {
        this.elementId = buf.readUtf();
        this.level = buf.readInt();
        this.xp = buf.readInt();
        this.souls = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(elementId);
        buf.writeInt(level);
        buf.writeInt(xp);
        buf.writeInt(souls);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerElementProvider.PLAYER_ELEMENT)
                        .ifPresent(data -> {
                            data.setElement(ElementType.fromId(elementId));
                            // Restore level data without resetting
                            data.addXp(0); // just init
                        });
            }
        });
        return true;
    }
}
