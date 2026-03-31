package com.noxgg.elementalpower.item;

import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.OpenSelectionS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElementResetPearlItem extends Item {

    public ElementResetPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Reset the player's element choice
            serverPlayer.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                String oldElement = data.getElement().getDisplayName();

                // Reset hasChosen so the screen works properly
                data.setElement(com.noxgg.elementalpower.element.ElementType.NONE);

                serverPlayer.sendSystemMessage(Component.literal("Ta connexion avec l'element ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(oldElement).withStyle(ChatFormatting.RED))
                        .append(Component.literal(" a ete brisee !").withStyle(ChatFormatting.GRAY)));

                // Open the selection screen again
                ModMessages.sendToPlayer(new OpenSelectionS2CPacket(), serverPlayer);
            });

            // Consume the pearl
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            // Effects
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0f, 0.5f);
            level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Brise ta connexion elementaire").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("et te permet de choisir un").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("nouvel element.").withStyle(ChatFormatting.GRAY));
    }
}
