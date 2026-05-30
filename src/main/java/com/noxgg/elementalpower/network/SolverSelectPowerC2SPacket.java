package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.world.AbsoluteSolverManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: pick the active Absolute Solver power from the R menu. */
public class SolverSelectPowerC2SPacket {
    private final int power;

    public SolverSelectPowerC2SPacket(int power) {
        this.power = power;
    }

    public SolverSelectPowerC2SPacket(FriendlyByteBuf buf) {
        this.power = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(power);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                if (data.getElement() != ElementType.ABSOLUTE_SOLVER) return;
                int p = power;
                if (p < 0 || p >= AbsoluteSolverManager.POWER_NAMES.length) p = 0;
                data.setSolverPower(p);
                player.sendSystemMessage(Component.literal(">> Pouvoir selectionne: ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(AbsoluteSolverManager.powerName(p))
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                        .append(Component.literal(" (clic droit pour declencher)")
                                .withStyle(ChatFormatting.GRAY)));
            });
        });
        return true;
    }
}
