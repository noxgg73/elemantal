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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

public class SpiritManager {
    private static final Set<UUID> spiritPlayers = new HashSet<>();
    private static final Map<UUID, StrangleTarget> strangles = new HashMap<>();

    private static final DustParticleOptions GHOST_WHITE = new DustParticleOptions(
            new Vector3f(0.8f, 0.85f, 0.95f), 1.5f);

    public static class StrangleTarget {
        public final LivingEntity target;
        public final ServerPlayer caster;
        public int ticksRemaining; // 100 ticks = 5 sec

        public StrangleTarget(LivingEntity target, ServerPlayer caster) {
            this.target = target;
            this.caster = caster;
            this.ticksRemaining = 100;
        }
    }

    public static void enterSpiritForm(ServerPlayer player) {
        spiritPlayers.add(player.getUUID());
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false));

        ServerLevel level = player.serverLevel();
        // Ghost transformation particles
        level.sendParticles(GHOST_WHITE,
                player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.05);
        level.sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + 1, player.getZ(), 15, 0.3, 0.5, 0.3, 0.08);

        level.playSound(null, player.blockPosition(),
                SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.5f);
    }

    public static void exitSpiritForm(ServerPlayer player) {
        spiritPlayers.remove(player.getUUID());
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);

        ServerLevel level = player.serverLevel();
        level.sendParticles(GHOST_WHITE,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.5, 0.3, 0.03);

        level.playSound(null, player.blockPosition(),
                SoundEvents.PHANTOM_DEATH, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static boolean isSpirit(UUID playerId) {
        return spiritPlayers.contains(playerId);
    }

    public static void startStrangle(ServerPlayer caster, LivingEntity target) {
        // Don't strangle creative players
        if (target instanceof Player p && p.isCreative()) {
            caster.sendSystemMessage(net.minecraft.network.chat.Component.literal("Cette cible est intouchable!")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }

        strangles.put(caster.getUUID(), new StrangleTarget(target, caster));

        ServerLevel level = caster.serverLevel();
        level.playSound(null, target.blockPosition(),
                SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 1.5f, 0.3f);

        caster.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Etranglement en cours... 5 secondes")
                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.BOLD));
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, StrangleTarget>> it = strangles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, StrangleTarget> entry = it.next();
            StrangleTarget st = entry.getValue();

            if (st.target == null || st.target.isRemoved() || st.caster == null || st.caster.isRemoved()) {
                it.remove();
                continue;
            }

            st.ticksRemaining--;
            ServerLevel level = st.caster.serverLevel();

            // Keep caster near target
            double dist = st.caster.distanceTo(st.target);
            if (dist > 3.0) {
                Vec3 dir = st.target.position().subtract(st.caster.position()).normalize();
                st.caster.teleportTo(st.target.getX() - dir.x * 1.5,
                        st.target.getY(), st.target.getZ() - dir.z * 1.5);
            }

            // Paralyze target
            st.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));
            st.target.setDeltaMovement(0, st.target.getDeltaMovement().y, 0);
            st.target.hurtMarked = true;

            // Strangle particles around target's neck area
            if (st.ticksRemaining % 3 == 0) {
                double neckY = st.target.getY() + st.target.getBbHeight() * 0.8;
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 / 4) * i + st.ticksRemaining * 0.1;
                    double r = 0.3;
                    level.sendParticles(GHOST_WHITE,
                            st.target.getX() + Math.cos(angle) * r,
                            neckY,
                            st.target.getZ() + Math.sin(angle) * r,
                            1, 0.02, 0.02, 0.02, 0.001);
                }
                // Dark smoke rising
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        st.target.getX(), neckY + 0.3, st.target.getZ(),
                        2, 0.1, 0.1, 0.1, 0.01);
            }

            // Target gasping for air particles
            if (st.ticksRemaining % 10 == 0) {
                level.sendParticles(ParticleTypes.SMOKE,
                        st.target.getX(), st.target.getY() + st.target.getBbHeight(), st.target.getZ(),
                        3, 0.1, 0.1, 0.1, 0.02);
                // Choking sound
                level.playSound(null, st.target.blockPosition(),
                        SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.8f, 0.3f);
            }

            // Progressive damage
            if (st.ticksRemaining % 20 == 0) {
                st.target.hurt(level.damageSources().magic(), 2.0f);
            }

            // Countdown display on caster
            if (st.ticksRemaining % 20 == 0) {
                int secondsLeft = st.ticksRemaining / 20;
                st.caster.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Etranglement: " + secondsLeft + "s")
                                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY), true);
            }

            // === DEATH at 0 ticks ===
            if (st.ticksRemaining <= 0) {
                killWithScreamer(st, level);
                it.remove();
            }
        }

        // Spirit form ambient particles
        for (UUID id : spiritPlayers) {
            // Particles handled per-player in tick events
        }
    }

    private static void killWithScreamer(StrangleTarget st, ServerLevel level) {
        double tx = st.target.getX();
        double ty = st.target.getY();
        double tz = st.target.getZ();

        // === UNDEAD SCREAMER EFFECT ===

        // Screen shake equivalent: flash + darkness on nearby players
        level.players().forEach(p -> {
            if (p.distanceToSqr(tx, ty, tz) < 30 * 30) {
                ((ServerPlayer)p).addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }
        });

        // Giant skull of smoke and souls
        for (int i = 0; i < 50; i++) {
            // Skull shape: two eye holes and mouth
            double sx = tx + (Math.random() - 0.5) * 3;
            double sy = ty + 2 + Math.random() * 3;
            double sz = tz + (Math.random() - 0.5) * 3;
            level.sendParticles(ParticleTypes.LARGE_SMOKE, sx, sy, sz, 3, 0.2, 0.2, 0.2, 0.02);
            level.sendParticles(ParticleTypes.SOUL, sx, sy, sz, 1, 0.1, 0.1, 0.1, 0.05);
        }

        // Eye sockets (bright)
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                tx - 0.5, ty + 4, tz - 0.5, 8, 0.15, 0.1, 0.05, 0.01);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                tx + 0.5, ty + 4, tz - 0.5, 8, 0.15, 0.1, 0.05, 0.01);

        // Screaming mouth
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                tx, ty + 3, tz - 0.5, 10, 0.3, 0.15, 0.05, 0.02);

        // Soul explosion
        level.sendParticles(ParticleTypes.SOUL,
                tx, ty + 2, tz, 40, 1, 2, 1, 0.15);
        level.sendParticles(GHOST_WHITE,
                tx, ty + 1, tz, 30, 1.5, 1, 1.5, 0.1);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                tx, ty + 1, tz, 20, 0.5, 1, 0.5, 0.08);

        // Death scream sounds (layered for terrifying effect)
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 2.0f, 0.3f);
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.PHANTOM_DEATH, SoundSource.PLAYERS, 2.0f, 0.2f);
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 0.3f);

        // Kill the target
        st.target.hurt(level.damageSources().magic(), 999.0f);

        // Exit spirit form for caster
        exitSpiritForm(st.caster);

        st.caster.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(">> L'esprit a fauche une vie...")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.BOLD));
    }
}
