package com.noxgg.elementalpower.event;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElement;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.OpenSelectionS2CPacket;
import com.noxgg.elementalpower.network.SyncElementS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalPowerMod.MOD_ID)
public class ElementEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerElementProvider.PLAYER_ELEMENT).isPresent()) {
                event.addCapability(new ResourceLocation(ElementalPowerMod.MOD_ID, "player_element"),
                        new PlayerElementProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                if (!data.hasChosen()) {
                    // Open selection screen
                    ModMessages.sendToPlayer(new OpenSelectionS2CPacket(), serverPlayer);
                } else {
                    // Sync element to client
                    ModMessages.sendToPlayer(new SyncElementS2CPacket(data.getElement().getId()), serverPlayer);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                ModMessages.sendToPlayer(new SyncElementS2CPacket(data.getElement().getId()), serverPlayer);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(oldData -> {
                event.getEntity().getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                });
            });
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 60 != 0) return; // Every 3 seconds

        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
            ElementType element = data.getElement();
            if (element == ElementType.NONE) return;

            // Passive effects based on element
            switch (element) {
                case FIRE -> {
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
                }
                case WATER -> {
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 100, 0, false, false));
                }
                case EARTH -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false));
                }
                case AIR -> {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 1, false, false));
                }
                case SPACE -> {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, false, false));
                }
                case TIME -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 1, false, false));
                }
                case POISON -> {
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0, false, false));
                    // Immune to poison (remove it)
                    if (player.hasEffect(MobEffects.POISON)) {
                        player.removeEffect(MobEffects.POISON);
                    }
                }
                case DARKNESS -> {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, false, false));
                }
                case LIGHT -> {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
                }
                case DEMON -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
                }
                case NATURE -> {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 0, false, false));
                }
                case LIGHTNING -> {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false));
                }
            }
        });
    }
}
