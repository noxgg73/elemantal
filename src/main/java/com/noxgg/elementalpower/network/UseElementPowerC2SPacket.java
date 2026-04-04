package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseElementPowerC2SPacket {
    public UseElementPowerC2SPacket() {}

    public UseElementPowerC2SPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                ElementType element = data.getElement();
                if (element == ElementType.NONE) return;
                if (player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem()) ||
                        player.tickCount % 20 != 0) {
                    // Basic cooldown check
                }

                ServerLevel level = player.serverLevel();
                Vec3 look = player.getLookAngle();

                switch (element) {
                    case FIRE -> {
                        SmallFireball fireball = new SmallFireball(level, player,
                                look.x * 2, look.y * 2, look.z * 2);
                        fireball.setPos(player.getX() + look.x, player.getEyeY() - 0.1, player.getZ() + look.z);
                        level.addFreshEntity(fireball);
                        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    case WATER -> {
                        player.heal(8.0f);
                        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                        level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    case EARTH -> {
                        // 50 bones behind the player - if looking at sky: AOE scatter, else: target mob
                        boolean lookingUp = look.y > 0.7; // looking at sky
                        net.minecraft.world.entity.LivingEntity boneTarget = null;

                        if (!lookingUp) {
                            // Find mob being looked at
                            Vec3 eye = player.getEyePosition();
                            double closest = 30.0;
                            for (net.minecraft.world.entity.Entity entity : level.getEntities(player,
                                    player.getBoundingBox().inflate(30),
                                    e -> e instanceof net.minecraft.world.entity.LivingEntity && e != player
                                            && !(e instanceof net.minecraft.world.entity.player.Player))) {
                                net.minecraft.world.entity.LivingEntity living = (net.minecraft.world.entity.LivingEntity) entity;
                                Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(eye);
                                double dist = toEntity.length();
                                if (dist > closest) continue;
                                if (look.dot(toEntity.normalize()) > 0.9) {
                                    closest = dist;
                                    boneTarget = living;
                                }
                            }

                            if (boneTarget == null) {
                                // No target found, force AOE mode
                                lookingUp = true;
                            }
                        }

                        com.noxgg.elementalpower.world.BoneBarrageManager.addBarrage(
                                new com.noxgg.elementalpower.world.BoneBarrageManager.BoneBarrage(
                                        level, player, lookingUp, boneTarget));

                        // Summoning particles burst
                        var summonDust = new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(0.9f, 0.9f, 0.85f), 2.5f);
                        level.sendParticles(summonDust,
                                player.getX(), player.getY() + 2, player.getZ(),
                                30, 1, 1, 1, 0.05);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                player.getX(), player.getY() + 2, player.getZ(),
                                15, 0.5, 0.5, 0.5, 0.1);

                        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.5f, 1.0f);

                        if (lookingUp) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> 50 Os invoques! ")
                                    .withStyle(net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.BOLD)
                                    .append(net.minecraft.network.chat.Component.literal("Dispersion totale - tous les mobs de la zone seront elimines!")
                                            .withStyle(net.minecraft.ChatFormatting.RED)));
                        } else {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> 50 Os invoques! ")
                                    .withStyle(net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.BOLD)
                                    .append(net.minecraft.network.chat.Component.literal("Ils foncent sur la cible!")
                                            .withStyle(net.minecraft.ChatFormatting.GOLD)));
                        }
                    }
                    case AIR -> {
                        player.push(0, 2.0, 0);
                        player.hurtMarked = true;
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2));
                        level.playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0f, 1.5f);
                    }
                    case SPACE -> {
                        // Teleport 15 blocks in look direction
                        Vec3 target = player.position().add(look.scale(15));
                        player.teleportTo(target.x, target.y, target.z);
                        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    case TIME -> {
                        // GIANT GREEN CRACKLING ENERGY DOME - 50 blocks, impenetrable barrier
                        double domeRadius = 25.0;

                        // Create persistent dome (15 seconds = 300 ticks)
                        com.noxgg.elementalpower.world.TimeDomeManager.addDome(
                                new com.noxgg.elementalpower.world.TimeDomeManager.ActiveDome(
                                        player.getX(), player.getY(), player.getZ(),
                                        domeRadius, level, player, 300));

                        // Initial paralysis of all mobs already inside
                        level.getEntities(player, player.getBoundingBox().inflate(domeRadius), e -> {
                            if (e == player) return false;
                            double dx = e.getX() - player.getX();
                            double dy = e.getY() - player.getY();
                            double dz = e.getZ() - player.getZ();
                            return (dx * dx + dy * dy + dz * dz) <= domeRadius * domeRadius;
                        }).forEach(e -> {
                            if (e instanceof net.minecraft.world.entity.LivingEntity living) {
                                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 127));
                                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 127));
                                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 300, 127));
                                living.setDeltaMovement(0, 0, 0);
                                living.hurtMarked = true;
                            }
                        });

                        // Player buffs
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 4));
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 3));

                        // Activation sounds
                        level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0f, 0.3f);
                        level.playSound(null, player.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0f, 0.5f);
                        level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5f, 2.0f);
                        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.5f);
                    }
                    case POISON -> {
                        // Poison all nearby enemies
                        level.getEntities(player, player.getBoundingBox().inflate(8), e -> e != player)
                                .forEach(e -> {
                                    if (e instanceof net.minecraft.world.entity.LivingEntity living) {
                                        living.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
                                        living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                                    }
                                });
                        level.playSound(null, player.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.8f);
                    }
                    case DARKNESS -> {
                        // GIANT SHADOW HAND - scales with level and souls
                        final float[] damageHolder = {22.0f};
                        final float[] scaleHolder = {1.0f};
                        player.getCapability(com.noxgg.elementalpower.element.PlayerElementProvider.PLAYER_ELEMENT).ifPresent(elemData -> {
                            damageHolder[0] = 22.0f + elemData.getLevel() * 0.5f + elemData.getSouls() * 0.02f;
                            scaleHolder[0] = 1.0f + elemData.getLevel() * 0.02f;
                        });
                        float handDamage = damageHolder[0];
                        float handScale = scaleHolder[0];

                        Vec3 lookDir = player.getLookAngle();
                        Vec3 flatLook = new Vec3(lookDir.x, 0, lookDir.z).normalize();
                        Vec3 handCenter = player.position().add(flatLook.scale(8));
                        double baseY = handCenter.y;

                        // Perpendicular direction for hand width
                        double perpX = -flatLook.z;
                        double perpZ = flatLook.x;

                        // Hand rises from ground: wrist at bottom, fingers at top
                        // The hand faces the player, palm open, then closes to crush

                        // === WRIST (base of hand, 2 blocks wide) ===
                        for (double w = -0.8; w <= 0.8; w += 0.3) {
                            for (double h = 0; h <= 1.5; h += 0.3) {
                                double wx = handCenter.x + perpX * w;
                                double wz = handCenter.z + perpZ * w;
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                        wx, baseY + h, wz, 2, 0.05, 0.05, 0.05, 0.005);
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                                        wx, baseY + h, wz, 1, 0.02, 0.02, 0.02, 0.005);
                            }
                        }

                        // === PALM (wide rectangle, 3 blocks wide, 2.5 blocks tall) ===
                        for (double w = -1.5; w <= 1.5; w += 0.25) {
                            for (double h = 1.5; h <= 4.0; h += 0.25) {
                                double px = handCenter.x + perpX * w;
                                double pz = handCenter.z + perpZ * w;
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                                        px, baseY + h, pz, 1, 0.03, 0.03, 0.03, 0.002);
                                if (h > 2.0 && h < 3.5 && Math.abs(w) < 1.0) {
                                    // Denser center of palm
                                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                            px, baseY + h, pz, 1, 0.02, 0.02, 0.02, 0.001);
                                }
                            }
                        }

                        // === 4 FINGERS (index, middle, ring, pinky) ===
                        double[] fingerOffsets = {-1.1, -0.37, 0.37, 1.1};
                        double[] fingerLengths = {2.8, 3.2, 3.0, 2.4};

                        for (int f = 0; f < 4; f++) {
                            double fOffset = fingerOffsets[f];
                            double fLen = fingerLengths[f];
                            double fingerBaseY = baseY + 4.0;

                            // Finger width
                            for (double seg = 0; seg < fLen; seg += 0.2) {
                                // Fingers curve inward (closing hand) at the tips
                                double curl = 0;
                                if (seg > fLen * 0.6) {
                                    curl = (seg - fLen * 0.6) * 0.8;
                                }
                                double fx = handCenter.x + perpX * fOffset - flatLook.x * curl;
                                double fz = handCenter.z + perpZ * fOffset - flatLook.z * curl;
                                double fy = fingerBaseY + seg - curl * 0.3;

                                // Finger thickness
                                for (double t = -0.15; t <= 0.15; t += 0.15) {
                                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                                            fx + perpX * t, fy, fz + perpZ * t, 1, 0.02, 0.02, 0.02, 0.001);
                                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                            fx + perpX * t, fy, fz + perpZ * t, 1, 0.03, 0.03, 0.03, 0.002);
                                }

                                // Knuckle joints (thicker at segment boundaries)
                                if (Math.abs(seg - fLen * 0.33) < 0.15 || Math.abs(seg - fLen * 0.66) < 0.15) {
                                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                                            fx, fy, fz, 3, 0.1, 0.05, 0.1, 0.005);
                                }
                            }
                        }

                        // === THUMB (shorter, angled outward) ===
                        double thumbBaseY = baseY + 2.0;
                        for (double seg = 0; seg < 2.0; seg += 0.2) {
                            double thumbAngle = -0.6;
                            double curl = seg > 1.2 ? (seg - 1.2) * 1.0 : 0;
                            double tx = handCenter.x + perpX * (-1.5 - seg * 0.3) - flatLook.x * curl;
                            double tz = handCenter.z + perpZ * (-1.5 - seg * 0.3) - flatLook.z * curl;
                            double ty = thumbBaseY + seg * 0.8;

                            for (double t = -0.12; t <= 0.12; t += 0.12) {
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                                        tx, ty, tz + t, 1, 0.02, 0.02, 0.02, 0.001);
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                        tx, ty, tz + t, 1, 0.03, 0.03, 0.03, 0.002);
                            }
                        }

                        // === SHADOW AURA around the hand ===
                        for (int i = 0; i < 30; i++) {
                            double ax = handCenter.x + (level.random.nextDouble() - 0.5) * 5;
                            double ay = baseY + level.random.nextDouble() * 8;
                            double az = handCenter.z + (level.random.nextDouble() - 0.5) * 5;
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                                    ax, ay, az, 1, 0, -0.05, 0, 0.02);
                        }

                        // === IMPACT: slam particles on the ground ===
                        for (int i = 0; i < 50; i++) {
                            double ix = handCenter.x + (level.random.nextDouble() - 0.5) * 4;
                            double iz = handCenter.z + (level.random.nextDouble() - 0.5) * 4;
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                                    ix, baseY + 0.2, iz, 1, 0.1, 0.3, 0.1, 0.08);
                        }
                        // Ground crack effect
                        for (double angle = 0; angle < Math.PI * 2; angle += 0.3) {
                            for (double dist = 0.5; dist < 3.0; dist += 0.4) {
                                double cx = handCenter.x + Math.cos(angle) * dist;
                                double cz = handCenter.z + Math.sin(angle) * dist;
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                                        cx, baseY + 0.1, cz, 1, 0, 0.05, 0, 0.001);
                            }
                        }

                        // === DAMAGE all enemies under the hand ===
                        var nearbyEntities = level.getEntities(player,
                                new AABB(handCenter.x - 4, baseY - 1, handCenter.z - 4,
                                        handCenter.x + 4, baseY + 8, handCenter.z + 4),
                                e -> e instanceof net.minecraft.world.entity.LivingEntity && e != player);

                        for (var entity : nearbyEntities) {
                            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                                living.hurt(level.damageSources().magic(), handDamage);
                                living.push(0, -2.5, 0);
                                living.hurtMarked = true;
                                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 1));
                                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
                                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                                        living.getX(), living.getY() + 1, living.getZ(), 5, 0.5, 0.5, 0.5, 0);
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                                        living.getX(), living.getY() + 1.5, living.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                            }
                        }

                        // Player buffs
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));

                        // Sounds
                        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 0.6f);
                        level.playSound(null, BlockPos.containing(handCenter), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 0.5f);
                    }
                    case LIGHT -> {
                        // Heal self + damage undead nearby
                        player.heal(10.0f);
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 2));
                        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                        level.getEntities(player, player.getBoundingBox().inflate(10), e -> e != player)
                                .forEach(e -> {
                                    if (e instanceof net.minecraft.world.entity.monster.Monster monster) {
                                        monster.hurt(level.damageSources().magic(), 8.0f);
                                        monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                                    }
                                });
                        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
                    }
                    case DEMON -> {
                        // Check if player is Alastor (reincarnated)
                        final boolean[] isAlastorHolder = {false};
                        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(elemData -> {
                            isAlastorHolder[0] = elemData.isAlastor();
                        });

                        if (isAlastorHolder[0]) {
                            // ALASTOR: Shadow Tentacles (R key)
                            com.noxgg.elementalpower.world.AlastorManager.castShadowTentacles(player);
                        } else {
                            // Normal Demon: Fire aura + strength
                            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 2));
                            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
                            level.getEntities(player, player.getBoundingBox().inflate(6), e -> e != player)
                                    .forEach(e -> {
                                        if (e instanceof net.minecraft.world.entity.LivingEntity living) {
                                            living.setSecondsOnFire(10);
                                            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                                        }
                                    });
                            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.2f);
                        }
                    }
                    case NATURE -> {
                        // Mega regen + grow plants nearby
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 3));
                        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 1));
                        // Grow flowers around
                        for (int dx = -3; dx <= 3; dx++) {
                            for (int dz = -3; dz <= 3; dz++) {
                                BlockPos pos = player.blockPosition().offset(dx, 0, dz);
                                BlockPos below = pos.below();
                                if (level.getBlockState(pos).isAir() &&
                                        level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
                                    if (Math.random() > 0.5) {
                                        level.setBlock(pos, Blocks.POPPY.defaultBlockState(), 3);
                                    }
                                }
                            }
                        }
                        level.playSound(null, player.blockPosition(), SoundEvents.FLOWERING_AZALEA_PLACE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    case LIGHTNING -> {
                        // Strike lightning where player is looking
                        Vec3 targetPos = player.position().add(look.scale(10));
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                        if (lightning != null) {
                            lightning.moveTo(targetPos.x, targetPos.y, targetPos.z);
                            lightning.setCause(player);
                            level.addFreshEntity(lightning);
                        }
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 3));
                        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    case ROYAL -> {
                        // ROYAL CASTLE: build a grand castle 60 blocks in front
                        Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
                        BlockPos castleCenter = player.blockPosition().offset(
                                (int)(flatLook.x * 60), 0, (int)(flatLook.z * 60));

                        // Find ground level at castle position
                        int groundY = castleCenter.getY();
                        for (int checkY = castleCenter.getY() + 10; checkY > castleCenter.getY() - 10; checkY--) {
                            if (!level.getBlockState(castleCenter.atY(checkY)).isAir()
                                    && level.getBlockState(castleCenter.atY(checkY + 1)).isAir()) {
                                groundY = checkY;
                                break;
                            }
                        }

                        // Generate the castle
                        com.noxgg.elementalpower.world.CastleGenerator.generate(level, castleCenter, groundY);

                        // Royal particles at player
                        var goldDust = new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(1.0f, 0.84f, 0.0f), 2.5f);
                        level.sendParticles(goldDust,
                                player.getX(), player.getY() + 2, player.getZ(),
                                30, 0.5, 0.5, 0.5, 0.05);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                                player.getX(), player.getY() + 1, player.getZ(),
                                50, 1, 2, 1, 0.3);

                        // Golden particle trail to castle
                        for (int t = 0; t < 60; t++) {
                            double progress = t / 60.0;
                            double px = player.getX() + flatLook.x * 60 * progress;
                            double pz = player.getZ() + flatLook.z * 60 * progress;
                            level.sendParticles(goldDust, px, groundY + 2, pz, 3, 0.2, 0.5, 0.2, 0.02);
                        }

                        // Sounds
                        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0f, 1.0f);
                        level.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).get(), SoundSource.PLAYERS, 2.0f, 0.8f);
                        level.playSound(null, castleCenter, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0f, 0.5f);

                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Chateau Royal erige! ")
                                .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)
                                .append(net.minecraft.network.chat.Component.literal("Votre forteresse vous attend, Majeste!")
                                        .withStyle(net.minecraft.ChatFormatting.YELLOW)));
                    }
                    default -> {}
                }
            });
        });
        return true;
    }
}
