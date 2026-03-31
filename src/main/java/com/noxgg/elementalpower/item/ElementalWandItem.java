package com.noxgg.elementalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ElementalWandItem extends Item {

    public enum Element {
        FIRE, WATER, EARTH, AIR
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
                    // Shoot a fireball
                    Vec3 look = player.getLookAngle();
                    SmallFireball fireball = new SmallFireball(level, player,
                            look.x * 2, look.y * 2, look.z * 2);
                    fireball.setPos(player.getX() + look.x, player.getEyeY() - 0.1, player.getZ() + look.z);
                    level.addFreshEntity(fireball);
                    level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                            SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                case WATER -> {
                    // Heal the player and give water breathing
                    player.heal(6.0f);
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                    level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL,
                            SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                case EARTH -> {
                    // Create a 3x3 wall of stone in front of the player
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
                    // Launch the player into the air and give slow falling
                    player.push(0, 1.5, 0);
                    player.hurtMarked = true;
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 2));
                    level.playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING,
                            SoundSource.PLAYERS, 1.0f, 1.5f);
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

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
