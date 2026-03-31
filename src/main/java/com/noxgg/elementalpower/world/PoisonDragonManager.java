package com.noxgg.elementalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PoisonDragonManager {
    private static final List<PoisonDragonAttack> activeAttacks = new ArrayList<>();

    // Phases: 0=lift, 1=poison jets, 2=dragons approach, 3=dragons impact, 4=explosion
    public static class PoisonDragonAttack {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final LivingEntity target;
        public final double startX, startY, startZ;
        public int tick = 0;
        public static final int LIFT_DURATION = 40;       // 2 sec lift
        public static final int JETS_DURATION = 30;        // 1.5 sec poison jets
        public static final int DRAGONS_DURATION = 40;     // 2 sec dragons approach
        public static final int EXPLOSION_TICK = LIFT_DURATION + JETS_DURATION + DRAGONS_DURATION;
        public static final int TOTAL_DURATION = EXPLOSION_TICK + 20;

        public PoisonDragonAttack(ServerLevel level, ServerPlayer caster, LivingEntity target) {
            this.level = level;
            this.caster = caster;
            this.target = target;
            this.startX = target.getX();
            this.startY = target.getY();
            this.startZ = target.getZ();
        }
    }

    public static void addAttack(PoisonDragonAttack attack) {
        activeAttacks.add(attack);
    }

    public static void tick() {
        Iterator<PoisonDragonAttack> it = activeAttacks.iterator();
        while (it.hasNext()) {
            PoisonDragonAttack atk = it.next();
            atk.tick++;

            if (atk.target == null || atk.target.isRemoved() || atk.tick > PoisonDragonAttack.TOTAL_DURATION) {
                it.remove();
                continue;
            }

            double targetY = atk.startY;

            // === PHASE 1: LIFT the mob 10 blocks into the air ===
            if (atk.tick <= PoisonDragonAttack.LIFT_DURATION) {
                double progress = (double) atk.tick / PoisonDragonAttack.LIFT_DURATION;
                double liftY = atk.startY + progress * 10.0;
                atk.target.teleportTo(atk.startX, liftY, atk.startZ);
                atk.target.setDeltaMovement(0, 0.3, 0);
                atk.target.hurtMarked = true;
                atk.target.fallDistance = 0;

                // Freeze the mob
                atk.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));

                // Upward poison spiral around the mob
                double mobY = liftY;
                for (int i = 0; i < 8; i++) {
                    double angle = (atk.tick * 0.3 + i * Math.PI / 4);
                    double spiralR = 1.5 + Math.sin(atk.tick * 0.2) * 0.5;
                    double px = atk.startX + Math.cos(angle) * spiralR;
                    double pz = atk.startZ + Math.sin(angle) * spiralR;
                    double py = mobY + (i * 0.3) - 1;
                    atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                            px, py, pz, 2, 0.05, 0.1, 0.05, 0.02);
                }

                // Green lifting column
                for (double h = atk.startY; h < mobY; h += 0.5) {
                    atk.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            atk.startX, h, atk.startZ, 1, 0.2, 0, 0.2, 0.01);
                }

                // Lifting sound
                if (atk.tick % 10 == 0) {
                    atk.level.playSound(null, BlockPos.containing(atk.startX, mobY, atk.startZ),
                            SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.0f, 1.5f);
                }
            }

            // === PHASE 2: POISON JETS surround the mob at altitude ===
            int jetsStart = PoisonDragonAttack.LIFT_DURATION;
            int jetsEnd = jetsStart + PoisonDragonAttack.JETS_DURATION;
            double floatY = atk.startY + 10.0;

            if (atk.tick > jetsStart && atk.tick <= jetsEnd) {
                // Keep mob floating
                atk.target.teleportTo(atk.startX, floatY, atk.startZ);
                atk.target.setDeltaMovement(0, 0, 0);
                atk.target.hurtMarked = true;
                atk.target.fallDistance = 0;
                atk.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));

                // Poison jets from all directions
                int jetPhase = atk.tick - jetsStart;
                for (int jet = 0; jet < 6; jet++) {
                    double jetAngle = (Math.PI * 2 / 6) * jet + jetPhase * 0.1;
                    for (double d = 4; d > 0; d -= 0.3) {
                        double jx = atk.startX + Math.cos(jetAngle) * d;
                        double jz = atk.startZ + Math.sin(jetAngle) * d;
                        atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                                jx, floatY + (Math.random() - 0.5) * 2, jz,
                                2, 0.05, 0.1, 0.05, 0.05);
                    }
                }

                // Poison cloud around mob
                for (int i = 0; i < 15; i++) {
                    double px = atk.startX + (Math.random() - 0.5) * 3;
                    double py = floatY + (Math.random() - 0.5) * 2;
                    double pz = atk.startZ + (Math.random() - 0.5) * 3;
                    atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                            px, py, pz, 1, 0.1, 0.1, 0.1, 0.02);
                }

                // Poison damage ticks
                if (jetPhase % 10 == 0) {
                    atk.target.hurt(atk.level.damageSources().magic(), 3.0f);
                    atk.target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 2));
                }

                if (jetPhase % 15 == 0) {
                    atk.level.playSound(null, BlockPos.containing(atk.startX, floatY, atk.startZ),
                            SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.5f);
                }
            }

            // === PHASE 3: TWO GIANT POISON DRAGONS approach from sides ===
            int dragonsStart = jetsEnd;
            int dragonsEnd = dragonsStart + PoisonDragonAttack.DRAGONS_DURATION;

            if (atk.tick > dragonsStart && atk.tick <= dragonsEnd) {
                // Keep mob floating
                atk.target.teleportTo(atk.startX, floatY, atk.startZ);
                atk.target.setDeltaMovement(0, 0, 0);
                atk.target.hurtMarked = true;
                atk.target.fallDistance = 0;
                atk.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));

                int dragonPhase = atk.tick - dragonsStart;
                double approachProgress = (double) dragonPhase / PoisonDragonAttack.DRAGONS_DURATION;

                // Dragon start distance
                double startDist = 20.0;
                double currentDist = startDist * (1.0 - approachProgress);

                // Two dragons: left and right
                for (int side = 0; side < 2; side++) {
                    double sideAngle = side == 0 ? Math.PI / 2 : -Math.PI / 2;
                    // Dragon center position
                    double dragonX = atk.startX + Math.cos(sideAngle) * currentDist;
                    double dragonZ = atk.startZ + Math.sin(sideAngle) * currentDist;
                    double dragonY = floatY;

                    // Direction toward target
                    double dirX = atk.startX - dragonX;
                    double dirZ = atk.startZ - dragonZ;
                    double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
                    if (dirLen < 0.1) dirLen = 0.1;
                    dirX /= dirLen;
                    dirZ /= dirLen;

                    // Perpendicular for wing width
                    double perpX = -dirZ;
                    double perpZ = dirX;

                    // === DRAGON HEAD (large cluster) ===
                    for (int i = 0; i < 12; i++) {
                        double hx = dragonX + (Math.random() - 0.5) * 1.5;
                        double hy = dragonY + (Math.random() - 0.5) * 1.2;
                        double hz = dragonZ + (Math.random() - 0.5) * 1.5;
                        atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                                hx, hy, hz, 2, 0.1, 0.1, 0.1, 0.02);
                    }
                    // Eyes (two bright points)
                    atk.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            dragonX + perpX * 0.4, dragonY + 0.3, dragonZ + perpZ * 0.4,
                            3, 0.05, 0.05, 0.05, 0.01);
                    atk.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            dragonX - perpX * 0.4, dragonY + 0.3, dragonZ - perpZ * 0.4,
                            3, 0.05, 0.05, 0.05, 0.01);
                    // Open jaw with dripping poison
                    atk.level.sendParticles(ParticleTypes.FALLING_DRIPSTONE_WATER,
                            dragonX + dirX * 0.8, dragonY - 0.3, dragonZ + dirZ * 0.8,
                            3, 0.2, 0.1, 0.2, 0.01);

                    // === DRAGON NECK (trailing behind head) ===
                    for (double seg = 1; seg < 4; seg += 0.4) {
                        double neckWobble = Math.sin(atk.tick * 0.3 + seg * 2) * 0.4;
                        double nx = dragonX - dirX * seg + perpX * neckWobble;
                        double nz = dragonZ - dirZ * seg + perpZ * neckWobble;
                        double ny = dragonY + Math.sin(seg * 0.8) * 0.3;
                        double thickness = 0.8 - seg * 0.1;
                        for (int p = 0; p < 4; p++) {
                            atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                                    nx + (Math.random() - 0.5) * thickness,
                                    ny + (Math.random() - 0.5) * thickness,
                                    nz + (Math.random() - 0.5) * thickness,
                                    1, 0.05, 0.05, 0.05, 0.01);
                        }
                    }

                    // === DRAGON BODY (large, behind neck) ===
                    for (double seg = 4; seg < 8; seg += 0.4) {
                        double bodyWobble = Math.sin(atk.tick * 0.2 + seg) * 0.5;
                        double bx = dragonX - dirX * seg + perpX * bodyWobble;
                        double bz = dragonZ - dirZ * seg + perpZ * bodyWobble;
                        double by = dragonY + Math.sin(seg * 0.5) * 0.5;
                        double bodyWidth = 1.2 - Math.abs(seg - 6) * 0.15;
                        for (int p = 0; p < 5; p++) {
                            atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                                    bx + (Math.random() - 0.5) * bodyWidth,
                                    by + (Math.random() - 0.5) * bodyWidth * 0.6,
                                    bz + (Math.random() - 0.5) * bodyWidth,
                                    1, 0.05, 0.05, 0.05, 0.01);
                        }
                    }

                    // === DRAGON WINGS (spread out perpendicular) ===
                    double wingFlap = Math.sin(atk.tick * 0.4 + side * Math.PI) * 0.8;
                    for (int wing = -1; wing <= 1; wing += 2) {
                        for (double w = 0; w < 5; w += 0.4) {
                            double wingCurve = -w * w * 0.05 + wingFlap;
                            double wx = dragonX - dirX * 3 + perpX * wing * (w + 1);
                            double wz = dragonZ - dirZ * 3 + perpZ * wing * (w + 1);
                            double wy = dragonY + wingCurve + 0.5;
                            atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                                    wx, wy, wz, 2, 0.05, 0.02, 0.05, 0.005);
                            // Wing membrane
                            if (w < 4) {
                                for (double m = 0; m < 1; m += 0.4) {
                                    atk.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                            wx - dirX * m, wy - m * 0.3, wz - dirZ * m,
                                            1, 0.02, 0.02, 0.02, 0.001);
                                }
                            }
                        }
                    }

                    // === DRAGON TAIL (wavy, trailing far behind) ===
                    for (double t = 8; t < 14; t += 0.4) {
                        double tailWobble = Math.sin(atk.tick * 0.25 + t * 1.5) * (t - 8) * 0.15;
                        double tx = dragonX - dirX * t + perpX * tailWobble;
                        double tz = dragonZ - dirZ * t + perpZ * tailWobble;
                        double ty = dragonY + Math.sin(t * 0.4) * 0.3;
                        double tailWidth = Math.max(0.1, 0.6 - (t - 8) * 0.08);
                        atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                                tx, ty, tz, 1, tailWidth * 0.3, tailWidth * 0.3, tailWidth * 0.3, 0.005);
                    }

                    // Dragon roar sound approaching
                    if (dragonPhase % 20 == 0) {
                        atk.level.playSound(null, BlockPos.containing(dragonX, dragonY, dragonZ),
                                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.5f);
                    }
                }
            }

            // === PHASE 4: EXPLOSION - both dragons hit the mob ===
            if (atk.tick == PoisonDragonAttack.EXPLOSION_TICK) {
                // Massive poison explosion
                for (int i = 0; i < 100; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double phi = Math.random() * Math.PI;
                    double dist = Math.random() * 5;
                    double ex = atk.startX + Math.cos(angle) * Math.sin(phi) * dist;
                    double ey = floatY + Math.cos(phi) * dist;
                    double ez = atk.startZ + Math.sin(angle) * Math.sin(phi) * dist;
                    atk.level.sendParticles(ParticleTypes.ITEM_SLIME,
                            ex, ey, ez, 3, 0.2, 0.2, 0.2, 0.15);
                }
                // Green explosion flash
                for (int i = 0; i < 40; i++) {
                    double ex = atk.startX + (Math.random() - 0.5) * 6;
                    double ey = floatY + (Math.random() - 0.5) * 6;
                    double ez = atk.startZ + (Math.random() - 0.5) * 6;
                    atk.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            ex, ey, ez, 3, 0.3, 0.3, 0.3, 0.2);
                }
                // Explosion particles
                atk.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        atk.startX, floatY, atk.startZ, 3, 1, 1, 1, 0);
                // Falling poison rain
                for (int i = 0; i < 60; i++) {
                    double rx = atk.startX + (Math.random() - 0.5) * 8;
                    double rz = atk.startZ + (Math.random() - 0.5) * 8;
                    atk.level.sendParticles(ParticleTypes.FALLING_DRIPSTONE_WATER,
                            rx, floatY + 3, rz, 2, 0.1, 0, 0.1, 0.01);
                }

                // MASSIVE DAMAGE
                atk.target.hurt(atk.level.damageSources().magic(), 40.0f);
                atk.target.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 4));
                atk.target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
                atk.target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 3));
                atk.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));

                // Drop the mob
                atk.target.setDeltaMovement(0, -1.5, 0);
                atk.target.hurtMarked = true;

                // Explosion sounds
                atk.level.playSound(null, BlockPos.containing(atk.startX, floatY, atk.startZ),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.6f);
                atk.level.playSound(null, BlockPos.containing(atk.startX, floatY, atk.startZ),
                        SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 1.5f, 0.8f);
                atk.level.playSound(null, BlockPos.containing(atk.startX, floatY, atk.startZ),
                        SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 2.0f, 0.3f);
            }
        }
    }
}
