package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.ElementalPowerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() { return packetId++; }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ElementalPowerMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ElementChoiceC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ElementChoiceC2SPacket::new)
                .encoder(ElementChoiceC2SPacket::toBytes)
                .consumerMainThread(ElementChoiceC2SPacket::handle)
                .add();

        net.messageBuilder(OpenSelectionS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenSelectionS2CPacket::new)
                .encoder(OpenSelectionS2CPacket::toBytes)
                .consumerMainThread(OpenSelectionS2CPacket::handle)
                .add();

        net.messageBuilder(SyncElementS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncElementS2CPacket::new)
                .encoder(SyncElementS2CPacket::toBytes)
                .consumerMainThread(SyncElementS2CPacket::handle)
                .add();

        net.messageBuilder(UseElementPowerC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UseElementPowerC2SPacket::new)
                .encoder(UseElementPowerC2SPacket::toBytes)
                .consumerMainThread(UseElementPowerC2SPacket::handle)
                .add();

        net.messageBuilder(DarkPrisonC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DarkPrisonC2SPacket::new)
                .encoder(DarkPrisonC2SPacket::toBytes)
                .consumerMainThread(DarkPrisonC2SPacket::handle)
                .add();

        net.messageBuilder(OpenRoyalScreenS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenRoyalScreenS2CPacket::new)
                .encoder(OpenRoyalScreenS2CPacket::toBytes)
                .consumerMainThread(OpenRoyalScreenS2CPacket::handle)
                .add();

        net.messageBuilder(RoyalJudgmentC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RoyalJudgmentC2SPacket::new)
                .encoder(RoyalJudgmentC2SPacket::toBytes)
                .consumerMainThread(RoyalJudgmentC2SPacket::handle)
                .add();

        net.messageBuilder(VisitPrisonC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(VisitPrisonC2SPacket::new)
                .encoder(VisitPrisonC2SPacket::toBytes)
                .consumerMainThread(VisitPrisonC2SPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
