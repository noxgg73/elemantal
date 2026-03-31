package com.noxgg.elementalpower.entity;

import com.noxgg.elementalpower.ElementalPowerMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ElementalPowerMod.MOD_ID);

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.001f, 0.001f)
                    .noSummon()
                    .build("seat"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
