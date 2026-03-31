package com.noxgg.elementalpower.world;

import com.noxgg.elementalpower.entity.SeatEntity;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.OpenThroneScreenS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

public class ThroneManager {
    // Track paralyzed entities per player
    private static final Map<UUID, List<Integer>> paralyzedEntities = new HashMap<>();

    private static final DustParticleOptions GOLD = new DustParticleOptions(
            new Vector3f(1.0f, 0.84f, 0.0f), 1.5f);

    public static void tick(ServerLevel level) {
        List<UUID> toRemove = new ArrayList<>();

        for (var entry : paralyzedEntities.entrySet()) {
            UUID playerId = entry.getKey();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player == null || !player.isPassenger() ||
                    !(player.getVehicle() instanceof SeatEntity)) {
                // Player not on throne anymore, free all entities
                for (int eid : entry.getValue()) {
                    Entity e = level.getEntity(eid);
                    if (e instanceof LivingEntity living) {
                        living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        living.removeEffect(MobEffects.WEAKNESS);
                    }
                }
                toRemove.add(playerId);
                continue;
            }

            // Player is on throne - paralyze entities in front (4 blocks)
            Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
            Vec3 playerPos = player.position();
            List<Integer> currentParalyzed = new ArrayList<>();

            for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(5),
                    ent -> ent instanceof LivingEntity && ent != player && !(ent instanceof SeatEntity))) {

                Vec3 toEntity = e.position().subtract(playerPos);
                Vec3 flatTo = new Vec3(toEntity.x, 0, toEntity.z);
                double dist = flatTo.length();
                if (dist > 4.0 || dist < 0.3) continue;

                Vec3 flatFacing = new Vec3(facing.x, 0, facing.z).normalize();
                double dot = flatFacing.dot(flatTo.normalize());
                if (dot < 0.5) continue;

                LivingEntity living = (LivingEntity) e;
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 127, false, false));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 127, false, false));
                living.setDeltaMovement(0, living.getDeltaMovement().y, 0);
                living.hurtMarked = true;
                currentParalyzed.add(e.getId());

                // Golden chains particles
                if (player.tickCount % 10 == 0) {
                    level.sendParticles(GOLD,
                            living.getX(), living.getY() + 1, living.getZ(),
                            5, 0.2, 0.3, 0.2, 0.01);
                    level.sendParticles(ParticleTypes.END_ROD,
                            living.getX(), living.getY() + 0.5, living.getZ(),
                            2, 0.1, 0.2, 0.1, 0.005);
                }
            }

            entry.setValue(currentParalyzed);

            // Send screen to player if there are paralyzed mobs and not already showing
            if (!currentParalyzed.isEmpty() && player.tickCount % 40 == 0) {
                ModMessages.sendToPlayer(new OpenThroneScreenS2CPacket(currentParalyzed), player);
            }
        }

        toRemove.forEach(paralyzedEntities::remove);
    }

    public static void addPlayer(UUID playerId) {
        paralyzedEntities.put(playerId, new ArrayList<>());
    }

    public static void removePlayer(UUID playerId) {
        paralyzedEntities.remove(playerId);
    }

    public static List<Integer> getParalyzedEntities(UUID playerId) {
        return paralyzedEntities.getOrDefault(playerId, List.of());
    }

    // Create bedrock prison and teleport entity there
    public static void imprisonEntity(ServerLevel level, LivingEntity entity) {
        // Prison location: deep underground at the entity's X/Z
        int prisonX = (int) entity.getX();
        int prisonY = -50;
        int prisonZ = (int) entity.getZ();

        // Build 5x5x5 bedrock box
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos pos = new BlockPos(prisonX + x, prisonY + y, prisonZ + z);
                    // Walls, floor, ceiling = bedrock, interior = air
                    if (x == -2 || x == 2 || y == -1 || y == 3 || z == -2 || z == 2) {
                        level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Add glowstone for light inside
        level.setBlock(new BlockPos(prisonX, prisonY + 2, prisonZ), Blocks.GLOWSTONE.defaultBlockState(), 3);

        // Portal effect at current position
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                entity.getX(), entity.getY() + 1, entity.getZ(), 50, 0.3, 0.5, 0.3, 0.1);
        level.sendParticles(GOLD,
                entity.getX(), entity.getY() + 1, entity.getZ(), 30, 0.5, 1, 0.5, 0.05);
        level.sendParticles(ParticleTypes.PORTAL,
                entity.getX(), entity.getY() + 1, entity.getZ(), 40, 0.3, 0.5, 0.3, 0.5);

        // Teleport to prison
        entity.teleportTo(prisonX + 0.5, prisonY, prisonZ + 0.5);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.WEAKNESS);
    }

    public static void releaseEntity(LivingEntity entity) {
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.WEAKNESS);
    }
}
