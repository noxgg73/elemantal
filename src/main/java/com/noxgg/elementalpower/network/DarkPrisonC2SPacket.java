package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.world.DarkPrisonManager;
import com.noxgg.elementalpower.world.PoisonDragonManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.function.Supplier;

public class DarkPrisonC2SPacket {
    public DarkPrisonC2SPacket() {}
    public DarkPrisonC2SPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                ElementType element = data.getElement();
                if (element != ElementType.DARKNESS && element != ElementType.POISON && element != ElementType.ROYAL) {
                    player.sendSystemMessage(Component.literal("Ce sort n'est pas disponible pour ta classe!")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                ServerLevel level = player.serverLevel();
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();

                // Find the mob the player is looking at (within 30 blocks)
                LivingEntity target = null;
                double closestDist = 30.0;

                for (Entity entity : level.getEntities(player,
                        player.getBoundingBox().inflate(30),
                        e -> e instanceof LivingEntity && e != player)) {

                    LivingEntity living = (LivingEntity) entity;
                    Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(eye);
                    double dist = toEntity.length();
                    if (dist > closestDist) continue;

                    Vec3 toEntityNorm = toEntity.normalize();
                    double dot = look.dot(toEntityNorm);

                    // Must be looking at the mob (within ~10 degree cone)
                    if (dot > 0.95) {
                        closestDist = dist;
                        target = living;
                    }
                }

                if (target == null) {
                    player.sendSystemMessage(Component.literal("Aucune cible en vue!")
                            .withStyle(ChatFormatting.GRAY));
                    return;
                }

                if (element == ElementType.DARKNESS) {
                    // === DARK PRISON ===
                    double prisonRadius = 3.5;
                    DarkPrisonManager.addPrison(new DarkPrisonManager.DarkPrison(
                            level, player, target, prisonRadius));

                    target.setDeltaMovement(0, 0, 0);
                    target.hurtMarked = true;

                    Vec3 targetPos = target.position();
                    for (int i = 0; i < 20; i++) {
                        double t = i / 20.0;
                        double px = player.getX() + (targetPos.x - player.getX()) * t;
                        double py = player.getEyeY() + (targetPos.y + 1 - player.getEyeY()) * t;
                        double pz = player.getZ() + (targetPos.z - player.getZ()) * t;
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                px, py, pz, 3, 0.05, 0.05, 0.05, 0.01);
                        level.sendParticles(ParticleTypes.SCULK_SOUL,
                                px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                    }

                    for (int i = 0; i < 60; i++) {
                        double theta = level.random.nextDouble() * Math.PI * 2;
                        double phi = level.random.nextDouble() * Math.PI;
                        double bx = targetPos.x + Math.cos(theta) * Math.sin(phi) * prisonRadius;
                        double by = targetPos.y + Math.cos(phi) * prisonRadius;
                        double bz = targetPos.z + Math.sin(theta) * Math.sin(phi) * prisonRadius;
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                bx, by, bz, 2, 0.1, 0.1, 0.1, 0.02);
                    }

                    level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 0.4f);
                    level.playSound(null, target.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0f, 0.6f);

                    player.sendSystemMessage(Component.literal(">> Prison de Tenebres invoquee! ")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)
                            .append(Component.literal("Gardez le regard fixe sur le prisonnier!")
                                    .withStyle(ChatFormatting.GRAY)));

                } else if (element == ElementType.POISON) {
                    // === POISON DRAGON ATTACK ===
                    PoisonDragonManager.addAttack(new PoisonDragonManager.PoisonDragonAttack(
                            level, player, target));

                    // Purple casting beam from player to target
                    Vec3 targetPos = target.position();
                    var purpleDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.6f, 0.0f, 0.8f), 1.5f);
                    for (int i = 0; i < 15; i++) {
                        double t = i / 15.0;
                        double px = player.getX() + (targetPos.x - player.getX()) * t;
                        double py = player.getEyeY() + (targetPos.y + 1 - player.getEyeY()) * t;
                        double pz = player.getZ() + (targetPos.z - player.getZ()) * t;
                        level.sendParticles(purpleDust, px, py, pz, 3, 0.05, 0.05, 0.05, 0.02);
                        level.sendParticles(ParticleTypes.WITCH, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                    }

                    level.playSound(null, target.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.6f);
                    level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.0f, 0.5f);

                    player.sendSystemMessage(Component.literal(">> Dragons de Poison invoques! ")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD)
                            .append(Component.literal("Ils fondent sur la cible!")
                                    .withStyle(ChatFormatting.GREEN)));

                } else if (element == ElementType.ROYAL) {
                    // === ROYAL JUDGMENT: golden execution ===
                    var goldDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(1.0f, 0.84f, 0.0f), 2.0f);
                    var whiteDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(1.0f, 1.0f, 0.85f), 1.5f);

                    // Golden beam from player to target (royal decree)
                    Vec3 targetPos = target.position();
                    for (int i = 0; i < 20; i++) {
                        double t = i / 20.0;
                        double px = player.getX() + (targetPos.x - player.getX()) * t;
                        double py = player.getEyeY() + (targetPos.y + 1 - player.getEyeY()) * t;
                        double pz = player.getZ() + (targetPos.z - player.getZ()) * t;
                        level.sendParticles(goldDust, px, py, pz, 3, 0.03, 0.03, 0.03, 0.01);
                        level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.02, 0.02, 0.02, 0.005);
                    }

                    // Lift the target up
                    target.teleportTo(target.getX(), target.getY() + 5, target.getZ());
                    target.setDeltaMovement(0, 0, 0);
                    target.hurtMarked = true;

                    // Golden cage around the target (4 pillars + top)
                    double tx = target.getX();
                    double ty = target.getY();
                    double tz = target.getZ();
                    for (int pillar = 0; pillar < 4; pillar++) {
                        double pAngle = (Math.PI * 2 / 4) * pillar + Math.PI / 4;
                        double pDist = 1.5;
                        double pillarX = tx + Math.cos(pAngle) * pDist;
                        double pillarZ = tz + Math.sin(pAngle) * pDist;
                        for (double h = -2; h < 3; h += 0.3) {
                            level.sendParticles(goldDust, pillarX, ty + h, pillarZ, 2, 0.02, 0.02, 0.02, 0.001);
                        }
                    }
                    // Top cross
                    for (double c = -1.5; c <= 1.5; c += 0.2) {
                        level.sendParticles(goldDust, tx + c, ty + 3, tz, 1, 0.02, 0.02, 0.02, 0.001);
                        level.sendParticles(goldDust, tx, ty + 3, tz + c, 1, 0.02, 0.02, 0.02, 0.001);
                    }

                    // Giant golden swords falling from above (3 swords)
                    for (int sword = 0; sword < 3; sword++) {
                        double sAngle = (Math.PI * 2 / 3) * sword;
                        double sOffset = 1.0;
                        double sx = tx + Math.cos(sAngle) * sOffset;
                        double sz = tz + Math.sin(sAngle) * sOffset;
                        // Sword blade (vertical line from high up)
                        for (double h = 3; h < 12; h += 0.3) {
                            level.sendParticles(goldDust, sx, ty + h, sz, 2, 0.02, 0, 0.02, 0.001);
                            level.sendParticles(whiteDust, sx, ty + h, sz, 1, 0.01, 0, 0.01, 0.001);
                        }
                        // Sword guard (horizontal)
                        for (double g = -0.5; g <= 0.5; g += 0.1) {
                            level.sendParticles(goldDust, sx + g * Math.cos(sAngle + Math.PI / 2),
                                    ty + 3, sz + g * Math.sin(sAngle + Math.PI / 2),
                                    2, 0.01, 0.01, 0.01, 0.001);
                        }
                    }

                    // Massive golden damage
                    target.hurt(level.damageSources().magic(), 35.0f);
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3));

                    // Golden explosion on target
                    level.sendParticles(goldDust, tx, ty + 1, tz, 50, 1.5, 1.5, 1.5, 0.15);
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, tx, ty + 1, tz, 30, 1, 2, 1, 0.3);
                    level.sendParticles(ParticleTypes.END_ROD, tx, ty + 1, tz, 20, 1, 2, 1, 0.1);

                    // Sounds
                    level.playSound(null, target.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0f, 0.7f);
                    level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.6f);

                    player.sendSystemMessage(Component.literal(">> Jugement Royal prononce! ")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                            .append(Component.literal("L'ennemi est condamne!")
                                    .withStyle(ChatFormatting.YELLOW)));
                }
            });
        });
        return true;
    }
}
