package com.noxgg.elementalpower.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Raccoon, the Siamese Cat God, in castle throne rooms.
 * When a player enters the throne room, Raccoon speaks to them.
 */
public class RaccoonManager {

    // Throne room positions (dimension -> list of throne positions)
    private static final Map<ResourceKey<Level>, List<BlockPos>> throneRooms = new ConcurrentHashMap<>();

    // Players who already received Raccoon's message (don't spam)
    private static final Set<UUID> playersGreeted = ConcurrentHashMap.newKeySet();

    // Detection radius around the throne
    private static final double DETECTION_RADIUS = 10.0;

    public static void registerThroneRoom(ResourceKey<Level> dimension, BlockPos thronePos) {
        throneRooms.computeIfAbsent(dimension, k -> Collections.synchronizedList(new ArrayList<>())).add(thronePos);
    }

    /**
     * Called every server tick to check if players are near a throne room.
     */
    public static void tick() {
        for (var entry : throneRooms.entrySet()) {
            ResourceKey<Level> dim = entry.getKey();
            List<BlockPos> thrones = entry.getValue();

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) continue;

            ServerLevel level = server.getLevel(dim);
            if (level == null) continue;

            for (BlockPos thronePos : thrones) {
                // Check all players
                for (ServerPlayer player : level.players()) {
                    double dist = player.position().distanceTo(
                            new net.minecraft.world.phys.Vec3(thronePos.getX() + 0.5, thronePos.getY(), thronePos.getZ() + 0.5));

                    if (dist <= DETECTION_RADIUS && !playersGreeted.contains(player.getUUID())) {
                        // Player entered the throne room!
                        playersGreeted.add(player.getUUID());
                        greetPlayer(player, level, thronePos);
                    }

                    // Reset greeting when player leaves far enough
                    if (dist > DETECTION_RADIUS * 3 && playersGreeted.contains(player.getUUID())) {
                        playersGreeted.remove(player.getUUID());
                    }
                }
            }
        }
    }

    private static void greetPlayer(ServerPlayer player, ServerLevel level, BlockPos thronePos) {
        DustParticleOptions goldDust = new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.0f), 2.5f);

        // Golden particles around the throne
        for (int i = 0; i < 30; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 3;
            double oy = level.random.nextDouble() * 2;
            double oz = (level.random.nextDouble() - 0.5) * 3;
            level.sendParticles(goldDust,
                    thronePos.getX() + 0.5 + ox, thronePos.getY() + oy, thronePos.getZ() + 0.5 + oz,
                    1, 0, 0, 0, 0);
        }

        // Cat purring particles
        level.sendParticles(ParticleTypes.HEART,
                thronePos.getX() + 0.5, thronePos.getY() + 1.5, thronePos.getZ() + 0.5,
                5, 0.3, 0.3, 0.3, 0.01);

        // Magical sound
        level.playSound(null, thronePos, SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.5f, 0.8f);
        level.playSound(null, thronePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.2f);

        // Raccoon speaks!
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("   Raccoon, Dieu des Chats").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" *ronronne*").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("   << ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("Prend soin de mon chateau et embete Mandibul!").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" >>").withStyle(ChatFormatting.GOLD)));
        player.sendSystemMessage(Component.literal(""));
    }

    /**
     * Called when a player logs out - clean up their greeting state.
     */
    public static void onPlayerLogout(UUID playerId) {
        playersGreeted.remove(playerId);
    }
}
