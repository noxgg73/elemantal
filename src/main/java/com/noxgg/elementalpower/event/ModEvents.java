package com.noxgg.elementalpower.event;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.item.ElementalArmorItem;
import com.noxgg.elementalpower.item.ElementalWandItem;
import com.noxgg.elementalpower.world.BlackHoleManager;
import com.noxgg.elementalpower.world.DarkPrisonManager;
import com.noxgg.elementalpower.world.PoisonDragonManager;
import com.noxgg.elementalpower.world.TimeDomeManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TimeDomeManager.tick();
            DarkPrisonManager.tick();
            PoisonDragonManager.tick();
            BlackHoleManager.tick();
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
