package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UndertaleSubClassC2SPacket {
    private final String subClass;

    public UndertaleSubClassC2SPacket(String subClass) {
        this.subClass = subClass;
    }

    public UndertaleSubClassC2SPacket(FriendlyByteBuf buf) {
        this.subClass = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(subClass);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!subClass.equals("chara") && !subClass.equals("frisk")) return;

            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                if (data.getElement() != ElementType.UNDERTALE) return;

                data.setSubClass(subClass);

                if (subClass.equals("chara")) {
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("* ").withStyle(ChatFormatting.RED))
                            .append(Component.literal("Tu as choisi la voie du GENOCIDE.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                    );
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("* ").withStyle(ChatFormatting.RED))
                            .append(Component.literal("Ta DETERMINATION te rend plus fort a chaque kill.").withStyle(ChatFormatting.RED))
                    );
                } else {
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("* ").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("Tu as choisi la voie du PACIFISTE.").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                    );
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("* ").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("Tu ne peux pas attaquer. Appuie sur G pour SPARE un ennemi.").withStyle(ChatFormatting.GOLD))
                    );
                }

                // Sync to client
                ModMessages.sendToPlayer(new SyncElementS2CPacket(
                        data.getElement().getId(), data.getLevel(), data.getXp(), data.getSouls()), player);
            });
        });
        return true;
    }
}
