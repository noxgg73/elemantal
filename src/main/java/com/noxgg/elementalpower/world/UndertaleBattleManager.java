package com.noxgg.elementalpower.world;

import com.noxgg.elementalpower.element.PlayerElement;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.OpenUndertaleBattleS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UndertaleBattleManager {
    // Track active battles per player
    private static final Map<UUID, ActiveBattle> activeBattles = new HashMap<>();

    public static class ActiveBattle {
        public final ServerPlayer player;
        public final LivingEntity target;
        public int spareCount; // how many times player has tried to spare

        public ActiveBattle(ServerPlayer player, LivingEntity target) {
            this.player = player;
            this.target = target;
            this.spareCount = 0;
        }
    }

    public static boolean isInBattle(UUID playerId) {
        return activeBattles.containsKey(playerId);
    }

    /**
     * Start a battle when an Undertale player hits a mob.
     */
    public static void startBattle(ServerPlayer player, LivingEntity target, boolean isFrisk) {
        UUID playerId = player.getUUID();

        // Don't start battle if already in one
        if (activeBattles.containsKey(playerId)) return;

        // Freeze both player and mob during battle
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.setTarget(null);
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6000, 127, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6000, 127, false, false));

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6000, 127, false, false));

        activeBattles.put(playerId, new ActiveBattle(player, target));

        String mobName = target.getType().getDescription().getString();

        // Open battle screen on client
        ModMessages.sendToPlayer(new OpenUndertaleBattleS2CPacket(
                target.getId(), mobName, target.getHealth(), target.getMaxHealth(), isFrisk), player);

        // Battle start sound
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    /**
     * Handle a player action during battle.
     */
    public static void handleAction(ServerPlayer player, int entityId, String action, PlayerElement data) {
        UUID playerId = player.getUUID();
        ActiveBattle battle = activeBattles.get(playerId);
        if (battle == null) return;

        ServerLevel level = player.serverLevel();

        switch (action) {
            case "attack" -> {
                if (data.isFrisk()) {
                    // Frisk can't attack
                    player.sendSystemMessage(Component.literal("* Tu refuses de te battre.")
                            .withStyle(ChatFormatting.YELLOW));
                    return;
                }

                // Chara attack: deal heavy damage
                float damage = 8.0f + data.getLevel() * 0.5f;
                battle.target.hurt(level.damageSources().playerAttack(player), damage);

                // Slash particles
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        battle.target.getX(), battle.target.getY() + 1, battle.target.getZ(),
                        3, 0.3, 0.3, 0.3, 0);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        battle.target.getX(), battle.target.getY() + 1, battle.target.getZ(),
                        5, 0.3, 0.3, 0.3, 0.1);

                level.playSound(null, battle.target.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);

                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("* ").withStyle(ChatFormatting.RED))
                        .append(Component.literal("Tu infliges " + (int)damage + " degats!").withStyle(ChatFormatting.WHITE)));

                // Check if mob died
                if (!battle.target.isAlive() || battle.target.getHealth() <= 0) {
                    endBattle(player, "kill");
                    return;
                }

                // Enemy will counter-attack with Gaster Blasters (handled client-side)
                // The client screen starts the enemy turn automatically after attack
                // Server will receive "endturn" when Gaster Blaster phase ends
            }

            case "spare" -> {
                battle.spareCount++;

                // Need to spare multiple times depending on mob health ratio
                int neededSpares = (int) Math.ceil(battle.target.getMaxHealth() / 10.0);
                neededSpares = Math.max(2, Math.min(neededSpares, 5));

                if (battle.spareCount >= neededSpares) {
                    // Mob is spared!
                    endBattle(player, "spare");
                } else {
                    // Not yet, mob resists but is softening
                    String[] spareTexts = {
                            "* " + battle.target.getType().getDescription().getString() + " hesite...",
                            "* " + battle.target.getType().getDescription().getString() + " semble se calmer...",
                            "* " + battle.target.getType().getDescription().getString() + " commence a te faire confiance...",
                            "* " + battle.target.getType().getDescription().getString() + " baisse sa garde..."
                    };
                    int textIndex = Math.min(battle.spareCount - 1, spareTexts.length - 1);
                    player.sendSystemMessage(Component.literal(spareTexts[textIndex])
                            .withStyle(ChatFormatting.YELLOW));

                    // Hearts appearing gradually
                    level.sendParticles(ParticleTypes.HEART,
                            battle.target.getX(), battle.target.getY() + battle.target.getBbHeight() + 0.5, battle.target.getZ(),
                            battle.spareCount * 2, 0.3, 0.3, 0.3, 0.05);

                    level.playSound(null, battle.target.blockPosition(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.8f + battle.spareCount * 0.15f);

                    // Enemy counter-attacks with Gaster Blasters (client handles dodge phase)
                    // Server receives "endturn" when done
                }
            }

            case "endturn" -> {
                // Enemy turn ended (Gaster Blaster phase over)
                float mobDamage = 2.0f + battle.target.getMaxHealth() * 0.1f;
                player.hurt(level.damageSources().mobAttack(battle.target), mobDamage);

                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getY() + 1, player.getZ(),
                        3, 0.2, 0.3, 0.2, 0.1);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.5f, 1.0f);

                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("* ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("Les Gaster Blasters se dissipent... ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("-" + (int)mobDamage + " PV").withStyle(ChatFormatting.DARK_RED)));

                // Check if player died
                if (!player.isAlive() || player.getHealth() <= 0) {
                    endBattle(player, "playerdeath");
                    return;
                }

                // Refresh screen for next player turn
                ModMessages.sendToPlayer(new OpenUndertaleBattleS2CPacket(
                        battle.target.getId(),
                        battle.target.getType().getDescription().getString(),
                        battle.target.getHealth(),
                        battle.target.getMaxHealth(),
                        data.isFrisk()), player);
            }

            case "flee" -> {
                // 50% chance to flee
                if (level.random.nextFloat() < 0.5f) {
                    endBattle(player, "flee");
                } else {
                    player.sendSystemMessage(Component.literal("* Tu ne peux pas fuir!")
                            .withStyle(ChatFormatting.RED));
                    mobCounterAttack(player, battle);

                    ModMessages.sendToPlayer(new OpenUndertaleBattleS2CPacket(
                            battle.target.getId(),
                            battle.target.getType().getDescription().getString(),
                            battle.target.getHealth(),
                            battle.target.getMaxHealth(),
                            data.isFrisk()), player);
                }
            }
        }
    }

    private static void mobCounterAttack(ServerPlayer player, ActiveBattle battle) {
        ServerLevel level = player.serverLevel();

        // Mob deals damage based on its attack damage
        float mobDamage = 2.0f + battle.target.getMaxHealth() * 0.1f;
        player.hurt(level.damageSources().mobAttack(battle.target), mobDamage);

        // Attack particles on player
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                player.getX(), player.getY() + 1, player.getZ(),
                3, 0.2, 0.3, 0.2, 0.1);

        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 1.0f);

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("* ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(battle.target.getType().getDescription().getString() + " attaque! ")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal("-" + (int)mobDamage + " PV").withStyle(ChatFormatting.DARK_RED)));
    }

    private static void endBattle(ServerPlayer player, String result) {
        UUID playerId = player.getUUID();
        ActiveBattle battle = activeBattles.remove(playerId);
        if (battle == null) return;

        ServerLevel level = player.serverLevel();

        // Remove slowdown from player
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        switch (result) {
            case "kill" -> {
                // XP reward
                player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                    data.addXp(30);
                    com.noxgg.elementalpower.event.ElementEvents.syncToClient(player, data);
                });

                level.sendParticles(ParticleTypes.SMOKE,
                        battle.target.getX(), battle.target.getY() + 1, battle.target.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1);

                level.playSound(null, player.blockPosition(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.5f);

                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("* ").withStyle(ChatFormatting.RED))
                        .append(Component.literal("VICTOIRE. Tu as gagne 30 XP.").withStyle(ChatFormatting.YELLOW)));
                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("* ").withStyle(ChatFormatting.RED))
                        .append(Component.literal("Mais personne n'est venu...").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
            }

            case "spare" -> {
                // Make mob friendly
                if (battle.target instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    mob.removeEffect(MobEffects.WEAKNESS);
                    mob.setPersistenceRequired();
                    mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2, false, true));
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));

                    String mobName = mob.getType().getDescription().getString();
                    mob.setCustomName(Component.literal("\u00A7a\u00A7l[Spare] \u00A72" + mobName));
                    mob.setCustomNameVisible(true);
                }

                // XP reward (more than kill)
                player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                    data.addXp(50);
                    com.noxgg.elementalpower.event.ElementEvents.syncToClient(player, data);
                });

                level.sendParticles(ParticleTypes.HEART,
                        battle.target.getX(), battle.target.getY() + 2, battle.target.getZ(),
                        15, 0.5, 0.5, 0.5, 0.1);

                var yellowDust = new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(1.0f, 1.0f, 0.0f), 2.0f);
                level.sendParticles(yellowDust,
                        battle.target.getX(), battle.target.getY() + 1, battle.target.getZ(),
                        25, 0.5, 0.5, 0.5, 0.05);

                level.playSound(null, player.blockPosition(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
                level.playSound(null, battle.target.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0f, 1.2f);

                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("* ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("Tu as EPARGNE " + battle.target.getType().getDescription().getString() + "!")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("* ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("Tu gagnes 50 XP. L'ennemi est devenu ton ami.").withStyle(ChatFormatting.GREEN)));
            }

            case "playerdeath" -> {
                battle.target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                battle.target.removeEffect(MobEffects.WEAKNESS);
                // Player died in combat, no message needed
            }

            case "flee" -> {
                battle.target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                battle.target.removeEffect(MobEffects.WEAKNESS);

                level.playSound(null, player.blockPosition(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

                player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("* ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("Tu as fui le combat.").withStyle(ChatFormatting.GRAY)));
            }
        }
    }

    public static void onPlayerLogout(UUID playerId) {
        ActiveBattle battle = activeBattles.remove(playerId);
        if (battle != null && battle.target instanceof Mob mob) {
            mob.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            mob.removeEffect(MobEffects.WEAKNESS);
        }
    }
}
