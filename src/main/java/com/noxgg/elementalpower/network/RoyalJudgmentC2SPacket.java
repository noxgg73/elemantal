package com.noxgg.elementalpower.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class RoyalJudgmentC2SPacket {
    private final int entityId;
    private final boolean imprison;

    private static final DustParticleOptions GOLD = new DustParticleOptions(
            new Vector3f(1.0f, 0.84f, 0.0f), 2.0f);

    public RoyalJudgmentC2SPacket(int entityId, boolean imprison) {
        this.entityId = entityId;
        this.imprison = imprison;
    }

    public RoyalJudgmentC2SPacket(FriendlyByteBuf buf) {
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
            if (player.distanceTo(entity) > 15) return;

            if (imprison) {
                // === PORTAL EFFECT: entity gets sucked in ===
                double ex = living.getX();
                double ey = living.getY();
                double ez = living.getZ();

                // Swirling portal particles around entity
                for (int i = 0; i < 60; i++) {
                    double angle = (Math.PI * 2 / 60) * i;
                    double r = 1.5 - (i * 0.02);
                    double px = ex + Math.cos(angle) * r;
                    double pz = ez + Math.sin(angle) * r;
                    double py = ey + (i * 0.05);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 2, 0.05, 0.05, 0.05, 0.02);
                    level.sendParticles(GOLD, px, py, pz, 1, 0.03, 0.03, 0.03, 0.01);
                }
                // Portal vortex
                level.sendParticles(ParticleTypes.PORTAL, ex, ey + 1, ez, 80, 0.3, 0.5, 0.3, 0.8);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, ex, ey + 1, ez, 50, 0.5, 1, 0.5, 0.2);
                level.sendParticles(GOLD, ex, ey + 1, ez, 30, 0.5, 1, 0.5, 0.1);

                // === BUILD BEDROCK PRISON underground ===
                int prisonX = (int) ex;
                int prisonY = -50;
                int prisonZ = (int) ez;

                for (int x = -2; x <= 2; x++) {
                    for (int y = -1; y <= 3; y++) {
                        for (int z = -2; z <= 2; z++) {
                            BlockPos pos = new BlockPos(prisonX + x, prisonY + y, prisonZ + z);
                            if (x == -2 || x == 2 || y == -1 || y == 3 || z == -2 || z == 2) {
                                level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
                            } else {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
                // Light inside
                level.setBlock(new BlockPos(prisonX, prisonY + 2, prisonZ), Blocks.GLOWSTONE.defaultBlockState(), 3);

                // Teleport to prison
                living.teleportTo(prisonX + 0.5, prisonY, prisonZ + 0.5);
                living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                living.removeEffect(MobEffects.WEAKNESS);

                // Sounds
                level.playSound(null, player.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.PLAYERS, 2.0f, 0.5f);
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.7f);
                level.playSound(null, new BlockPos((int)ex, (int)ey, (int)ez), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 0.5f);

                player.sendSystemMessage(Component.literal(">> Le prisonnier a ete enferme dans la prison de bedrock!")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            } else {
                // === RELEASE ===
                living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                living.removeEffect(MobEffects.WEAKNESS);
                living.removeEffect(MobEffects.GLOWING);

                // Freedom particles
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        living.getX(), living.getY() + 1, living.getZ(), 15, 0.5, 0.5, 0.5, 0.1);

                level.playSound(null, player.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1.5f, 1.0f);

                player.sendSystemMessage(Component.literal(">> Le prisonnier a ete libere.")
                        .withStyle(ChatFormatting.GREEN));
            }
        });
        return true;
    }
}
