package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ElementChoiceC2SPacket {
    private final String elementId;

    public ElementChoiceC2SPacket(String elementId) {
        this.elementId = elementId;
    }

    public ElementChoiceC2SPacket(FriendlyByteBuf buf) {
        this.elementId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(elementId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ElementType chosen = ElementType.fromId(elementId);
                if (chosen != ElementType.NONE) {
                    player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                        data.setElement(chosen);
                        player.sendSystemMessage(Component.literal("Tu as choisi l'element: ")
                                .withStyle(ChatFormatting.GOLD)
                                .append(Component.literal(chosen.getDisplayName())
                                        .withStyle(chosen.getColor())));
                        // Sync to client
                        ModMessages.sendToPlayer(new SyncElementS2CPacket(
                                chosen.getId(), data.getLevel(), data.getXp(), data.getSouls()), player);
                    });
                }
            }
        });
        return true;
    }
}
