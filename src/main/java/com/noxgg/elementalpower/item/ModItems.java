package com.noxgg.elementalpower.item;

import com.noxgg.elementalpower.ElementalPowerMod;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ElementalPowerMod.MOD_ID);

    // Register items under mahoutsukai namespace (for items missing from their registry)
    public static final DeferredRegister<Item> MAHOU_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "mahoutsukai");

    // Nobu - not registered by Mahou Tsukai in this version
    public static final RegistryObject<Item> NOBU = MAHOU_ITEMS.register("nobu",
            () -> new stepsword.mahoutsukai.item.nobu.Nobu());

    // Elemental Crystals (dropped from ores)
    public static final RegistryObject<Item> FIRE_CRYSTAL = ITEMS.register("fire_crystal",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WATER_CRYSTAL = ITEMS.register("water_crystal",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EARTH_CRYSTAL = ITEMS.register("earth_crystal",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AIR_CRYSTAL = ITEMS.register("air_crystal",
            () -> new Item(new Item.Properties()));

    // Elemental Ingots (smelted from crystals)
    public static final RegistryObject<Item> FIRE_INGOT = ITEMS.register("fire_ingot",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WATER_INGOT = ITEMS.register("water_ingot",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EARTH_INGOT = ITEMS.register("earth_ingot",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AIR_INGOT = ITEMS.register("air_ingot",
            () -> new Item(new Item.Properties()));

    // Elemental Wands
    public static final RegistryObject<Item> FIRE_WAND = ITEMS.register("fire_wand",
            () -> new ElementalWandItem(new Item.Properties().stacksTo(1).durability(200),
                    ElementalWandItem.Element.FIRE));
    public static final RegistryObject<Item> WATER_WAND = ITEMS.register("water_wand",
            () -> new ElementalWandItem(new Item.Properties().stacksTo(1).durability(200),
                    ElementalWandItem.Element.WATER));
    public static final RegistryObject<Item> EARTH_WAND = ITEMS.register("earth_wand",
            () -> new ElementalWandItem(new Item.Properties().stacksTo(1).durability(200),
                    ElementalWandItem.Element.EARTH));
    public static final RegistryObject<Item> AIR_WAND = ITEMS.register("air_wand",
            () -> new ElementalWandItem(new Item.Properties().stacksTo(1).durability(200),
                    ElementalWandItem.Element.AIR));

    // Elemental Swords
    public static final RegistryObject<Item> FIRE_SWORD = ITEMS.register("fire_sword",
            () -> new ElementalSwordItem(Tiers.DIAMOND, 5, -2.4f,
                    new Item.Properties(), ElementalWandItem.Element.FIRE));
    public static final RegistryObject<Item> WATER_SWORD = ITEMS.register("water_sword",
            () -> new ElementalSwordItem(Tiers.DIAMOND, 4, -2.2f,
                    new Item.Properties(), ElementalWandItem.Element.WATER));
    public static final RegistryObject<Item> EARTH_SWORD = ITEMS.register("earth_sword",
            () -> new ElementalSwordItem(Tiers.DIAMOND, 6, -2.6f,
                    new Item.Properties(), ElementalWandItem.Element.EARTH));
    public static final RegistryObject<Item> AIR_SWORD = ITEMS.register("air_sword",
            () -> new ElementalSwordItem(Tiers.DIAMOND, 3, -1.8f,
                    new Item.Properties(), ElementalWandItem.Element.AIR));

    // Elemental Armor - Fire Set
    public static final RegistryObject<Item> FIRE_HELMET = ITEMS.register("fire_helmet",
            () -> new ElementalArmorItem(ModArmorMaterials.FIRE, ArmorItem.Type.HELMET,
                    new Item.Properties(), ElementalWandItem.Element.FIRE));
    public static final RegistryObject<Item> FIRE_CHESTPLATE = ITEMS.register("fire_chestplate",
            () -> new ElementalArmorItem(ModArmorMaterials.FIRE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties(), ElementalWandItem.Element.FIRE));
    public static final RegistryObject<Item> FIRE_LEGGINGS = ITEMS.register("fire_leggings",
            () -> new ElementalArmorItem(ModArmorMaterials.FIRE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties(), ElementalWandItem.Element.FIRE));
    public static final RegistryObject<Item> FIRE_BOOTS = ITEMS.register("fire_boots",
            () -> new ElementalArmorItem(ModArmorMaterials.FIRE, ArmorItem.Type.BOOTS,
                    new Item.Properties(), ElementalWandItem.Element.FIRE));

    // Elemental Armor - Water Set
    public static final RegistryObject<Item> WATER_HELMET = ITEMS.register("water_helmet",
            () -> new ElementalArmorItem(ModArmorMaterials.WATER, ArmorItem.Type.HELMET,
                    new Item.Properties(), ElementalWandItem.Element.WATER));
    public static final RegistryObject<Item> WATER_CHESTPLATE = ITEMS.register("water_chestplate",
            () -> new ElementalArmorItem(ModArmorMaterials.WATER, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties(), ElementalWandItem.Element.WATER));
    public static final RegistryObject<Item> WATER_LEGGINGS = ITEMS.register("water_leggings",
            () -> new ElementalArmorItem(ModArmorMaterials.WATER, ArmorItem.Type.LEGGINGS,
                    new Item.Properties(), ElementalWandItem.Element.WATER));
    public static final RegistryObject<Item> WATER_BOOTS = ITEMS.register("water_boots",
            () -> new ElementalArmorItem(ModArmorMaterials.WATER, ArmorItem.Type.BOOTS,
                    new Item.Properties(), ElementalWandItem.Element.WATER));

    // Elemental Armor - Earth Set
    public static final RegistryObject<Item> EARTH_HELMET = ITEMS.register("earth_helmet",
            () -> new ElementalArmorItem(ModArmorMaterials.EARTH, ArmorItem.Type.HELMET,
                    new Item.Properties(), ElementalWandItem.Element.EARTH));
    public static final RegistryObject<Item> EARTH_CHESTPLATE = ITEMS.register("earth_chestplate",
            () -> new ElementalArmorItem(ModArmorMaterials.EARTH, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties(), ElementalWandItem.Element.EARTH));
    public static final RegistryObject<Item> EARTH_LEGGINGS = ITEMS.register("earth_leggings",
            () -> new ElementalArmorItem(ModArmorMaterials.EARTH, ArmorItem.Type.LEGGINGS,
                    new Item.Properties(), ElementalWandItem.Element.EARTH));
    public static final RegistryObject<Item> EARTH_BOOTS = ITEMS.register("earth_boots",
            () -> new ElementalArmorItem(ModArmorMaterials.EARTH, ArmorItem.Type.BOOTS,
                    new Item.Properties(), ElementalWandItem.Element.EARTH));

    // Elemental Armor - Air Set
    public static final RegistryObject<Item> AIR_HELMET = ITEMS.register("air_helmet",
            () -> new ElementalArmorItem(ModArmorMaterials.AIR, ArmorItem.Type.HELMET,
                    new Item.Properties(), ElementalWandItem.Element.AIR));
    public static final RegistryObject<Item> AIR_CHESTPLATE = ITEMS.register("air_chestplate",
            () -> new ElementalArmorItem(ModArmorMaterials.AIR, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties(), ElementalWandItem.Element.AIR));
    public static final RegistryObject<Item> AIR_LEGGINGS = ITEMS.register("air_leggings",
            () -> new ElementalArmorItem(ModArmorMaterials.AIR, ArmorItem.Type.LEGGINGS,
                    new Item.Properties(), ElementalWandItem.Element.AIR));
    public static final RegistryObject<Item> AIR_BOOTS = ITEMS.register("air_boots",
            () -> new ElementalArmorItem(ModArmorMaterials.AIR, ArmorItem.Type.BOOTS,
                    new Item.Properties(), ElementalWandItem.Element.AIR));

    // Royal Crown
    public static final RegistryObject<Item> ROYAL_CROWN = ITEMS.register("royal_crown",
            () -> new RoyalCrownItem(ModArmorMaterials.FIRE, ArmorItem.Type.HELMET,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // Soul Scythe (Darkness class weapon)
    public static final RegistryObject<Item> SOUL_SCYTHE = ITEMS.register("soul_scythe",
            () -> new SoulScytheItem(net.minecraft.world.item.Tiers.NETHERITE, 8, -2.8f,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // Soul Contract (dream quest item)
    public static final RegistryObject<Item> SOUL_CONTRACT = ITEMS.register("soul_contract",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)) {
                @Override
                public boolean isFoil(net.minecraft.world.item.ItemStack stack) { return true; }
            });

    // Soul Stone (dropped by Pure Vanilla, given to Shadow Milk for Puppeteer power)
    public static final RegistryObject<Item> SOUL_STONE = ITEMS.register("soul_stone",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)) {
                @Override
                public boolean isFoil(net.minecraft.world.item.ItemStack stack) { return true; }
            });

    // Element Reset Pearl
    public static final RegistryObject<Item> ELEMENT_RESET_PEARL = ITEMS.register("element_reset_pearl",
            () -> new ElementResetPearlItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        MAHOU_ITEMS.register(eventBus);
    }
}
