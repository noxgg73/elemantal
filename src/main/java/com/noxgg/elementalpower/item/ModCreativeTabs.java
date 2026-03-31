package com.noxgg.elementalpower.item;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ElementalPowerMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ELEMENTAL_TAB = CREATIVE_MODE_TABS.register("elemental_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.FIRE_CRYSTAL.get()))
                    .title(Component.translatable("creativetab.elementalpower"))
                    .displayItems((parameters, output) -> {
                        // Crystals
                        output.accept(ModItems.FIRE_CRYSTAL.get());
                        output.accept(ModItems.WATER_CRYSTAL.get());
                        output.accept(ModItems.EARTH_CRYSTAL.get());
                        output.accept(ModItems.AIR_CRYSTAL.get());

                        // Ingots
                        output.accept(ModItems.FIRE_INGOT.get());
                        output.accept(ModItems.WATER_INGOT.get());
                        output.accept(ModItems.EARTH_INGOT.get());
                        output.accept(ModItems.AIR_INGOT.get());

                        // Ores
                        output.accept(ModBlocks.FIRE_ORE.get());
                        output.accept(ModBlocks.WATER_ORE.get());
                        output.accept(ModBlocks.EARTH_ORE.get());
                        output.accept(ModBlocks.AIR_ORE.get());

                        // Storage Blocks
                        output.accept(ModBlocks.FIRE_BLOCK.get());
                        output.accept(ModBlocks.WATER_BLOCK.get());
                        output.accept(ModBlocks.EARTH_BLOCK.get());
                        output.accept(ModBlocks.AIR_BLOCK.get());

                        // Wands
                        output.accept(ModItems.FIRE_WAND.get());
                        output.accept(ModItems.WATER_WAND.get());
                        output.accept(ModItems.EARTH_WAND.get());
                        output.accept(ModItems.AIR_WAND.get());

                        // Swords
                        output.accept(ModItems.FIRE_SWORD.get());
                        output.accept(ModItems.WATER_SWORD.get());
                        output.accept(ModItems.EARTH_SWORD.get());
                        output.accept(ModItems.AIR_SWORD.get());

                        // Fire Armor
                        output.accept(ModItems.FIRE_HELMET.get());
                        output.accept(ModItems.FIRE_CHESTPLATE.get());
                        output.accept(ModItems.FIRE_LEGGINGS.get());
                        output.accept(ModItems.FIRE_BOOTS.get());

                        // Water Armor
                        output.accept(ModItems.WATER_HELMET.get());
                        output.accept(ModItems.WATER_CHESTPLATE.get());
                        output.accept(ModItems.WATER_LEGGINGS.get());
                        output.accept(ModItems.WATER_BOOTS.get());

                        // Earth Armor
                        output.accept(ModItems.EARTH_HELMET.get());
                        output.accept(ModItems.EARTH_CHESTPLATE.get());
                        output.accept(ModItems.EARTH_LEGGINGS.get());
                        output.accept(ModItems.EARTH_BOOTS.get());

                        // Royal Crown & Throne
                        output.accept(ModItems.ROYAL_CROWN.get());
                        output.accept(ModBlocks.GOLDEN_THRONE.get());

                        // Reset Pearl
                        output.accept(ModItems.ELEMENT_RESET_PEARL.get());

                        // Air Armor
                        output.accept(ModItems.AIR_HELMET.get());
                        output.accept(ModItems.AIR_CHESTPLATE.get());
                        output.accept(ModItems.AIR_LEGGINGS.get());
                        output.accept(ModItems.AIR_BOOTS.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
