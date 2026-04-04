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
    private final String subClass;
    private final boolean isAlastor;

    public SyncElementS2CPacket(String elementId, int level, int xp, int souls) {
        this(elementId, level, xp, souls, "", false);
    }

    public SyncElementS2CPacket(String elementId, int level, int xp, int souls, String subClass) {
        this(elementId, level, xp, souls, subClass, false);
    }

    public SyncElementS2CPacket(String elementId, int level, int xp, int souls, String subClass, boolean isAlastor) {
        this.elementId = elementId;
        this.level = level;
        this.xp = xp;
        this.souls = souls;
        this.subClass = subClass;
        this.isAlastor = isAlastor;
    }

    // Keep backwards-compatible constructor
    public SyncElementS2CPacket(String elementId) {
        this(elementId, 1, 0, 0, "", false);
    }

    public SyncElementS2CPacket(FriendlyByteBuf buf) {
        this.elementId = buf.readUtf();
        this.level = buf.readInt();
        this.xp = buf.readInt();
        this.souls = buf.readInt();
        this.subClass = buf.readUtf();
        this.isAlastor = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(elementId);
        buf.writeInt(level);
        buf.writeInt(xp);
        buf.writeInt(souls);
        buf.writeUtf(subClass);
        buf.writeBoolean(isAlastor);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerElementProvider.PLAYER_ELEMENT)
                        .ifPresent(data -> {
                            data.setElement(ElementType.fromId(elementId));
                            data.setSubClass(subClass);
                            data.setAlastor(isAlastor);
                            data.addXp(0); // just init
                        });
            }
        });
        return true;
    }
}
