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

    public static class EarthBlast {
        public final ServerLevel level;
        public final ServerPlayer owner;
        public final LivingEntity target;
        public int ticksElapsed;
        public int phase;

        // Blaster positions
        public double leftX, leftY, leftZ;
        public double rightX, rightY, rightZ;
        // Direction each blaster faces (toward target)
        public Vec3 leftDir, rightDir;
        // Perpendicular vectors for skull width
        public Vec3 leftPerp, rightPerp;

        public EarthBlast(ServerLevel level, ServerPlayer owner, LivingEntity target) {
            this.level = level;
            this.owner = owner;
            this.target = target;
            this.ticksElapsed = 0;
            this.phase = 0;

            Vec3 look = owner.getLookAngle();
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize();

            this.leftX = owner.getX() + side.x * 7;
            this.leftY = owner.getY() + 3.5;
            this.leftZ = owner.getZ() + side.z * 7;

            this.rightX = owner.getX() - side.x * 7;
            this.rightY = owner.getY() + 3.5;
            this.rightZ = owner.getZ() - side.z * 7;

            // Direction from each blaster to target
            Vec3 tPos = target.position().add(0, target.getBbHeight() / 2, 0);
            this.leftDir = tPos.subtract(leftX, leftY, leftZ).normalize();
            this.rightDir = tPos.subtract(rightX, rightY, rightZ).normalize();

            // Perpendicular (horizontal) for skull width
            this.leftPerp = new Vec3(-leftDir.z, 0, leftDir.x).normalize();
            this.rightPerp = new Vec3(-rightDir.z, 0, rightDir.x).normalize();
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

    // ==========================================
    // GASTER BLASTER SKULL RENDERING
    // Reproduces the iconic skull shape:
    // - Elongated dragon/beast cranium
    // - Two large eye sockets (glowing cyan/orange)
    // - Nasal cavity
    // - Snout/muzzle pointing toward target
    // - Lower jaw that opens before firing
    // ==========================================

    /**
     * Render a full Gaster Blaster skull facing a direction.
     * @param jawOpen 0.0 = closed, 1.0 = fully open
     * @param firing whether the blaster is currently firing
     */
    private static void renderGasterBlaster(ServerLevel level, double cx, double cy, double cz,
                                             Vec3 forward, Vec3 perp, double scale,
                                             double jawOpen, boolean firing, int tick) {
        Vec3 up = new Vec3(0, 1, 0);
        var bone = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.85f), 2.5f * (float)scale);
        var boneEdge = new DustParticleOptions(new Vector3f(0.7f, 0.7f, 0.65f), 2.0f * (float)scale);
        var eyeColor = firing
                ? new DustParticleOptions(new Vector3f(0.0f, 1.0f, 1.0f), 3.0f * (float)scale)  // cyan when firing
                : new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 2.5f * (float)scale); // orange when aiming

        double s = scale;

        // === CRANIUM (back of skull - rounded dome) ===
        // Top dome
        for (double a = 0; a < Math.PI * 2; a += 0.4) {
            for (double h = 0; h < 1.2; h += 0.4) {
                double r = (1.3 - h * 0.4) * s;
                double px = cx - forward.x * 0.5 * s + perp.x * Math.cos(a) * r;
                double py = cy + (0.5 + h) * s;
                double pz = cz - forward.z * 0.5 * s + perp.z * Math.cos(a) * r;
                level.sendParticles(bone, px, py, pz, 1, 0, 0, 0, 0);
            }
        }

        // === BROW RIDGE (above eyes - thick ridge) ===
        for (double t = -1.2; t <= 1.2; t += 0.3) {
            double px = cx + perp.x * t * s + forward.x * 0.3 * s;
            double py = cy + 1.0 * s;
            double pz = cz + perp.z * t * s + forward.z * 0.3 * s;
            level.sendParticles(bone, px, py, pz, 1, 0, 0, 0, 0);
            level.sendParticles(boneEdge, px, py + 0.15 * s, pz, 1, 0, 0, 0, 0);
        }

        // === EYE SOCKETS (two large hollow eyes) ===
        // Left eye
        double leX = cx + perp.x * 0.6 * s + forward.x * 0.5 * s;
        double leY = cy + 0.65 * s;
        double leZ = cz + perp.z * 0.6 * s + forward.z * 0.5 * s;
        // Eye socket border (bone ring)
        for (double a = 0; a < Math.PI * 2; a += 0.5) {
            double r = 0.4 * s;
            level.sendParticles(boneEdge,
                    leX + perp.x * Math.cos(a) * r, leY + Math.sin(a) * r,
                    leZ + perp.z * Math.cos(a) * r, 1, 0, 0, 0, 0);
        }
        // Eye glow (fills socket)
        level.sendParticles(eyeColor, leX, leY, leZ, 3, 0.1 * s, 0.1 * s, 0.1 * s, 0.01);
        if (firing) {
            level.sendParticles(eyeColor, leX, leY + 0.3 * s, leZ, 2, 0.05, 0.2 * s, 0.05, 0.02);
        }

        // Right eye
        double reX = cx - perp.x * 0.6 * s + forward.x * 0.5 * s;
        double reY = cy + 0.65 * s;
        double reZ = cz - perp.z * 0.6 * s + forward.z * 0.5 * s;
        for (double a = 0; a < Math.PI * 2; a += 0.5) {
            double r = 0.4 * s;
            level.sendParticles(boneEdge,
                    reX + perp.x * Math.cos(a) * r, reY + Math.sin(a) * r,
                    reZ + perp.z * Math.cos(a) * r, 1, 0, 0, 0, 0);
        }
        level.sendParticles(eyeColor, reX, reY, reZ, 3, 0.1 * s, 0.1 * s, 0.1 * s, 0.01);
        if (firing) {
            level.sendParticles(eyeColor, reX, reY + 0.3 * s, reZ, 2, 0.05, 0.2 * s, 0.05, 0.02);
        }

        // === NASAL CAVITY (small dark triangle between eyes and snout) ===
        double noseX = cx + forward.x * 0.8 * s;
        double noseY = cy + 0.4 * s;
        double noseZ = cz + forward.z * 0.8 * s;
        var noseDark = new DustParticleOptions(new Vector3f(0.3f, 0.3f, 0.3f), 1.5f * (float)s);
        level.sendParticles(noseDark, noseX, noseY, noseZ, 2, 0.08 * s, 0.05 * s, 0.08 * s, 0);

        // === SNOUT / MUZZLE (upper jaw - extends forward) ===
        for (double d = 0.6; d <= 2.0; d += 0.3) {
            double width = (1.0 - (d - 0.6) * 0.25) * s; // narrows toward tip
            double height = 0.3 * s;
            double mx = cx + forward.x * d * s;
            double my = cy + 0.15 * s;
            double mz = cz + forward.z * d * s;

            // Top of snout
            level.sendParticles(bone, mx, my + height, mz, 1, 0, 0, 0, 0);
            // Sides
            level.sendParticles(bone, mx + perp.x * width * 0.5, my, mz + perp.z * width * 0.5, 1, 0, 0, 0, 0);
            level.sendParticles(bone, mx - perp.x * width * 0.5, my, mz - perp.z * width * 0.5, 1, 0, 0, 0, 0);
            // Bottom of upper jaw
            level.sendParticles(boneEdge, mx, my - 0.1 * s, mz, 1, 0, 0, 0, 0);
        }

        // === UPPER TEETH (hanging from upper jaw) ===
        var toothColor = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.95f), 1.5f * (float)s);
        for (int t = 0; t < 5; t++) {
            double d = 0.8 + t * 0.3;
            double tx = cx + forward.x * d * s;
            double tz = cz + forward.z * d * s;
            double tWidth = (0.8 - t * 0.1) * s;
            // Left tooth
            level.sendParticles(toothColor,
                    tx + perp.x * tWidth * 0.3, cy - 0.1 * s, tz + perp.z * tWidth * 0.3,
                    1, 0, 0, 0, 0);
            // Right tooth
            level.sendParticles(toothColor,
                    tx - perp.x * tWidth * 0.3, cy - 0.1 * s, tz - perp.z * tWidth * 0.3,
                    1, 0, 0, 0, 0);
        }

        // === LOWER JAW (opens downward based on jawOpen) ===
        double jawAngle = jawOpen * 0.8; // radians of jaw opening
        double jawDropY = Math.sin(jawAngle) * 1.5 * s;
        double jawForwardShift = (1.0 - Math.cos(jawAngle)) * 0.5 * s;

        for (double d = 0.4; d <= 1.8; d += 0.3) {
            double width = (0.9 - (d - 0.4) * 0.2) * s;
            double jx = cx + forward.x * (d * s - jawForwardShift);
            double jy = cy - 0.3 * s - jawDropY * (d / 1.8);
            double jz = cz + forward.z * (d * s - jawForwardShift);

            // Bottom of jaw
            level.sendParticles(bone, jx, jy, jz, 1, 0, 0, 0, 0);
            // Sides
            level.sendParticles(boneEdge, jx + perp.x * width * 0.4, jy + 0.1 * s, jz + perp.z * width * 0.4, 1, 0, 0, 0, 0);
            level.sendParticles(boneEdge, jx - perp.x * width * 0.4, jy + 0.1 * s, jz - perp.z * width * 0.4, 1, 0, 0, 0, 0);
        }

        // === LOWER TEETH (pointing up from lower jaw) ===
        for (int t = 0; t < 4; t++) {
            double d = 0.7 + t * 0.3;
            double tx = cx + forward.x * (d * s - jawForwardShift);
            double ty = cy - 0.2 * s - jawDropY * (d / 1.8);
            double tz = cz + forward.z * (d * s - jawForwardShift);
            double tWidth = (0.7 - t * 0.1) * s;
            level.sendParticles(toothColor,
                    tx + perp.x * tWidth * 0.25, ty + 0.15 * s, tz + perp.z * tWidth * 0.25,
                    1, 0, 0, 0, 0);
            level.sendParticles(toothColor,
                    tx - perp.x * tWidth * 0.25, ty + 0.15 * s, tz - perp.z * tWidth * 0.25,
                    1, 0, 0, 0, 0);
        }

        // === ENERGY CHARGING IN MOUTH (when jaw is open) ===
        if (jawOpen > 0.3) {
            double mouthX = cx + forward.x * 1.2 * s;
            double mouthY = cy - 0.1 * s - jawDropY * 0.5;
            double mouthZ = cz + forward.z * 1.2 * s;

            var chargeDust = firing
                    ? new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 3.0f * (float)s)
                    : new DustParticleOptions(new Vector3f(0.5f, 0.8f, 1.0f), 2.0f * (float)s);
            level.sendParticles(chargeDust, mouthX, mouthY, mouthZ,
                    firing ? 5 : 2, 0.15 * s, 0.15 * s, 0.15 * s, 0.02);

            if (firing) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        mouthX, mouthY, mouthZ, 3, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // === DECORATIVE SIDE HORNS/RIDGES ===
        for (int horn = 0; horn < 2; horn++) {
            double sign = horn == 0 ? 1 : -1;
            double hx = cx + perp.x * sign * 1.3 * s - forward.x * 0.2 * s;
            double hy = cy + 1.2 * s;
            double hz = cz + perp.z * sign * 1.3 * s - forward.z * 0.2 * s;
            level.sendParticles(boneEdge, hx, hy, hz, 1, 0, 0, 0, 0);
            level.sendParticles(boneEdge, hx + perp.x * sign * 0.3 * s, hy + 0.3 * s,
                    hz + perp.z * sign * 0.3 * s, 1, 0, 0, 0, 0);
        }
    }

    // ==========================================
    // PHASE TICKS
    // ==========================================

    private static void tickSummon(EarthBlast blast) {
        double progress = blast.ticksElapsed / 40.0;

        // Render skulls forming (scale grows, jaw closed)
        renderGasterBlaster(blast.level, blast.leftX, blast.leftY, blast.leftZ,
                blast.leftDir, blast.leftPerp, progress * 1.5, 0, false, blast.ticksElapsed);
        renderGasterBlaster(blast.level, blast.rightX, blast.rightY, blast.rightZ,
                blast.rightDir, blast.rightPerp, progress * 1.5, 0, false, blast.ticksElapsed);

        // Formation particles swirling around each blaster
        var formDust = new DustParticleOptions(new Vector3f(0.8f, 0.8f, 0.75f), 1.5f);
        for (int i = 0; i < 6; i++) {
            double angle = (blast.ticksElapsed * 0.3) + (Math.PI * 2.0 / 6) * i;
            double r = 2.5 * (1.0 - progress);
            // Left
            blast.level.sendParticles(formDust,
                    blast.leftX + Math.cos(angle) * r, blast.leftY + Math.sin(angle * 0.5) * 0.5,
                    blast.leftZ + Math.sin(angle) * r, 1, 0, 0, 0, 0);
            // Right
            blast.level.sendParticles(formDust,
                    blast.rightX + Math.cos(angle) * r, blast.rightY + Math.sin(angle * 0.5) * 0.5,
                    blast.rightZ + Math.sin(angle) * r, 1, 0, 0, 0, 0);
        }

        if (blast.ticksElapsed % 8 == 0) {
            blast.level.playSound(null, blast.owner.blockPosition(),
                    SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 2.0f, 0.3f + (float)progress * 0.5f);
        }
    }

    private static void tickAim(EarthBlast blast) {
        int localTick = blast.ticksElapsed - 40;
        double jawProgress = localTick / 30.0; // jaw opens over aim phase

        Vec3 targetPos = blast.target.position().add(0, blast.target.getBbHeight() / 2, 0);

        // Render blasters with jaw opening
        renderGasterBlaster(blast.level, blast.leftX, blast.leftY, blast.leftZ,
                blast.leftDir, blast.leftPerp, 1.5, jawProgress, false, blast.ticksElapsed);
        renderGasterBlaster(blast.level, blast.rightX, blast.rightY, blast.rightZ,
                blast.rightDir, blast.rightPerp, 1.5, jawProgress, false, blast.ticksElapsed);

        // Warning lines
        if (localTick % 2 == 0) {
            var warningDust = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.0f), 1.0f);
            renderWarningLine(blast.level, blast.leftX + blast.leftDir.x * 2.5,
                    blast.leftY, blast.leftZ + blast.leftDir.z * 2.5,
                    targetPos.x, targetPos.y, targetPos.z, warningDust);
            renderWarningLine(blast.level, blast.rightX + blast.rightDir.x * 2.5,
                    blast.rightY, blast.rightZ + blast.rightDir.z * 2.5,
                    targetPos.x, targetPos.y, targetPos.z, warningDust);
        }

        blast.target.setDeltaMovement(0, 0, 0);
        blast.target.hurtMarked = true;

        if (localTick % 10 == 0) {
            blast.level.playSound(null, blast.owner.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.5f + localTick * 0.02f);
        }
    }

    private static void tickFire(EarthBlast blast) {
        int localTick = blast.ticksElapsed - 70;
        Vec3 targetPos = blast.target.position().add(0, blast.target.getBbHeight() / 2, 0);

        // Render blasters with fully open jaw, firing
        renderGasterBlaster(blast.level, blast.leftX, blast.leftY, blast.leftZ,
                blast.leftDir, blast.leftPerp, 1.5, 1.0, true, blast.ticksElapsed);
        renderGasterBlaster(blast.level, blast.rightX, blast.rightY, blast.rightZ,
                blast.rightDir, blast.rightPerp, 1.5, 1.0, true, blast.ticksElapsed);

        // === BEAMS from mouth to target ===
        var beamCore = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 3.5f);
        var beamOuter = new DustParticleOptions(new Vector3f(0.6f, 0.9f, 1.0f), 2.5f);
        var beamGlow = new DustParticleOptions(new Vector3f(0.3f, 0.7f, 1.0f), 2.0f);

        // Beam starts from mouth position (in front of skull)
        double lmX = blast.leftX + blast.leftDir.x * 3;
        double lmY = blast.leftY - 0.2;
        double lmZ = blast.leftZ + blast.leftDir.z * 3;

        double rmX = blast.rightX + blast.rightDir.x * 3;
        double rmY = blast.rightY - 0.2;
        double rmZ = blast.rightZ + blast.rightDir.z * 3;

        renderBeam(blast.level, lmX, lmY, lmZ,
                targetPos.x, targetPos.y, targetPos.z, beamCore, beamOuter, beamGlow, localTick);
        renderBeam(blast.level, rmX, rmY, rmZ,
                targetPos.x, targetPos.y, targetPos.z, beamCore, beamOuter, beamGlow, localTick);

        // Damage
        if (localTick % 5 == 0) {
            blast.target.hurt(blast.level.damageSources().magic(), 6.0f);
            blast.target.setDeltaMovement(0, 0, 0);
            blast.target.hurtMarked = true;
        }

        // Impact at target
        blast.level.sendParticles(ParticleTypes.FLASH, targetPos.x, targetPos.y, targetPos.z, 1, 0, 0, 0, 0);
        blast.level.sendParticles(ParticleTypes.END_ROD, targetPos.x, targetPos.y, targetPos.z, 3, 0.3, 0.3, 0.3, 0.1);
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2.0 / 6) * i + localTick * 0.4;
            blast.level.sendParticles(beamGlow,
                    targetPos.x + Math.cos(angle) * 1.2, targetPos.y,
                    targetPos.z + Math.sin(angle) * 1.2, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Sounds
        if (localTick % 10 == 0) {
            blast.level.playSound(null, blast.target.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.5f);
        }
        if (localTick == 0) {
            blast.level.playSound(null, blast.owner.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.3f);
        }

        // Final explosion
        if (localTick == 49) {
            blast.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    targetPos.x, targetPos.y, targetPos.z, 1, 0, 0, 0, 0);
            var finalDust = new DustParticleOptions(new Vector3f(0.8f, 0.9f, 1.0f), 5.0f);
            blast.level.sendParticles(finalDust, targetPos.x, targetPos.y, targetPos.z, 40, 2, 2, 2, 0.15);
            blast.level.sendParticles(ParticleTypes.END_ROD, targetPos.x, targetPos.y + 1, targetPos.z, 25, 1.5, 1.5, 1.5, 0.2);

            blast.target.hurt(blast.level.damageSources().magic(), 15.0f);

            blast.level.playSound(null, blast.target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.3f);

            if (blast.owner != null && blast.owner.isAlive()) {
                blast.owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Les Gaster Blasters se sont dechaines!")
                        .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
            }
        }
    }

    // ==========================================
    // UTILITY RENDERING
    // ==========================================

    private static void renderWarningLine(ServerLevel level, double x1, double y1, double z1,
                                           double x2, double y2, double z2, DustParticleOptions dust) {
        int steps = 25;
        for (int i = 0; i < steps; i++) {
            double t = i / (double)steps;
            level.sendParticles(dust,
                    x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t,
                    1, 0.02, 0.02, 0.02, 0);
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

            // Core (white bright center)
            level.sendParticles(core, px, py, pz, 2, 0.05, 0.05, 0.05, 0);
            // Outer with wobble
            double wobble = Math.sin(tick * 0.5 + i * 0.3) * 0.25;
            level.sendParticles(outer, px + wobble, py + wobble * 0.3, pz - wobble, 2, 0.12, 0.08, 0.12, 0.01);
            level.sendParticles(outer, px - wobble, py - wobble * 0.3, pz + wobble, 2, 0.12, 0.08, 0.12, 0.01);

            if (i % 3 == 0) {
                level.sendParticles(glow, px, py + 0.2, pz, 1, 0.15, 0.15, 0.15, 0.02);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 2, 0.15, 0.15, 0.15, 0.04);
            }
        }
    }
}
