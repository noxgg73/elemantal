package com.noxgg.elementalpower.world;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RainbowChuzzleManager {
    private static final List<RainbowChuzzle> activeChuzzles = new ArrayList<>();

    // Rainbow colors (ROYGBIV)
    private static final Vector3f[] RAINBOW_COLORS = {
            new Vector3f(1.0f, 0.0f, 0.0f),   // Rouge
            new Vector3f(1.0f, 0.5f, 0.0f),   // Orange
            new Vector3f(1.0f, 1.0f, 0.0f),   // Jaune
            new Vector3f(0.0f, 1.0f, 0.0f),   // Vert
            new Vector3f(0.0f, 0.5f, 1.0f),   // Bleu
            new Vector3f(0.3f, 0.0f, 0.8f),   // Indigo
            new Vector3f(0.6f, 0.0f, 1.0f),   // Violet
    };

    public static class RainbowChuzzle {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final LivingEntity target;
        public final double x, y, z;
        public int ticksAlive = 0;

        // Phase 1: 0-20 ticks  = rayon arc-en-ciel descend du ciel
        // Phase 2: 20-40 ticks = rayon entre dans le corps, surcharge (mob tremble/brille)
        // Phase 3: tick 40     = EXPLOSION arc-en-ciel
        public static final int RAY_PHASE = 20;
        public static final int OVERCHARGE_PHASE = 40;

        public RainbowChuzzle(ServerLevel level, ServerPlayer caster, LivingEntity target) {
            this.level = level;
            this.caster = caster;
            this.target = target;
            this.x = target.getX();
            this.y = target.getY();
            this.z = target.getZ();
        }
    }

    public static void addChuzzle(RainbowChuzzle chuzzle) {
        activeChuzzles.add(chuzzle);

        // Transformer le mob en Chuzzle Arc-en-ciel
        LivingEntity target = chuzzle.target;
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        target.setCustomName(Component.literal("\u00A7c\u00A7lC\u00A76\u00A7lh\u00A7e\u00A7lu\u00A7a\u00A7lz\u00A7b\u00A7lz\u00A79\u00A7ll\u00A7d\u00A7le \u00A7c\u00A7lA\u00A76\u00A7lr\u00A7e\u00A7lc\u00A7a\u00A7l-\u00A7b\u00A7le\u00A79\u00A7ln\u00A7d\u00A7l-\u00A7c\u00A7lc\u00A76\u00A7li\u00A7e\u00A7le\u00A7a\u00A7ll"));
        target.setCustomNameVisible(true);

        // Immobiliser et effet de glow
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 127, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
    }

    public static void tick() {
        Iterator<RainbowChuzzle> it = activeChuzzles.iterator();
        while (it.hasNext()) {
            RainbowChuzzle chuzzle = it.next();
            chuzzle.ticksAlive++;

            if (chuzzle.target == null || chuzzle.target.isRemoved()) {
                it.remove();
                continue;
            }

            // Garder le mob immobile
            chuzzle.target.setDeltaMovement(0, 0, 0);
            chuzzle.target.hurtMarked = true;
            chuzzle.target.teleportTo(chuzzle.x, chuzzle.y, chuzzle.z);

            if (chuzzle.ticksAlive <= RainbowChuzzle.RAY_PHASE) {
                // === PHASE 1: Rayon arc-en-ciel descend du ciel ===
                renderRainbowRay(chuzzle);
            } else if (chuzzle.ticksAlive <= RainbowChuzzle.OVERCHARGE_PHASE) {
                // === PHASE 2: Le rayon entre dans le corps - surcharge ===
                renderOvercharge(chuzzle);
            } else {
                // === PHASE 3: EXPLOSION ARC-EN-CIEL ===
                rainbowExplosion(chuzzle);
                it.remove();
                continue;
            }
        }
    }

    private static void renderRainbowRay(RainbowChuzzle chuzzle) {
        ServerLevel level = chuzzle.level;
        double tx = chuzzle.x;
        double ty = chuzzle.y;
        double tz = chuzzle.z;
        int tick = chuzzle.ticksAlive;

        // Le rayon descend progressivement du ciel (y+30) vers le mob
        double rayTop = 30.0;
        double progress = (double) tick / RainbowChuzzle.RAY_PHASE; // 0 -> 1
        double currentBottom = ty + rayTop * (1.0 - progress);

        // Dessiner le rayon arc-en-ciel vertical
        for (double h = ty + rayTop; h >= currentBottom; h -= 0.4) {
            int colorIndex = (int) ((h + tick * 0.5) % RAINBOW_COLORS.length);
            if (colorIndex < 0) colorIndex += RAINBOW_COLORS.length;
            Vector3f color = RAINBOW_COLORS[colorIndex];
            DustParticleOptions dust = new DustParticleOptions(color, 1.5f);

            // Colonne centrale
            level.sendParticles(dust, tx, h, tz, 1, 0.05, 0, 0.05, 0);

            // Anneau autour du rayon (spirale)
            double angle = h * 2.0 + tick * 0.3;
            double ringR = 0.5;
            double px = tx + Math.cos(angle) * ringR;
            double pz = tz + Math.sin(angle) * ringR;
            level.sendParticles(dust, px, h, pz, 1, 0.02, 0, 0.02, 0);
        }

        // Particules de lumiere autour du mob
        level.sendParticles(ParticleTypes.END_ROD,
                tx, ty + 1, tz, 3, 0.3, 0.5, 0.3, 0.02);

        // Son de charge (montant en pitch)
        if (tick % 5 == 0) {
            float pitch = 0.5f + (float) progress * 1.5f;
            level.playSound(null, BlockPos.containing(tx, ty, tz),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, pitch);
        }

        // Son d'amethyste pour l'ambiance arc-en-ciel
        if (tick % 3 == 0) {
            level.playSound(null, BlockPos.containing(tx, ty, tz),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.8f + (float) progress);
        }
    }

    private static void renderOvercharge(RainbowChuzzle chuzzle) {
        ServerLevel level = chuzzle.level;
        double tx = chuzzle.x;
        double ty = chuzzle.y;
        double tz = chuzzle.z;
        int tick = chuzzle.ticksAlive;
        double overchargeProgress = (double) (tick - RainbowChuzzle.RAY_PHASE) /
                (RainbowChuzzle.OVERCHARGE_PHASE - RainbowChuzzle.RAY_PHASE);

        // Le mob vibre/tremble de plus en plus - particules arc-en-ciel sortent de son corps
        double intensity = 0.2 + overchargeProgress * 1.5;
        int particleCount = (int) (5 + overchargeProgress * 20);

        for (int i = 0; i < particleCount; i++) {
            Vector3f color = RAINBOW_COLORS[i % RAINBOW_COLORS.length];
            DustParticleOptions dust = new DustParticleOptions(color, 1.2f + (float) overchargeProgress);

            // Particules qui sortent du corps dans toutes les directions
            level.sendParticles(dust,
                    tx, ty + 1, tz,
                    1, intensity * 0.5, intensity * 0.5, intensity * 0.5, 0.05 + overchargeProgress * 0.1);
        }

        // Eclairs arc-en-ciel autour du mob
        if (tick % 2 == 0) {
            for (int i = 0; i < 3; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double dist = 0.5 + level.random.nextDouble() * intensity;
                Vector3f color = RAINBOW_COLORS[level.random.nextInt(RAINBOW_COLORS.length)];
                DustParticleOptions dust = new DustParticleOptions(color, 2.0f);
                level.sendParticles(dust,
                        tx + Math.cos(angle) * dist,
                        ty + 0.5 + level.random.nextDouble() * 1.5,
                        tz + Math.sin(angle) * dist,
                        1, 0.02, 0.02, 0.02, 0);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    tx, ty + 1, tz,
                    (int) (2 + overchargeProgress * 5), 0.3, 0.5, 0.3, 0.1);
        }

        // Anneau de couleur qui pulse autour du mob
        if (tick % 4 == 0) {
            double ringR = 1.0 + overchargeProgress * 1.5;
            for (double angle = 0; angle < Math.PI * 2; angle += 0.3) {
                int ci = (int) ((angle / (Math.PI * 2) * RAINBOW_COLORS.length + tick * 0.2)) % RAINBOW_COLORS.length;
                if (ci < 0) ci += RAINBOW_COLORS.length;
                DustParticleOptions dust = new DustParticleOptions(RAINBOW_COLORS[ci], 1.5f);
                level.sendParticles(dust,
                        tx + Math.cos(angle) * ringR,
                        ty + 0.5,
                        tz + Math.sin(angle) * ringR,
                        1, 0, 0, 0, 0);
            }
        }

        // Son de surcharge qui monte
        if (tick % 4 == 0) {
            float pitch = 1.0f + (float) overchargeProgress * 1.0f;
            level.playSound(null, BlockPos.containing(tx, ty, tz),
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8f, pitch);
        }

        // Tremblement: totem particles
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                tx, ty + 1, tz,
                (int) (2 + overchargeProgress * 8), 0.2, 0.3, 0.2, 0.05 + overchargeProgress * 0.1);
    }

    private static void rainbowExplosion(RainbowChuzzle chuzzle) {
        ServerLevel level = chuzzle.level;
        double tx = chuzzle.x;
        double ty = chuzzle.y;
        double tz = chuzzle.z;

        // === MEGA EXPLOSION ARC-EN-CIEL ===

        // Sphere de particules arc-en-ciel qui s'etend
        for (double radius = 1; radius <= 8; radius += 0.5) {
            int points = (int) (radius * 12);
            for (int i = 0; i < points; i++) {
                double theta = level.random.nextDouble() * Math.PI * 2;
                double phi = level.random.nextDouble() * Math.PI;
                double px = tx + Math.cos(theta) * Math.sin(phi) * radius;
                double py = ty + 1 + Math.cos(phi) * radius;
                double pz = tz + Math.sin(theta) * Math.sin(phi) * radius;

                Vector3f color = RAINBOW_COLORS[i % RAINBOW_COLORS.length];
                DustParticleOptions dust = new DustParticleOptions(color, 2.5f);
                level.sendParticles(dust, px, py, pz, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }

        // Anneaux horizontaux arc-en-ciel qui montent
        for (double h = 0; h <= 6; h += 0.5) {
            double ringR = 5.0 - (h / 6.0) * 3.0; // de large en bas a petit en haut
            for (double angle = 0; angle < Math.PI * 2; angle += 0.15) {
                int ci = (int) ((angle / (Math.PI * 2) * RAINBOW_COLORS.length + h * 2)) % RAINBOW_COLORS.length;
                if (ci < 0) ci += RAINBOW_COLORS.length;
                DustParticleOptions dust = new DustParticleOptions(RAINBOW_COLORS[ci], 2.0f);
                level.sendParticles(dust,
                        tx + Math.cos(angle) * ringR,
                        ty + h,
                        tz + Math.sin(angle) * ringR,
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Rayons arc-en-ciel dans toutes les directions (etoile)
        for (int ray = 0; ray < 12; ray++) {
            double angle = (Math.PI * 2 / 12) * ray;
            for (double d = 0; d < 10; d += 0.3) {
                int ci = (int) (d + ray) % RAINBOW_COLORS.length;
                DustParticleOptions dust = new DustParticleOptions(RAINBOW_COLORS[ci], 1.8f);
                level.sendParticles(dust,
                        tx + Math.cos(angle) * d,
                        ty + 1 + Math.sin(d * 0.5) * 0.5,
                        tz + Math.sin(angle) * d,
                        1, 0.02, 0.02, 0.02, 0);
            }
        }

        // Pluie de particules totem (les plus colorees de Minecraft)
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                tx, ty + 2, tz, 200, 3, 3, 3, 0.8);

        // Flash blanc central
        level.sendParticles(ParticleTypes.FLASH,
                tx, ty + 1.5, tz, 3, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD,
                tx, ty + 1, tz, 60, 2, 2, 2, 0.3);

        // Etincelles electriques
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                tx, ty + 1, tz, 40, 3, 3, 3, 0.2);

        // === SONS D'EXPLOSION ===
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 1.2f);
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 2.0f, 0.8f);
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 2.0f, 1.2f);
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.5f, 1.0f);

        // === DEGATS DE L'EXPLOSION ===
        // Tuer le chuzzle
        chuzzle.target.kill();

        // Degats aux mobs proches (rayon 6 blocs) - l'explosion arc-en-ciel fait des degats
        float damage = 20.0f; // 10 coeurs
        level.getEntities(chuzzle.target,
                chuzzle.target.getBoundingBox().inflate(6),
                e -> e instanceof LivingEntity && e != chuzzle.caster).forEach(e -> {
            if (e instanceof LivingEntity living) {
                double dist = living.position().distanceTo(new Vec3(tx, ty, tz));
                // Degats inversement proportionnels a la distance
                float scaledDamage = damage * (float) Math.max(0, 1.0 - dist / 6.0);
                living.hurt(level.damageSources().magic(), scaledDamage);
                // Repousser
                Vec3 knockback = living.position().subtract(tx, ty, tz).normalize().scale(1.5);
                living.setDeltaMovement(knockback.x, 0.5, knockback.z);
                living.hurtMarked = true;
            }
        });
    }
}
