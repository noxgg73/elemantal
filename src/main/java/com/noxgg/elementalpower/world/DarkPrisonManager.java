package com.noxgg.elementalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DarkPrisonManager {
    private static final List<DarkPrison> activePrisons = new ArrayList<>();

    public static class DarkPrison {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final LivingEntity prisoner;
        public final double x, y, z;
        public final double radius;
        public int ticksAlive = 0;
        public static final int MAX_DURATION = 1200; // 60 sec max safety

        public DarkPrison(ServerLevel level, ServerPlayer caster, LivingEntity prisoner, double radius) {
            this.level = level;
            this.caster = caster;
            this.prisoner = prisoner;
            this.x = prisoner.getX();
            this.y = prisoner.getY();
            this.z = prisoner.getZ();
            this.radius = radius;
        }

        public boolean isInsidePrison(Entity entity) {
            double dx = entity.getX() - x;
            double dy = entity.getY() - y;
            double dz = entity.getZ() - z;
            return (dx * dx + dy * dy + dz * dz) <= radius * radius;
        }

        public boolean isCasterLookingAtPrisoner() {
            if (caster == null || caster.isRemoved() || prisoner == null || prisoner.isRemoved()) {
                return false;
            }
            Vec3 eyePos = caster.getEyePosition();
            Vec3 lookVec = caster.getLookAngle();
            Vec3 toPrisoner = new Vec3(x - eyePos.x, (y + 1) - eyePos.y, z - eyePos.z).normalize();

            // Dot product: 1.0 = looking directly, 0 = 90 degrees
            double dot = lookVec.dot(toPrisoner);
            // Allow generous angle (~60 degrees cone)
            return dot > 0.5;
        }
    }

    public static void addPrison(DarkPrison prison) {
        // Remove any existing prison from same caster
        activePrisons.removeIf(p -> p.caster.equals(prison.caster));
        activePrisons.add(prison);
    }

    public static void tick() {
        Iterator<DarkPrison> it = activePrisons.iterator();
        while (it.hasNext()) {
            DarkPrison prison = it.next();
            prison.ticksAlive++;

            // Check if prison should break
            if (prison.caster == null || prison.caster.isRemoved() ||
                    prison.prisoner == null || prison.prisoner.isRemoved() ||
                    prison.ticksAlive > DarkPrison.MAX_DURATION ||
                    !prison.caster.level().equals(prison.level)) {
                dissolvePrison(prison);
                it.remove();
                continue;
            }

            // Check if caster is still looking at the prisoner
            if (!prison.isCasterLookingAtPrisoner()) {
                dissolvePrison(prison);
                it.remove();
                continue;
            }

            // === KEEP PRISONER FROZEN ===
            if (prison.ticksAlive % 5 == 0) {
                prison.prisoner.setDeltaMovement(0, 0, 0);
                prison.prisoner.hurtMarked = true;
                prison.prisoner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 127, false, false));
                prison.prisoner.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 127, false, false));
                prison.prisoner.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false));
                prison.prisoner.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 1, false, false));
                // Keep prisoner at center
                prison.prisoner.teleportTo(prison.x, prison.y, prison.z);
            }

            // === REPEL anything trying to enter ===
            Entity nullEntity = null;
            prison.level.getEntities(nullEntity,
                    new net.minecraft.world.phys.AABB(
                            prison.x - prison.radius - 2, prison.y - prison.radius - 2, prison.z - prison.radius - 2,
                            prison.x + prison.radius + 2, prison.y + prison.radius + 2, prison.z + prison.radius + 2),
                    e -> e != prison.prisoner && e != prison.caster).forEach(e -> {
                double dx = e.getX() - prison.x;
                double dy = e.getY() - prison.y;
                double dz = e.getZ() - prison.z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                // Push away entities near the barrier
                if (dist < prison.radius + 1.5 && dist > prison.radius - 1.5) {
                    double len = Math.max(dist, 0.1);
                    e.setDeltaMovement(dx / len * 1.2, 0.2, dz / len * 1.2);
                    e.hurtMarked = true;
                    if (prison.ticksAlive % 10 == 0) {
                        prison.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                e.getX(), e.getY() + 1, e.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            });

            // === PREVENT PRISONER FROM ESCAPING ===
            if (!prison.isInsidePrison(prison.prisoner)) {
                prison.prisoner.teleportTo(prison.x, prison.y, prison.z);
            }

            // === RENDER DARK PRISON DOME ===
            if (prison.ticksAlive % 2 == 0) {
                renderPrison(prison);
            }
        }
    }

    private static void renderPrison(DarkPrison prison) {
        double r = prison.radius;
        int tick = prison.ticksAlive;

        // === DARK DOME SHELL: soul fire + smoke + sculk ===
        // Horizontal rings
        for (double phi = 0.1; phi < Math.PI; phi += 0.2) {
            double ringR = Math.sin(phi) * r;
            double ringY = prison.y + Math.cos(phi) * r;
            int points = Math.max(10, (int)(ringR * 3));
            for (int p = 0; p < points; p++) {
                double theta = (Math.PI * 2.0 / points) * p;
                double wobble = Math.sin(tick * 0.3 + theta * 5 + phi * 3) * 0.15;
                double px = prison.x + Math.cos(theta) * (ringR + wobble);
                double pz = prison.z + Math.sin(theta) * (ringR + wobble);
                prison.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        px, ringY, pz, 1, 0, 0, 0, 0.001);
            }
        }

        // === DARK VERTICAL CHAINS (like prison bars) ===
        for (int bar = 0; bar < 8; bar++) {
            double theta = (Math.PI * 2.0 / 8) * bar + tick * 0.01;
            for (double h = -r; h < r; h += 0.3) {
                double phi = Math.acos(Math.max(-1, Math.min(1, h / r)));
                double barR = Math.sin(phi) * r;
                double px = prison.x + Math.cos(theta) * barR;
                double pz = prison.z + Math.sin(theta) * barR;
                double py = prison.y + h;

                prison.level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        px, py, pz, 1, 0.02, 0.02, 0.02, 0.001);

                // Chain links (thicker every 1.5 blocks)
                if (Math.abs(h % 1.5) < 0.2) {
                    prison.level.sendParticles(ParticleTypes.SCULK_SOUL,
                            px, py, pz, 2, 0.05, 0.05, 0.05, 0.005);
                }
            }
        }

        // === DARK ENERGY CRACKLING on surface ===
        for (int arc = 0; arc < 4; arc++) {
            double aTheta = prison.level.random.nextDouble() * Math.PI * 2;
            double aPhi = prison.level.random.nextDouble() * Math.PI;
            double ax = prison.x + Math.cos(aTheta) * Math.sin(aPhi) * r;
            double ay = prison.y + Math.cos(aPhi) * r;
            double az = prison.z + Math.sin(aTheta) * Math.sin(aPhi) * r;
            prison.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    ax, ay, az, 5, 0.2, 0.2, 0.2, 0.03);
            prison.level.sendParticles(ParticleTypes.SMOKE,
                    ax, ay, az, 3, 0.15, 0.15, 0.15, 0.02);
        }

        // === GROUND SEAL (dark circle) ===
        for (double angle = 0; angle < Math.PI * 2; angle += 0.1) {
            double gx = prison.x + Math.cos(angle) * r;
            double gz = prison.z + Math.sin(angle) * r;
            prison.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    gx, prison.y + 0.1, gz, 1, 0.02, 0, 0.02, 0.001);
        }
        // Inner pentagram
        for (int i = 0; i < 5; i++) {
            double a1 = (Math.PI * 2 / 5) * i + tick * 0.005;
            double a2 = (Math.PI * 2 / 5) * ((i + 2) % 5) + tick * 0.005;
            for (double t = 0; t < 1; t += 0.05) {
                double lx = prison.x + Math.cos(a1) * (r * 0.8) * (1 - t) + Math.cos(a2) * (r * 0.8) * t;
                double lz = prison.z + Math.sin(a1) * (r * 0.8) * (1 - t) + Math.sin(a2) * (r * 0.8) * t;
                prison.level.sendParticles(ParticleTypes.SCULK_SOUL,
                        lx, prison.y + 0.15, lz, 1, 0, 0, 0, 0);
            }
        }

        // === PRISONER SOUL DRAIN effect ===
        if (tick % 10 == 0) {
            prison.level.sendParticles(ParticleTypes.SOUL,
                    prison.prisoner.getX(), prison.prisoner.getY() + 1, prison.prisoner.getZ(),
                    5, 0.2, 0.5, 0.2, 0.05);
            prison.level.sendParticles(ParticleTypes.SCULK_SOUL,
                    prison.prisoner.getX(), prison.prisoner.getY() + 0.5, prison.prisoner.getZ(),
                    3, 0.1, 0.3, 0.1, 0.02);
        }

        // === AMBIENT SOUND ===
        if (tick % 40 == 0) {
            prison.level.playSound(null, BlockPos.containing(prison.x, prison.y, prison.z),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.0f, 0.5f);
        }
    }

    private static void dissolvePrison(DarkPrison prison) {
        if (prison.level == null) return;

        // Prison breaking effect
        for (int i = 0; i < 80; i++) {
            double theta = prison.level.random.nextDouble() * Math.PI * 2;
            double phi = prison.level.random.nextDouble() * Math.PI;
            double px = prison.x + Math.cos(theta) * Math.sin(phi) * prison.radius;
            double py = prison.y + Math.cos(phi) * prison.radius;
            double pz = prison.z + Math.sin(theta) * Math.sin(phi) * prison.radius;
            prison.level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    px, py, pz, 3, 0.3, 0.3, 0.3, 0.05);
            prison.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    px, py, pz, 1, 0.1, 0.1, 0.1, 0.05);
        }

        // Remove effects from prisoner
        if (prison.prisoner != null && !prison.prisoner.isRemoved()) {
            prison.prisoner.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            prison.prisoner.removeEffect(MobEffects.WEAKNESS);
            prison.prisoner.removeEffect(MobEffects.BLINDNESS);
            prison.prisoner.removeEffect(MobEffects.DARKNESS);
        }

        // Dissolve sound
        prison.level.playSound(null, BlockPos.containing(prison.x, prison.y, prison.z),
                SoundEvents.WARDEN_DEATH, SoundSource.AMBIENT, 1.0f, 0.8f);
    }
}
