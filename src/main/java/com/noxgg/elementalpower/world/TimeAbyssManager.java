package com.noxgg.elementalpower.world;

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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class TimeAbyssManager {
    private static final List<TimeAbyss> activeAbysses = new ArrayList<>();
    private static final UUID AGING_HEALTH_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID AGING_SPEED_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    // Phases:
    // 0 = PARALYZE (ticks 0-40): mob frozen, time vortex appears
    // 1 = ABYSS (ticks 40-140): mob sinks into ground, ages
    // 2 = RETURN (ticks 140-160): mob rises back up, visibly aged
    // 3 = AGED WAIT (ticks 160-240): mob stands aged for 4 seconds before bombardment
    // 4 = BOMBARDMENT (ticks 240-340): green energy spheres rain down on mob
    // 5 = DONE

    public static class TimeAbyss {
        public final ServerLevel level;
        public final ServerPlayer owner;
        public final LivingEntity target;
        public final Vec3 originalPos;
        public final Component originalName;
        public final float originalMaxHealth;
        public int ticksElapsed;
        public int phase;
        public boolean targetHidden;
        public boolean agingApplied;

        public TimeAbyss(ServerLevel level, ServerPlayer owner, LivingEntity target) {
            this.level = level;
            this.owner = owner;
            this.target = target;
            this.originalPos = target.position();
            this.originalName = target.getCustomName();
            this.originalMaxHealth = target.getMaxHealth();
            this.ticksElapsed = 0;
            this.phase = 0;
            this.targetHidden = false;
            this.agingApplied = false;
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
                tickParalyze(abyss);
                if (abyss.ticksElapsed >= 40) abyss.phase = 1;

            } else if (abyss.phase == 1) {
                tickAbyss(abyss);
                if (abyss.ticksElapsed >= 140) abyss.phase = 2;

            } else if (abyss.phase == 2) {
                tickReturn(abyss);
                if (abyss.ticksElapsed >= 160) abyss.phase = 3;

            } else if (abyss.phase == 3) {
                // === PHASE 3: AGED WAIT - 4 seconds (160-240) ===
                tickAgedWait(abyss);
                if (abyss.ticksElapsed >= 240) abyss.phase = 4;

            } else if (abyss.phase == 4) {
                tickBombardment(abyss);
                if (abyss.ticksElapsed >= 340) {
                    abyss.phase = 5;
                    it.remove();
                }
            }
        }
    }

    private static void tickParalyze(TimeAbyss abyss) {
        LivingEntity target = abyss.target;

        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 127, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 127, false, false));

        double tx = target.getX();
        double ty = target.getY() + target.getBbHeight() / 2;
        double tz = target.getZ();
        int tick = abyss.ticksElapsed;

        for (int i = 0; i < 6; i++) {
            double angle = (tick * 0.3) + (Math.PI * 2.0 / 6) * i;
            double r = 1.5 - (tick * 0.02);
            double px = tx + Math.cos(angle) * r;
            double pz = tz + Math.sin(angle) * r;
            abyss.level.sendParticles(ParticleTypes.END_ROD, px, ty, pz, 1, 0, 0, 0, 0.001);
            abyss.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, ty, pz, 2, 0.05, 0.05, 0.05, 0.02);
        }

        var timeDust = new DustParticleOptions(new Vector3f(0.1f, 0.8f, 0.4f), 1.5f);
        for (int i = 0; i < 8; i++) {
            double angle = (tick * 0.5) + (Math.PI * 2.0 / 8) * i;
            double px = tx + Math.cos(angle) * 2.0;
            double pz = tz + Math.sin(angle) * 2.0;
            abyss.level.sendParticles(timeDust, px, target.getY(), pz, 2, 0.1, 0.05, 0.1, 0.01);
        }

        if (tick % 5 == 0) {
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.HOSTILE, 0.8f, 1.5f + tick * 0.02f);
        }

        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
    }

    private static void tickAbyss(TimeAbyss abyss) {
        LivingEntity target = abyss.target;
        int localTick = abyss.ticksElapsed - 40;

        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 127, false, false));

        double tx = abyss.originalPos.x;
        double tz = abyss.originalPos.z;

        if (localTick < 40) {
            double sinkProgress = localTick / 40.0;
            double newY = abyss.originalPos.y - (sinkProgress * 3.0);
            target.teleportTo(tx, newY, tz);

            for (int i = 0; i < 12; i++) {
                double angle = (abyss.ticksElapsed * 0.4) + (Math.PI * 2.0 / 12) * i;
                double r = 2.0 * (1.0 - sinkProgress * 0.5);
                double px = tx + Math.cos(angle) * r;
                double pz = tz + Math.sin(angle) * r;
                abyss.level.sendParticles(ParticleTypes.PORTAL, px, abyss.originalPos.y, pz, 3, 0.1, 0.5, 0.1, 0.3);
            }

            var darkTimeDust = new DustParticleOptions(new Vector3f(0.05f, 0.2f, 0.15f), 2.0f);
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2.0 / 20) * i;
                double px = tx + Math.cos(angle) * 2.5;
                double pz = tz + Math.sin(angle) * 2.5;
                abyss.level.sendParticles(darkTimeDust, px, abyss.originalPos.y + 0.1, pz, 1, 0, 0, 0, 0);
            }
        } else {
            // Mob fully underground - REAL aging
            target.teleportTo(tx, abyss.originalPos.y - 3.0, tz);
            target.setInvisible(true);
            abyss.targetHidden = true;

            // Apply aging: reduce max health, wither, extreme weakness
            if (!abyss.agingApplied) {
                abyss.agingApplied = true;

                // Reduce max health by 50% (aging drains vitality)
                var healthAttr = target.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.removeModifier(AGING_HEALTH_UUID);
                    healthAttr.addPermanentModifier(new AttributeModifier(
                            AGING_HEALTH_UUID, "time_abyss_aging",
                            -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }

                // Reduce movement speed permanently (old and frail)
                var speedAttr = target.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    speedAttr.removeModifier(AGING_SPEED_UUID);
                    speedAttr.addPermanentModifier(new AttributeModifier(
                            AGING_SPEED_UUID, "time_abyss_aging_speed",
                            -0.6, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }

                // Set custom name to show aging
                target.setCustomName(Component.literal("\u00A77\u00A7l\u00A7o[Vieilli] \u00A78" +
                        (abyss.originalName != null ? abyss.originalName.getString() : target.getType().getDescription().getString())));
                target.setCustomNameVisible(true);
            }

            if (localTick % 20 == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 2, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 127, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 5, false, false));
            }

            // Surface rift
            var riftDust = new DustParticleOptions(new Vector3f(0.0f, 0.6f, 0.3f), 2.5f);
            for (int i = 0; i < 10; i++) {
                double angle = (abyss.ticksElapsed * 0.2) + (Math.PI * 2.0 / 10) * i;
                double px = tx + Math.cos(angle) * 1.5;
                double pz = tz + Math.sin(angle) * 1.5;
                abyss.level.sendParticles(riftDust, px, abyss.originalPos.y + 0.5, pz, 2, 0.05, 0.3, 0.05, 0.01);
                abyss.level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, abyss.originalPos.y + 0.2, pz, 1, 0.1, 0.1, 0.1, 0.05);
            }

            if (localTick % 30 == 0) {
                abyss.level.playSound(null, target.blockPosition(),
                        SoundEvents.AMBIENT_CAVE.get(), SoundSource.HOSTILE, 1.5f, 0.3f);
            }
        }

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

        double newY = abyss.originalPos.y - 3.0 + (progress * 3.0);
        target.teleportTo(tx, newY, tz);
        target.setInvisible(false);
        abyss.targetHidden = false;

        // Aged debuffs
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 4, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 3, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0, false, false));
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        // Grey/white particles = old, decrepit appearance
        var agedDust = new DustParticleOptions(new Vector3f(0.7f, 0.7f, 0.7f), 2.0f); // grey = old
        var whiteDust = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.85f), 1.5f); // white hair
        for (int i = 0; i < 15; i++) {
            double angle = (abyss.ticksElapsed * 0.3) + (Math.PI * 2.0 / 15) * i;
            double r = 1.5 + Math.sin(abyss.ticksElapsed * 0.5 + i) * 0.3;
            double px = tx + Math.cos(angle) * r;
            double pz = tz + Math.sin(angle) * r;
            abyss.level.sendParticles(agedDust, px, newY + 1, pz, 2, 0.1, 0.3, 0.1, 0.02);
            abyss.level.sendParticles(whiteDust, px, newY + target.getBbHeight(), pz, 1, 0.15, 0.1, 0.15, 0.01);
            abyss.level.sendParticles(ParticleTypes.SMOKE, px, newY + 0.5, pz, 1, 0.1, 0.2, 0.1, 0.01);
        }

        if (localTick == 0) {
            abyss.level.playSound(null,
                    new net.minecraft.core.BlockPos((int) tx, (int) abyss.originalPos.y, (int) tz),
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.5f, 1.5f);
            abyss.level.playSound(null,
                    new net.minecraft.core.BlockPos((int) tx, (int) abyss.originalPos.y, (int) tz),
                    SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0f, 0.5f);

            // Announce the aged mob
            if (abyss.owner != null && abyss.owner.isAlive()) {
                abyss.owner.sendSystemMessage(Component.literal(">> La cible est revenue... vieillie et affaiblie!")
                        .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
            }
        }

        if (localTick == 19) {
            abyss.level.sendParticles(ParticleTypes.FLASH,
                    tx, abyss.originalPos.y + 1, tz, 3, 0, 0, 0, 0);
            abyss.level.playSound(null,
                    new net.minecraft.core.BlockPos((int) tx, (int) abyss.originalPos.y, (int) tz),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.0f, 1.5f);
        }
    }

    private static void tickAgedWait(TimeAbyss abyss) {
        LivingEntity target = abyss.target;
        int localTick = abyss.ticksElapsed - 160;

        // Keep the mob slow and weak - it's old now
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 3, false, false));

        double tx = target.getX();
        double ty = target.getY();
        double tz = target.getZ();

        // === AGED APPEARANCE PARTICLES ===
        // Grey dust falling off the mob (decaying skin/fur)
        var greyDust = new DustParticleOptions(new Vector3f(0.6f, 0.6f, 0.55f), 1.2f);
        var whiteDust = new DustParticleOptions(new Vector3f(0.85f, 0.85f, 0.8f), 0.8f);
        if (localTick % 3 == 0) {
            for (int i = 0; i < 5; i++) {
                double ox = (abyss.level.random.nextDouble() - 0.5) * 0.8;
                double oy = abyss.level.random.nextDouble() * target.getBbHeight();
                double oz = (abyss.level.random.nextDouble() - 0.5) * 0.8;
                // Grey particles falling = aging skin flaking off
                abyss.level.sendParticles(greyDust, tx + ox, ty + oy, tz + oz, 1, 0, -0.05, 0, 0.01);
                // White particles on top = white hair
                abyss.level.sendParticles(whiteDust,
                        tx + ox * 0.3, ty + target.getBbHeight() + 0.1, tz + oz * 0.3,
                        1, 0.1, 0.05, 0.1, 0.005);
            }
            // Smoke = decaying, old breath
            abyss.level.sendParticles(ParticleTypes.SMOKE,
                    tx, ty + target.getBbHeight() * 0.7, tz, 2, 0.15, 0.1, 0.15, 0.01);
        }

        // Crackling bones sound (old joints)
        if (localTick % 15 == 0) {
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 0.6f, 0.5f);
        }

        // Slow heartbeat
        if (localTick % 25 == 0) {
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.NOTE_BLOCK_BASEDRUM.get(), SoundSource.HOSTILE, 1.0f, 0.3f);
        }

        // Countdown warning before bombardment: green sparks intensify
        if (localTick > 50) {
            double intensity = (localTick - 50) / 30.0;
            var warnGreen = new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.2f), (float)(1.0 + intensity));
            int count = (int)(2 + intensity * 6);
            for (int i = 0; i < count; i++) {
                double angle = (abyss.ticksElapsed * 0.5) + (Math.PI * 2.0 / count) * i;
                double r = 3.0 - intensity;
                double px = tx + Math.cos(angle) * r;
                double pz = tz + Math.sin(angle) * r;
                double py = ty + 3 + Math.sin(abyss.ticksElapsed * 0.3 + i) * 1.5;
                abyss.level.sendParticles(warnGreen, px, py, pz, 2, 0.1, 0.1, 0.1, 0.02);
            }

            // Warning sound crescendo
            if (localTick % 10 == 0) {
                abyss.level.playSound(null, target.blockPosition(),
                        SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.HOSTILE, 0.5f + (float)intensity * 0.5f, 1.0f + (float)intensity * 0.5f);
            }
        }

        // At the end: announce bombardment
        if (localTick == 79) {
            if (abyss.owner != null && abyss.owner.isAlive()) {
                abyss.owner.sendSystemMessage(Component.literal(">> Bombardement temporel imminent!")
                        .withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
            }
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 1.5f);
        }
    }

    private static void tickBombardment(TimeAbyss abyss) {
        LivingEntity target = abyss.target;
        int localTick = abyss.ticksElapsed - 240;

        if (!target.isAlive()) return;

        double tx = target.getX();
        double ty = target.getY();
        double tz = target.getZ();

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, false));
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        // Green energy spheres every 5 ticks
        if (localTick % 5 == 0) {
            double sphereStartY = ty + 10;
            var greenEnergy = new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.2f), 3.0f);
            var greenCore = new DustParticleOptions(new Vector3f(0.3f, 1.0f, 0.5f), 2.0f);
            var greenGlow = new DustParticleOptions(new Vector3f(0.1f, 0.8f, 0.3f), 1.5f);

            double offsetX = (abyss.level.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (abyss.level.random.nextDouble() - 0.5) * 2.0;

            for (int i = 0; i < 20; i++) {
                double progress = i / 20.0;
                double py = sphereStartY - (sphereStartY - ty - 1) * progress;
                double px = tx + offsetX * progress;
                double pz = tz + offsetZ * progress;
                abyss.level.sendParticles(greenEnergy, px, py, pz, 2, 0.1, 0.1, 0.1, 0.01);
                abyss.level.sendParticles(greenGlow, px, py, pz, 1, 0.05, 0.05, 0.05, 0.005);
            }

            for (int i = 0; i < 15; i++) {
                double angle = (Math.PI * 2.0 / 15) * i;
                double px = tx + Math.cos(angle) * 1.0;
                double pz = tz + Math.sin(angle) * 1.0;
                abyss.level.sendParticles(greenCore, px, ty + 1, pz, 3, 0.2, 0.2, 0.2, 0.05);
            }

            abyss.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tx, ty + 1, tz, 15, 0.8, 0.8, 0.8, 0.1);
            abyss.level.sendParticles(ParticleTypes.END_ROD, tx, ty + 1, tz, 8, 0.5, 0.5, 0.5, 0.1);

            target.hurt(abyss.level.damageSources().magic(), 4.0f);

            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.0f, 0.8f);
        }

        // Green aura
        if (localTick % 2 == 0) {
            var auraDust = new DustParticleOptions(new Vector3f(0.0f, 0.9f, 0.2f), 1.0f);
            for (int i = 0; i < 8; i++) {
                double angle = (abyss.ticksElapsed * 0.4) + (Math.PI * 2.0 / 8) * i;
                double px = tx + Math.cos(angle) * 1.2;
                double pz = tz + Math.sin(angle) * 1.2;
                double py = ty + 0.5 + Math.sin(abyss.ticksElapsed * 0.3 + i) * 0.5;
                abyss.level.sendParticles(auraDust, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Orbiting spheres
        if (localTick % 3 == 0) {
            var orbitDust = new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.0f), 2.5f);
            for (int orb = 0; orb < 4; orb++) {
                double angle = (abyss.ticksElapsed * 0.15) + (Math.PI * 2.0 / 4) * orb;
                double orbY = ty + 2 + Math.sin(abyss.ticksElapsed * 0.2 + orb) * 1.5;
                double orbX = tx + Math.cos(angle) * 2.5;
                double orbZ = tz + Math.sin(angle) * 2.5;
                abyss.level.sendParticles(orbitDust, orbX, orbY, orbZ, 3, 0.1, 0.1, 0.1, 0.01);
                abyss.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, orbX, orbY, orbZ, 2, 0.05, 0.05, 0.05, 0.02);
            }
        }

        if (localTick % 20 == 0) {
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8f, 1.2f);
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.5f, 1.5f);
        }

        // Final explosion
        if (localTick == 99) {
            var finalGreen = new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.1f), 4.0f);
            abyss.level.sendParticles(finalGreen, tx, ty + 1, tz, 50, 2, 2, 2, 0.2);
            abyss.level.sendParticles(ParticleTypes.FLASH, tx, ty + 1, tz, 5, 0, 0, 0, 0);
            abyss.level.sendParticles(ParticleTypes.EXPLOSION, tx, ty + 1, tz, 3, 0.5, 0.5, 0.5, 0);
            abyss.level.sendParticles(ParticleTypes.END_ROD, tx, ty + 2, tz, 30, 1.5, 1.5, 1.5, 0.15);

            target.hurt(abyss.level.damageSources().magic(), 10.0f);

            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.5f);
            abyss.level.playSound(null, target.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0f, 0.8f);

            if (abyss.owner != null && abyss.owner.isAlive()) {
                abyss.owner.sendSystemMessage(Component.literal(">> L'Abime du Temps a consume sa proie!")
                        .withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
            }
        }
    }
}
