package com.noxgg.elementalpower.world;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BoneBarrageManager {
    private static final List<BoneBarrage> activeBarrages = new ArrayList<>();

    // Phases:
    // 0 = SUMMON (0-40 ticks): 50 bones appear behind player, floating
    // 1 = HOVER (40-80 ticks): bones hover menacingly, vibrating
    // 2 = LAUNCH (80+): bones fly toward target(s)
    //   - Single target mode: all bones converge on looked-at mob
    //   - AOE mode (looking at sky): bones scatter and hit all mobs in 20 block radius

    public static final int BONE_COUNT = 50;

    public static class BoneBarrage {
        public final ServerLevel level;
        public final ServerPlayer owner;
        public int ticksElapsed;
        public int phase;
        public boolean aoeMode; // true = looking at sky = scatter kill all

        // Target for single mode
        public LivingEntity singleTarget;

        // Bone positions
        public double[] boneX = new double[BONE_COUNT];
        public double[] boneY = new double[BONE_COUNT];
        public double[] boneZ = new double[BONE_COUNT];

        // Bone velocities (used in launch phase)
        public double[] boneDX = new double[BONE_COUNT];
        public double[] boneDY = new double[BONE_COUNT];
        public double[] boneDZ = new double[BONE_COUNT];

        // Each bone's assigned target (for AOE mode)
        public LivingEntity[] boneTargets = new LivingEntity[BONE_COUNT];

        public boolean[] boneHit = new boolean[BONE_COUNT]; // already hit target

        public BoneBarrage(ServerLevel level, ServerPlayer owner, boolean aoeMode, LivingEntity singleTarget) {
            this.level = level;
            this.owner = owner;
            this.ticksElapsed = 0;
            this.phase = 0;
            this.aoeMode = aoeMode;
            this.singleTarget = singleTarget;

            // Initialize bone positions behind the player in a spread formation
            Vec3 look = owner.getLookAngle();
            Vec3 behind = new Vec3(-look.x, 0, -look.z).normalize();
            Vec3 side = new Vec3(-behind.z, 0, behind.x).normalize();

            for (int i = 0; i < BONE_COUNT; i++) {
                // Spread in a fan shape behind the player
                int row = i / 10; // 5 rows of 10
                int col = i % 10;

                double backDist = 2.0 + row * 0.8;
                double sideDist = (col - 4.5) * 0.6;
                double height = 1.5 + Math.random() * 2.5;

                boneX[i] = owner.getX() + behind.x * backDist + side.x * sideDist;
                boneY[i] = owner.getY() + height;
                boneZ[i] = owner.getZ() + behind.z * backDist + side.z * sideDist;

                boneHit[i] = false;
            }
        }
    }

    public static void addBarrage(BoneBarrage barrage) {
        activeBarrages.add(barrage);
    }

    public static void tick() {
        Iterator<BoneBarrage> it = activeBarrages.iterator();
        while (it.hasNext()) {
            BoneBarrage barrage = it.next();
            barrage.ticksElapsed++;

            if (barrage.level == null || barrage.owner == null || !barrage.owner.isAlive()) {
                it.remove();
                continue;
            }

            if (barrage.phase == 0) {
                tickSummon(barrage);
                if (barrage.ticksElapsed >= 40) barrage.phase = 1;
            } else if (barrage.phase == 1) {
                tickHover(barrage);
                if (barrage.ticksElapsed >= 80) {
                    barrage.phase = 2;
                    initLaunch(barrage);
                }
            } else if (barrage.phase == 2) {
                tickLaunch(barrage);
                // End after 3 seconds of flight or all bones hit
                if (barrage.ticksElapsed >= 140) {
                    it.remove();
                }
            }
        }
    }

    private static void tickSummon(BoneBarrage barrage) {
        double progress = barrage.ticksElapsed / 40.0;
        var boneDust = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.85f), 2.0f);

        for (int i = 0; i < BONE_COUNT; i++) {
            // Only show bones that have "appeared" (staggered)
            if (barrage.ticksElapsed < i * 0.8) continue;

            // Render bone at its position
            renderBone(barrage.level, barrage.boneX[i], barrage.boneY[i], barrage.boneZ[i], boneDust);

            // Summoning sparkle
            if (barrage.ticksElapsed == (int)(i * 0.8) + 1) {
                barrage.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        barrage.boneX[i], barrage.boneY[i], barrage.boneZ[i],
                        5, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // Crackling summoning sounds
        if (barrage.ticksElapsed % 5 == 0) {
            barrage.level.playSound(null, barrage.owner.blockPosition(),
                    SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 1.0f, 0.5f + barrage.ticksElapsed * 0.02f);
        }
    }

    private static void tickHover(BoneBarrage barrage) {
        int localTick = barrage.ticksElapsed - 40;
        var boneDust = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.85f), 2.0f);
        var glowDust = new DustParticleOptions(new Vector3f(0.6f, 0.8f, 1.0f), 1.5f);

        for (int i = 0; i < BONE_COUNT; i++) {
            // Vibrate/hover effect
            double vibX = Math.sin(barrage.ticksElapsed * 0.8 + i * 1.3) * 0.05;
            double vibY = Math.cos(barrage.ticksElapsed * 0.6 + i * 0.9) * 0.08;
            double vibZ = Math.sin(barrage.ticksElapsed * 0.7 + i * 1.1) * 0.05;

            double x = barrage.boneX[i] + vibX;
            double y = barrage.boneY[i] + vibY;
            double z = barrage.boneZ[i] + vibZ;

            renderBone(barrage.level, x, y, z, boneDust);

            // Intensifying glow as launch approaches
            if (localTick > 20 && i % 3 == 0) {
                double intensity = (localTick - 20) / 20.0;
                barrage.level.sendParticles(glowDust,
                        x, y, z, (int)(1 + intensity * 2), 0.1, 0.1, 0.1, 0.02);
            }
        }

        // Menacing hum
        if (localTick % 15 == 0) {
            barrage.level.playSound(null, barrage.owner.blockPosition(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.8f);
        }

        // Warning message before launch
        if (localTick == 30) {
            if (barrage.aoeMode) {
                barrage.owner.sendSystemMessage(net.minecraft.network.chat.Component.literal("")
                        .append(net.minecraft.network.chat.Component.literal(">> ").withStyle(net.minecraft.ChatFormatting.WHITE))
                        .append(net.minecraft.network.chat.Component.literal("Les os se dispersent dans toutes les directions!")
                                .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)));
            }
        }
    }

    private static void initLaunch(BoneBarrage barrage) {
        if (barrage.aoeMode) {
            // Find all mobs in 20 block radius
            List<LivingEntity> targets = new ArrayList<>();
            for (Entity entity : barrage.level.getEntities(barrage.owner,
                    barrage.owner.getBoundingBox().inflate(20),
                    e -> e instanceof LivingEntity && e != barrage.owner && !(e instanceof Player))) {
                targets.add((LivingEntity) entity);
            }

            // Assign each bone to a target (distribute evenly)
            for (int i = 0; i < BONE_COUNT; i++) {
                if (!targets.isEmpty()) {
                    barrage.boneTargets[i] = targets.get(i % targets.size());
                } else {
                    // No targets, bones fly outward in all directions
                    double angle = (Math.PI * 2.0 / BONE_COUNT) * i;
                    barrage.boneDX[i] = Math.cos(angle) * 1.5;
                    barrage.boneDY[i] = -0.1;
                    barrage.boneDZ[i] = Math.sin(angle) * 1.5;
                    barrage.boneTargets[i] = null;
                }
            }
        } else {
            // All bones target the single mob
            for (int i = 0; i < BONE_COUNT; i++) {
                barrage.boneTargets[i] = barrage.singleTarget;
            }
        }

        // Launch sound
        barrage.level.playSound(null, barrage.owner.blockPosition(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.0f, 1.5f);
        barrage.level.playSound(null, barrage.owner.blockPosition(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    private static void tickLaunch(BoneBarrage barrage) {
        int localTick = barrage.ticksElapsed - 80;
        var boneDust = new DustParticleOptions(new Vector3f(0.95f, 0.95f, 0.9f), 2.0f);
        var trailDust = new DustParticleOptions(new Vector3f(0.7f, 0.85f, 1.0f), 1.0f);

        int allHit = 0;

        for (int i = 0; i < BONE_COUNT; i++) {
            if (barrage.boneHit[i]) { allHit++; continue; }

            // Stagger launch: each bone launches 1 tick apart for wave effect
            if (localTick < i * 0.4) {
                // Still hovering, waiting to launch
                renderBone(barrage.level, barrage.boneX[i], barrage.boneY[i], barrage.boneZ[i], boneDust);
                continue;
            }

            LivingEntity target = barrage.boneTargets[i];

            if (target != null && target.isAlive()) {
                // Home toward target
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
                double dx = targetPos.x - barrage.boneX[i];
                double dy = targetPos.y - barrage.boneY[i];
                double dz = targetPos.z - barrage.boneZ[i];
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (dist < 1.0) {
                    // HIT!
                    barrage.boneHit[i] = true;
                    target.hurt(barrage.level.damageSources().magic(), 3.0f);

                    // Impact particles
                    barrage.level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                            2, 0.2, 0.2, 0.2, 0.1);
                    barrage.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            barrage.boneX[i], barrage.boneY[i], barrage.boneZ[i],
                            5, 0.2, 0.2, 0.2, 0.05);

                    // Bone shatter sound (stagger)
                    if (i % 5 == 0) {
                        barrage.level.playSound(null, target.blockPosition(),
                                SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + (float)(Math.random() * 0.4));
                    }
                    continue;
                }

                // Move toward target (fast homing)
                double speed = 2.0;
                barrage.boneX[i] += (dx / dist) * speed;
                barrage.boneY[i] += (dy / dist) * speed;
                barrage.boneZ[i] += (dz / dist) * speed;
            } else {
                // No target or dead: fly in stored direction
                if (barrage.boneDX[i] == 0 && barrage.boneDZ[i] == 0) {
                    // No direction set, fly forward
                    Vec3 look = barrage.owner.getLookAngle();
                    double angle = (Math.PI * 2.0 / BONE_COUNT) * i;
                    barrage.boneDX[i] = look.x + Math.cos(angle) * 0.5;
                    barrage.boneDY[i] = look.y * 0.3;
                    barrage.boneDZ[i] = look.z + Math.sin(angle) * 0.5;
                }
                barrage.boneX[i] += barrage.boneDX[i];
                barrage.boneY[i] += barrage.boneDY[i];
                barrage.boneZ[i] += barrage.boneDZ[i];

                // Check if hit any mob nearby during AOE scatter
                if (barrage.aoeMode) {
                    for (Entity e : barrage.level.getEntities(barrage.owner,
                            new AABB(barrage.boneX[i] - 1, barrage.boneY[i] - 1, barrage.boneZ[i] - 1,
                                    barrage.boneX[i] + 1, barrage.boneY[i] + 1, barrage.boneZ[i] + 1),
                            ent -> ent instanceof LivingEntity && ent != barrage.owner && !(ent instanceof Player))) {
                        ((LivingEntity) e).hurt(barrage.level.damageSources().magic(), 3.0f);
                        barrage.boneHit[i] = true;
                        barrage.level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                                e.getX(), e.getY() + 1, e.getZ(), 2, 0.2, 0.2, 0.2, 0.1);
                        break;
                    }
                }
            }

            if (!barrage.boneHit[i]) {
                // Render flying bone
                renderBone(barrage.level, barrage.boneX[i], barrage.boneY[i], barrage.boneZ[i], boneDust);
                // Trail
                barrage.level.sendParticles(trailDust,
                        barrage.boneX[i], barrage.boneY[i], barrage.boneZ[i],
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Whoosh sounds during barrage
        if (localTick % 8 == 0) {
            barrage.level.playSound(null, barrage.owner.blockPosition(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 1.5f + localTick * 0.01f);
        }

        // All hit or timeout
        if (allHit >= BONE_COUNT) {
            if (barrage.owner.isAlive()) {
                barrage.owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Tous les os ont touche leur cible!")
                        .withStyle(net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.BOLD));
            }
        }
    }

    private static void renderBone(ServerLevel level, double x, double y, double z, DustParticleOptions dust) {
        // Bone shape: vertical bar with knobs at top and bottom
        level.sendParticles(dust, x, y - 0.3, z, 1, 0, 0, 0, 0);
        level.sendParticles(dust, x, y - 0.15, z, 1, 0, 0, 0, 0);
        level.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(dust, x, y + 0.15, z, 1, 0, 0, 0, 0);
        level.sendParticles(dust, x, y + 0.3, z, 1, 0, 0, 0, 0);
        // Knobs
        level.sendParticles(dust, x - 0.1, y + 0.3, z, 1, 0, 0, 0, 0);
        level.sendParticles(dust, x + 0.1, y + 0.3, z, 1, 0, 0, 0, 0);
        level.sendParticles(dust, x - 0.1, y - 0.3, z, 1, 0, 0, 0, 0);
        level.sendParticles(dust, x + 0.1, y - 0.3, z, 1, 0, 0, 0, 0);
    }
}
