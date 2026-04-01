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

        net.messageBuilder(OpenDemonPortalS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenDemonPortalS2CPacket::new)
                .encoder(OpenDemonPortalS2CPacket::toBytes)
                .consumerMainThread(OpenDemonPortalS2CPacket::handle)
                .add();

        net.messageBuilder(DemonPortalC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DemonPortalC2SPacket::new)
                .encoder(DemonPortalC2SPacket::toBytes)
                .consumerMainThread(DemonPortalC2SPacket::handle)
                .add();

        net.messageBuilder(SpiritStrangleC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SpiritStrangleC2SPacket::new)
                .encoder(SpiritStrangleC2SPacket::toBytes)
                .consumerMainThread(SpiritStrangleC2SPacket::handle)
                .add();

        net.messageBuilder(DreamSequenceS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(DreamSequenceS2CPacket::new)
                .encoder(DreamSequenceS2CPacket::toBytes)
                .consumerMainThread(DreamSequenceS2CPacket::handle)
                .add();

        net.messageBuilder(OpenUndertaleScreenS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenUndertaleScreenS2CPacket::new)
                .encoder(OpenUndertaleScreenS2CPacket::toBytes)
                .consumerMainThread(OpenUndertaleScreenS2CPacket::handle)
                .add();

        net.messageBuilder(UndertaleSubClassC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UndertaleSubClassC2SPacket::new)
                .encoder(UndertaleSubClassC2SPacket::toBytes)
                .consumerMainThread(UndertaleSubClassC2SPacket::handle)
                .add();

        net.messageBuilder(OpenUndertaleBattleS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenUndertaleBattleS2CPacket::new)
                .encoder(OpenUndertaleBattleS2CPacket::toBytes)
                .consumerMainThread(OpenUndertaleBattleS2CPacket::handle)
                .add();

        net.messageBuilder(UndertaleBattleActionC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UndertaleBattleActionC2SPacket::new)
                .encoder(UndertaleBattleActionC2SPacket::toBytes)
                .consumerMainThread(UndertaleBattleActionC2SPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
