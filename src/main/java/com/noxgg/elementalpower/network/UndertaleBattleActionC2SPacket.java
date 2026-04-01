package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.world.UndertaleBattleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UndertaleBattleActionC2SPacket {
    private final int entityId;
    private final String action; // "attack", "spare", "flee"

    public UndertaleBattleActionC2SPacket(int entityId, String action) {
        this.entityId = entityId;
        this.action = action;
    }

    public UndertaleBattleActionC2SPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.action = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(action);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                if (data.getElement() != ElementType.UNDERTALE) return;
                UndertaleBattleManager.handleAction(player, entityId, action, data);
            });
        });
        return true;
    }
}
