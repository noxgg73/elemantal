package com.noxgg.elementalpower.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class ElementalSwordItem extends SwordItem {
    private final ElementalWandItem.Element element;

    public ElementalSwordItem(Tier tier, int attackDamage, float attackSpeed,
                              Properties properties, ElementalWandItem.Element element) {
        super(tier, attackDamage, attackSpeed, properties);
        this.element = element;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        switch (element) {
            case FIRE -> target.setSecondsOnFire(5);
            case WATER -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                attacker.heal(2.0f);
            }
            case EARTH -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            case AIR -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
