package com.noxgg.elementalpower.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ShadowFormManager {

    // Shadow states
    public enum ShadowState {
        NONE,           // Normal
        SHADOW,         // Invisible, attached to a mob as its shadow
        MATERIALIZED    // Visible as dark shadow form, disguised as a mob
    }

    public static class ShadowData {
        public ShadowState state;
        public LivingEntity attachedMob;      // The mob we're shadowing (state SHADOW)
        public EntityType<?> disguiseType;     // The mob type we're disguised as (state MATERIALIZED)
        public Mob disguiseEntity;             // The fake mob following us
        public String disguiseName;            // Name of the mob type

        public ShadowData() {
            this.state = ShadowState.NONE;
        }
    }

    private static final Map<UUID, ShadowData> shadowPlayers = new HashMap<>();

    private static final DustParticleOptions SHADOW_DUST = new DustParticleOptions(
            new Vector3f(0.05f, 0.0f, 0.05f), 1.5f);
    private static final DustParticleOptions DARK_DUST = new DustParticleOptions(
            new Vector3f(0.1f, 0.0f, 0.15f), 2.0f);

    public static ShadowData getData(UUID playerId) {
        return shadowPlayers.get(playerId);
    }

    public static boolean isInShadowForm(UUID playerId) {
        ShadowData data = shadowPlayers.get(playerId);
        return data != null && data.state == ShadowState.SHADOW;
    }

    public static boolean isMaterialized(UUID playerId) {
        ShadowData data = shadowPlayers.get(playerId);
        return data != null && data.state == ShadowState.MATERIALIZED;
    }

    /**
     * Toggle between NONE -> SHADOW -> MATERIALIZED -> NONE
     */
    public static void toggleShadowForm(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ShadowData data = shadowPlayers.computeIfAbsent(playerId, k -> new ShadowData());
        ServerLevel level = player.serverLevel();

        switch (data.state) {
            case NONE -> {
                // Enter SHADOW mode: become invisible shadow attached to looked-at mob
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                LivingEntity target = null;
                double closest = 30.0;

                for (Entity entity : level.getEntities(player,
                        player.getBoundingBox().inflate(30),
                        e -> e instanceof LivingEntity && e != player)) {
                    LivingEntity living = (LivingEntity) entity;
                    Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(eye);
                    double dist = toEntity.length();
                    if (dist > closest) continue;
                    Vec3 toEntityNorm = toEntity.normalize();
                    if (look.dot(toEntityNorm) > 0.9) {
                        closest = dist;
                        target = living;
                    }
                }

                if (target == null) {
                    player.sendSystemMessage(Component.literal("Aucun mob en vue pour devenir son ombre!")
                            .withStyle(ChatFormatting.GRAY));
                    return;
                }

                data.state = ShadowState.SHADOW;
                data.attachedMob = target;

                // Make player invisible and silent
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));

                // Teleport to mob's shadow position
                player.teleportTo(target.getX(), target.getY(), target.getZ());

                // Shadow merge effect
                level.sendParticles(SHADOW_DUST,
                        player.getX(), player.getY() + 1, player.getZ(),
                        30, 0.5, 1.0, 0.5, 0.05);
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        20, 0.3, 0.3, 0.3, 0.02);
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                        player.getX(), player.getY(), player.getZ(),
                        10, 0.3, 0.1, 0.3, 0.01);

                level.playSound(null, player.blockPosition(),
                        SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 0.3f);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.PLAYERS, 1.5f, 0.5f);

                String mobName = target.getType().getDescription().getString();
                player.sendSystemMessage(Component.literal(">> Forme d'Ombre activee! ")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)
                        .append(Component.literal("Tu es l'ombre de " + mobName + ". L pour te materialiser.")
                                .withStyle(ChatFormatting.GRAY)));
            }

            case SHADOW -> {
                // Enter MATERIALIZED mode: detach from mob, become dark shadow form
                // Player needs to hit a mob to take its appearance (handled in onPlayerHitMob)
                data.state = ShadowState.MATERIALIZED;
                data.attachedMob = null;

                // Keep invisible until they hit a mob to absorb its form
                // But show dark shadow particles

                // Dark materialization burst
                level.sendParticles(DARK_DUST,
                        player.getX(), player.getY() + 1, player.getZ(),
                        40, 0.5, 1.0, 0.5, 0.08);
                level.sendParticles(ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        25, 0.4, 0.6, 0.4, 0.03);
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        15, 0.3, 0.5, 0.3, 0.02);

                level.playSound(null, player.blockPosition(),
                        SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.0f, 1.5f);

                player.sendSystemMessage(Component.literal(">> Materialisation d'Ombre! ")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                        .append(Component.literal("Frappe un mob pour prendre son apparence. L pour redevenir normal.")
                                .withStyle(ChatFormatting.GRAY)));
            }

            case MATERIALIZED -> {
                // Return to NORMAL
                exitShadowForm(player);
            }
        }
    }

    /**
     * When a materialized shadow player hits a mob, absorb its appearance.
     */
    public static void onPlayerHitMob(ServerPlayer player, LivingEntity target) {
        UUID playerId = player.getUUID();
        ShadowData data = shadowPlayers.get(playerId);
        if (data == null || data.state != ShadowState.MATERIALIZED) return;
        if (data.disguiseEntity != null) return; // Already disguised

        ServerLevel level = player.serverLevel();

        // Store the disguise type
        data.disguiseType = target.getType();
        data.disguiseName = target.getType().getDescription().getString();

        // Spawn a fake mob of the same type that follows the player
        Entity spawned = data.disguiseType.create(level);
        if (spawned instanceof Mob mob) {
            mob.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            mob.setInvulnerable(true);
            mob.setSilent(true);
            // Dark shadow name
            mob.setCustomName(Component.literal("\u00A78\u00A7l[Ombre] \u00A70" + data.disguiseName));
            mob.setCustomNameVisible(true);
            // Make it dark with glowing dark effect
            mob.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Integer.MAX_VALUE, 0, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

            level.addFreshEntity(mob);
            data.disguiseEntity = mob;
        }

        // Absorption effect
        level.sendParticles(DARK_DUST,
                target.getX(), target.getY() + 1, target.getZ(),
                30, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1, player.getZ(),
                20, 0.3, 0.5, 0.3, 0.03);

        // Dark beam from mob to player (absorption)
        Vec3 targetPos = target.position();
        for (int i = 0; i < 15; i++) {
            double t = i / 15.0;
            double px = targetPos.x + (player.getX() - targetPos.x) * t;
            double py = targetPos.y + 1 + (player.getY() + 1 - targetPos.y - 1) * t;
            double pz = targetPos.z + (player.getZ() - targetPos.z) * t;
            level.sendParticles(SHADOW_DUST, px, py, pz, 3, 0.05, 0.05, 0.05, 0.01);
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.0f, 0.5f);

        player.sendSystemMessage(Component.literal(">> Apparence absorbee: ")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)
                .append(Component.literal(data.disguiseName + "!")
                        .withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" L pour redevenir normal.")
                        .withStyle(ChatFormatting.GRAY)));
    }

    public static void exitShadowForm(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ShadowData data = shadowPlayers.remove(playerId);
        ServerLevel level = player.serverLevel();

        // Remove effects
        player.removeEffect(MobEffects.INVISIBILITY);
        // Keep night vision from darkness passive, just reapply short duration
        player.removeEffect(MobEffects.NIGHT_VISION);

        // Kill disguise entity if exists
        if (data != null && data.disguiseEntity != null && !data.disguiseEntity.isRemoved()) {
            // Death poof
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    data.disguiseEntity.getX(), data.disguiseEntity.getY() + 0.5, data.disguiseEntity.getZ(),
                    20, 0.3, 0.5, 0.3, 0.05);
            data.disguiseEntity.discard();
        }

        // Reappearance effect
        level.sendParticles(DARK_DUST,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 0.5, 1.0, 0.5, 0.08);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 0.5, player.getZ(),
                15, 0.3, 0.5, 0.3, 0.03);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1.5, player.getZ(),
                10, 0.2, 0.3, 0.2, 0.02);

        level.playSound(null, player.blockPosition(),
                SoundEvents.PHANTOM_DEATH, SoundSource.PLAYERS, 1.0f, 0.5f);

        player.sendSystemMessage(Component.literal(">> Forme d'Ombre desactivee.")
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * Called every server tick to update shadow forms.
     */
    public static void tick() {
        Iterator<Map.Entry<UUID, ShadowData>> it = shadowPlayers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ShadowData> entry = it.next();
            ShadowData data = entry.getValue();

            // Find the player
            ServerPlayer player = null;
            if (data.state == ShadowState.SHADOW && data.attachedMob != null) {
                player = (ServerPlayer) data.attachedMob.level().getPlayerByUUID(entry.getKey());
            } else if (data.state == ShadowState.MATERIALIZED && data.disguiseEntity != null) {
                player = (ServerPlayer) data.disguiseEntity.level().getPlayerByUUID(entry.getKey());
            }

            if (player == null || player.isRemoved()) {
                // Cleanup
                if (data.disguiseEntity != null && !data.disguiseEntity.isRemoved()) {
                    data.disguiseEntity.discard();
                }
                it.remove();
                continue;
            }

            ServerLevel level = player.serverLevel();

            if (data.state == ShadowState.SHADOW) {
                // === SHADOW: Follow the mob as its shadow ===
                if (data.attachedMob == null || data.attachedMob.isRemoved() || !data.attachedMob.isAlive()) {
                    // Mob died, exit shadow form
                    exitShadowForm(player);
                    it.remove();
                    continue;
                }

                LivingEntity mob = data.attachedMob;

                // Teleport player to mob's position (shadow stays at feet)
                player.teleportTo(mob.getX(), mob.getY(), mob.getZ());
                player.setDeltaMovement(0, 0, 0);
                player.hurtMarked = true;

                // Keep invisible
                if (!player.hasEffect(MobEffects.INVISIBILITY)) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                }

                // Shadow particles at mob's feet (dark flat particles)
                if (player.tickCount % 3 == 0) {
                    // Dark shadow under the mob
                    double mx = mob.getX();
                    double my = mob.getY() + 0.05;
                    double mz = mob.getZ();
                    double shadowSize = mob.getBbWidth() * 0.8;

                    for (int i = 0; i < 5; i++) {
                        double ox = (level.random.nextDouble() - 0.5) * shadowSize * 2;
                        double oz = (level.random.nextDouble() - 0.5) * shadowSize * 2;
                        level.sendParticles(SHADOW_DUST,
                                mx + ox, my, mz + oz,
                                1, 0, 0, 0, 0);
                    }

                    // Occasional dark wisps
                    if (player.tickCount % 15 == 0) {
                        level.sendParticles(ParticleTypes.SCULK_SOUL,
                                mx, my + 0.1, mz,
                                1, 0.2, 0, 0.2, 0.005);
                    }
                }
            }

            if (data.state == ShadowState.MATERIALIZED) {
                // === MATERIALIZED: Dark shadow form ===

                // Keep invisible (the disguise mob represents us visually)
                if (!player.hasEffect(MobEffects.INVISIBILITY)) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                }

                // Update disguise entity to follow player
                if (data.disguiseEntity != null && !data.disguiseEntity.isRemoved()) {
                    Mob disguise = data.disguiseEntity;
                    disguise.teleportTo(player.getX(), player.getY(), player.getZ());
                    disguise.setYRot(player.getYRot());
                    disguise.setYHeadRot(player.getYHeadRot());
                    disguise.setXRot(player.getXRot());

                    // Dark aura particles around the disguise
                    if (player.tickCount % 4 == 0) {
                        level.sendParticles(SHADOW_DUST,
                                disguise.getX(), disguise.getY() + disguise.getBbHeight() / 2, disguise.getZ(),
                                3, 0.3, 0.4, 0.3, 0.01);
                        level.sendParticles(ParticleTypes.SMOKE,
                                disguise.getX(), disguise.getY() + 0.3, disguise.getZ(),
                                2, 0.2, 0.1, 0.2, 0.005);
                    }

                    // Dark dripping effect
                    if (player.tickCount % 10 == 0) {
                        level.sendParticles(ParticleTypes.SCULK_SOUL,
                                disguise.getX(), disguise.getY() + disguise.getBbHeight(), disguise.getZ(),
                                1, 0.1, 0.1, 0.1, 0.002);
                    }
                } else if (data.disguiseEntity == null) {
                    // Not yet disguised (hasn't hit a mob yet) - show dark form particles on player
                    if (player.tickCount % 3 == 0) {
                        level.sendParticles(SHADOW_DUST,
                                player.getX(), player.getY() + 1, player.getZ(),
                                4, 0.2, 0.5, 0.2, 0.02);
                        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                                player.getX(), player.getY() + 0.5, player.getZ(),
                                2, 0.15, 0.3, 0.15, 0.01);
                    }
                }
            }
        }
    }

    public static void onPlayerLogout(UUID playerId) {
        ShadowData data = shadowPlayers.remove(playerId);
        if (data != null && data.disguiseEntity != null && !data.disguiseEntity.isRemoved()) {
            data.disguiseEntity.discard();
        }
    }
}
