package com.noxgg.elementalpower.block;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.item.ModItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ElementalPowerMod.MOD_ID);

    // Elemental Ore Blocks
    public static final RegistryObject<Block> FIRE_ORE = registerBlock("fire_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(4.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 7),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<Block> WATER_ORE = registerBlock("water_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .strength(4.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(2, 5)));

    public static final RegistryObject<Block> EARTH_ORE = registerBlock("earth_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(3.5f, 5.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(2, 5)));

    public static final RegistryObject<Block> AIR_ORE = registerBlock("air_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(3.0f, 4.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(3, 6)));

    // Elemental Blocks (storage blocks)
    public static final RegistryObject<Block> FIRE_BLOCK = registerBlock("fire_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(5.0f, 8.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 12)));

    public static final RegistryObject<Block> WATER_BLOCK = registerBlock("water_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .strength(5.0f, 8.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> EARTH_BLOCK = registerBlock("earth_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(5.0f, 8.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> AIR_BLOCK = registerBlock("air_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(5.0f, 8.0f)
                    .sound(SoundType.METAL)));

    // Golden Throne
    public static final RegistryObject<Block> GOLDEN_THRONE = registerBlock("golden_throne",
            () -> new GoldenThroneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(5.0f, 10.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 5)
                    .noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
