package com.noxgg.elementalpower.event;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.item.ElementalArmorItem;
import com.noxgg.elementalpower.item.ElementalWandItem;
import com.noxgg.elementalpower.world.BlackHoleManager;
import com.noxgg.elementalpower.world.CarnivorousFlowerManager;
import com.noxgg.elementalpower.world.SoulTsunamiManager;
import com.noxgg.elementalpower.world.DarkPrisonManager;
import com.noxgg.elementalpower.world.PoisonDragonManager;
import com.noxgg.elementalpower.world.TimeDomeManager;
import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.OpenDemonPortalS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return; // Check every second

        // Detect demon portal: player standing on nether portal with lodestone below
        BlockPos feetPos = player.blockPosition();
        if (player.level().getBlockState(feetPos).is(Blocks.NETHER_PORTAL)) {
            // Check for lodestone marker below (demon village portal)
            for (int dy = -3; dy <= -1; dy++) {
                if (player.level().getBlockState(feetPos.offset(0, dy, 0)).is(Blocks.LODESTONE)) {
                    // This is a demon village portal - check if demon class
                    player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                        if (data.getElement() == ElementType.DEMON) {
                            ModMessages.sendToPlayer(new OpenDemonPortalS2CPacket(), player);
                        }
                    });
                    break;
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
            SoulTsunamiManager.tick();
            CarnivorousFlowerManager.tick();
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
