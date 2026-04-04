package com.noxgg.elementalpower.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alastor (Hazbin Hotel) - 3 spells for the Demon class after reincarnation.
 * 1. Shadow Tentacles (Tentacules d'Ombre) - R key
 * 2. Voodoo Symbols (Symboles Vaudou) - G key
 * 3. Demonic Radio Wave (Onde Radio Demoniaque) - K key
 */
public class AlastorManager {

    // Active tentacle attacks (for tick-based animation)
    private static final Map<UUID, TentacleAttack> activeTentacles = new ConcurrentHashMap<>();

    public static class TentacleAttack {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 origin;
        public final List<Vec3> targets;
        public int ticksAlive = 0;
        public final int maxTicks = 40; // 2 seconds

        public TentacleAttack(ServerLevel level, ServerPlayer caster, Vec3 origin, List<Vec3> targets) {
            this.level = level;
            this.caster = caster;
            this.origin = origin;
            this.targets = targets;
        }
    }

    // ==========================================
    // SPELL 1: TENTACULES D'OMBRE (Shadow Tentacles)
    // Alastor summons shadow tentacles from the ground that grab and damage nearby enemies
    // ==========================================
    public static void castShadowTentacles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        Vec3 playerPos = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();

        // Find targets in a cone in front of the player
        List<LivingEntity> targets = new ArrayList<>();
        List<Vec3> targetPositions = new ArrayList<>();

        var entities = level.getEntities(player,
                player.getBoundingBox().inflate(15),
                e -> e instanceof LivingEntity && e != player && !(e instanceof Player));

        for (Entity e : entities) {
            LivingEntity living = (LivingEntity) e;
            Vec3 toEntity = living.position().subtract(playerPos).normalize();
            // Cone check - wider than usual
            if (flatLook.dot(new Vec3(toEntity.x, 0, toEntity.z).normalize()) > 0.3) {
                targets.add(living);
                targetPositions.add(living.position());
            }
        }

        // Shadow portal on the ground beneath the player
        DustParticleOptions shadowDust = new DustParticleOptions(new Vector3f(0.1f, 0.0f, 0.0f), 3.0f);
        DustParticleOptions redDust = new DustParticleOptions(new Vector3f(0.7f, 0.0f, 0.0f), 2.0f);

        // Ground shadow circle (Alastor's shadow portal)
        for (double angle = 0; angle < Math.PI * 2; angle += 0.1) {
            for (double r = 0.5; r <= 2.5; r += 0.4) {
                double px = playerPos.x + Math.cos(angle) * r;
                double pz = playerPos.z + Math.sin(angle) * r;
                level.sendParticles(shadowDust, px, playerPos.y + 0.1, pz, 1, 0, 0, 0, 0);
            }
        }

        // Tentacles rising from the ground toward each target
        for (LivingEntity target : targets) {
            Vec3 targetPos = target.position();
            Vec3 groundBelow = new Vec3(targetPos.x, targetPos.y, targetPos.z);

            // Tentacle segments from ground up, with sinuous movement
            for (double h = 0; h < 4.0; h += 0.2) {
                double sway = Math.sin(h * 2.5) * 0.5;
                double swayZ = Math.cos(h * 2.0) * 0.3;

                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        groundBelow.x + sway, groundBelow.y + h, groundBelow.z + swayZ,
                        2, 0.05, 0.05, 0.05, 0.005);
                level.sendParticles(redDust,
                        groundBelow.x + sway, groundBelow.y + h, groundBelow.z + swayZ,
                        1, 0.02, 0.02, 0.02, 0);

                // Tips glow red
                if (h > 3.0) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            groundBelow.x + sway, groundBelow.y + h, groundBelow.z + swayZ,
                            2, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Shadow circle at base of tentacle
            for (double a = 0; a < Math.PI * 2; a += 0.4) {
                level.sendParticles(shadowDust,
                        groundBelow.x + Math.cos(a) * 0.8, groundBelow.y + 0.05, groundBelow.z + Math.sin(a) * 0.8,
                        1, 0, 0, 0, 0);
            }

            // Damage and effects
            target.hurt(level.damageSources().magic(), 12.0f + player.experienceLevel * 0.2f);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            target.setDeltaMovement(0, 0.5, 0); // Lift up
            target.hurtMarked = true;
        }

        // If no targets, still show tentacles in look direction
        if (targets.isEmpty()) {
            Vec3 tentaclePos = playerPos.add(flatLook.scale(5));
            for (int t = 0; t < 5; t++) {
                double ox = (level.random.nextDouble() - 0.5) * 3;
                double oz = (level.random.nextDouble() - 0.5) * 3;
                Vec3 tPos = tentaclePos.add(ox, 0, oz);
                for (double h = 0; h < 4.0; h += 0.25) {
                    double sway = Math.sin(h * 2.5 + t) * 0.5;
                    level.sendParticles(ParticleTypes.LARGE_SMOKE,
                            tPos.x + sway, tPos.y + h, tPos.z,
                            2, 0.05, 0.05, 0.05, 0.005);
                    level.sendParticles(redDust,
                            tPos.x + sway, tPos.y + h, tPos.z,
                            1, 0.02, 0.02, 0.02, 0);
                }
            }
        }

        // Sounds - eerie tentacle sounds
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6f, 1.8f);

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal(">> ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal("Tentacules d'Ombre! ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("Les ombres obeissent a votre volonte!").withStyle(ChatFormatting.DARK_RED)));
    }

    // ==========================================
    // SPELL 2: SYMBOLES VAUDOU (Voodoo Symbols)
    // Alastor trace des symboles vaudou au sol qui maudissent et lient les ennemis
    // ==========================================
    public static void castVoodooSymbols(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        Vec3 playerPos = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();

        // Center of the voodoo circle - 8 blocks in front
        Vec3 circleCenter = playerPos.add(flatLook.scale(8));

        DustParticleOptions greenDust = new DustParticleOptions(new Vector3f(0.0f, 0.6f, 0.0f), 2.5f);
        DustParticleOptions redDust = new DustParticleOptions(new Vector3f(0.6f, 0.0f, 0.0f), 2.0f);
        DustParticleOptions darkDust = new DustParticleOptions(new Vector3f(0.15f, 0.0f, 0.1f), 2.0f);

        double radius = 6.0;

        // Outer circle
        for (double angle = 0; angle < Math.PI * 2; angle += 0.08) {
            double px = circleCenter.x + Math.cos(angle) * radius;
            double pz = circleCenter.z + Math.sin(angle) * radius;
            level.sendParticles(greenDust, px, circleCenter.y + 0.1, pz, 1, 0, 0, 0, 0);
        }

        // Inner circle
        for (double angle = 0; angle < Math.PI * 2; angle += 0.1) {
            double px = circleCenter.x + Math.cos(angle) * (radius * 0.5);
            double pz = circleCenter.z + Math.sin(angle) * (radius * 0.5);
            level.sendParticles(redDust, px, circleCenter.y + 0.1, pz, 1, 0, 0, 0, 0);
        }

        // Pentagram inside the circle (5-pointed star)
        for (int i = 0; i < 5; i++) {
            double angle1 = (i * 2 * Math.PI / 5) - Math.PI / 2;
            double angle2 = ((i + 2) % 5 * 2 * Math.PI / 5) - Math.PI / 2;
            double x1 = circleCenter.x + Math.cos(angle1) * (radius * 0.45);
            double z1 = circleCenter.z + Math.sin(angle1) * (radius * 0.45);
            double x2 = circleCenter.x + Math.cos(angle2) * (radius * 0.45);
            double z2 = circleCenter.z + Math.sin(angle2) * (radius * 0.45);

            // Draw line between points
            for (double t = 0; t <= 1.0; t += 0.03) {
                double lx = x1 + (x2 - x1) * t;
                double lz = z1 + (z2 - z1) * t;
                level.sendParticles(darkDust, lx, circleCenter.y + 0.15, lz, 1, 0, 0, 0, 0);
            }
        }

        // Voodoo symbols floating up from the circle
        for (int i = 0; i < 20; i++) {
            double ox = (level.random.nextDouble() - 0.5) * radius * 2;
            double oz = (level.random.nextDouble() - 0.5) * radius * 2;
            if (ox * ox + oz * oz <= radius * radius) {
                level.sendParticles(ParticleTypes.ENCHANT,
                        circleCenter.x + ox, circleCenter.y + 0.5, circleCenter.z + oz,
                        3, 0.1, 1.0, 0.1, 0.5);
                level.sendParticles(ParticleTypes.SOUL,
                        circleCenter.x + ox, circleCenter.y + 0.3, circleCenter.z + oz,
                        1, 0.1, 0.3, 0.1, 0.02);
            }
        }

        // Pillars of dark energy at pentagram points
        for (int i = 0; i < 5; i++) {
            double angle = (i * 2 * Math.PI / 5) - Math.PI / 2;
            double px = circleCenter.x + Math.cos(angle) * (radius * 0.45);
            double pz = circleCenter.z + Math.sin(angle) * (radius * 0.45);
            for (double h = 0; h < 5; h += 0.3) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        px, circleCenter.y + h, pz, 2, 0.05, 0.1, 0.05, 0.01);
            }
        }

        // Affect all enemies in the circle
        var entities = level.getEntities(player,
                new AABB(circleCenter.x - radius, circleCenter.y - 2, circleCenter.z - radius,
                        circleCenter.x + radius, circleCenter.y + 5, circleCenter.z + radius),
                e -> e instanceof LivingEntity && e != player);

        for (Entity e : entities) {
            if (e instanceof LivingEntity living) {
                double dx = living.getX() - circleCenter.x;
                double dz = living.getZ() - circleCenter.z;
                if (dx * dx + dz * dz <= radius * radius) {
                    // Voodoo curse effects
                    living.hurt(level.damageSources().magic(), 8.0f);
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));      // Wither curse
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));     // Weakened
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2)); // Bound
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));      // Marked
                    living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));    // Lifted by voodoo

                    // Chain particles from victim to circle center
                    for (double t = 0; t < 1.0; t += 0.1) {
                        double cx = living.getX() + (circleCenter.x - living.getX()) * t;
                        double cy = living.getY() + 1 + (circleCenter.y + 2 - living.getY() - 1) * t;
                        double cz = living.getZ() + (circleCenter.z - living.getZ()) * t;
                        level.sendParticles(greenDust, cx, cy, cz, 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        // Sounds
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, BlockPos.containing(circleCenter), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.7f);
        level.playSound(null, BlockPos.containing(circleCenter), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 0.8f, 0.5f);

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal(">> ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal("Symboles Vaudou! ").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD))
                .append(Component.literal("Un pacte est un pacte, mon ami!").withStyle(ChatFormatting.DARK_RED)));
    }

    // ==========================================
    // SPELL 3: ONDE RADIO DEMONIAQUE (Demonic Radio Wave)
    // Alastor emet une onde radio devastatrice en cercle autour de lui
    // ==========================================
    public static void castRadioWave(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        Vec3 playerPos = player.position();
        double maxRadius = 20.0;

        DustParticleOptions redDust = new DustParticleOptions(new Vector3f(0.8f, 0.0f, 0.0f), 2.5f);
        DustParticleOptions staticDust = new DustParticleOptions(new Vector3f(0.9f, 0.2f, 0.2f), 1.5f);

        // Expanding radio wave rings
        for (double r = 1; r <= maxRadius; r += 1.5) {
            for (double angle = 0; angle < Math.PI * 2; angle += 0.15) {
                double px = playerPos.x + Math.cos(angle) * r;
                double pz = playerPos.z + Math.sin(angle) * r;

                // Main wave ring
                level.sendParticles(redDust, px, playerPos.y + 1.5, pz, 1, 0, 0, 0, 0);

                // Static interference
                if (level.random.nextFloat() < 0.3) {
                    double jitterY = (level.random.nextDouble() - 0.5) * 2;
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            px, playerPos.y + 1.5 + jitterY, pz, 1, 0.1, 0.3, 0.1, 0.05);
                }
            }
        }

        // Radio antenna effect above player (Alastor's microphone/staff)
        for (double h = 0; h < 6; h += 0.2) {
            double flicker = Math.sin(h * 8) * 0.15;
            level.sendParticles(staticDust,
                    playerPos.x + flicker, playerPos.y + 2 + h, playerPos.z + flicker,
                    1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    playerPos.x + flicker, playerPos.y + 2 + h, playerPos.z + flicker,
                    1, 0.05, 0.05, 0.05, 0.01);
        }

        // Radio wave top burst
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                playerPos.x, playerPos.y + 8, playerPos.z, 20, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                playerPos.x, playerPos.y + 8, playerPos.z, 30, 1.0, 0.5, 1.0, 0.15);

        // Cross-shaped radio interference pattern on the ground
        for (double d = -maxRadius; d <= maxRadius; d += 0.5) {
            level.sendParticles(redDust, playerPos.x + d, playerPos.y + 0.1, playerPos.z, 1, 0, 0, 0, 0);
            level.sendParticles(redDust, playerPos.x, playerPos.y + 0.1, playerPos.z + d, 1, 0, 0, 0, 0);
        }

        // Damage and disorient all entities in radius
        var entities = level.getEntities(player,
                player.getBoundingBox().inflate(maxRadius),
                e -> e instanceof LivingEntity && e != player);

        for (Entity e : entities) {
            if (e instanceof LivingEntity living) {
                double dist = living.position().distanceTo(playerPos);
                if (dist <= maxRadius) {
                    // Damage decreases with distance
                    float damage = (float) (18.0 - (dist / maxRadius) * 10.0);
                    living.hurt(level.damageSources().sonicBoom(player), Math.max(damage, 4.0f));

                    // Radio static disorientation
                    living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1));     // Nausea
                    living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));       // Darkness
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));       // Weakness

                    // Knockback away from player
                    Vec3 knockDir = living.position().subtract(playerPos).normalize();
                    living.setDeltaMovement(knockDir.x * 1.5, 0.5, knockDir.z * 1.5);
                    living.hurtMarked = true;

                    // Static particles on affected mobs
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            living.getX(), living.getY() + 1, living.getZ(),
                            10, 0.3, 0.5, 0.3, 0.1);
                }
            }
        }

        // Player gets a speed boost from the radio energy
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, false));

        // Sounds - radio static + demonic broadcast
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 2.0f);
        level.playSound(null, player.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.3f);

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal(">> ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal("Onde Radio Demoniaque! ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("Restez a l'ecoute!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC)));
    }

    // Tick active tentacles (for animated versions)
    public static void tick() {
        var it = activeTentacles.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            TentacleAttack attack = entry.getValue();
            attack.ticksAlive++;
            if (attack.ticksAlive >= attack.maxTicks) {
                it.remove();
            }
        }
    }
}
