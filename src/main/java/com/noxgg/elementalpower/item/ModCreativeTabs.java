package com.noxgg.elementalpower.item;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ElementalPowerMod.MOD_ID);

    private static <T extends ItemLike> void add(CreativeModeTab.Output output, RegistryObject<T> obj) {
        try {
            if (obj != null && obj.isPresent()) {
                output.accept(obj.get());
            }
        } catch (Throwable ignored) {
            // Item missing from registry (server desync) — skip silently
        }
    }

    public static final RegistryObject<CreativeModeTab> ELEMENTAL_TAB = CREATIVE_MODE_TABS.register("elemental_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> {
                        try {
                            if (ModItems.FIRE_CRYSTAL.isPresent()) {
                                return new ItemStack(ModItems.FIRE_CRYSTAL.get());
                            }
                        } catch (Throwable ignored) {}
                        return new ItemStack(Items.FIRE_CHARGE);
                    })
                    .title(Component.translatable("creativetab.elementalpower"))
                    .displayItems((parameters, output) -> {
                        // Crystals
                        add(output, ModItems.FIRE_CRYSTAL);
                        add(output, ModItems.WATER_CRYSTAL);
                        add(output, ModItems.EARTH_CRYSTAL);
                        add(output, ModItems.AIR_CRYSTAL);

                        // Ingots
                        add(output, ModItems.FIRE_INGOT);
                        add(output, ModItems.WATER_INGOT);
                        add(output, ModItems.EARTH_INGOT);
                        add(output, ModItems.AIR_INGOT);

                        // Ores
                        add(output, ModBlocks.FIRE_ORE);
                        add(output, ModBlocks.WATER_ORE);
                        add(output, ModBlocks.EARTH_ORE);
                        add(output, ModBlocks.AIR_ORE);

                        // Storage Blocks
                        add(output, ModBlocks.FIRE_BLOCK);
                        add(output, ModBlocks.WATER_BLOCK);
                        add(output, ModBlocks.EARTH_BLOCK);
                        add(output, ModBlocks.AIR_BLOCK);

                        // Wands
                        add(output, ModItems.FIRE_WAND);
                        add(output, ModItems.WATER_WAND);
                        add(output, ModItems.EARTH_WAND);
                        add(output, ModItems.AIR_WAND);

                        // Swords
                        add(output, ModItems.FIRE_SWORD);
                        add(output, ModItems.WATER_SWORD);
                        add(output, ModItems.EARTH_SWORD);
                        add(output, ModItems.AIR_SWORD);

                        // Fire Armor
                        add(output, ModItems.FIRE_HELMET);
                        add(output, ModItems.FIRE_CHESTPLATE);
                        add(output, ModItems.FIRE_LEGGINGS);
                        add(output, ModItems.FIRE_BOOTS);

                        // Water Armor
                        add(output, ModItems.WATER_HELMET);
                        add(output, ModItems.WATER_CHESTPLATE);
                        add(output, ModItems.WATER_LEGGINGS);
                        add(output, ModItems.WATER_BOOTS);

                        // Earth Armor
                        add(output, ModItems.EARTH_HELMET);
                        add(output, ModItems.EARTH_CHESTPLATE);
                        add(output, ModItems.EARTH_LEGGINGS);
                        add(output, ModItems.EARTH_BOOTS);

                        // Royal Crown
                        add(output, ModItems.ROYAL_CROWN);

                        // Soul Scythe
                        add(output, ModItems.SOUL_SCYTHE);

                        // Soul Contract
                        add(output, ModItems.SOUL_CONTRACT);

                        // Soul Stone
                        add(output, ModItems.SOUL_STONE);

                        // Reset Pearl
                        add(output, ModItems.ELEMENT_RESET_PEARL);

                        // Moon Contract Rewards
                        add(output, ModItems.EXPLOSIVE_CONCENTRATION);
                        add(output, ModItems.PROTECTION_GAUNTLET);
                        add(output, ModItems.MORGAN_SWORD);

                        // Air Armor
                        add(output, ModItems.AIR_HELMET);
                        add(output, ModItems.AIR_CHESTPLATE);
                        add(output, ModItems.AIR_LEGGINGS);
                        add(output, ModItems.AIR_BOOTS);

                        // Freak Circus
                        add(output, ModItems.PIERROT_CRYSTAL);
                        add(output, ModItems.HARLEQUIN_CRYSTAL);
                        add(output, ModItems.TICKET_TAKER_CRYSTAL);
                        add(output, ModItems.PIERROT_INGOT);
                        add(output, ModItems.HARLEQUIN_INGOT);
                        add(output, ModItems.TICKET_TAKER_INGOT);
                        add(output, ModItems.PIERROT_WAND);
                        add(output, ModItems.HARLEQUIN_WAND);
                        add(output, ModItems.TICKET_TAKER_WAND);
                        add(output, ModItems.PIERROT_DAGGER);
                        add(output, ModItems.HARLEQUIN_BLADE);
                        add(output, ModItems.TICKET_PUNCH);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
