package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.world.ThroneManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ThroneActionC2SPacket {
    private final int entityId;
    private final boolean imprison; // true = imprison, false = release

    public ThroneActionC2SPacket(int entityId, boolean imprison) {
        this.entityId = entityId;
        this.imprison = imprison;
    }

    public ThroneActionC2SPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.imprison = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(imprison);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof LivingEntity living)) return;

            // Check distance
            if (player.distanceTo(entity) > 10) return;

            if (imprison) {
                ThroneManager.imprisonEntity(level, living);

                player.sendSystemMessage(Component.literal(">> Le prisonnier a ete emprisonne dans la prison de bedrock!")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

                level.playSound(null, player.blockPosition(),
                        SoundEvents.IRON_DOOR_CLOSE, SoundSource.PLAYERS, 2.0f, 0.5f);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.7f);
            } else {
                ThroneManager.releaseEntity(living);

                player.sendSystemMessage(Component.literal(">> Le prisonnier a ete libere.")
                        .withStyle(ChatFormatting.GREEN));

                level.playSound(null, player.blockPosition(),
                        SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1.5f, 1.0f);
            }
        });
        return true;
    }
}
