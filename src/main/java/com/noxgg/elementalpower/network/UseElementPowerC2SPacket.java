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
                        BlockPos base = player.blockPosition().offset(
                                (int)(look.x * 3), 0, (int)(look.z * 3));
                        for (int y = 0; y < 4; y++) {
                            for (int x = -1; x <= 1; x++) {
                                BlockPos wallPos = base.offset((int)(look.z * x), y, (int)(-look.x * x));
                                if (level.getBlockState(wallPos).isAir()) {
                                    level.setBlock(wallPos, Blocks.STONE.defaultBlockState(), 3);
                                }
                            }
                        }
                        level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);
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
                        // Slow all nearby entities
                        level.getEntities(player, player.getBoundingBox().inflate(10), e -> e != player)
                                .forEach(e -> {
                                    if (e instanceof net.minecraft.world.entity.LivingEntity living) {
                                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
                                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                                    }
                                });
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 3));
                        level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 0.5f);
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
                        // Giant Shadow Hand - crushes the nearest enemy
                        Vec3 lookDir = player.getLookAngle();
                        Vec3 handTarget = player.position().add(lookDir.scale(8));

                        // Find nearest entity in look direction
                        var nearbyEntities = level.getEntities(player,
                                new AABB(handTarget.x - 5, handTarget.y - 5, handTarget.z - 5,
                                        handTarget.x + 5, handTarget.y + 5, handTarget.z + 5),
                                e -> e instanceof net.minecraft.world.entity.LivingEntity && e != player);

                        // Dark particles rising from ground (shadow hand effect)
                        for (int i = 0; i < 60; i++) {
                            double px = handTarget.x + (level.random.nextDouble() - 0.5) * 3;
                            double py = handTarget.y + level.random.nextDouble() * 5;
                            double pz = handTarget.z + (level.random.nextDouble() - 0.5) * 3;
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                    px, py, pz, 1, 0, 0.1, 0, 0.02);
                        }
                        // Shadow fingers (5 lines of particles going up then slamming down)
                        for (int finger = 0; finger < 5; finger++) {
                            double angle = (Math.PI * 2 / 5) * finger;
                            double fx = handTarget.x + Math.cos(angle) * 2;
                            double fz = handTarget.z + Math.sin(angle) * 2;
                            for (int h = 0; h < 8; h++) {
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                                        fx, handTarget.y + h * 0.5, fz, 2, 0.1, 0, 0.1, 0.01);
                            }
                        }
                        // Palm slam particles
                        for (int i = 0; i < 40; i++) {
                            double px = handTarget.x + (level.random.nextDouble() - 0.5) * 4;
                            double pz = handTarget.z + (level.random.nextDouble() - 0.5) * 4;
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                                    px, handTarget.y + 0.5, pz, 1, 0, 0.2, 0, 0.05);
                        }

                        // Crush: massive damage + effects on all enemies in zone
                        if (!nearbyEntities.isEmpty()) {
                            for (var entity : nearbyEntities) {
                                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                                    // Slam damage (20 = 10 hearts)
                                    living.hurt(level.damageSources().magic(), 20.0f);
                                    // Slam the entity into the ground
                                    living.push(0, -2.0, 0);
                                    living.hurtMarked = true;
                                    // Darkness + slowness (crushed)
                                    living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 1));
                                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
                                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                                    // Impact particles on entity
                                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                                            living.getX(), living.getY() + 1, living.getZ(), 3, 0.5, 0.5, 0.5, 0);
                                }
                            }
                        }

                        // Also give player invisibility + night vision
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));

                        // Sound: warden roar for the shadow hand
                        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 0.6f);
                        level.playSound(null, BlockPos.containing(handTarget), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 0.5f);
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
                        // Fire aura + strength
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
                    default -> {}
                }
            });
        });
        return true;
    }
}
