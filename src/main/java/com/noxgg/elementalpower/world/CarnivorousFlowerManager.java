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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

import java.util.*;

public class CarnivorousFlowerManager {
    private static final List<CarnivorousFlower> activeFlowers = new ArrayList<>();

    private static final DustParticleOptions MOSS_GREEN = new DustParticleOptions(
            new Vector3f(0.2f, 0.6f, 0.1f), 2.0f);
    private static final DustParticleOptions FLOWER_PINK = new DustParticleOptions(
            new Vector3f(0.9f, 0.2f, 0.5f), 1.5f);
    private static final DustParticleOptions DIGEST_YELLOW = new DustParticleOptions(
            new Vector3f(0.7f, 0.7f, 0.0f), 1.0f);

    public static class CapturedPrey {
        public final LivingEntity entity;
        public int mossCoverage = 0; // 0-100
        public boolean fullyDigested = false;
        public final double captureX, captureY, captureZ;

        public CapturedPrey(LivingEntity entity) {
            this.entity = entity;
            this.captureX = entity.getX();
            this.captureY = entity.getY();
            this.captureZ = entity.getZ();
        }
    }

    public static class CarnivorousFlower {
        public final double x, y, z;
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final double radius;
        public int tick = 0;
        public boolean isOriginal;
        public final List<CapturedPrey> prey = new ArrayList<>();
        public int digestedCount = 0;
        public boolean destroyed = false;

        public CarnivorousFlower(double x, double y, double z, double radius,
                                 ServerLevel level, ServerPlayer caster, boolean isOriginal) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.level = level;
            this.caster = caster;
            this.isOriginal = isOriginal;
        }
    }

    public static void addFlower(CarnivorousFlower flower) {
        activeFlowers.add(flower);
    }

    public static void tick() {
        List<CarnivorousFlower> toAdd = new ArrayList<>();
        Iterator<CarnivorousFlower> it = activeFlowers.iterator();

        while (it.hasNext()) {
            CarnivorousFlower flower = it.next();
            flower.tick++;

            // Check if original flower block is destroyed
            if (flower.isOriginal) {
                BlockPos flowerPos = BlockPos.containing(flower.x, flower.y, flower.z);
                if (!flower.level.getBlockState(flowerPos).is(Blocks.WITHER_ROSE) &&
                        !flower.level.getBlockState(flowerPos).is(Blocks.TORCHFLOWER) && flower.tick > 10) {
                    flower.destroyed = true;
                }
            }

            // If original destroyed, mark all children for removal
            if (flower.destroyed) {
                // Release remaining prey
                for (CapturedPrey p : flower.prey) {
                    if (!p.fullyDigested && !p.entity.isRemoved()) {
                        p.entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        p.entity.removeEffect(MobEffects.WEAKNESS);
                    }
                }
                // Death particles
                flower.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        flower.x, flower.y + 1, flower.z, 30, 1, 1, 1, 0.1);
                flower.level.sendParticles(MOSS_GREEN,
                        flower.x, flower.y + 0.5, flower.z, 20, 1, 0.5, 1, 0.05);
                it.remove();
                continue;
            }

            // === CAPTURE NEW PREY (paralyze mobs in radius) ===
            if (flower.tick % 20 == 0) {
                Entity nullRef = null;
                flower.level.getEntities(nullRef,
                        new net.minecraft.world.phys.AABB(
                                flower.x - flower.radius, flower.y - 2, flower.z - flower.radius,
                                flower.x + flower.radius, flower.y + 4, flower.z + flower.radius),
                        e -> e instanceof LivingEntity && e != flower.caster).forEach(e -> {
                    LivingEntity living = (LivingEntity) e;
                    double dist = Math.sqrt(Math.pow(living.getX() - flower.x, 2) + Math.pow(living.getZ() - flower.z, 2));
                    if (dist > flower.radius) return;

                    // Check not already captured
                    boolean alreadyCaptured = flower.prey.stream()
                            .anyMatch(p -> p.entity.getId() == living.getId());
                    if (!alreadyCaptured) {
                        flower.prey.add(new CapturedPrey(living));
                        // Capture vines particles
                        flower.level.sendParticles(MOSS_GREEN,
                                living.getX(), living.getY() + 0.5, living.getZ(),
                                15, 0.3, 0.5, 0.3, 0.03);
                        flower.level.playSound(null, living.blockPosition(),
                                SoundEvents.VINE_PLACE, SoundSource.BLOCKS, 1.0f, 0.5f);
                    }
                });
            }

            // === PROCESS EACH PREY ===
            Iterator<CapturedPrey> preyIt = flower.prey.iterator();
            while (preyIt.hasNext()) {
                CapturedPrey p = preyIt.next();

                if (p.entity.isRemoved()) {
                    preyIt.remove();
                    continue;
                }

                if (p.fullyDigested) continue;

                // Keep paralyzed and in place
                p.entity.teleportTo(p.captureX, p.captureY, p.captureZ);
                p.entity.setDeltaMovement(0, 0, 0);
                p.entity.hurtMarked = true;
                p.entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 127, false, false));
                p.entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 127, false, false));

                // Increase moss coverage
                p.mossCoverage += 2;

                // === MOSS GROWING ON PREY ===
                double coveragePercent = p.mossCoverage / 100.0;
                double entityHeight = p.entity.getBbHeight();

                // Moss particles climbing up the entity
                double mossHeight = entityHeight * coveragePercent;
                if (flower.tick % 3 == 0) {
                    for (double h = 0; h < mossHeight; h += 0.3) {
                        double angle = Math.random() * Math.PI * 2;
                        double r = 0.3 + Math.random() * 0.2;
                        double mx = p.captureX + Math.cos(angle) * r;
                        double mz = p.captureZ + Math.sin(angle) * r;
                        flower.level.sendParticles(MOSS_GREEN,
                                mx, p.captureY + h, mz, 1, 0.05, 0.05, 0.05, 0.002);
                    }
                    // Vine tendrils wrapping
                    if (coveragePercent > 0.3) {
                        for (int vine = 0; vine < 3; vine++) {
                            double vAngle = (Math.PI * 2 / 3) * vine + flower.tick * 0.05;
                            double vr = 0.4;
                            for (double vh = 0; vh < mossHeight; vh += 0.4) {
                                double vx = p.captureX + Math.cos(vAngle + vh) * vr;
                                double vz = p.captureZ + Math.sin(vAngle + vh) * vr;
                                flower.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                        vx, p.captureY + vh, vz, 1, 0.02, 0.02, 0.02, 0.001);
                            }
                        }
                    }
                }

                // Digestion damage once mostly covered
                if (p.mossCoverage > 60 && flower.tick % 10 == 0) {
                    p.entity.hurt(flower.level.damageSources().magic(), 4.0f);
                    flower.level.sendParticles(DIGEST_YELLOW,
                            p.captureX, p.captureY + entityHeight / 2, p.captureZ,
                            5, 0.2, 0.2, 0.2, 0.02);
                }

                // Squelch sounds during digestion
                if (p.mossCoverage > 40 && flower.tick % 30 == 0) {
                    flower.level.playSound(null, p.entity.blockPosition(),
                            SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.8f, 0.5f);
                }

                // === FULLY COVERED -> DIGEST AND SPAWN NEW FLOWER ===
                if (p.mossCoverage >= 100) {
                    p.fullyDigested = true;
                    flower.digestedCount++;

                    // Kill the entity
                    p.entity.hurt(flower.level.damageSources().magic(), 999.0f);

                    // Digestion burst
                    flower.level.sendParticles(MOSS_GREEN,
                            p.captureX, p.captureY + 1, p.captureZ, 30, 0.5, 0.5, 0.5, 0.1);
                    flower.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            p.captureX, p.captureY + 1, p.captureZ, 20, 0.5, 0.5, 0.5, 0.08);
                    flower.level.sendParticles(DIGEST_YELLOW,
                            p.captureX, p.captureY + 0.5, p.captureZ, 15, 0.3, 0.3, 0.3, 0.05);

                    flower.level.playSound(null, BlockPos.containing(p.captureX, p.captureY, p.captureZ),
                            SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.5f, 0.6f);

                    // === SPREAD: grow moss carpet 3x3 at prey location ===
                    BlockPos preyPos = BlockPos.containing(p.captureX, p.captureY, p.captureZ);
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos mossPos = preyPos.offset(dx, 0, dz);
                            BlockPos below = mossPos.below();
                            if (flower.level.getBlockState(mossPos).isAir() &&
                                    !flower.level.getBlockState(below).isAir()) {
                                flower.level.setBlock(mossPos, Blocks.MOSS_CARPET.defaultBlockState(), 3);
                            }
                            // Replace ground with moss block
                            if (!flower.level.getBlockState(below).isAir() &&
                                    flower.level.getBlockState(below).getDestroySpeed(flower.level, below) >= 0) {
                                flower.level.setBlock(below, Blocks.MOSS_BLOCK.defaultBlockState(), 3);
                            }
                        }
                    }

                    // === SPAWN NEW CHILD FLOWER at prey location ===
                    BlockPos newFlowerPos = preyPos;
                    // Find air block above moss
                    for (int dy = 0; dy < 3; dy++) {
                        if (flower.level.getBlockState(newFlowerPos.above(dy)).isAir()) {
                            newFlowerPos = newFlowerPos.above(dy);
                            break;
                        }
                    }
                    flower.level.setBlock(newFlowerPos, Blocks.WITHER_ROSE.defaultBlockState(), 3);

                    // Create child flower with same properties (linked to original)
                    CarnivorousFlower child = new CarnivorousFlower(
                            newFlowerPos.getX() + 0.5, newFlowerPos.getY(),
                            newFlowerPos.getZ() + 0.5, flower.radius,
                            flower.level, flower.caster, false);
                    toAdd.add(child);

                    // Notify caster
                    flower.caster.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(">> La Fleur Carnivore a digere une proie et s'est etendue!")
                                    .withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            }

            // === RENDER THE FLOWER ===
            if (flower.tick % 3 == 0) {
                renderFlower(flower);
            }
        }

        // Add new child flowers
        activeFlowers.addAll(toAdd);
    }

    private static void renderFlower(CarnivorousFlower flower) {
        int tick = flower.tick;
        double x = flower.x;
        double y = flower.y;
        double z = flower.z;

        // === STEM (green, slightly swaying) ===
        double sway = Math.sin(tick * 0.05) * 0.2;
        for (double h = 0; h < 2.0; h += 0.2) {
            double stemSway = sway * (h / 2.0);
            flower.level.sendParticles(MOSS_GREEN,
                    x + stemSway, y + h, z + stemSway * 0.5,
                    1, 0.02, 0.02, 0.02, 0.001);
        }

        // === PETALS (opening and closing slowly) ===
        double petalOpen = 0.8 + Math.sin(tick * 0.08) * 0.3;
        for (int petal = 0; petal < 6; petal++) {
            double pAngle = (Math.PI * 2 / 6) * petal + tick * 0.01;
            double petalLen = 1.2 * petalOpen;
            for (double p = 0; p < petalLen; p += 0.15) {
                double droop = p * p * 0.1;
                double px = x + Math.cos(pAngle) * p + sway;
                double pz = z + Math.sin(pAngle) * p + sway * 0.5;
                double py = y + 2.0 - droop;

                flower.level.sendParticles(FLOWER_PINK,
                        px, py, pz, 1, 0.02, 0.02, 0.02, 0.001);
            }
            // Petal tip
            double tipX = x + Math.cos(pAngle) * petalLen + sway;
            double tipZ = z + Math.sin(pAngle) * petalLen + sway * 0.5;
            flower.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    tipX, y + 2.0 - petalLen * petalLen * 0.1, tipZ,
                    1, 0.02, 0.02, 0.02, 0.005);
        }

        // === CENTER (yellow/green pollen) ===
        flower.level.sendParticles(DIGEST_YELLOW,
                x + sway, y + 2.0, z + sway * 0.5, 3, 0.15, 0.05, 0.15, 0.005);

        // === SPORE CLOUD (attraction lure) ===
        if (tick % 10 == 0) {
            for (int s = 0; s < 5; s++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * flower.radius;
                flower.level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        x + Math.cos(angle) * dist, y + 1 + Math.random() * 2,
                        z + Math.sin(angle) * dist,
                        1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        // === ROOT TENDRILS on ground ===
        if (tick % 6 == 0) {
            for (int root = 0; root < 4; root++) {
                double rAngle = (Math.PI * 2 / 4) * root + tick * 0.02;
                for (double rd = 0.5; rd < flower.radius * 0.6; rd += 0.5) {
                    double wobble = Math.sin(rd * 3 + tick * 0.1) * 0.3;
                    double rx = x + Math.cos(rAngle + wobble * 0.1) * rd;
                    double rz = z + Math.sin(rAngle + wobble * 0.1) * rd;
                    flower.level.sendParticles(MOSS_GREEN,
                            rx, y + 0.1, rz, 1, 0.05, 0, 0.05, 0.001);
                }
            }
        }

        // === AMBIENT SOUND ===
        if (tick % 40 == 0) {
            flower.level.playSound(null, BlockPos.containing(x, y, z),
                    SoundEvents.FLOWERING_AZALEA_PLACE, SoundSource.BLOCKS, 0.8f, 0.4f);
        }
    }

    // Remove all flowers from a specific caster (called when original destroyed)
    public static void destroyAllFromCaster(ServerPlayer caster) {
        activeFlowers.stream()
                .filter(f -> f.caster.equals(caster))
                .forEach(f -> f.destroyed = true);
    }
}
