package com.noxgg.elementalpower.event;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.item.ElementalArmorItem;
import com.noxgg.elementalpower.item.ElementalWandItem;
import com.noxgg.elementalpower.entity.SeatEntity;
import com.noxgg.elementalpower.world.DarkPrisonManager;
import com.noxgg.elementalpower.world.PoisonDragonManager;
import com.noxgg.elementalpower.world.ThroneManager;
import com.noxgg.elementalpower.world.TimeDomeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalPowerMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                if (hasFullElementalArmor(player, ElementalWandItem.Element.FIRE)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.getEntityMounting() instanceof net.minecraft.world.entity.player.Player player) {
            if (event.isMounting() && event.getEntityBeingMounted() instanceof SeatEntity) {
                ThroneManager.addPlayer(player.getUUID());
            } else if (!event.isMounting()) {
                ThroneManager.removePlayer(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TimeDomeManager.tick();
            DarkPrisonManager.tick();
            PoisonDragonManager.tick();
            // Throne tick for all server levels
            net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getAllLevels()
                    .forEach(ThroneManager::tick);
        }
    }

    private static boolean hasFullElementalArmor(Player player, ElementalWandItem.Element element) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (!(armorStack.getItem() instanceof ElementalArmorItem armorItem)) {
                return false;
            }
        }
        return true;
    }
}
