package com.noxgg.elementalpower.item;

import com.noxgg.elementalpower.ElementalPowerMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {
    FIRE("fire", 35, new int[]{3, 8, 6, 3}, 18, SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.5f, 0.1f, () -> Ingredient.of(ModItems.FIRE_INGOT.get())),
    WATER("water", 33, new int[]{3, 7, 5, 3}, 20, SoundEvents.ARMOR_EQUIP_DIAMOND,
            2.0f, 0.0f, () -> Ingredient.of(ModItems.WATER_INGOT.get())),
    EARTH("earth", 37, new int[]{4, 9, 7, 4}, 12, SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0f, 0.15f, () -> Ingredient.of(ModItems.EARTH_INGOT.get())),
    AIR("air", 28, new int[]{2, 6, 5, 2}, 25, SoundEvents.ARMOR_EQUIP_LEATHER,
            1.0f, 0.0f, () -> Ingredient.of(ModItems.AIR_INGOT.get()));

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionAmounts;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

    ModArmorMaterials(String name, int durabilityMultiplier, int[] protectionAmounts,
                      int enchantmentValue, SoundEvent equipSound, float toughness,
                      float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionAmounts = protectionAmounts;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.ordinal()] * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return protectionAmounts[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return ElementalPowerMod.MOD_ID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
