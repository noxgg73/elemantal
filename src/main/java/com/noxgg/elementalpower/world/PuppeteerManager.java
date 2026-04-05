package com.noxgg.elementalpower.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puppeteer power for Fire class (obtained by killing Pure Vanilla).
 * Press R to control mobs in a radius like a puppeteer with strings.
 * Controlled mobs follow the player's look direction and attack targets.
 */
public class PuppeteerManager {

    private static final Map<UUID, PuppeteerSession> activeSessions = new ConcurrentHashMap<>();

    public static class PuppeteerSession {
        public final ServerPlayer player;
        public final ServerLevel level;
        public final List<LivingEntity> controlledEntities = new ArrayList<>();
        public int ticksActive = 0; // Infinite duration - just counts up

        public PuppeteerSession(ServerPlayer player, ServerLevel level) {
            this.player = player;
            this.level = level;
        }
    }

    /**
     * Activate puppeteer mode for a Fire class player.
     */
    public static void activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        // Cancel existing session
        if (activeSessions.containsKey(player.getUUID())) {
            deactivate(player.getUUID());
            player.sendSystemMessage(Component.literal("")
                    .append(Component.literal(">> ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("Mode Marionnettiste desactive!").withStyle(ChatFormatting.YELLOW)));
            return;
        }

        double controlRadius = 15.0;

        // Find all mobs AND players in radius (including creative mode players)
        var entities = level.getEntities(player,
                player.getBoundingBox().inflate(controlRadius),
                e -> e instanceof LivingEntity && e != player);

        List<LivingEntity> controlledEntities = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof LivingEntity living) {
                if (living.getTags().contains("shadow_milk_boss")) continue; // Can't control Shadow Milk
                controlledEntities.add(living);
            }
        }

        if (controlledEntities.isEmpty()) {
            player.sendSystemMessage(Component.literal("")
                    .append(Component.literal(">> ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("Aucune entite a controler dans la zone!").withStyle(ChatFormatting.GRAY)));
            return;
        }

        PuppeteerSession session = new PuppeteerSession(player, level);
        session.controlledEntities.addAll(controlledEntities);
        activeSessions.put(player.getUUID(), session);

        // Count mobs and players separately for message
        int mobCount = 0;
        int playerCount = 0;

        // Visual: strings connecting player to each entity
        DustParticleOptions stringDust = new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 1.0f);
        for (LivingEntity entity : controlledEntities) {
            // String from player hand to entity
            Vec3 start = player.position().add(0, 2.5, 0);
            Vec3 end = entity.position().add(0, entity.getBbHeight(), 0);
            for (double t = 0; t < 1.0; t += 0.05) {
                double x = start.x + (end.x - start.x) * t;
                double y = start.y + (end.y - start.y) * t + Math.sin(t * Math.PI) * 1.5; // Arc
                double z = start.z + (end.z - start.z) * t;
                level.sendParticles(stringDust, x, y, z, 1, 0, 0, 0, 0);
            }

            // Glowing effect on controlled entity (infinite duration)
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));

            if (entity instanceof Mob mob) {
                mob.setTarget(null);
                mobCount++;
            } else if (entity instanceof Player) {
                playerCount++;
            }
        }

        // Player buffs during puppeteer mode (infinite)
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));

        // Sounds
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.5f, 1.5f);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.5f);

        String controlMsg = "";
        if (mobCount > 0 && playerCount > 0) {
            controlMsg = mobCount + " mobs et " + playerCount + " joueurs sous votre controle!";
        } else if (playerCount > 0) {
            controlMsg = playerCount + " joueurs sous votre controle!";
        } else {
            controlMsg = mobCount + " mobs sous votre controle!";
        }

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal(">> ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("Mode Marionnettiste active! ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(controlMsg).withStyle(ChatFormatting.GOLD)));
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("   Duree: INFINIE. ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("Appuyez sur R pour desactiver.").withStyle(ChatFormatting.YELLOW)));
    }

    /**
     * Deactivate puppeteer mode.
     */
    public static void deactivate(UUID playerId) {
        PuppeteerSession session = activeSessions.remove(playerId);
        if (session != null) {
            for (LivingEntity entity : session.controlledEntities) {
                if (entity.isAlive()) {
                    entity.removeEffect(MobEffects.GLOWING);
                    if (entity instanceof Mob mob) {
                        mob.setTarget(null);
                    }
                }
            }
            // Remove glowing from the puppeteer player too
            session.player.removeEffect(MobEffects.GLOWING);
        }
    }

    public static boolean isActive(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    /**
     * Tick all active puppeteer sessions.
     */
    public static void tick() {
        var it = activeSessions.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            PuppeteerSession session = entry.getValue();
            session.ticksActive++;

            // Only end if player dies or disconnects - NO time limit
            if (!session.player.isAlive() || session.player.isRemoved()) {
                for (LivingEntity entity : session.controlledEntities) {
                    if (entity.isAlive()) {
                        entity.removeEffect(MobEffects.GLOWING);
                        if (entity instanceof Mob mob) {
                            mob.setTarget(null);
                        }
                    }
                }
                session.player.removeEffect(MobEffects.GLOWING);
                it.remove();
                continue;
            }

            ServerLevel level = session.level;
            ServerPlayer player = session.player;
            Vec3 look = player.getLookAngle();
            Vec3 playerPos = player.position();

            // Find what the player is looking at (target entity or point)
            LivingEntity lookTarget = null;
            Vec3 eye = player.getEyePosition();
            double closestDist = 30.0;
            for (Entity entity : level.getEntities(player,
                    player.getBoundingBox().inflate(30),
                    e -> e instanceof LivingEntity && e != player && !session.controlledEntities.contains(e))) {
                LivingEntity living = (LivingEntity) entity;
                Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(eye);
                double dist = toEntity.length();
                if (dist > closestDist) continue;
                if (look.dot(toEntity.normalize()) > 0.9) {
                    closestDist = dist;
                    lookTarget = living;
                }
            }

            // Every tick: make entities move toward look direction or attack target
            DustParticleOptions stringDust = new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 0.8f);

            session.controlledEntities.removeIf(e -> !e.isAlive() || e.isRemoved());

            for (LivingEntity entity : session.controlledEntities) {
                if (entity instanceof Mob mob) {
                    // Mob AI control
                    if (lookTarget != null) {
                        mob.setTarget(lookTarget);
                    } else {
                        Vec3 targetPoint = playerPos.add(look.scale(15));
                        mob.getNavigation().moveTo(targetPoint.x, targetPoint.y, targetPoint.z, 1.5);
                    }
                } else if (entity instanceof ServerPlayer controlledPlayer) {
                    // Player control - force movement even in creative mode
                    if (lookTarget != null) {
                        // Move controlled player toward the look target
                        Vec3 toTarget = lookTarget.position().subtract(controlledPlayer.position()).normalize().scale(0.4);
                        controlledPlayer.setDeltaMovement(toTarget.x, controlledPlayer.getDeltaMovement().y, toTarget.z);
                        controlledPlayer.hurtMarked = true;
                        // Make them face the target
                        double dx = lookTarget.getX() - controlledPlayer.getX();
                        double dz = lookTarget.getZ() - controlledPlayer.getZ();
                        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                        controlledPlayer.setYRot(yaw);
                        controlledPlayer.setYHeadRot(yaw);
                        // Force attack if close enough
                        if (controlledPlayer.distanceTo(lookTarget) < 3.0) {
                            controlledPlayer.attack(lookTarget);
                        }
                    } else {
                        // Move toward where puppeteer is looking (15 blocks ahead)
                        Vec3 targetPoint = playerPos.add(look.scale(15));
                        Vec3 toTarget = targetPoint.subtract(controlledPlayer.position()).normalize().scale(0.3);
                        controlledPlayer.setDeltaMovement(toTarget.x, controlledPlayer.getDeltaMovement().y, toTarget.z);
                        controlledPlayer.hurtMarked = true;
                        // Make them face the direction they're moving
                        float yaw = (float) (Math.atan2(toTarget.z, toTarget.x) * (180.0 / Math.PI)) - 90.0f;
                        controlledPlayer.setYRot(yaw);
                        controlledPlayer.setYHeadRot(yaw);
                    }

                    // Prevent controlled player from flying away in creative
                    if (controlledPlayer.getAbilities().flying) {
                        controlledPlayer.getAbilities().flying = false;
                        controlledPlayer.onUpdateAbilities();
                    }
                }

                // Draw puppet strings every 5 ticks
                if (session.ticksActive % 5 == 0) {
                    Vec3 start = playerPos.add(0, 2.5, 0);
                    Vec3 end = entity.position().add(0, entity.getBbHeight(), 0);
                    for (double t = 0; t < 1.0; t += 0.08) {
                        double x = start.x + (end.x - start.x) * t;
                        double y = start.y + (end.y - start.y) * t + Math.sin(t * Math.PI) * 1.0;
                        double z = start.z + (end.z - start.z) * t;
                        level.sendParticles(stringDust, x, y, z, 1, 0, 0, 0, 0);
                    }

                    // Cross marker above head
                    level.sendParticles(ParticleTypes.ENCHANT,
                            entity.getX(), entity.getY() + entity.getBbHeight() + 0.5, entity.getZ(),
                            2, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }
    }

    /**
     * Cut all puppet strings with giant scissors - instant kill all controlled mobs.
     * Triggered by pressing G while puppeteer mode is active.
     */
    public static void cutStrings(ServerPlayer player) {
        PuppeteerSession session = activeSessions.get(player.getUUID());
        if (session == null) return;

        ServerLevel level = session.level;
        Vec3 playerPos = player.position();

        DustParticleOptions goldDust = new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.0f), 2.5f);
        DustParticleOptions silverDust = new DustParticleOptions(new Vector3f(0.8f, 0.8f, 0.9f), 3.0f);

        for (LivingEntity entity : session.controlledEntities) {
            if (!entity.isAlive()) continue;

            Vec3 stringTop = playerPos.add(0, 2.5, 0);
            Vec3 entityTop = entity.position().add(0, entity.getBbHeight(), 0);

            // Find the midpoint of the string (where scissors cut)
            double cutT = 0.5;
            double cutX = stringTop.x + (entityTop.x - stringTop.x) * cutT;
            double cutY = stringTop.y + (entityTop.y - stringTop.y) * cutT + Math.sin(cutT * Math.PI) * 1.0;
            double cutZ = stringTop.z + (entityTop.z - stringTop.z) * cutT;

            // === GIANT SCISSORS PARTICLES ===
            // Blade 1 (top-left to bottom-right)
            for (double s = -1.5; s <= 1.5; s += 0.1) {
                level.sendParticles(silverDust,
                        cutX + s * 0.7, cutY + s, cutZ + s * 0.3,
                        1, 0, 0, 0, 0);
            }
            // Blade 2 (top-right to bottom-left)
            for (double s = -1.5; s <= 1.5; s += 0.1) {
                level.sendParticles(silverDust,
                        cutX - s * 0.7, cutY + s, cutZ - s * 0.3,
                        1, 0, 0, 0, 0);
            }
            // Pivot point (center of scissors)
            level.sendParticles(goldDust, cutX, cutY, cutZ, 5, 0.1, 0.1, 0.1, 0);

            // === GOLDEN STRING SNAPPING EFFECT ===
            // String from player to cut point (snapping upward)
            for (double t = 0; t < cutT; t += 0.05) {
                double x = stringTop.x + (entityTop.x - stringTop.x) * t;
                double y = stringTop.y + (entityTop.y - stringTop.y) * t + Math.sin(t * Math.PI) * 1.0;
                double z = stringTop.z + (entityTop.z - stringTop.z) * t;
                level.sendParticles(goldDust, x, y + 0.3, z, 1, 0, 0.1, 0, 0.02);
            }
            // String from cut point to entity (falling down)
            for (double t = cutT; t < 1.0; t += 0.05) {
                double x = stringTop.x + (entityTop.x - stringTop.x) * t;
                double y = stringTop.y + (entityTop.y - stringTop.y) * t + Math.sin(t * Math.PI) * 1.0;
                double z = stringTop.z + (entityTop.z - stringTop.z) * t;
                level.sendParticles(goldDust, x, y - 0.5, z, 1, 0, -0.1, 0, 0.02);
            }

            // Spark burst at cut point
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    cutX, cutY, cutZ, 15, 0.3, 0.3, 0.3, 0.15);
            level.sendParticles(ParticleTypes.CRIT,
                    cutX, cutY, cutZ, 10, 0.2, 0.2, 0.2, 0.1);

            // === INSTANT KILL ===
            // Death particles on entity
            level.sendParticles(ParticleTypes.EXPLOSION,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                    3, 0.3, 0.3, 0.3, 0);
            level.sendParticles(goldDust,
                    entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(),
                    10, 0.2, 0.5, 0.2, 0.05);

            // Kill the entity instantly (works on creative players too)
            if (entity instanceof ServerPlayer controlledPlayer) {
                // For players: force kill even in creative
                controlledPlayer.getAbilities().invulnerable = false;
                controlledPlayer.onUpdateAbilities();
                controlledPlayer.hurt(level.damageSources().magic(), Float.MAX_VALUE);
                if (controlledPlayer.isAlive()) {
                    controlledPlayer.kill();
                }
            } else {
                entity.hurt(level.damageSources().magic(), Float.MAX_VALUE);
                if (entity.isAlive()) {
                    entity.kill();
                }
            }
        }

        // Scissor sound effect
        level.playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 2.0f, 0.5f);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.5f, 1.0f);

        // Message
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal(">> ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("*SNIP* ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal("Les fils sont coupes! Mort instantanee!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));

        // End session
        activeSessions.remove(player.getUUID());
    }

    public static void onPlayerLogout(UUID playerId) {
        deactivate(playerId);
    }
}
