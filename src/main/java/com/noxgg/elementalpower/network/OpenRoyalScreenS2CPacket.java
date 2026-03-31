package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.screen.RoyalJudgmentScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenRoyalScreenS2CPacket {
    private final List<Integer> entityIds;

    public OpenRoyalScreenS2CPacket(List<Integer> entityIds) {
        this.entityIds = entityIds;
    }

    public OpenRoyalScreenS2CPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        entityIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            entityIds.add(buf.readInt());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityIds.size());
        for (int id : entityIds) {
            buf.writeInt(id);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new RoyalJudgmentScreen(entityIds));
        });
        return true;
    }
}
