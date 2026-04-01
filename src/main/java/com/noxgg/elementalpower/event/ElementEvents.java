package com.noxgg.elementalpower.event;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElement;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.OpenSelectionS2CPacket;
import com.noxgg.elementalpower.network.SyncElementS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import com.noxgg.elementalpower.world.CombatMusicManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
                    ModMessages.sendToPlayer(new OpenSelectionS2CPacket(), serverPlayer);
                } else {
                    syncToClient(serverPlayer, data);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                syncToClient(serverPlayer, data);
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

    // === FRISK: Block all attacks from Frisk players ===
    @SubscribeEvent
    public static void onFriskAttack(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
            if (data.getElement() == ElementType.UNDERTALE && data.isFrisk()) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("* ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("Tu ne peux pas attaquer. Utilise G pour SPARE.").withStyle(ChatFormatting.GOLD)));
            }
        });
    }

    // === COMBAT MUSIC: Megalovania on hit, stop on kill ===
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();

        // Block damage from Frisk players entirely
        if (attacker instanceof ServerPlayer player && !(victim instanceof Player)) {
            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                if (data.getElement() == ElementType.UNDERTALE && data.isFrisk()) {
                    event.setCanceled(true);
                    return;
                }
            });
            if (event.isCanceled()) return;
            CombatMusicManager.onPlayerHitMob(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CombatMusicManager.onPlayerLogout(event.getEntity().getUUID());
    }

    // === SOUL ABSORPTION ON MOB KILL ===
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();
        Entity source = event.getSource().getEntity();

        if (!(source instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Stop Megalovania when a mob (not a player) is killed
        if (!(killed instanceof Player)) {
            CombatMusicManager.onMobKilled(player);
        }

        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
            ElementType element = data.getElement();
            if (element == ElementType.NONE) return;

            // XP amounts based on mob type
            int xpGain;
            int soulValue;
            if (killed instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
                xpGain = 500;
                soulValue = 10;
            } else if (killed instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                xpGain = 1000;
                soulValue = 20;
            } else if (killed instanceof Monster) {
                xpGain = 15 + (int)(killed.getMaxHealth() / 2);
                soulValue = 1;
            } else if (killed instanceof LivingEntity) {
                xpGain = 5;
                soulValue = 0;
            } else {
                return;
            }

            // Darkness class gets BONUS souls and XP
            if (element == ElementType.DARKNESS) {
                xpGain = (int)(xpGain * 1.5);
                soulValue *= 2;

                // Soul absorption particles: soul rises from dead mob to player
                double mobX = killed.getX();
                double mobY = killed.getY() + 1;
                double mobZ = killed.getZ();
                double dirX = (player.getX() - mobX) / 10;
                double dirY = (player.getEyeY() - mobY) / 10;
                double dirZ = (player.getZ() - mobZ) / 10;

                // Soul trail from mob to player
                for (int i = 0; i < 10; i++) {
                    double px = mobX + dirX * i;
                    double py = mobY + dirY * i + Math.sin(i * 0.5) * 0.3;
                    double pz = mobZ + dirZ * i;
                    serverLevel.sendParticles(ParticleTypes.SOUL,
                            px, py, pz, 2, 0.05, 0.05, 0.05, 0.02);
                    serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                            px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                }

                // Soul burst on mob death position
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        mobX, mobY, mobZ, 15, 0.3, 0.5, 0.3, 0.05);
                serverLevel.sendParticles(ParticleTypes.SOUL,
                        mobX, mobY, mobZ, 8, 0.2, 0.3, 0.2, 0.08);

                // Soul absorbed particles around player
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                        player.getX(), player.getEyeY(), player.getZ(), 5, 0.3, 0.3, 0.3, 0.03);

                // Sound
                serverLevel.playSound(null, player.blockPosition(),
                        SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 0.6f, 1.2f);

                // Add souls
                for (int i = 0; i < soulValue; i++) {
                    data.addSoul();
                }
            }

            // All classes gain XP from kills
            boolean leveledUp = data.addXp(xpGain);

            if (leveledUp) {
                // Level up notification
                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal(">> LEVEL UP! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                        .append(Component.literal(element.getDisplayName()).withStyle(element.getColor()))
                        .append(Component.literal(" Niv." + data.getLevel()).withStyle(ChatFormatting.YELLOW)));

                // Level up particles
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1.0, 0.5, 0.3);

                if (element == ElementType.DARKNESS) {
                    serverLevel.sendParticles(ParticleTypes.SOUL,
                            player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1.0, 0.5, 0.1);
                }

                // Level up sound
                serverLevel.playSound(null, player.blockPosition(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);

                // Bonus effects on level up
                if (data.getLevel() % 10 == 0) {
                    // Every 10 levels: big bonus
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2));
                    player.sendSystemMessage(Component.literal(">> PALIER " + data.getLevel() + " ATTEINT! Bonus de puissance!")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                }
            }

            // Sync level data to client
            syncToClient(player, data);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 60 != 0) return;

        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
            ElementType element = data.getElement();
            if (element == ElementType.NONE) return;

            int level = data.getLevel();

            // Passive effects based on element (scale with level)
            int bonusAmplifier = level >= 30 ? 1 : 0;
            int bonusDuration = 100 + level * 2;

            switch (element) {
                case FIRE -> {
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, bonusDuration, 0, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonusDuration, 0, false, false));
                }
                case WATER -> {
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, bonusDuration, 0, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, bonusDuration, 0, false, false));
                }
                case EARTH -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, bonusDuration, bonusAmplifier, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, bonusDuration, 0, false, false));
                }
                case AIR -> {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, bonusDuration, bonusAmplifier, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, bonusDuration, 1 + bonusAmplifier, false, false));
                }
                case SPACE -> {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, bonusDuration, 0, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, bonusDuration, 0, false, false));
                }
                case TIME -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, bonusDuration, 1 + bonusAmplifier, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, bonusDuration, 0, false, false));
                }
                case POISON -> {
                    if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonusDuration, 0, false, false));
                }
                case DARKNESS -> {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, bonusDuration, 0, false, false));
                    // Darkness passives scale with souls
                    int souls = data.getSouls();
                    if (souls >= 50) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonusDuration, 0, false, false));
                    if (souls >= 150) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonusDuration, 1, false, false));
                    if (souls >= 300) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, bonusDuration, 0, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, bonusDuration, 0, false, false));
                }
                case LIGHT -> {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, bonusDuration, bonusAmplifier, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.GLOWING, bonusDuration, 0, false, false));
                }
                case DEMON -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonusDuration, bonusAmplifier, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, bonusDuration, 0, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, bonusDuration, 0, false, false));
                }
                case NATURE -> {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, bonusDuration, bonusAmplifier, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, bonusDuration, 0, false, false));
                }
                case LIGHTNING -> {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, bonusDuration, 1 + bonusAmplifier, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonusDuration, 0, false, false));
                }
                case UNDERTALE -> {
                    // Frisk: regeneration + resistance (pacifist protection)
                    if (data.isFrisk()) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, bonusDuration, bonusAmplifier, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, bonusDuration, 1, false, false));
                    }
                    // Chara: damage boost + speed (genocide power)
                    if (data.isChara()) {
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonusDuration, 1 + bonusAmplifier, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, bonusDuration, bonusAmplifier, false, false));
                    }
                }
                case ROYAL -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonusDuration, bonusAmplifier, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, bonusDuration, bonusAmplifier, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, bonusDuration, 0, false, false));
                    if (level >= 20) player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, bonusDuration, 1, false, false));
                }
            }

            // Show level in actionbar every 15 seconds
            if (player.tickCount % 300 == 0) {
                int xpNeeded = data.getXpForNextLevel();
                String soulText = element == ElementType.DARKNESS ? " | Ames: " + data.getSouls() : "";
                player.displayClientMessage(Component.literal("")
                        .append(Component.literal(element.getDisplayName()).withStyle(element.getColor()))
                        .append(Component.literal(" Niv." + level).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" [" + data.getXp() + "/" + xpNeeded + " XP]").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(soulText).withStyle(ChatFormatting.DARK_PURPLE)),
                        true);
            }
        });
    }

    public static void syncToClient(ServerPlayer player, PlayerElement data) {
        ModMessages.sendToPlayer(new SyncElementS2CPacket(
                data.getElement().getId(), data.getLevel(), data.getXp(), data.getSouls(), data.getSubClass()), player);
    }
}
