package com.noxgg.elementalpower.world;

import net.minecraft.core.particles.ParticleTypes;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TimeDomeManager {
    private static final List<ActiveDome> activeDomes = new ArrayList<>();

    public static class ActiveDome {
        public final double x, y, z;
        public final double radius;
        public final ServerLevel level;
        public final ServerPlayer owner;
        public int ticksRemaining;

        public ActiveDome(double x, double y, double z, double radius,
                          ServerLevel level, ServerPlayer owner, int durationTicks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.level = level;
            this.owner = owner;
            this.ticksRemaining = durationTicks;
        }

        public boolean isInside(Entity entity) {
            double dx = entity.getX() - x;
            double dy = entity.getY() - y;
            double dz = entity.getZ() - z;
            return (dx * dx + dy * dy + dz * dz) <= radius * radius;
        }

        public double distFromCenter(Entity entity) {
            double dx = entity.getX() - x;
            double dy = entity.getY() - y;
            double dz = entity.getZ() - z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public static void addDome(ActiveDome dome) {
        activeDomes.add(dome);
    }

    public static void tick() {
        Iterator<ActiveDome> it = activeDomes.iterator();
        while (it.hasNext()) {
            ActiveDome dome = it.next();
            dome.ticksRemaining--;

            if (dome.ticksRemaining <= 0 || dome.level == null) {
                it.remove();
                continue;
            }

            // Every tick: repel entities at barrier edge (both directions — nothing enters or exits)
            // Every 2 ticks: render barrier particles
            // Every 10 ticks: re-paralyze mobs inside

            // === BARRIER: nothing enters, nothing exits (except owner) ===
            double r = dome.radius;
            AABB searchBox = new AABB(
                    dome.x - r - 5, dome.y - r - 5, dome.z - r - 5,
                    dome.x + r + 5, dome.y + r + 5, dome.z + r + 5);
            dome.level.getEntities(dome.owner, searchBox, e -> e != dome.owner).forEach(e -> {
                double dx = e.getX() - dome.x;
                double dy = e.getY() - dome.y;
                double dz = e.getZ() - dome.z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (dist < 0.1) return;

                double nx = dx / dist;
                double ny = dy / dist;
                double nz = dz / dist;

                boolean isInside = dist < r;
                boolean nearBarrier = Math.abs(dist - r) < 2.0;

                if (nearBarrier) {
                    if (isInside) {
                        // Entity inside approaching the edge — push back toward center
                        double safeR = r - 2.5;
                        e.moveTo(dome.x + nx * safeR, e.getY(), dome.z + nz * safeR);
                        e.setDeltaMovement(-nx * 0.5, 0.1, -nz * 0.5);
                    } else {
                        // Entity outside approaching the edge — push away from dome
                        double safeR = r + 2.5;
                        e.moveTo(dome.x + nx * safeR, e.getY(), dome.z + nz * safeR);
                        e.setDeltaMovement(nx * 0.5, 0.1, nz * 0.5);
                    }
                    e.hurtMarked = true;

                    // Barrier impact particles & sound
                    if (dome.ticksRemaining % 5 == 0) {
                        dome.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                e.getX(), e.getY() + 1, e.getZ(), 8, 0.2, 0.3, 0.2, 0.1);
                        dome.level.sendParticles(ParticleTypes.END_ROD,
                                e.getX(), e.getY() + 1, e.getZ(), 3, 0.1, 0.2, 0.1, 0.05);
                        dome.level.playSound(null, e.blockPosition(),
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 0.3f, 2.0f);
                    }
                }
            });

            // === RE-PARALYZE mobs inside every 10 ticks ===
            if (dome.ticksRemaining % 10 == 0) {
                dome.level.getEntities(dome.owner, searchBox,
                        e -> e != dome.owner && dome.isInside(e)).forEach(e -> {
                    if (e instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 127, false, false));
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 127, false, false));
                        living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 127, false, false));
                        living.setDeltaMovement(0, living.getDeltaMovement().y, 0);
                        living.hurtMarked = true;
                    }
                });
            }

            // === CRACKLING GREEN ENERGY BARRIER PARTICLES ===
            if (dome.ticksRemaining % 2 == 0) {
                renderDomeBarrier(dome);
            }
        }
    }

    private static void renderDomeBarrier(ActiveDome dome) {
        double r = dome.radius;
        int tick = 300 - dome.ticksRemaining; // animation timer

        // === Main dome shell: dense crackling green energy ===
        // Horizontal rings with electrical arcs
        for (double phi = 0.05; phi < Math.PI; phi += 0.12) {
            double ringRadius = Math.sin(phi) * r;
            double ringY = dome.y + Math.cos(phi) * r;
            int points = Math.max(16, (int)(ringRadius * 1.8));

            for (int p = 0; p < points; p++) {
                double theta = (Math.PI * 2.0 / points) * p;
                // Add wobble for crackling effect
                double wobble = Math.sin(tick * 0.5 + theta * 3 + phi * 7) * 0.4;
                double pr = ringRadius + wobble;
                double px = dome.x + Math.cos(theta) * pr;
                double pz = dome.z + Math.sin(theta) * pr;

                // Green end rod particles (main glow)
                dome.level.sendParticles(ParticleTypes.END_ROD,
                        px, ringY, pz, 1, 0, 0, 0, 0.001);
            }
        }

        // === Electric arcs crackling along the surface ===
        int numArcs = 8;
        for (int arc = 0; arc < numArcs; arc++) {
            double arcStart = ((tick * 0.1) + (Math.PI * 2.0 / numArcs) * arc) % (Math.PI * 2);
            double arcPhi = (Math.sin(tick * 0.07 + arc) * 0.5 + 0.5) * Math.PI;

            for (int seg = 0; seg < 12; seg++) {
                double theta = arcStart + seg * 0.15;
                double phi = arcPhi + Math.sin(seg * 1.5 + tick * 0.3) * 0.3;
                double px = dome.x + Math.cos(theta) * Math.sin(phi) * r;
                double py = dome.y + Math.cos(phi) * r;
                double pz = dome.z + Math.sin(theta) * Math.sin(phi) * r;

                // Electric spark particles (crackling energy)
                dome.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        px, py, pz, 3, 0.15, 0.15, 0.15, 0.05);
                // Occasional flash
                if (seg % 3 == 0) {
                    dome.level.sendParticles(ParticleTypes.FLASH,
                            px, py, pz, 1, 0, 0, 0, 0);
                }
            }
        }

        // === Vertical energy veins (slow rotating) ===
        for (int v = 0; v < 6; v++) {
            double baseAngle = (Math.PI * 2.0 / 6) * v + tick * 0.02;
            for (double phi = 0.1; phi < Math.PI; phi += 0.1) {
                double jitter = Math.sin(phi * 8 + tick * 0.4 + v) * 0.5;
                double px = dome.x + Math.cos(baseAngle + jitter * 0.1) * Math.sin(phi) * r;
                double py = dome.y + Math.cos(phi) * r;
                double pz = dome.z + Math.sin(baseAngle + jitter * 0.1) * Math.sin(phi) * r;

                dome.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        px + jitter * 0.3, py, pz + jitter * 0.3, 1, 0, 0, 0, 0);
            }
        }

        // === Ground ring: bright crackling circle ===
        for (double angle = 0; angle < Math.PI * 2; angle += 0.05) {
            double wobble = Math.sin(angle * 10 + tick * 0.6) * 0.3;
            double gx = dome.x + Math.cos(angle) * (r + wobble);
            double gz = dome.z + Math.sin(angle) * (r + wobble);
            dome.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    gx, dome.y + 0.3, gz, 1, 0.05, 0.1, 0.05, 0.02);
            if (angle % 0.3 < 0.06) {
                dome.level.sendParticles(ParticleTypes.END_ROD,
                        gx, dome.y + 0.3, gz, 1, 0, 0.2, 0, 0.01);
            }
        }

        // === Random energy bursts on surface ===
        for (int burst = 0; burst < 5; burst++) {
            double bTheta = dome.level.random.nextDouble() * Math.PI * 2;
            double bPhi = dome.level.random.nextDouble() * Math.PI;
            double bx = dome.x + Math.cos(bTheta) * Math.sin(bPhi) * r;
            double by = dome.y + Math.cos(bPhi) * r;
            double bz = dome.z + Math.sin(bTheta) * Math.sin(bPhi) * r;
            dome.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    bx, by, bz, 10, 0.3, 0.3, 0.3, 0.15);
        }

        // === Ambient hum every 20 ticks ===
        if (dome.ticksRemaining % 20 == 0) {
            dome.level.playSound(null,
                    new net.minecraft.core.BlockPos((int)dome.x, (int)dome.y, (int)dome.z),
                    SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, 1.5f, 0.3f);
        }
    }
}
