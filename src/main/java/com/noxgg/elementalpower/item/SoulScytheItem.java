package com.noxgg.elementalpower.item;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class SoulScytheItem extends SwordItem {

    private static final DustParticleOptions SOUL_DARK = new DustParticleOptions(
            new Vector3f(0.1f, 0.0f, 0.15f), 2.5f);
    private static final DustParticleOptions SOUL_CYAN = new DustParticleOptions(
            new Vector3f(0.2f, 0.8f, 0.9f), 1.5f);

    public SoulScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayer player)) return super.hurtEnemy(stack, target, attacker);
        if (!(player.level() instanceof ServerLevel level)) return super.hurtEnemy(stack, target, attacker);

        // Check if player has Darkness class
        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
            if (data.getElement() != ElementType.DARKNESS) return;

            // Check if the target dies from this hit
            if (target.getHealth() <= 0 || target.isDeadOrDying()) {
                reapSoul(player, target, level, data);
            }
        });

        return super.hurtEnemy(stack, target, attacker);
    }

    private void reapSoul(ServerPlayer player, LivingEntity target, ServerLevel level,
                          com.noxgg.elementalpower.element.PlayerElement data) {
        double tx = target.getX();
        double ty = target.getY();
        double tz = target.getZ();

        // === SOUL EXTRACTION: soul rips out of the body ===
        // Soul rising from body
        for (int i = 0; i < 15; i++) {
            double h = i * 0.3;
            level.sendParticles(ParticleTypes.SOUL,
                    tx, ty + h, tz, 3, 0.2, 0.1, 0.2, 0.03);
            level.sendParticles(SOUL_CYAN,
                    tx, ty + h, tz, 2, 0.15, 0.1, 0.15, 0.02);
        }

        // Soul trail flying to player
        double dirX = player.getX() - tx;
        double dirY = player.getEyeY() - ty;
        double dirZ = player.getZ() - tz;
        for (int i = 0; i < 15; i++) {
            double t = i / 15.0;
            double px = tx + dirX * t;
            double py = ty + 2 + dirY * t + Math.sin(t * Math.PI) * 2;
            double pz = tz + dirZ * t;
            level.sendParticles(ParticleTypes.SOUL,
                    px, py, pz, 2, 0.05, 0.05, 0.05, 0.02);
            level.sendParticles(SOUL_DARK,
                    px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Soul absorbed into player
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                player.getX(), player.getEyeY(), player.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(SOUL_CYAN,
                player.getX(), player.getY() + 1, player.getZ(), 8, 0.2, 0.5, 0.2, 0.03);

        // === GAIN 8 LEVELS ===
        for (int i = 0; i < 8; i++) {
            data.addXp(data.getXpForNextLevel()); // Force level up
        }
        data.addSoul();

        // Level up message
        player.sendSystemMessage(Component.literal(">> Ame fauchee! ")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("+8 niveaux! ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal("(Niv." + data.getLevel() + ")")
                        .withStyle(ChatFormatting.YELLOW)));

        // Level up particles
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 1, 0.5, 0.2);

        // Sounds
        level.playSound(null, player.blockPosition(),
                SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.8f);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 0.4f);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Faux des Tenebres").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Fauche l'ame des ennemis tues").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("+8 niveaux par ame recoltee").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Classe Tenebres uniquement").withStyle(ChatFormatting.DARK_GRAY));
    }
}
