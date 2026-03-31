package com.noxgg.elementalpower.event;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.item.ElementalArmorItem;
import com.noxgg.elementalpower.item.ElementalWandItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalPowerMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Fire armor makes immune to fire damage
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                if (hasFullElementalArmor(player, ElementalWandItem.Element.FIRE)) {
                    event.setCanceled(true);
                }
            }
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
