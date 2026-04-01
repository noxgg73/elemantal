package com.noxgg.elementalpower.world;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EarthBlasterManager {
    private static final List<EarthBlast> activeBlasts = new ArrayList<>();

    // Phases:
    // 0 = SUMMON (0-40 ticks): two giant skulls materialize on each side of player
    // 1 = AIM (40-70 ticks): skulls rotate to face the target, warning beams
    // 2 = FIRE (70-120 ticks): two massive beams converge on target
    // 3 = DONE

    public static class EarthBlast {
        public final ServerLevel level;
        public final ServerPlayer owner;
        public final LivingEntity target;
        public int ticksElapsed;
        public int phase;

        // Blaster positions (left and right of player)
        public double leftX, leftY, leftZ;
        public double rightX, rightY, rightZ;

        public EarthBlast(ServerLevel level, ServerPlayer owner, LivingEntity target) {
            this.level = level;
            this.owner = owner;
            this.target = target;
            this.ticksElapsed = 0;
            this.phase = 0;

            // Position blasters 6 blocks to the left and right of player, 3 blocks up
            Vec3 look = owner.getLookAngle();
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize();

            this.leftX = owner.getX() + side.x * 6;
            this.leftY = owner.getY() + 3;
            this.leftZ = owner.getZ() + side.z * 6;

            this.rightX = owner.getX() - side.x * 6;
            this.rightY = owner.getY() + 3;
            this.rightZ = owner.getZ() - side.z * 6;
        }
    }

    public static void addBlast(EarthBlast blast) {
        activeBlasts.add(blast);
    }

    public static void tick() {
        Iterator<EarthBlast> it = activeBlasts.iterator();
        while (it.hasNext()) {
            EarthBlast blast = it.next();
            blast.ticksElapsed++;

            if (blast.target == null || !blast.target.isAlive() || blast.level == null) {
                it.remove();
                continue;
            }

            if (blast.phase == 0) {
                tickSummon(blast);
                if (blast.ticksElapsed >= 40) blast.phase = 1;
            } else if (blast.phase == 1) {
                tickAim(blast);
                if (blast.ticksElapsed >= 70) blast.phase = 2;
            } else if (blast.phase == 2) {
                tickFire(blast);
                if (blast.ticksElapsed >= 120) {
                    blast.phase = 3;
                    it.remove();
                }
            }
        }
    }

    private static void tickSummon(EarthBlast blast) {
        // Giant skulls materializing with earth/stone particles
        var stoneDust = new DustParticleOptions(new Vector3f(0.5f, 0.4f, 0.3f), 3.0f);
        var darkDust = new DustParticleOptions(new Vector3f(0.3f, 0.25f, 0.15f), 4.0f);

        double progress = blast.ticksElapsed / 40.0;

        // Left blaster forming
        renderSkullFormation(blast.level, blast.leftX, blast.leftY, blast.leftZ,
                stoneDust, darkDust, progress, blast.ticksElapsed);

        // Right blaster forming
        renderSkullFormation(blast.level, blast.rightX, blast.rightY, blast.rightZ,
                stoneDust, darkDust, progress, blast.ticksElapsed);

        // Rumbling ground sounds
        if (blast.ticksElapsed % 10 == 0) {
            blast.level.playSound(null, blast.owner.blockPosition(),
                    SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 2.0f, 0.3f);
            blast.level.playSound(null, blast.owner.blockPosition(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 0.3f + (float)progress * 0.5f);
        }
    }

    private static void renderSkullFormation(ServerLevel level, double x, double y, double z,
                                              DustParticleOptions stone, DustParticleOptions dark,
                                              double progress, int tick) {
        // Skull grows in size as it forms
        double size = 2.0 * progress;

        // Main skull body (rectangular)
        for (int i = 0; i < (int)(15 * progress); i++) {
            double ox = (level.random.nextDouble() - 0.5) * size * 1.5;
            double oy = (level.random.nextDouble() - 0.5) * size;
            double oz = (level.random.nextDouble() - 0.5) * size * 1.5;
            level.sendParticles(stone, x + ox, y + oy, z + oz, 1, 0, 0, 0, 0);
        }

        // Eyes (glowing orange/brown)
        if (progress > 0.5) {
            var eyeDust = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.0f), 2.0f);
            level.sendParticles(eyeDust, x - 0.5, y + 0.3, z, 2, 0.1, 0.1, 0.1, 0.01);
            level.sendParticles(eyeDust, x + 0.5, y + 0.3, z, 2, 0.1, 0.1, 0.1, 0.01);
        }

        // Stone debris falling
        level.sendParticles(ParticleTypes.ASH,
                x, y - 1, z, 3, 0.5, 0.2, 0.5, 0.01);

        // Electric sparks building up
        if (progress > 0.7 && tick % 3 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x, y, z, 5, 0.8, 0.5, 0.8, 0.05);
        }
    }

    private static void tickAim(EarthBlast blast) {
        Vec3 targetPos = blast.target.position().add(0, blast.target.getBbHeight() / 2, 0);

        var stoneDust = new DustParticleOptions(new Vector3f(0.5f, 0.4f, 0.3f), 3.0f);
        var eyeDust = new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 2.5f);
        var warningDust = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.0f), 1.0f);

        // Render both skulls fully formed
        renderFullSkull(blast.level, blast.leftX, blast.leftY, blast.leftZ, stoneDust, eyeDust);
        renderFullSkull(blast.level, blast.rightX, blast.rightY, blast.rightZ, stoneDust, eyeDust);

        // Warning laser lines from each skull to target
        int localTick = blast.ticksElapsed - 40;
        if (localTick % 2 == 0) {
            // Left beam warning
            renderWarningLine(blast.level, blast.leftX, blast.leftY, blast.leftZ,
                    targetPos.x, targetPos.y, targetPos.z, warningDust);
            // Right beam warning
            renderWarningLine(blast.level, blast.rightX, blast.rightY, blast.rightZ,
                    targetPos.x, targetPos.y, targetPos.z, warningDust);
        }

        // Charging sound
        if (localTick % 10 == 0) {
            blast.level.playSound(null, blast.owner.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.5f + localTick * 0.02f);
        }

        // Freeze the target
        blast.target.setDeltaMovement(0, 0, 0);
        blast.target.hurtMarked = true;

        // Jaw opening animation - more electric sparks
        level_sparks(blast.level, blast.leftX, blast.leftY - 0.5, blast.leftZ, blast.ticksElapsed);
        level_sparks(blast.level, blast.rightX, blast.rightY - 0.5, blast.rightZ, blast.ticksElapsed);
    }

    private static void level_sparks(ServerLevel level, double x, double y, double z, int tick) {
        if (tick % 2 == 0) {
            var chargeDust = new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.0f), 1.5f);
            level.sendParticles(chargeDust, x, y, z, 3, 0.3, 0.3, 0.3, 0.02);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 4, 0.4, 0.3, 0.4, 0.08);
        }
    }

    private static void renderFullSkull(ServerLevel level, double x, double y, double z,
                                         DustParticleOptions body, DustParticleOptions eyes) {
        // Skull body
        for (int i = 0; i < 20; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 3.0;
            double oy = (level.random.nextDouble() - 0.5) * 2.0;
            double oz = (level.random.nextDouble() - 0.5) * 3.0;
            level.sendParticles(body, x + ox, y + oy, z + oz, 1, 0, 0, 0, 0);
        }
        // Eyes
        level.sendParticles(eyes, x - 0.6, y + 0.4, z, 3, 0.1, 0.1, 0.1, 0.01);
        level.sendParticles(eyes, x + 0.6, y + 0.4, z, 3, 0.1, 0.1, 0.1, 0.01);
    }

    private static void renderWarningLine(ServerLevel level, double x1, double y1, double z1,
                                           double x2, double y2, double z2, DustParticleOptions dust) {
        int steps = 20;
        for (int i = 0; i < steps; i++) {
            double t = i / (double)steps;
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            double pz = z1 + (z2 - z1) * t;
            level.sendParticles(dust, px, py, pz, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static void tickFire(EarthBlast blast) {
        int localTick = blast.ticksElapsed - 70;
        Vec3 targetPos = blast.target.position().add(0, blast.target.getBbHeight() / 2, 0);

        var beamCore = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.5f), 3.5f);
        var beamOuter = new DustParticleOptions(new Vector3f(0.8f, 0.5f, 0.1f), 2.5f);
        var beamGlow = new DustParticleOptions(new Vector3f(1.0f, 0.7f, 0.2f), 2.0f);

        // === LEFT BEAM ===
        renderBeam(blast.level, blast.leftX, blast.leftY, blast.leftZ,
                targetPos.x, targetPos.y, targetPos.z, beamCore, beamOuter, beamGlow, localTick);

        // === RIGHT BEAM ===
        renderBeam(blast.level, blast.rightX, blast.rightY, blast.rightZ,
                targetPos.x, targetPos.y, targetPos.z, beamCore, beamOuter, beamGlow, localTick);

        // Keep rendering skulls
        var stoneDust = new DustParticleOptions(new Vector3f(0.5f, 0.4f, 0.3f), 3.0f);
        var eyeFire = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.5f), 3.0f);
        renderFullSkull(blast.level, blast.leftX, blast.leftY, blast.leftZ, stoneDust, eyeFire);
        renderFullSkull(blast.level, blast.rightX, blast.rightY, blast.rightZ, stoneDust, eyeFire);

        // === DAMAGE TARGET ===
        if (localTick % 5 == 0) {
            float damage = 6.0f;
            blast.target.hurt(blast.level.damageSources().magic(), damage);
            blast.target.setDeltaMovement(0, 0, 0);
            blast.target.hurtMarked = true;
        }

        // === IMPACT EXPLOSION at target ===
        blast.level.sendParticles(ParticleTypes.EXPLOSION,
                targetPos.x, targetPos.y, targetPos.z, 1, 0.3, 0.3, 0.3, 0);
        blast.level.sendParticles(ParticleTypes.FLASH,
                targetPos.x, targetPos.y, targetPos.z, 1, 0, 0, 0, 0);
        blast.level.sendParticles(ParticleTypes.LAVA,
                targetPos.x, targetPos.y, targetPos.z, 3, 0.5, 0.5, 0.5, 0.1);

        // Impact particles ring at convergence point
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2.0 / 8) * i + localTick * 0.3;
            double r = 1.5;
            double px = targetPos.x + Math.cos(angle) * r;
            double pz = targetPos.z + Math.sin(angle) * r;
            blast.level.sendParticles(beamGlow, px, targetPos.y, pz, 2, 0.1, 0.1, 0.1, 0.02);
        }

        // Sounds
        if (localTick % 10 == 0) {
            blast.level.playSound(null, blast.target.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.5f);
            blast.level.playSound(null, blast.target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        if (localTick == 0) {
            blast.level.playSound(null, blast.owner.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.3f);
        }

        // Final explosion
        if (localTick == 49) {
            blast.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    targetPos.x, targetPos.y, targetPos.z, 1, 0, 0, 0, 0);
            var finalDust = new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.2f), 5.0f);
            blast.level.sendParticles(finalDust,
                    targetPos.x, targetPos.y, targetPos.z, 40, 2, 2, 2, 0.15);
            blast.level.sendParticles(ParticleTypes.END_ROD,
                    targetPos.x, targetPos.y + 1, targetPos.z, 25, 1.5, 1.5, 1.5, 0.2);

            blast.target.hurt(blast.level.damageSources().magic(), 15.0f);

            blast.level.playSound(null, blast.target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.3f);
            blast.level.playSound(null, blast.target.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.5f);

            if (blast.owner != null && blast.owner.isAlive()) {
                blast.owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Les Gaster Blasters se sont dechaines!")
                        .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
            }
        }
    }

    private static void renderBeam(ServerLevel level, double x1, double y1, double z1,
                                    double x2, double y2, double z2,
                                    DustParticleOptions core, DustParticleOptions outer,
                                    DustParticleOptions glow, int tick) {
        int steps = 30;
        for (int i = 0; i < steps; i++) {
            double t = i / (double)steps;
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            double pz = z1 + (z2 - z1) * t;

            // Thick beam: core + outer ring
            level.sendParticles(core, px, py, pz, 2, 0.05, 0.05, 0.05, 0);

            // Outer beam with wobble
            double wobble = Math.sin(tick * 0.5 + i * 0.3) * 0.3;
            level.sendParticles(outer, px + wobble, py, pz + wobble, 2, 0.15, 0.1, 0.15, 0.01);
            level.sendParticles(outer, px - wobble, py, pz - wobble, 2, 0.15, 0.1, 0.15, 0.01);

            // Scattered glow
            if (i % 3 == 0) {
                level.sendParticles(glow, px, py + 0.3, pz, 1, 0.2, 0.2, 0.2, 0.02);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 2, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }
}
