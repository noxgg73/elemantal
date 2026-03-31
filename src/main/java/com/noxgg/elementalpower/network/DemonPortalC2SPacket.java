package com.noxgg.elementalpower.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class DemonPortalC2SPacket {
    private final boolean goToEnd; // false = nether, true = end

    private static final DustParticleOptions DEMON_RED = new DustParticleOptions(
            new Vector3f(0.7f, 0.05f, 0.0f), 2.0f);

    public DemonPortalC2SPacket(boolean goToEnd) {
        this.goToEnd = goToEnd;
    }

    public DemonPortalC2SPacket(FriendlyByteBuf buf) {
        this.goToEnd = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(goToEnd);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ServerLevel currentLevel = player.serverLevel();

            // Departure particles
            currentLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.2);
            currentLevel.sendParticles(DEMON_RED,
                    player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 1, 0.5, 0.1);
            currentLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1, player.getZ(), 30, 0.3, 0.8, 0.3, 0.05);

            currentLevel.playSound(null, player.blockPosition(),
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0f, 0.5f);

            ResourceKey<Level> targetDim = goToEnd ? Level.END : Level.NETHER;
            ServerLevel targetLevel = player.server.getLevel(targetDim);

            if (targetLevel == null) {
                player.sendSystemMessage(Component.literal("Dimension inaccessible!")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Calculate target position
            double targetX, targetZ;
            if (goToEnd) {
                // End: spawn at the obsidian platform
                targetX = 100;
                targetZ = 0;
            } else {
                // Nether: scale coordinates /8
                targetX = player.getX() / 8.0;
                targetZ = player.getZ() / 8.0;
            }

            // Build safe arrival platform
            int targetY;
            if (goToEnd) {
                targetY = 50;
                // Build obsidian platform in End
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        targetLevel.setBlock(new BlockPos((int)targetX + dx, targetY - 1, (int)targetZ + dz),
                                Blocks.OBSIDIAN.defaultBlockState(), 3);
                        targetLevel.setBlock(new BlockPos((int)targetX + dx, targetY, (int)targetZ + dz),
                                Blocks.AIR.defaultBlockState(), 3);
                        targetLevel.setBlock(new BlockPos((int)targetX + dx, targetY + 1, (int)targetZ + dz),
                                Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            } else {
                // Nether: find safe spot
                targetY = 64;
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        targetLevel.setBlock(new BlockPos((int)targetX + dx, targetY - 1, (int)targetZ + dz),
                                Blocks.NETHERRACK.defaultBlockState(), 3);
                        targetLevel.setBlock(new BlockPos((int)targetX + dx, targetY, (int)targetZ + dz),
                                Blocks.AIR.defaultBlockState(), 3);
                        targetLevel.setBlock(new BlockPos((int)targetX + dx, targetY + 1, (int)targetZ + dz),
                                Blocks.AIR.defaultBlockState(), 3);
                        targetLevel.setBlock(new BlockPos((int)targetX + dx, targetY + 2, (int)targetZ + dz),
                                Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }

            // Teleport player to target dimension
            player.teleportTo(targetLevel, targetX + 0.5, targetY, targetZ + 0.5,
                    player.getYRot(), player.getXRot());

            // Arrival particles
            targetLevel.sendParticles(ParticleTypes.PORTAL,
                    targetX + 0.5, targetY + 1, targetZ + 0.5, 50, 0.5, 1, 0.5, 0.5);
            targetLevel.sendParticles(DEMON_RED,
                    targetX + 0.5, targetY + 1, targetZ + 0.5, 30, 0.5, 1, 0.5, 0.1);
            targetLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    targetX + 0.5, targetY + 1, targetZ + 0.5, 20, 0.3, 0.8, 0.3, 0.05);

            targetLevel.playSound(null, BlockPos.containing(targetX, targetY, targetZ),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0f, 0.5f);

            String destName = goToEnd ? "The End" : "le Monde des Demons (Nether)";
            player.sendSystemMessage(Component.literal(">> Teleporte vers " + destName + "!")
                    .withStyle(goToEnd ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        });
        return true;
    }
}
