package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.screen.UndertaleBattleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenUndertaleBattleS2CPacket {
    private final int entityId;
    private final String mobName;
    private final float mobHealth;
    private final float mobMaxHealth;
    private final boolean isFrisk;
    private final boolean isPlayerBattle;

    public OpenUndertaleBattleS2CPacket(int entityId, String mobName, float mobHealth, float mobMaxHealth, boolean isFrisk, boolean isPlayerBattle) {
        this.entityId = entityId;
        this.mobName = mobName;
        this.mobHealth = mobHealth;
        this.mobMaxHealth = mobMaxHealth;
        this.isFrisk = isFrisk;
        this.isPlayerBattle = isPlayerBattle;
    }

    public OpenUndertaleBattleS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.mobName = buf.readUtf();
        this.mobHealth = buf.readFloat();
        this.mobMaxHealth = buf.readFloat();
        this.isFrisk = buf.readBoolean();
        this.isPlayerBattle = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(mobName);
        buf.writeFloat(mobHealth);
        buf.writeFloat(mobMaxHealth);
        buf.writeBoolean(isFrisk);
        buf.writeBoolean(isPlayerBattle);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new UndertaleBattleScreen(entityId, mobName, mobHealth, mobMaxHealth, isFrisk, isPlayerBattle));
        });
        return true;
    }
}
