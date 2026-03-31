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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SoulTsunamiManager {
    private static final List<SoulTsunami> activeTsunamis = new ArrayList<>();

    private static final DustParticleOptions SOUL_RED = new DustParticleOptions(
            new Vector3f(0.6f, 0.05f, 0.0f), 2.0f);
    private static final DustParticleOptions SOUL_DARK = new DustParticleOptions(
            new Vector3f(0.3f, 0.0f, 0.0f), 2.5f);
    private static final DustParticleOptions SOUL_ORANGE = new DustParticleOptions(
            new Vector3f(0.9f, 0.3f, 0.0f), 1.5f);

    public static class SoulTsunami {
        public final double startX, startY, startZ;
        public final double dirX, dirZ; // direction the wave travels
        public final ServerLevel level;
        public final ServerPlayer caster;
        public int tick = 0;
        public static final int TOTAL_DURATION = 120; // 6 seconds
        public static final double MAX_DISTANCE = 40.0;
        public static final double WAVE_HEIGHT = 8.0;
        public static final double WAVE_WIDTH = 20.0;

        public SoulTsunami(ServerLevel level, ServerPlayer caster) {
            this.level = level;
            this.caster = caster;
            this.startX = caster.getX();
            this.startY = caster.getY();
            this.startZ = caster.getZ();
            Vec3 look = caster.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
            this.dirX = flat.x;
            this.dirZ = flat.z;
        }
    }

    public static void addTsunami(SoulTsunami tsunami) {
        activeTsunamis.add(tsunami);
    }

    public static void tick() {
        Iterator<SoulTsunami> it = activeTsunamis.iterator();
        while (it.hasNext()) {
            SoulTsunami ts = it.next();
            ts.tick++;

            if (ts.tick > SoulTsunami.TOTAL_DURATION) {
                it.remove();
                continue;
            }

            double progress = (double) ts.tick / SoulTsunami.TOTAL_DURATION;
            double waveFront = progress * SoulTsunami.MAX_DISTANCE;

            // Perpendicular direction for wave width
            double perpX = -ts.dirZ;
            double perpZ = ts.dirX;

            // === RENDER THE WAVE ===
            // The wave is a curved wall of souls moving forward
            double waveThickness = 3.0;

            for (double w = -SoulTsunami.WAVE_WIDTH / 2; w <= SoulTsunami.WAVE_WIDTH / 2; w += 0.6) {
                // Wave center position
                double waveX = ts.startX + ts.dirX * waveFront + perpX * w;
                double waveZ = ts.startZ + ts.dirZ * waveFront + perpZ * w;

                // Wave height curve (taller in center, lower at edges)
                double edgeFactor = 1.0 - Math.abs(w) / (SoulTsunami.WAVE_WIDTH / 2);
                double height = SoulTsunami.WAVE_HEIGHT * edgeFactor;

                // Wave crest (top curl)
                double curlOffset = Math.sin(progress * Math.PI) * 2.0;

                for (double h = 0; h < height; h += 0.5) {
                    // Wave face (curved forward at top like a real tsunami)
                    double curlAmount = (h / height) * (h / height) * curlOffset;
                    double faceX = waveX + ts.dirX * curlAmount;
                    double faceZ = waveZ + ts.dirZ * curlAmount;
                    double faceY = ts.startY + h;

                    // Main wave body: souls
                    ts.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            faceX, faceY, faceZ, 1, 0.1, 0.1, 0.1, 0.005);

                    // Dense lower part
                    if (h < height * 0.5) {
                        ts.level.sendParticles(SOUL_DARK,
                                faceX, faceY, faceZ, 1, 0.1, 0.05, 0.1, 0.002);
                    }

                    // Glowing upper part
                    if (h > height * 0.6) {
                        ts.level.sendParticles(SOUL_ORANGE,
                                faceX, faceY, faceZ, 1, 0.1, 0.1, 0.1, 0.003);
                    }
                }

                // Wave crest: foam of souls at the top
                double crestX = waveX + ts.dirX * curlOffset;
                double crestZ = waveZ + ts.dirZ * curlOffset;
                ts.level.sendParticles(ParticleTypes.SOUL,
                        crestX, ts.startY + height, crestZ, 2, 0.2, 0.1, 0.2, 0.05);
                ts.level.sendParticles(SOUL_RED,
                        crestX, ts.startY + height + 0.3, crestZ, 1, 0.15, 0.05, 0.15, 0.01);
            }

            // === WAVE SPRAY (souls flying ahead of the wave) ===
            for (int spray = 0; spray < 8; spray++) {
                double sw = (Math.random() - 0.5) * SoulTsunami.WAVE_WIDTH;
                double sprayX = ts.startX + ts.dirX * (waveFront + 2 + Math.random() * 3) + perpX * sw;
                double sprayZ = ts.startZ + ts.dirZ * (waveFront + 2 + Math.random() * 3) + perpZ * sw;
                double sprayY = ts.startY + Math.random() * SoulTsunami.WAVE_HEIGHT * 1.2;
                ts.level.sendParticles(ParticleTypes.SOUL,
                        sprayX, sprayY, sprayZ, 1, 0.2, 0.3, 0.2, 0.08);
            }

            // === TRAIL behind the wave (dissipating souls) ===
            if (ts.tick % 3 == 0) {
                for (double d = Math.max(0, waveFront - 8); d < waveFront - 2; d += 1.5) {
                    for (double w = -SoulTsunami.WAVE_WIDTH / 2; w <= SoulTsunami.WAVE_WIDTH / 2; w += 3) {
                        double trailX = ts.startX + ts.dirX * d + perpX * w;
                        double trailZ = ts.startZ + ts.dirZ * d + perpZ * w;
                        ts.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                trailX, ts.startY + Math.random() * 2, trailZ,
                                1, 0.3, 0.2, 0.3, 0.02);
                    }
                }
            }

            // === GROUND FIRE TRAIL ===
            if (ts.tick % 2 == 0) {
                for (double d = Math.max(0, waveFront - 5); d < waveFront; d += 1.0) {
                    for (double w = -SoulTsunami.WAVE_WIDTH / 2; w <= SoulTsunami.WAVE_WIDTH / 2; w += 2) {
                        double gx = ts.startX + ts.dirX * d + perpX * w;
                        double gz = ts.startZ + ts.dirZ * d + perpZ * w;
                        ts.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                gx, ts.startY + 0.2, gz, 1, 0.1, 0, 0.1, 0.001);
                    }
                }
            }

            // === FACE SCREAMING SOULS in the wave ===
            if (ts.tick % 4 == 0) {
                for (int face = 0; face < 5; face++) {
                    double fw = (Math.random() - 0.5) * SoulTsunami.WAVE_WIDTH * 0.8;
                    double fh = Math.random() * SoulTsunami.WAVE_HEIGHT * 0.7 + 1;
                    double fx = ts.startX + ts.dirX * waveFront + perpX * fw;
                    double fz = ts.startZ + ts.dirZ * waveFront + perpZ * fw;
                    ts.level.sendParticles(ParticleTypes.SOUL,
                            fx, ts.startY + fh, fz, 3, 0.05, 0.1, 0.05, 0.03);
                    // Screaming mouth (larger cluster)
                    ts.level.sendParticles(SOUL_RED,
                            fx, ts.startY + fh - 0.2, fz, 2, 0.08, 0.03, 0.08, 0.005);
                }
            }

            // === DAMAGE ENTITIES HIT BY THE WAVE ===
            double hitZoneStart = waveFront - waveThickness;
            double hitZoneEnd = waveFront + 1;

            Entity nullRef = null;
            ts.level.getEntities(nullRef,
                    new net.minecraft.world.phys.AABB(
                            ts.startX + ts.dirX * hitZoneStart + perpX * (-SoulTsunami.WAVE_WIDTH / 2) - 2,
                            ts.startY - 1,
                            ts.startZ + ts.dirZ * hitZoneStart + perpZ * (-SoulTsunami.WAVE_WIDTH / 2) - 2,
                            ts.startX + ts.dirX * hitZoneEnd + perpX * (SoulTsunami.WAVE_WIDTH / 2) + 2,
                            ts.startY + SoulTsunami.WAVE_HEIGHT + 2,
                            ts.startZ + ts.dirZ * hitZoneEnd + perpZ * (SoulTsunami.WAVE_WIDTH / 2) + 2),
                    e -> e != ts.caster && e instanceof LivingEntity).forEach(e -> {

                // Check if entity is actually in the wave path
                Vec3 toEntity = e.position().subtract(ts.startX, ts.startY, ts.startZ);
                double forwardDist = toEntity.x * ts.dirX + toEntity.z * ts.dirZ;
                double sideDist = Math.abs(toEntity.x * perpX + toEntity.z * perpZ);

                if (forwardDist >= hitZoneStart && forwardDist <= hitZoneEnd
                        && sideDist <= SoulTsunami.WAVE_WIDTH / 2) {
                    LivingEntity living = (LivingEntity) e;

                    // Push with the wave
                    living.setDeltaMovement(
                            ts.dirX * 2.0,
                            0.8,
                            ts.dirZ * 2.0
                    );
                    living.hurtMarked = true;

                    // Damage
                    if (ts.tick % 5 == 0) {
                        living.hurt(ts.level.damageSources().magic(), 8.0f);
                        living.setSecondsOnFire(5);
                        living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
                    }

                    // Soul rip particles on hit
                    ts.level.sendParticles(ParticleTypes.SOUL,
                            living.getX(), living.getY() + 1, living.getZ(),
                            5, 0.2, 0.3, 0.2, 0.08);
                }
            });

            // === SOUNDS ===
            if (ts.tick % 10 == 0) {
                BlockPos soundPos = BlockPos.containing(
                        ts.startX + ts.dirX * waveFront,
                        ts.startY,
                        ts.startZ + ts.dirZ * waveFront);
                ts.level.playSound(null, soundPos,
                        SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.3f);
            }
            if (ts.tick % 20 == 0) {
                ts.level.playSound(null, BlockPos.containing(ts.startX, ts.startY, ts.startZ),
                        SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 0.4f);
            }
        }
    }
}
