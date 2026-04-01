package com.noxgg.elementalpower.world;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TimeAbyssManager {
    private static final List<TimeAbyss> activeAbysses = new ArrayList<>();

    // Phases:
    // 0 = PARALYZE (ticks 0-40): mob frozen, time vortex appears
    // 1 = ABYSS (ticks 40-140): mob sinks into ground, ages (gets wither/slowness, name changes)
    // 2 = RETURN (ticks 140-160): mob rises back up, visibly "aged"
    // 3 = BOMBARDMENT (ticks 160-260): green energy spheres rain down on mob
    // 4 = DONE

    public static class TimeAbyss {
        public final ServerLevel level;
        public final ServerPlayer owner;
        public final LivingEntity target;
        public final Vec3 originalPos;
        public int ticksElapsed;
        public int phase;
        public boolean targetHidden;

        public TimeAbyss(ServerLevel level, ServerPlayer owner, LivingEntity target) {
            this.level = level;
            this.owner = owner;
            this.target = target;
            this.originalPos = target.position();
            this.ticksElapsed = 0;
            this.phase = 0;
            this.targetHidden = false;
        }
    }

    public static void addAbyss(TimeAbyss abyss) {
        activeAbysses.add(abyss);
    }

    public static void tick() {
        Iterator<TimeAbyss> it = activeAbysses.iterator();
        while (it.hasNext()) {
            TimeAbyss abyss = it.next();
            abyss.ticksElapsed++;

            if (abyss.target == null || !abyss.target.isAlive() || abyss.level == null) {
                it.remove();
                continue;
            }

            if (abyss.phase == 0) {
                // === PHASE 0: PARALYZE (0-40 ticks) ===
                tickParalyze(abyss);
                if (abyss.ticksElapsed >= 40) {
                    abyss.phase = 1;
                }

            } else if (abyss.phase == 1) {
                // === PHASE 1: ABYSS - sink and age (40-140 ticks) ===
                tickAbyss(abyss);
                if (abyss.ticksElapsed >= 140) {
                    abyss.phase = 2;
                }

            } else if (abyss.phase == 2) {
                // === PHASE 2: RETURN - rise back up (140-160 ticks) ===
                tickReturn(abyss);
                if (abyss.ticksElapsed >= 160) {
                    abyss.phase = 3;
                }

            } else if (abyss.phase == 3) {
                // === PHASE 3: BOMBARDMENT - green energy spheres (160-260 ticks) ===
                tickBombardment(abyss);
                if (abyss.ticksElapsed >= 260) {
                    abyss.phase = 4;
                    it.remove();
                }
            }
        }
    }

    private static void tickParalyze(TimeAbyss abyss) {
        LivingEntity target = abyss.target;

        // Freeze the mob completely
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 127, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 127, false, false));

        // Time vortex particles spiraling around the target
        double tx = target.getX();
        double ty = target.getY() + target.getBbHeight() / 2;
        double tz = target.getZ();
        int tick = abyss.ticksElapsed;

        // Swirling clock-like particles
        for (int i = 0; i < 6; i++) {
            double angle = (tick * 0.3) + (Math.PI * 2.0 / 6) * i;
            double r = 1.5 - (tick * 0.02); // spiral inward
            double px = tx + Math.cos(angle) * r;
            double pz = tz + Math.sin(angle) * r;
            abyss.level.sendParticles(ParticleTypes.END_ROD,
                    px, ty, pz, 1, 0, 0, 0, 0.001);
            abyss.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    px, ty, pz, 2, 0.05, 0.05, 0.05, 0.02);
        }

        // Dust particles forming a vortex underneath
        var timeDust = new DustParticleOptions(new Vector3f(0.1f, 0.8f, 0.4f), 1.5f);
        for (int i = 0; i < 8; i++) {
            double angle = (tick * 0.5) + (Math.PI * 2.0 / 8) * i;
            double px = tx + Math.cos(angle) * 2.0;
            double pz = tz + Math.sin(angle) * 2.0;
            abyss.level.sendParticles(timeDust,
                    px, target.getY(), pz, 2, 0.1, 0.05, 0.1, 0.01);
        }

        // Sound: ticking clock getting faster
        if (tick % 5 == 0) {
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.HOSTILE, 0.8f, 1.5f + tick * 0.02f);
        }

        // Glowing effect
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
    }

    private static void tickAbyss(TimeAbyss abyss) {
        LivingEntity target = abyss.target;
        int localTick = abyss.ticksElapsed - 40;

        // Keep frozen
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 127, false, false));

        double tx = abyss.originalPos.x;
        double tz = abyss.originalPos.z;

        // Sink the mob into the ground slowly
        if (localTick < 40) {
            double sinkProgress = localTick / 40.0;
            double newY = abyss.originalPos.y - (sinkProgress * 3.0);
            target.teleportTo(tx, newY, tz);

            // Vortex particles where mob is sinking
            for (int i = 0; i < 12; i++) {
                double angle = (abyss.ticksElapsed * 0.4) + (Math.PI * 2.0 / 12) * i;
                double r = 2.0 * (1.0 - sinkProgress * 0.5);
                double px = tx + Math.cos(angle) * r;
                double pz = tz + Math.sin(angle) * r;
                abyss.level.sendParticles(ParticleTypes.PORTAL,
                        px, abyss.originalPos.y, pz, 3, 0.1, 0.5, 0.1, 0.3);
            }

            // Dark time portal on the ground
            var darkTimeDust = new DustParticleOptions(new Vector3f(0.05f, 0.2f, 0.15f), 2.0f);
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2.0 / 20) * i;
                double px = tx + Math.cos(angle) * 2.5;
                double pz = tz + Math.sin(angle) * 2.5;
                abyss.level.sendParticles(darkTimeDust,
                        px, abyss.originalPos.y + 0.1, pz, 1, 0, 0, 0, 0);
            }
        } else {
            // Mob is fully underground - apply aging effects
            target.teleportTo(tx, abyss.originalPos.y - 3.0, tz);
            target.setInvisible(true);
            abyss.targetHidden = true;

            // Aging damage (wither = aging)
            if (localTick % 20 == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 2, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 127, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 5, false, false));
            }

            // Surface effects - time rift remains visible
            var riftDust = new DustParticleOptions(new Vector3f(0.0f, 0.6f, 0.3f), 2.5f);
            for (int i = 0; i < 10; i++) {
                double angle = (abyss.ticksElapsed * 0.2) + (Math.PI * 2.0 / 10) * i;
                double px = tx + Math.cos(angle) * 1.5;
                double pz = tz + Math.sin(angle) * 1.5;
                abyss.level.sendParticles(riftDust,
                        px, abyss.originalPos.y + 0.5, pz, 2, 0.05, 0.3, 0.05, 0.01);
                abyss.level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        px, abyss.originalPos.y + 0.2, pz, 1, 0.1, 0.1, 0.1, 0.05);
            }

            // Deep rumbling sound
            if (localTick % 30 == 0) {
                abyss.level.playSound(null, target.blockPosition(),
                        SoundEvents.AMBIENT_CAVE.get(), SoundSource.HOSTILE, 1.5f, 0.3f);
            }
        }

        // Ambient time sound
        if (localTick % 10 == 0) {
            abyss.level.playSound(null,
                    new net.minecraft.core.BlockPos((int) tx, (int) abyss.originalPos.y, (int) tz),
                    SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.0f, 0.5f);
        }
    }

    private static void tickReturn(TimeAbyss abyss) {
        LivingEntity target = abyss.target;
        int localTick = abyss.ticksElapsed - 140;
        double progress = localTick / 20.0;

        double tx = abyss.originalPos.x;
        double tz = abyss.originalPos.z;

        // Rise back up from the abyss
        double newY = abyss.originalPos.y - 3.0 + (progress * 3.0);
        target.teleportTo(tx, newY, tz);
        target.setInvisible(false);
        abyss.targetHidden = false;

        // Keep weakened (aged)
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        // Rising particles - aged entity emerging
        var agedDust = new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.4f), 2.0f); // grey/old color
        for (int i = 0; i < 15; i++) {
            double angle = (abyss.ticksElapsed * 0.3) + (Math.PI * 2.0 / 15) * i;
            double r = 1.5 + Math.sin(abyss.ticksElapsed * 0.5 + i) * 0.3;
            double px = tx + Math.cos(angle) * r;
            double pz = tz + Math.sin(angle) * r;
            abyss.level.sendParticles(agedDust,
                    px, newY + 1, pz, 2, 0.1, 0.3, 0.1, 0.02);
            abyss.level.sendParticles(ParticleTypes.SMOKE,
                    px, newY + 0.5, pz, 1, 0.1, 0.2, 0.1, 0.01);
        }

        // Dramatic emergence sound
        if (localTick == 0) {
            abyss.level.playSound(null,
                    new net.minecraft.core.BlockPos((int) tx, (int) abyss.originalPos.y, (int) tz),
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.5f, 1.5f);
            abyss.level.playSound(null,
                    new net.minecraft.core.BlockPos((int) tx, (int) abyss.originalPos.y, (int) tz),
                    SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0f, 0.5f);
        }

        // Flash when fully emerged
        if (localTick == 19) {
            abyss.level.sendParticles(ParticleTypes.FLASH,
                    tx, abyss.originalPos.y + 1, tz, 3, 0, 0, 0, 0);
            abyss.level.playSound(null,
                    new net.minecraft.core.BlockPos((int) tx, (int) abyss.originalPos.y, (int) tz),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.0f, 1.5f);
        }
    }

    private static void tickBombardment(TimeAbyss abyss) {
        LivingEntity target = abyss.target;
        int localTick = abyss.ticksElapsed - 160;

        if (!target.isAlive()) return;

        double tx = target.getX();
        double ty = target.getY();
        double tz = target.getZ();

        // Keep the mob slowed (it's old and weak)
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, false));
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        // === GREEN ENERGY SPHERES bombardment ===
        // Every 5 ticks, a sphere rains down
        if (localTick % 5 == 0) {
            // Sphere falls from above
            double sphereStartY = ty + 10;
            var greenEnergy = new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.2f), 3.0f);
            var greenCore = new DustParticleOptions(new Vector3f(0.3f, 1.0f, 0.5f), 2.0f);
            var greenGlow = new DustParticleOptions(new Vector3f(0.1f, 0.8f, 0.3f), 1.5f);

            // Random offset so spheres don't all hit same spot
            double offsetX = (abyss.level.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (abyss.level.random.nextDouble() - 0.5) * 2.0;

            // Trail from sky to target
            for (int i = 0; i < 20; i++) {
                double progress = i / 20.0;
                double py = sphereStartY - (sphereStartY - ty - 1) * progress;
                double px = tx + offsetX * progress;
                double pz = tz + offsetZ * progress;

                // Green sphere trail
                abyss.level.sendParticles(greenEnergy,
                        px, py, pz, 2, 0.1, 0.1, 0.1, 0.01);
                abyss.level.sendParticles(greenGlow,
                        px, py, pz, 1, 0.05, 0.05, 0.05, 0.005);
            }

            // Impact sphere at target position
            for (int i = 0; i < 15; i++) {
                double angle = (Math.PI * 2.0 / 15) * i;
                double r = 1.0;
                double px = tx + Math.cos(angle) * r;
                double pz = tz + Math.sin(angle) * r;
                abyss.level.sendParticles(greenCore,
                        px, ty + 1, pz, 3, 0.2, 0.2, 0.2, 0.05);
            }

            // Explosion burst
            abyss.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    tx, ty + 1, tz, 15, 0.8, 0.8, 0.8, 0.1);
            abyss.level.sendParticles(ParticleTypes.END_ROD,
                    tx, ty + 1, tz, 8, 0.5, 0.5, 0.5, 0.1);

            // Damage the target
            target.hurt(abyss.level.damageSources().magic(), 4.0f);

            // Impact sound
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.0f, 0.8f);
        }

        // Continuous green aura around the target during bombardment
        if (localTick % 2 == 0) {
            var auraDust = new DustParticleOptions(new Vector3f(0.0f, 0.9f, 0.2f), 1.0f);
            for (int i = 0; i < 8; i++) {
                double angle = (abyss.ticksElapsed * 0.4) + (Math.PI * 2.0 / 8) * i;
                double r = 1.2;
                double px = tx + Math.cos(angle) * r;
                double pz = tz + Math.sin(angle) * r;
                double py = ty + 0.5 + Math.sin(abyss.ticksElapsed * 0.3 + i) * 0.5;
                abyss.level.sendParticles(auraDust,
                        px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Orbiting green spheres visible around target
        if (localTick % 3 == 0) {
            var orbitDust = new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.0f), 2.5f);
            int numOrbs = 4;
            for (int orb = 0; orb < numOrbs; orb++) {
                double angle = (abyss.ticksElapsed * 0.15) + (Math.PI * 2.0 / numOrbs) * orb;
                double orbR = 2.5;
                double orbY = ty + 2 + Math.sin(abyss.ticksElapsed * 0.2 + orb) * 1.5;
                double orbX = tx + Math.cos(angle) * orbR;
                double orbZ = tz + Math.sin(angle) * orbR;

                abyss.level.sendParticles(orbitDust,
                        orbX, orbY, orbZ, 3, 0.1, 0.1, 0.1, 0.01);
                abyss.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        orbX, orbY, orbZ, 2, 0.05, 0.05, 0.05, 0.02);
            }
        }

        // Big booming sounds periodically
        if (localTick % 20 == 0) {
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8f, 1.2f);
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.5f, 1.5f);
        }

        // Final explosion at the end
        if (localTick == 99) {
            var finalGreen = new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.1f), 4.0f);
            abyss.level.sendParticles(finalGreen,
                    tx, ty + 1, tz, 50, 2, 2, 2, 0.2);
            abyss.level.sendParticles(ParticleTypes.FLASH,
                    tx, ty + 1, tz, 5, 0, 0, 0, 0);
            abyss.level.sendParticles(ParticleTypes.EXPLOSION,
                    tx, ty + 1, tz, 3, 0.5, 0.5, 0.5, 0);
            abyss.level.sendParticles(ParticleTypes.END_ROD,
                    tx, ty + 2, tz, 30, 1.5, 1.5, 1.5, 0.15);

            // Final big damage
            target.hurt(abyss.level.damageSources().magic(), 10.0f);

            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.5f);
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0f, 0.8f);

            // Notify the caster
            if (abyss.owner != null && abyss.owner.isAlive()) {
                abyss.owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> L'Abime du Temps a consume sa proie!")
                        .withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
            }
        }
    }
}
