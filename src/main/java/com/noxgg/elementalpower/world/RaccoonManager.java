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
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Raccoon, the Siamese Cat God, in castle throne rooms.
 * When a player enters the throne room, Raccoon speaks to them,
 * then moves to lie down on her cushion.
 */
public class RaccoonManager {

    public static class ThroneRoomData {
        public final BlockPos thronePos;
        public final BlockPos cushionPos;

        public ThroneRoomData(BlockPos thronePos, BlockPos cushionPos) {
            this.thronePos = thronePos;
            this.cushionPos = cushionPos;
        }
    }

    // Pending Raccoon moves: tick when she should move -> data
    private static final Map<Long, PendingMove> pendingMoves = new ConcurrentHashMap<>();
    private static long currentTick = 0;

    private static class PendingMove {
        final ResourceKey<Level> dimension;
        final BlockPos thronePos;
        final BlockPos cushionPos;

        PendingMove(ResourceKey<Level> dimension, BlockPos thronePos, BlockPos cushionPos) {
            this.dimension = dimension;
            this.thronePos = thronePos;
            this.cushionPos = cushionPos;
        }
    }

    // Throne room positions (dimension -> list of throne room data)
    private static final Map<ResourceKey<Level>, List<ThroneRoomData>> throneRooms = new ConcurrentHashMap<>();

    // Players who already received Raccoon's message (don't spam)
    private static final Set<UUID> playersGreeted = ConcurrentHashMap.newKeySet();

    // Detection radius around the throne
    private static final double DETECTION_RADIUS = 10.0;

    public static void registerThroneRoom(ResourceKey<Level> dimension, BlockPos thronePos, BlockPos cushionPos) {
        throneRooms.computeIfAbsent(dimension, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new ThroneRoomData(thronePos, cushionPos));
    }

    /**
     * Called every server tick to check if players are near a throne room.
     */
    public static void tick() {
        currentTick++;

        // Process pending Raccoon moves
        var moveIt = pendingMoves.entrySet().iterator();
        while (moveIt.hasNext()) {
            var entry = moveIt.next();
            if (currentTick >= entry.getKey()) {
                PendingMove move = entry.getValue();
                moveRaccoonToCushion(move);
                moveIt.remove();
            }
        }

        for (var entry : throneRooms.entrySet()) {
            ResourceKey<Level> dim = entry.getKey();
            List<ThroneRoomData> rooms = entry.getValue();

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) continue;

            ServerLevel level = server.getLevel(dim);
            if (level == null) continue;

            for (ThroneRoomData room : rooms) {
                for (ServerPlayer player : level.players()) {
                    double dist = player.position().distanceTo(
                            new net.minecraft.world.phys.Vec3(room.thronePos.getX() + 0.5, room.thronePos.getY(), room.thronePos.getZ() + 0.5));

                    if (dist <= DETECTION_RADIUS && !playersGreeted.contains(player.getUUID())) {
                        playersGreeted.add(player.getUUID());
                        greetPlayer(player, level, room);
                    }

                    // Reset greeting when player leaves far enough
                    if (dist > DETECTION_RADIUS * 3 && playersGreeted.contains(player.getUUID())) {
                        playersGreeted.remove(player.getUUID());
                    }
                }
            }
        }
    }

    private static void greetPlayer(ServerPlayer player, ServerLevel level, ThroneRoomData room) {
        DustParticleOptions goldDust = new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.0f), 2.5f);

        // Golden particles around the throne
        for (int i = 0; i < 30; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 3;
            double oy = level.random.nextDouble() * 2;
            double oz = (level.random.nextDouble() - 0.5) * 3;
            level.sendParticles(goldDust,
                    room.thronePos.getX() + 0.5 + ox, room.thronePos.getY() + oy, room.thronePos.getZ() + 0.5 + oz,
                    1, 0, 0, 0, 0);
        }

        // Cat purring particles
        level.sendParticles(ParticleTypes.HEART,
                room.thronePos.getX() + 0.5, room.thronePos.getY() + 1.5, room.thronePos.getZ() + 0.5,
                5, 0.3, 0.3, 0.3, 0.01);

        // Magical sound
        level.playSound(null, room.thronePos, SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.5f, 0.8f);
        level.playSound(null, room.thronePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.2f);

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

        // Schedule Raccoon to move to cushion after 3 seconds (60 ticks)
        pendingMoves.put(currentTick + 60, new PendingMove(level.dimension(), room.thronePos, room.cushionPos));
    }

    /**
     * Move Raccoon from the throne to her cushion and make her lie down.
     */
    private static void moveRaccoonToCushion(PendingMove move) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerLevel level = server.getLevel(move.dimension);
        if (level == null) return;

        // Find Raccoon near the throne
        List<Cat> cats = level.getEntitiesOfClass(Cat.class,
                new AABB(move.thronePos).inflate(5),
                cat -> cat.getTags().contains("raccoon_cat_god"));

        for (Cat raccoon : cats) {
            // Teleport to cushion
            double cushionX = move.cushionPos.getX() + 0.5;
            double cushionY = move.cushionPos.getY() + 1.1; // On top of the carpet
            double cushionZ = move.cushionPos.getZ() + 0.5;
            raccoon.moveTo(cushionX, cushionY, cushionZ, 90, 0);

            // Lie down
            raccoon.setOrderedToSit(false);
            raccoon.setLying(true);

            // Particles at cushion
            level.sendParticles(ParticleTypes.HEART,
                    cushionX, cushionY + 0.5, cushionZ,
                    3, 0.2, 0.2, 0.2, 0.01);
            DustParticleOptions goldDust = new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.0f), 1.5f);
            level.sendParticles(goldDust,
                    cushionX, cushionY + 0.3, cushionZ,
                    10, 0.3, 0.1, 0.3, 0.01);

            // Purring sound
            level.playSound(null, move.cushionPos, SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.0f, 1.2f);
        }
    }

    /**
     * Called when a player logs out - clean up their greeting state.
     */
    public static void onPlayerLogout(UUID playerId) {
        playersGreeted.remove(playerId);
    }
}
