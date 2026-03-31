package com.noxgg.elementalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PoisonDragonManager {
    private static final List<PoisonDragonAttack> activeAttacks = new ArrayList<>();

    // Purple poison dust particle
    private static final DustParticleOptions PURPLE_POISON = new DustParticleOptions(
            new Vector3f(0.6f, 0.0f, 0.8f), 1.5f);
    private static final DustParticleOptions DARK_PURPLE = new DustParticleOptions(
            new Vector3f(0.4f, 0.0f, 0.6f), 2.0f);
    private static final DustParticleOptions BRIGHT_PURPLE = new DustParticleOptions(
            new Vector3f(0.8f, 0.2f, 1.0f), 1.0f);

    public static class PoisonDragonAttack {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final LivingEntity target;
        public final double startX, startY, startZ;
        public int tick = 0;
        public static final int LIFT_DURATION = 40;
        public static final int JETS_DURATION = 30;
        public static final int DRAGONS_DURATION = 40;
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

            // === PHASE 1: LIFT the mob 10 blocks ===
            if (atk.tick <= PoisonDragonAttack.LIFT_DURATION) {
                double progress = (double) atk.tick / PoisonDragonAttack.LIFT_DURATION;
                double liftY = atk.startY + progress * 10.0;
                atk.target.teleportTo(atk.startX, liftY, atk.startZ);
                atk.target.setDeltaMovement(0, 0.3, 0);
                atk.target.hurtMarked = true;
                atk.target.fallDistance = 0;
                atk.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));

                // Purple poison spiral
                for (int i = 0; i < 8; i++) {
                    double angle = (atk.tick * 0.3 + i * Math.PI / 4);
                    double spiralR = 1.5 + Math.sin(atk.tick * 0.2) * 0.5;
                    double px = atk.startX + Math.cos(angle) * spiralR;
                    double pz = atk.startZ + Math.sin(angle) * spiralR;
                    double py = liftY + (i * 0.3) - 1;
                    atk.level.sendParticles(PURPLE_POISON, px, py, pz, 2, 0.05, 0.1, 0.05, 0.02);
                    atk.level.sendParticles(ParticleTypes.WITCH, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                }

                // Purple lifting column
                for (double h = atk.startY; h < liftY; h += 0.5) {
                    atk.level.sendParticles(BRIGHT_PURPLE, atk.startX, h, atk.startZ, 1, 0.2, 0, 0.2, 0.01);
                }

                if (atk.tick % 10 == 0) {
                    atk.level.playSound(null, BlockPos.containing(atk.startX, liftY, atk.startZ),
                            SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.0f, 1.5f);
                }
            }

            // === PHASE 2: PURPLE POISON JETS ===
            int jetsStart = PoisonDragonAttack.LIFT_DURATION;
            int jetsEnd = jetsStart + PoisonDragonAttack.JETS_DURATION;
            double floatY = atk.startY + 10.0;

            if (atk.tick > jetsStart && atk.tick <= jetsEnd) {
                atk.target.teleportTo(atk.startX, floatY, atk.startZ);
                atk.target.setDeltaMovement(0, 0, 0);
                atk.target.hurtMarked = true;
                atk.target.fallDistance = 0;
                atk.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));

                int jetPhase = atk.tick - jetsStart;
                for (int jet = 0; jet < 6; jet++) {
                    double jetAngle = (Math.PI * 2 / 6) * jet + jetPhase * 0.1;
                    for (double d = 4; d > 0; d -= 0.3) {
                        double jx = atk.startX + Math.cos(jetAngle) * d;
                        double jz = atk.startZ + Math.sin(jetAngle) * d;
                        atk.level.sendParticles(PURPLE_POISON,
                                jx, floatY + (Math.random() - 0.5) * 2, jz,
                                2, 0.05, 0.1, 0.05, 0.05);
                    }
                }

                // Purple poison cloud
                for (int i = 0; i < 15; i++) {
                    double px = atk.startX + (Math.random() - 0.5) * 3;
                    double py = floatY + (Math.random() - 0.5) * 2;
                    double pz = atk.startZ + (Math.random() - 0.5) * 3;
                    atk.level.sendParticles(DARK_PURPLE, px, py, pz, 1, 0.1, 0.1, 0.1, 0.02);
                    atk.level.sendParticles(ParticleTypes.WITCH, px, py, pz, 1, 0.1, 0.1, 0.1, 0.01);
                }

                if (jetPhase % 10 == 0) {
                    atk.target.hurt(atk.level.damageSources().magic(), 3.0f);
                    atk.target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 2));
                }

                if (jetPhase % 15 == 0) {
                    atk.level.playSound(null, BlockPos.containing(atk.startX, floatY, atk.startZ),
                            SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.5f);
                }
            }

            // === PHASE 3: TWO GIANT POISON DRAGONS ===
            int dragonsStart = jetsEnd;
            int dragonsEnd = dragonsStart + PoisonDragonAttack.DRAGONS_DURATION;

            if (atk.tick > dragonsStart && atk.tick <= dragonsEnd) {
                atk.target.teleportTo(atk.startX, floatY, atk.startZ);
                atk.target.setDeltaMovement(0, 0, 0);
                atk.target.hurtMarked = true;
                atk.target.fallDistance = 0;
                atk.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));

                int dragonPhase = atk.tick - dragonsStart;
                double approachProgress = (double) dragonPhase / PoisonDragonAttack.DRAGONS_DURATION;
                double startDist = 20.0;
                double currentDist = startDist * (1.0 - approachProgress);

                for (int side = 0; side < 2; side++) {
                    double sideAngle = side == 0 ? Math.PI / 2 : -Math.PI / 2;
                    double dragonX = atk.startX + Math.cos(sideAngle) * currentDist;
                    double dragonZ = atk.startZ + Math.sin(sideAngle) * currentDist;
                    double dragonY = floatY;

                    double dirX = atk.startX - dragonX;
                    double dirZ = atk.startZ - dragonZ;
                    double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
                    if (dirLen < 0.1) dirLen = 0.1;
                    dirX /= dirLen;
                    dirZ /= dirLen;
                    double perpX = -dirZ;
                    double perpZ = dirX;

                    // === DRAGON HEAD: triangular snout shape ===
                    // Snout tip (pointed forward)
                    for (int i = 0; i < 6; i++) {
                        double snoutLen = 0.3 * i;
                        double snoutWidth = i * 0.15;
                        double sx = dragonX + dirX * (1.5 - snoutLen);
                        double sz = dragonZ + dirZ * (1.5 - snoutLen);
                        for (double sw = -snoutWidth; sw <= snoutWidth; sw += 0.15) {
                            atk.level.sendParticles(DARK_PURPLE,
                                    sx + perpX * sw, dragonY + 0.1, sz + perpZ * sw,
                                    1, 0.02, 0.02, 0.02, 0.001);
                        }
                    }
                    // Skull (wider, behind snout)
                    for (double hx = -0.8; hx <= 0.8; hx += 0.2) {
                        for (double hy = -0.5; hy <= 0.6; hy += 0.2) {
                            atk.level.sendParticles(PURPLE_POISON,
                                    dragonX + perpX * hx, dragonY + hy, dragonZ + perpZ * hx,
                                    1, 0.03, 0.03, 0.03, 0.001);
                        }
                    }
                    // Horns (two curved horns going back and up)
                    for (int horn = -1; horn <= 1; horn += 2) {
                        for (double h = 0; h < 1.5; h += 0.15) {
                            double hornX = dragonX - dirX * h * 0.5 + perpX * horn * (0.6 + h * 0.3);
                            double hornZ = dragonZ - dirZ * h * 0.5 + perpZ * horn * (0.6 + h * 0.3);
                            double hornY = dragonY + 0.5 + h * 0.6;
                            atk.level.sendParticles(BRIGHT_PURPLE,
                                    hornX, hornY, hornZ, 1, 0.02, 0.02, 0.02, 0.001);
                        }
                    }
                    // Eyes (glowing purple)
                    atk.level.sendParticles(BRIGHT_PURPLE,
                            dragonX + perpX * 0.4 + dirX * 0.3, dragonY + 0.35, dragonZ + perpZ * 0.4 + dirZ * 0.3,
                            4, 0.03, 0.03, 0.03, 0.01);
                    atk.level.sendParticles(BRIGHT_PURPLE,
                            dragonX - perpX * 0.4 + dirX * 0.3, dragonY + 0.35, dragonZ - perpZ * 0.4 + dirZ * 0.3,
                            4, 0.03, 0.03, 0.03, 0.01);
                    // Open jaw with dripping purple poison
                    atk.level.sendParticles(ParticleTypes.WITCH,
                            dragonX + dirX * 1.2, dragonY - 0.3, dragonZ + dirZ * 1.2,
                            5, 0.15, 0.1, 0.15, 0.02);
                    // Poison breath trail from mouth
                    for (double b = 0; b < 2; b += 0.3) {
                        atk.level.sendParticles(PURPLE_POISON,
                                dragonX + dirX * (1.5 + b), dragonY - 0.1 + Math.sin(atk.tick * 0.5 + b) * 0.2,
                                dragonZ + dirZ * (1.5 + b),
                                2, 0.1, 0.1, 0.1, 0.02);
                    }

                    // === DRAGON NECK: sinuous, segmented ===
                    for (double seg = 0.5; seg < 4; seg += 0.25) {
                        double neckWobble = Math.sin(atk.tick * 0.3 + seg * 2) * 0.5;
                        double vertWobble = Math.sin(atk.tick * 0.2 + seg * 1.5) * 0.2;
                        double nx = dragonX - dirX * seg + perpX * neckWobble;
                        double nz = dragonZ - dirZ * seg + perpZ * neckWobble;
                        double ny = dragonY + vertWobble;
                        double thickness = 0.7 - seg * 0.05;
                        // Neck cross-section (ring)
                        for (double a = 0; a < Math.PI * 2; a += 0.8) {
                            double rx = perpX * Math.cos(a) * thickness * 0.5;
                            double ry = Math.sin(a) * thickness * 0.5;
                            double rz = perpZ * Math.cos(a) * thickness * 0.5;
                            atk.level.sendParticles(PURPLE_POISON,
                                    nx + rx, ny + ry, nz + rz, 1, 0.02, 0.02, 0.02, 0.001);
                        }
                        // Spine ridges on top
                        if (seg % 0.75 < 0.3) {
                            atk.level.sendParticles(BRIGHT_PURPLE,
                                    nx, ny + thickness * 0.6, nz, 2, 0.02, 0.05, 0.02, 0.001);
                        }
                    }

                    // === DRAGON BODY: larger, barrel-shaped ===
                    for (double seg = 4; seg < 9; seg += 0.3) {
                        double bodyWobble = Math.sin(atk.tick * 0.15 + seg) * 0.4;
                        double bx = dragonX - dirX * seg + perpX * bodyWobble;
                        double bz = dragonZ - dirZ * seg + perpZ * bodyWobble;
                        double by = dragonY + Math.sin(seg * 0.4) * 0.3;
                        // Body is wider in the middle
                        double bodyWidth = 1.4 - Math.abs(seg - 6.5) * 0.2;
                        double bodyHeight = bodyWidth * 0.7;
                        // Body cross-section
                        for (double a = 0; a < Math.PI * 2; a += 0.6) {
                            double rx = perpX * Math.cos(a) * bodyWidth * 0.5;
                            double ry = Math.sin(a) * bodyHeight * 0.5;
                            double rz = perpZ * Math.cos(a) * bodyWidth * 0.5;
                            atk.level.sendParticles(DARK_PURPLE,
                                    bx + rx, by + ry, bz + rz, 1, 0.03, 0.03, 0.03, 0.001);
                        }
                        // Belly scales (bottom, brighter)
                        atk.level.sendParticles(BRIGHT_PURPLE,
                                bx, by - bodyHeight * 0.4, bz, 1, bodyWidth * 0.2, 0.02, bodyWidth * 0.2, 0.001);
                    }

                    // === DRAGON WINGS: large, membrane-like ===
                    double wingFlap = Math.sin(atk.tick * 0.35 + side * Math.PI) * 1.2;
                    for (int wing = -1; wing <= 1; wing += 2) {
                        // Wing bone (leading edge)
                        for (double w = 0; w < 6; w += 0.3) {
                            double wingCurve = -w * w * 0.04 + wingFlap;
                            double wx = dragonX - dirX * (4 + w * 0.3) + perpX * wing * (w + 1.2);
                            double wz = dragonZ - dirZ * (4 + w * 0.3) + perpZ * wing * (w + 1.2);
                            double wy = dragonY + wingCurve + 0.8;
                            atk.level.sendParticles(DARK_PURPLE,
                                    wx, wy, wz, 2, 0.03, 0.02, 0.03, 0.002);
                            // Wing membrane (triangular fill between bone and body)
                            for (double m = 0; m < w * 0.4; m += 0.4) {
                                double memX = wx + dirX * m * 0.5;
                                double memZ = wz + dirZ * m * 0.5;
                                double memY = wy - m * 0.15;
                                atk.level.sendParticles(PURPLE_POISON,
                                        memX, memY, memZ, 1, 0.02, 0.02, 0.02, 0.001);
                            }
                        }
                        // Wing claw at tip
                        double tipX = dragonX - dirX * 5.8 + perpX * wing * 7;
                        double tipZ = dragonZ - dirZ * 5.8 + perpZ * wing * 7;
                        double tipY = dragonY + wingFlap - 1.0;
                        atk.level.sendParticles(BRIGHT_PURPLE,
                                tipX, tipY, tipZ, 3, 0.05, 0.05, 0.05, 0.01);
                    }

                    // === DRAGON TAIL: long, tapering, spiked ===
                    for (double t = 9; t < 16; t += 0.3) {
                        double tailWobble = Math.sin(atk.tick * 0.2 + t * 1.2) * (t - 9) * 0.12;
                        double vertTail = Math.sin(atk.tick * 0.15 + t * 0.8) * 0.3;
                        double tx = dragonX - dirX * t + perpX * tailWobble;
                        double tz = dragonZ - dirZ * t + perpZ * tailWobble;
                        double ty = dragonY + vertTail;
                        double tailWidth = Math.max(0.08, 0.8 - (t - 9) * 0.1);
                        atk.level.sendParticles(PURPLE_POISON,
                                tx, ty, tz, 1, tailWidth * 0.3, tailWidth * 0.2, tailWidth * 0.3, 0.002);
                        // Tail spikes every 2 blocks
                        if (t % 2 < 0.4) {
                            atk.level.sendParticles(BRIGHT_PURPLE,
                                    tx, ty + tailWidth + 0.3, tz, 2, 0.02, 0.1, 0.02, 0.005);
                        }
                    }
                    // Tail tip (sharp)
                    double tailEndX = dragonX - dirX * 16 + perpX * Math.sin(atk.tick * 0.2 + 16 * 1.2) * 0.8;
                    double tailEndZ = dragonZ - dirZ * 16 + perpZ * Math.sin(atk.tick * 0.2 + 16 * 1.2) * 0.8;
                    atk.level.sendParticles(BRIGHT_PURPLE,
                            tailEndX, dragonY, tailEndZ, 4, 0.05, 0.05, 0.05, 0.02);

                    // === DRAGON LEGS (4 legs, 2 front near neck, 2 back near tail) ===
                    double[][] legPositions = {{3, -1}, {3, 1}, {7, -1}, {7, 1}};
                    for (double[] leg : legPositions) {
                        double legSeg = leg[0];
                        double legSide = leg[1];
                        double lx = dragonX - dirX * legSeg + perpX * legSide * 0.8;
                        double lz = dragonZ - dirZ * legSeg + perpZ * legSide * 0.8;
                        for (double ld = 0; ld < 1.5; ld += 0.2) {
                            atk.level.sendParticles(DARK_PURPLE,
                                    lx + perpX * legSide * ld * 0.3,
                                    dragonY - ld,
                                    lz + perpZ * legSide * ld * 0.3,
                                    1, 0.02, 0.02, 0.02, 0.001);
                        }
                        // Claws
                        atk.level.sendParticles(BRIGHT_PURPLE,
                                lx + perpX * legSide * 0.5, dragonY - 1.5, lz + perpZ * legSide * 0.5,
                                2, 0.05, 0.02, 0.05, 0.005);
                    }

                    // Dragon roar
                    if (dragonPhase % 20 == 0) {
                        atk.level.playSound(null, BlockPos.containing(dragonX, dragonY, dragonZ),
                                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 2.0f, 0.4f);
                    }
                }
            }

            // === PHASE 4: EXPLOSION ===
            if (atk.tick == PoisonDragonAttack.EXPLOSION_TICK) {
                // Massive purple poison explosion
                for (int i = 0; i < 120; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double phi = Math.random() * Math.PI;
                    double dist = Math.random() * 6;
                    double ex = atk.startX + Math.cos(angle) * Math.sin(phi) * dist;
                    double ey = floatY + Math.cos(phi) * dist;
                    double ez = atk.startZ + Math.sin(angle) * Math.sin(phi) * dist;
                    atk.level.sendParticles(PURPLE_POISON, ex, ey, ez, 3, 0.2, 0.2, 0.2, 0.2);
                }
                // Purple explosion flash
                for (int i = 0; i < 50; i++) {
                    double ex = atk.startX + (Math.random() - 0.5) * 7;
                    double ey = floatY + (Math.random() - 0.5) * 7;
                    double ez = atk.startZ + (Math.random() - 0.5) * 7;
                    atk.level.sendParticles(BRIGHT_PURPLE, ex, ey, ez, 3, 0.3, 0.3, 0.3, 0.25);
                    atk.level.sendParticles(ParticleTypes.WITCH, ex, ey, ez, 2, 0.2, 0.2, 0.2, 0.1);
                }
                atk.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        atk.startX, floatY, atk.startZ, 3, 1, 1, 1, 0);
                // Purple poison rain
                for (int i = 0; i < 60; i++) {
                    double rx = atk.startX + (Math.random() - 0.5) * 10;
                    double rz = atk.startZ + (Math.random() - 0.5) * 10;
                    atk.level.sendParticles(DARK_PURPLE, rx, floatY + 4, rz, 2, 0.1, 0.5, 0.1, 0.08);
                    atk.level.sendParticles(ParticleTypes.WITCH, rx, floatY + 2, rz, 1, 0.1, 0.3, 0.1, 0.02);
                }

                // MASSIVE DAMAGE
                atk.target.hurt(atk.level.damageSources().magic(), 40.0f);
                atk.target.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 4));
                atk.target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
                atk.target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 3));
                atk.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));

                atk.target.setDeltaMovement(0, -1.5, 0);
                atk.target.hurtMarked = true;

                // Explosion sounds (no ender dragon)
                atk.level.playSound(null, BlockPos.containing(atk.startX, floatY, atk.startZ),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.6f);
                atk.level.playSound(null, BlockPos.containing(atk.startX, floatY, atk.startZ),
                        SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 2.0f, 0.3f);
                atk.level.playSound(null, BlockPos.containing(atk.startX, floatY, atk.startZ),
                        SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 2.0f, 0.3f);
            }
        }
    }
}
