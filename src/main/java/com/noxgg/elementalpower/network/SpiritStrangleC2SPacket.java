package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.world.SpiritManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpiritStrangleC2SPacket {
    public SpiritStrangleC2SPacket() {}
    public SpiritStrangleC2SPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!SpiritManager.isSpirit(player.getUUID())) return;

            ServerLevel level = player.serverLevel();
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();

            // Find closest entity player is looking at (within 4 blocks)
            LivingEntity target = null;
            double closestDist = 4.0;

            for (Entity entity : level.getEntities(player,
                    player.getBoundingBox().inflate(4),
                    e -> e instanceof LivingEntity && e != player)) {

                LivingEntity living = (LivingEntity) entity;
                Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(eye);
                double dist = toEntity.length();
                if (dist > closestDist) continue;

                Vec3 toEntityNorm = toEntity.normalize();
                double dot = look.dot(toEntityNorm);
                if (dot > 0.9) {
                    closestDist = dist;
                    target = living;
                }
            }

            if (target != null) {
                SpiritManager.startStrangle(player, target);
            }
        });
        return true;
    }
}
