package com.noxgg.elementalpower.item;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

public class ElementalArmorItem extends ArmorItem {
    private final ElementalWandItem.Element element;

    private static final Map<ElementalWandItem.Element, MobEffectInstance> FULL_SET_EFFECTS =
            ImmutableMap.of(
                    ElementalWandItem.Element.FIRE, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, false, false),
                    ElementalWandItem.Element.WATER, new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 0, false, false),
                    ElementalWandItem.Element.EARTH, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false),
                    ElementalWandItem.Element.AIR, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false)
            );

    public ElementalArmorItem(ArmorMaterial material, Type type, Properties properties,
                              ElementalWandItem.Element element) {
        super(material, type, properties);
        this.element = element;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            if (hasFullSet(player)) {
                applyFullSetEffect(player);
            }
        }
    }

    private boolean hasFullSet(Player player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (!(armorStack.getItem() instanceof ElementalArmorItem armorItem) ||
                    armorItem.element != this.element) {
                return false;
            }
        }
        return true;
    }

    private void applyFullSetEffect(Player player) {
        MobEffectInstance effect = FULL_SET_EFFECTS.get(element);
        if (effect != null && !player.hasEffect(effect.getEffect())) {
            player.addEffect(new MobEffectInstance(effect));
        }
    }
}
