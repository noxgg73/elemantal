package com.noxgg.elementalpower.world;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlackHoleManager {
    private static final List<BlackHole> activeHoles = new ArrayList<>();

    private static final DustParticleOptions DARK_PURPLE = new DustParticleOptions(
            new Vector3f(0.2f, 0.0f, 0.3f), 3.0f);
    private static final DustParticleOptions VOID_BLACK = new DustParticleOptions(
            new Vector3f(0.02f, 0.0f, 0.05f), 4.0f);
    private static final DustParticleOptions ACCRETION_PURPLE = new DustParticleOptions(
            new Vector3f(0.5f, 0.1f, 0.8f), 1.5f);

    public static class BlackHole {
        public final double x, y, z;
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final double radius;
        public int ticksRemaining;

        public BlackHole(double x, double y, double z, double radius,
                         ServerLevel level, ServerPlayer caster, int durationTicks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.level = level;
            this.caster = caster;
            this.ticksRemaining = durationTicks;
        }
    }

    public static void addBlackHole(BlackHole hole) {
        activeHoles.add(hole);
    }

    public static void tick() {
        Iterator<BlackHole> it = activeHoles.iterator();
        while (it.hasNext()) {
            BlackHole hole = it.next();
            hole.ticksRemaining--;

            if (hole.ticksRemaining <= 0 || hole.level == null) {
                // Black hole collapse
                collapse(hole);
                it.remove();
                continue;
            }

            // === PULL ENTITIES ===
            Entity nullRef = null;
            hole.level.getEntities(nullRef,
                    new net.minecraft.world.phys.AABB(
                            hole.x - hole.radius, hole.y - hole.radius, hole.z - hole.radius,
                            hole.x + hole.radius, hole.y + hole.radius, hole.z + hole.radius),
                    e -> true).forEach(e -> {

                // Skip caster
                if (e == hole.caster) return;

                // Skip Darkness class players
                if (e instanceof Player otherPlayer) {
                    otherPlayer.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                        // handled below
                    });
                    boolean[] isDarkness = {false};
                    e.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                        if (data.getElement() == ElementType.DARKNESS) isDarkness[0] = true;
                    });
                    if (isDarkness[0]) return;
                }

                double dx = hole.x - e.getX();
                double dy = hole.y - e.getY();
                double dz = hole.z - e.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (dist > hole.radius || dist < 0.5) return;

                // Pull strength inversely proportional to distance (stronger near center)
                double strength = (1.0 - (dist / hole.radius)) * 0.8;

                // Normalize direction
                double nx = dx / dist;
                double ny = dy / dist;
                double nz = dz / dist;

                // Apply pull
                Vec3 currentMotion = e.getDeltaMovement();
                e.setDeltaMovement(
                        currentMotion.x + nx * strength,
                        currentMotion.y + ny * strength * 0.5,
                        currentMotion.z + nz * strength
                );
                e.hurtMarked = true;

                // Damage entities close to the center
                if (dist < 3.0 && e instanceof LivingEntity living) {
                    if (hole.ticksRemaining % 10 == 0) {
                        living.hurt(hole.level.damageSources().magic(), 5.0f);
                        living.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 1, false, false));
                    }
                    // Spin them around the center
                    double spinAngle = Math.atan2(dz, dx) + 0.3;
                    e.setDeltaMovement(
                            -Math.cos(spinAngle) * 0.5,
                            currentMotion.y * 0.5,
                            -Math.sin(spinAngle) * 0.5
                    );
                    e.hurtMarked = true;
                }

                // Spaghettification particles on pulled entities
                if (hole.ticksRemaining % 5 == 0 && dist < hole.radius * 0.7) {
                    hole.level.sendParticles(ACCRETION_PURPLE,
                            e.getX(), e.getY() + 0.5, e.getZ(),
                            2, 0.1, 0.2, 0.1, 0.03);
                }
            });

            // === ABSORB BLOCKS (every 4 ticks, expanding radius over time) ===
            if (hole.ticksRemaining % 4 == 0) {
                absorbBlocks(hole);
            }

            // === RENDER BLACK HOLE ===
            if (hole.ticksRemaining % 2 == 0) {
                renderBlackHole(hole);
            }

            // === AMBIENT SOUND ===
            if (hole.ticksRemaining % 15 == 0) {
                hole.level.playSound(null, BlockPos.containing(hole.x, hole.y, hole.z),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.AMBIENT, 0.4f, 0.2f);
            }
            if (hole.ticksRemaining % 30 == 0) {
                hole.level.playSound(null, BlockPos.containing(hole.x, hole.y, hole.z),
                        SoundEvents.AMBIENT_CAVE.get(), SoundSource.AMBIENT, 2.0f, 0.3f);
            }
        }
    }

    private static void absorbBlocks(BlackHole hole) {
        int elapsed = 150 - hole.ticksRemaining;
        // Block absorption radius grows over time: starts at 2, max 10
        double blockRadius = Math.min(10.0, 2.0 + elapsed * 0.08);
        int blocksPerTick = 3 + elapsed / 15; // more blocks absorbed as time goes on
        int destroyed = 0;

        int cx = (int) Math.round(hole.x);
        int cy = (int) Math.round(hole.y);
        int cz = (int) Math.round(hole.z);

        // Scan from the outside in (further blocks first)
        for (double r = blockRadius; r >= 1.5 && destroyed < blocksPerTick; r -= 1.0) {
            for (int attempt = 0; attempt < 20 && destroyed < blocksPerTick; attempt++) {
                // Random position on this shell
                double theta = hole.level.random.nextDouble() * Math.PI * 2;
                double phi = hole.level.random.nextDouble() * Math.PI;
                int bx = cx + (int)(Math.cos(theta) * Math.sin(phi) * r);
                int by = cy + (int)(Math.cos(phi) * r);
                int bz = cz + (int)(Math.sin(theta) * Math.sin(phi) * r);

                BlockPos pos = new BlockPos(bx, by, bz);
                net.minecraft.world.level.block.state.BlockState state = hole.level.getBlockState(pos);

                // Don't absorb air, bedrock, or unbreakable blocks
                if (state.isAir()) continue;
                if (state.getDestroySpeed(hole.level, pos) < 0) continue; // bedrock etc
                // Don't absorb liquids to avoid flooding
                if (state.liquid()) continue;

                // Break the block with particles flying toward center
                double px = bx + 0.5;
                double py = by + 0.5;
                double pz = bz + 0.5;

                // Block break particles heading toward black hole center
                double dirX = (hole.x - px);
                double dirY = (hole.y - py);
                double dirZ = (hole.z - pz);
                double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                if (len > 0.1) {
                    hole.level.sendParticles(ParticleTypes.SMOKE,
                            px, py, pz, 5, 0.2, 0.2, 0.2, 0.05);
                    hole.level.sendParticles(ACCRETION_PURPLE,
                            px, py, pz, 3, 0.1, 0.1, 0.1, 0.08);
                    // Trail toward center
                    for (double t = 0; t < 1; t += 0.25) {
                        hole.level.sendParticles(ParticleTypes.PORTAL,
                                px + dirX * t, py + dirY * t, pz + dirZ * t,
                                1, 0.05, 0.05, 0.05, 0.01);
                    }
                }

                // Destroy the block
                hole.level.destroyBlock(pos, false);
                destroyed++;
            }
        }

        // Block absorption crumbling sound
        if (destroyed > 0 && hole.ticksRemaining % 8 == 0) {
            hole.level.playSound(null, BlockPos.containing(hole.x, hole.y, hole.z),
                    SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5f, 0.4f);
        }
    }

    private static void renderBlackHole(BlackHole hole) {
        int tick = 150 - hole.ticksRemaining;

        // === VOID CORE: dense black sphere ===
        for (int i = 0; i < 30; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double r = 1.0 + Math.random() * 0.5;
            double px = hole.x + Math.cos(theta) * Math.sin(phi) * r;
            double py = hole.y + Math.cos(phi) * r;
            double pz = hole.z + Math.sin(theta) * Math.sin(phi) * r;
            hole.level.sendParticles(VOID_BLACK, px, py, pz, 1, 0, 0, 0, 0);
        }

        // === EVENT HORIZON: dark purple ring ===
        for (double a = 0; a < Math.PI * 2; a += 0.08) {
            double wobble = Math.sin(tick * 0.3 + a * 5) * 0.2;
            double r = 2.0 + wobble;
            double px = hole.x + Math.cos(a) * r;
            double pz = hole.z + Math.sin(a) * r;
            hole.level.sendParticles(DARK_PURPLE, px, hole.y, pz, 1, 0, 0, 0, 0);
            // Vertical event horizon ring
            double py = hole.y + Math.sin(a) * r;
            hole.level.sendParticles(DARK_PURPLE,
                    hole.x + Math.cos(a) * r * 0.7, py, hole.z, 1, 0, 0, 0, 0);
        }

        // === ACCRETION DISK: spinning ring of purple/violet particles ===
        for (int ring = 0; ring < 3; ring++) {
            double ringR = 3.0 + ring * 1.5;
            double ringSpeed = 0.15 - ring * 0.03;
            int points = (int)(ringR * 8);
            for (int p = 0; p < points; p++) {
                double a = (Math.PI * 2.0 / points) * p + tick * ringSpeed;
                double wobble = Math.sin(tick * 0.5 + p * 0.5) * 0.3;
                double px = hole.x + Math.cos(a) * (ringR + wobble);
                double pz = hole.z + Math.sin(a) * (ringR + wobble);
                double py = hole.y + Math.sin(a * 3 + tick * 0.2) * 0.4;
                hole.level.sendParticles(ACCRETION_PURPLE, px, py, pz, 1, 0, 0, 0, 0.001);
            }
        }

        // === GRAVITATIONAL LENSING: distorted light streaks ===
        for (int streak = 0; streak < 6; streak++) {
            double sAngle = (Math.PI * 2.0 / 6) * streak + tick * 0.05;
            for (double d = 2.5; d < 6; d += 0.3) {
                double curve = Math.sin(d * 0.8 + tick * 0.2) * 1.0;
                double sx = hole.x + Math.cos(sAngle + curve * 0.1) * d;
                double sz = hole.z + Math.sin(sAngle + curve * 0.1) * d;
                double sy = hole.y + curve * 0.3;
                hole.level.sendParticles(ParticleTypes.END_ROD,
                        sx, sy, sz, 1, 0, 0, 0, 0.001);
            }
        }

        // === INWARD SPIRAL STREAMS (matter being sucked in) ===
        for (int stream = 0; stream < 4; stream++) {
            double baseAngle = (Math.PI * 2.0 / 4) * stream + tick * 0.08;
            for (double d = hole.radius; d > 2; d -= 1.0) {
                double spiralAngle = baseAngle + (hole.radius - d) * 0.3;
                double sx = hole.x + Math.cos(spiralAngle) * d;
                double sz = hole.z + Math.sin(spiralAngle) * d;
                double sy = hole.y + Math.sin(d * 0.5 + tick * 0.1) * 0.5;
                hole.level.sendParticles(ParticleTypes.PORTAL,
                        sx, sy, sz, 1, 0.1, 0.1, 0.1, 0.02);
                if (d < 8) {
                    hole.level.sendParticles(ACCRETION_PURPLE,
                            sx, sy, sz, 1, 0.05, 0.05, 0.05, 0.005);
                }
            }
        }

        // === RANDOM VOID FLASHES ===
        if (tick % 5 == 0) {
            for (int i = 0; i < 3; i++) {
                double fx = hole.x + (Math.random() - 0.5) * 4;
                double fy = hole.y + (Math.random() - 0.5) * 4;
                double fz = hole.z + (Math.random() - 0.5) * 4;
                hole.level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        fx, fy, fz, 5, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    private static void collapse(BlackHole hole) {
        // Final implosion then explosion
        // Implosion: particles rush to center
        for (int i = 0; i < 80; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double r = hole.radius * 0.8;
            double px = hole.x + Math.cos(theta) * Math.sin(phi) * r;
            double py = hole.y + Math.cos(phi) * r;
            double pz = hole.z + Math.sin(theta) * Math.sin(phi) * r;
            hole.level.sendParticles(VOID_BLACK, px, py, pz, 3, 0, 0, 0, 0.3);
        }
        // Explosion burst
        for (int i = 0; i < 50; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double px = hole.x + Math.cos(theta) * Math.sin(phi) * 2;
            double py = hole.y + Math.cos(phi) * 2;
            double pz = hole.z + Math.sin(theta) * Math.sin(phi) * 2;
            hole.level.sendParticles(ACCRETION_PURPLE, px, py, pz, 5, 0.5, 0.5, 0.5, 0.3);
            hole.level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 2, 0.3, 0.3, 0.3, 0.15);
        }
        hole.level.sendParticles(ParticleTypes.FLASH, hole.x, hole.y, hole.z, 3, 0, 0, 0, 0);
        hole.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hole.x, hole.y, hole.z, 1, 0, 0, 0, 0);

        // Collapse sound
        hole.level.playSound(null, BlockPos.containing(hole.x, hole.y, hole.z),
                SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 2.0f, 0.3f);
        hole.level.playSound(null, BlockPos.containing(hole.x, hole.y, hole.z),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.AMBIENT, 1.5f, 0.4f);
    }
}
