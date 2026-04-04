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
        public final List<Mob> controlledMobs = new ArrayList<>();
        public int ticksRemaining;
        public final int maxTicks = 400; // 20 seconds

        public PuppeteerSession(ServerPlayer player, ServerLevel level) {
            this.player = player;
            this.level = level;
            this.ticksRemaining = maxTicks;
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

        // Find all mobs in radius
        var entities = level.getEntities(player,
                player.getBoundingBox().inflate(controlRadius),
                e -> e instanceof Mob && !(e instanceof Player));

        List<Mob> controlledMobs = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof Mob mob) {
                if (mob.getTags().contains("shadow_milk_boss")) continue; // Can't control Shadow Milk
                controlledMobs.add(mob);
            }
        }

        if (controlledMobs.isEmpty()) {
            player.sendSystemMessage(Component.literal("")
                    .append(Component.literal(">> ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("Aucun mob a controler dans la zone!").withStyle(ChatFormatting.GRAY)));
            return;
        }

        PuppeteerSession session = new PuppeteerSession(player, level);
        session.controlledMobs.addAll(controlledMobs);
        activeSessions.put(player.getUUID(), session);

        // Visual: strings connecting player to each mob
        DustParticleOptions stringDust = new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 1.0f);
        for (Mob mob : controlledMobs) {
            // String from player hand to mob
            Vec3 start = player.position().add(0, 2.5, 0);
            Vec3 end = mob.position().add(0, mob.getBbHeight(), 0);
            for (double t = 0; t < 1.0; t += 0.05) {
                double x = start.x + (end.x - start.x) * t;
                double y = start.y + (end.y - start.y) * t + Math.sin(t * Math.PI) * 1.5; // Arc
                double z = start.z + (end.z - start.z) * t;
                level.sendParticles(stringDust, x, y, z, 1, 0, 0, 0, 0);
            }

            // Glowing effect on controlled mob
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 500, 0, false, false));

            // Stop mob's current AI goals
            mob.setTarget(null);
        }

        // Player buffs during puppeteer mode
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0, false, false));

        // Sounds
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.5f, 1.5f);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.5f);

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal(">> ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("Mode Marionnettiste active! ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(controlledMobs.size() + " mobs sous votre controle!").withStyle(ChatFormatting.GOLD)));
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("   Les mobs attaquent la ou vous regardez. ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Appuyez sur R pour desactiver.").withStyle(ChatFormatting.YELLOW)));
    }

    /**
     * Deactivate puppeteer mode.
     */
    public static void deactivate(UUID playerId) {
        PuppeteerSession session = activeSessions.remove(playerId);
        if (session != null) {
            for (Mob mob : session.controlledMobs) {
                if (mob.isAlive()) {
                    mob.removeEffect(MobEffects.GLOWING);
                    mob.setTarget(null);
                }
            }
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
            session.ticksRemaining--;

            if (session.ticksRemaining <= 0 || !session.player.isAlive() || session.player.isRemoved()) {
                // Clean up
                for (Mob mob : session.controlledMobs) {
                    if (mob.isAlive()) {
                        mob.removeEffect(MobEffects.GLOWING);
                        mob.setTarget(null);
                    }
                }
                session.player.sendSystemMessage(Component.literal("")
                        .append(Component.literal(">> ").withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("Mode Marionnettiste termine!").withStyle(ChatFormatting.YELLOW)));
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
                    e -> e instanceof LivingEntity && e != player && !session.controlledMobs.contains(e))) {
                LivingEntity living = (LivingEntity) entity;
                Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(eye);
                double dist = toEntity.length();
                if (dist > closestDist) continue;
                if (look.dot(toEntity.normalize()) > 0.9) {
                    closestDist = dist;
                    lookTarget = living;
                }
            }

            // Every tick: make mobs move toward look direction or attack target
            DustParticleOptions stringDust = new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 0.8f);

            session.controlledMobs.removeIf(mob -> !mob.isAlive() || mob.isRemoved());

            for (Mob mob : session.controlledMobs) {
                if (lookTarget != null) {
                    // Attack the target
                    mob.setTarget(lookTarget);
                } else {
                    // Move toward where player is looking (15 blocks ahead)
                    Vec3 targetPoint = playerPos.add(look.scale(15));
                    mob.getNavigation().moveTo(targetPoint.x, targetPoint.y, targetPoint.z, 1.5);
                }

                // Draw puppet strings every 5 ticks
                if (session.ticksRemaining % 5 == 0) {
                    Vec3 start = playerPos.add(0, 2.5, 0);
                    Vec3 end = mob.position().add(0, mob.getBbHeight(), 0);
                    for (double t = 0; t < 1.0; t += 0.08) {
                        double x = start.x + (end.x - start.x) * t;
                        double y = start.y + (end.y - start.y) * t + Math.sin(t * Math.PI) * 1.0;
                        double z = start.z + (end.z - start.z) * t;
                        level.sendParticles(stringDust, x, y, z, 1, 0, 0, 0, 0);
                    }

                    // Cross marker above mob head
                    level.sendParticles(ParticleTypes.ENCHANT,
                            mob.getX(), mob.getY() + mob.getBbHeight() + 0.5, mob.getZ(),
                            2, 0.1, 0.1, 0.1, 0.05);
                }
            }

            // Time remaining warning
            if (session.ticksRemaining == 100) {
                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal(">> ").withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("Marionnettiste: 5 secondes restantes!").withStyle(ChatFormatting.YELLOW)));
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

        for (Mob mob : session.controlledMobs) {
            if (!mob.isAlive()) continue;

            Vec3 stringTop = playerPos.add(0, 2.5, 0);
            Vec3 mobTop = mob.position().add(0, mob.getBbHeight(), 0);

            // Find the midpoint of the string (where scissors cut)
            double cutT = 0.5;
            double cutX = stringTop.x + (mobTop.x - stringTop.x) * cutT;
            double cutY = stringTop.y + (mobTop.y - stringTop.y) * cutT + Math.sin(cutT * Math.PI) * 1.0;
            double cutZ = stringTop.z + (mobTop.z - stringTop.z) * cutT;

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
                double x = stringTop.x + (mobTop.x - stringTop.x) * t;
                double y = stringTop.y + (mobTop.y - stringTop.y) * t + Math.sin(t * Math.PI) * 1.0;
                double z = stringTop.z + (mobTop.z - stringTop.z) * t;
                level.sendParticles(goldDust, x, y + 0.3, z, 1, 0, 0.1, 0, 0.02);
            }
            // String from cut point to mob (falling down)
            for (double t = cutT; t < 1.0; t += 0.05) {
                double x = stringTop.x + (mobTop.x - stringTop.x) * t;
                double y = stringTop.y + (mobTop.y - stringTop.y) * t + Math.sin(t * Math.PI) * 1.0;
                double z = stringTop.z + (mobTop.z - stringTop.z) * t;
                level.sendParticles(goldDust, x, y - 0.5, z, 1, 0, -0.1, 0, 0.02);
            }

            // Spark burst at cut point
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    cutX, cutY, cutZ, 15, 0.3, 0.3, 0.3, 0.15);
            level.sendParticles(ParticleTypes.CRIT,
                    cutX, cutY, cutZ, 10, 0.2, 0.2, 0.2, 0.1);

            // === INSTANT KILL ===
            // Death particles on mob
            level.sendParticles(ParticleTypes.EXPLOSION,
                    mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(),
                    3, 0.3, 0.3, 0.3, 0);
            level.sendParticles(goldDust,
                    mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    10, 0.2, 0.5, 0.2, 0.05);

            // Kill the mob instantly
            mob.hurt(level.damageSources().magic(), Float.MAX_VALUE);
            if (mob.isAlive()) {
                mob.kill(); // Force kill if somehow survived
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
