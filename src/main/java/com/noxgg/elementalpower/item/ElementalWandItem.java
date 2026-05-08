package com.noxgg.elementalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ElementalWandItem extends Item {

    public enum Element {
        FIRE, WATER, EARTH, AIR,
        PIERROT, HARLEQUIN, TICKET_TAKER
    }

    private final Element element;

    public ElementalWandItem(Properties properties, Element element) {
        super(properties);
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            switch (element) {
                case FIRE -> {
                    Vec3 look = player.getLookAngle();
                    SmallFireball fireball = new SmallFireball(level, player,
                            look.x * 2, look.y * 2, look.z * 2);
                    fireball.setPos(player.getX() + look.x, player.getEyeY() - 0.1, player.getZ() + look.z);
                    level.addFreshEntity(fireball);
                    level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                            SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                case WATER -> {
                    player.heal(6.0f);
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                    level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL,
                            SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                case EARTH -> {
                    Vec3 lookVec = player.getLookAngle();
                    BlockPos base = player.blockPosition().offset(
                            (int)(lookVec.x * 3), 0, (int)(lookVec.z * 3));
                    for (int y = 0; y < 3; y++) {
                        for (int x = -1; x <= 1; x++) {
                            BlockPos wallPos = base.offset(
                                    (int)(lookVec.z * x), y, (int)(-lookVec.x * x));
                            if (level.getBlockState(wallPos).isAir()) {
                                level.setBlock(wallPos, Blocks.STONE.defaultBlockState(), 3);
                            }
                        }
                    }
                    level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE,
                            SoundSource.PLAYERS, 1.0f, 0.8f);
                }
                case AIR -> {
                    player.push(0, 1.5, 0);
                    player.hurtMarked = true;
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 2));
                    level.playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING,
                            SoundSource.PLAYERS, 1.0f, 1.5f);
                }
                case PIERROT -> {
                    LivingEntity target = findNearestTarget(level, player, 16.0);
                    if (target != null) {
                        Vec3 behind = target.position().subtract(target.getLookAngle().scale(1.2));
                        player.teleportTo(behind.x, behind.y, behind.z);
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
                        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0));
                        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                                SoundSource.PLAYERS, 1.0f, 0.6f);
                    } else {
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
                        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_AMBIENT,
                                SoundSource.PLAYERS, 1.0f, 0.5f);
                    }
                }
                case HARLEQUIN -> {
                    AABB box = player.getBoundingBox().inflate(5.0);
                    List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e != player && e.isAlive());
                    for (LivingEntity v : victims) {
                        v.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 2));
                        v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    }
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 400, 1));
                    level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                            SoundSource.PLAYERS, 1.0f, 1.4f);
                }
                case TICKET_TAKER -> {
                    LivingEntity target = findNearestTarget(level, player, 20.0);
                    if (target != null) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
                        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2));
                        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                                SoundSource.PLAYERS, 1.2f, 0.8f);
                    }
                    level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE,
                            SoundSource.PLAYERS, 1.0f, 1.2f);
                }
            }

            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            player.getCooldowns().addCooldown(this, 40);
        }

        if (level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // particles handled server-side
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private LivingEntity findNearestTarget(Level level, Player player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive())) {
            double d = e.distanceToSqr(player);
            if (d < bestDist) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
