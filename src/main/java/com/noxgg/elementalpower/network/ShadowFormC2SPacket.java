package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.world.ShadowFormManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShadowFormC2SPacket {
    public ShadowFormC2SPacket() {}
    public ShadowFormC2SPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                if (data.getElement() != ElementType.DARKNESS) {
                    player.sendSystemMessage(Component.literal("Ce sort n'est pas disponible pour ta classe!")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                if (data.getLevel() < 10 && !player.isCreative() && !player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.literal(">> Sort L verrouille! Niveau 10 requis (actuel: " + data.getLevel() + ")")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                ShadowFormManager.toggleShadowForm(player);
            });
        });
        return true;
    }
}
