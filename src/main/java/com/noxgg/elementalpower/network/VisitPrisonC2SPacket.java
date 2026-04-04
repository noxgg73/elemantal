package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class VisitPrisonC2SPacket {

    private static final DustParticleOptions GOLD = new DustParticleOptions(
            new Vector3f(1.0f, 0.84f, 0.0f), 2.0f);

    public VisitPrisonC2SPacket() {}
    public VisitPrisonC2SPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                ElementType element = data.getElement();

                // Alastor override: K key = Demonic Radio Wave
                if (element == ElementType.DEMON && data.isAlastor()) {
                    com.noxgg.elementalpower.world.AlastorManager.castRadioWave(player);
                    return;
                }

                if (element != ElementType.ROYAL && element != ElementType.DARKNESS && element != ElementType.DEMON) {
                    player.sendSystemMessage(Component.literal("Ce pouvoir n'est pas disponible pour ta classe!")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                ServerLevel level = player.serverLevel();

                // DEMON: Spawn demon village
                if (element == ElementType.DEMON) {
                    net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                    int villageX = (int)(player.getX() + look.x * 30);
                    int villageZ = (int)(player.getZ() + look.z * 30);

                    com.noxgg.elementalpower.world.DemonVillageGenerator.generate(
                            level, villageX, villageZ, player.getUUID());

                    // Teleport player to village center
                    int villageY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            villageX, villageZ);
                    player.teleportTo(villageX + 0.5, villageY + 1, villageZ + 0.5);

                    level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0f, 0.5f);

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Village Demoniaque invoque! ")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                            .append(net.minecraft.network.chat.Component.literal("Les demons vous accueillent...")
                                    .withStyle(ChatFormatting.RED)));
                    return;
                }

                // Check if player is already in the prison (y < -40) -> teleport back up
                if (player.getY() < -40) {
                    // Return to surface: teleport to saved position or spawn
                    BlockPos spawn = level.getSharedSpawnPos();
                    double returnX = player.getX();
                    double returnZ = player.getZ();

                    // Find safe Y on surface
                    int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                            (int) returnX, (int) returnZ);

                    // Departure particles in prison
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            30, 0.3, 0.5, 0.3, 0.1);

                    player.teleportTo(returnX, surfaceY + 1, returnZ);

                    // Arrival particles on surface
                    level.sendParticles(ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            40, 0.5, 1, 0.5, 0.5);
                    level.sendParticles(GOLD,
                            player.getX(), player.getY() + 1, player.getZ(),
                            20, 0.3, 0.5, 0.3, 0.05);

                    level.playSound(null, player.blockPosition(),
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.0f);

                    player.sendSystemMessage(Component.literal(">> Retour a la surface.")
                            .withStyle(ChatFormatting.GREEN));
                    return;
                }

                // Go to prison: teleport to y=-50 at player's current X/Z
                int prisonX = (int) player.getX();
                int prisonY = -50;
                int prisonZ = (int) player.getZ();

                // Departure particles
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        50, 0.3, 0.5, 0.3, 0.15);
                level.sendParticles(GOLD,
                        player.getX(), player.getY() + 1, player.getZ(),
                        25, 0.5, 1, 0.5, 0.05);

                level.playSound(null, player.blockPosition(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 0.5f);

                // Build a visitor room next to the prison (so player doesn't get stuck in bedrock)
                // Visitor corridor: 3 wide, 3 tall, 5 long, next to prison
                for (int x = -1; x <= 1; x++) {
                    for (int y = 0; y <= 2; y++) {
                        for (int z = 3; z <= 7; z++) {
                            BlockPos pos = new BlockPos(prisonX + x, prisonY + y, prisonZ + z);
                            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
                // Floor
                for (int x = -1; x <= 1; x++) {
                    for (int z = 3; z <= 7; z++) {
                        BlockPos pos = new BlockPos(prisonX + x, prisonY - 1, prisonZ + z);
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                }
                // Ceiling
                for (int x = -1; x <= 1; x++) {
                    for (int z = 3; z <= 7; z++) {
                        BlockPos pos = new BlockPos(prisonX + x, prisonY + 3, prisonZ + z);
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                }
                // Walls
                for (int y = -1; y <= 3; y++) {
                    for (int z = 3; z <= 7; z++) {
                        level.setBlock(new BlockPos(prisonX - 2, prisonY + y, prisonZ + z),
                                net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 3);
                        level.setBlock(new BlockPos(prisonX + 2, prisonY + y, prisonZ + z),
                                net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                }
                // Back wall
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 3; y++) {
                        level.setBlock(new BlockPos(prisonX + x, prisonY + y, prisonZ + 8),
                                net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                }
                // Light
                level.setBlock(new BlockPos(prisonX, prisonY + 2, prisonZ + 5),
                        net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState(), 3);

                // Window between corridor and prison (iron bars)
                for (int y = 0; y <= 1; y++) {
                    for (int x = -1; x <= 1; x++) {
                        level.setBlock(new BlockPos(prisonX + x, prisonY + y, prisonZ + 2),
                                net.minecraft.world.level.block.Blocks.IRON_BARS.defaultBlockState(), 3);
                    }
                }

                // Teleport player to visitor corridor
                player.teleportTo(prisonX + 0.5, prisonY, prisonZ + 5.5);

                // Arrival particles
                level.sendParticles(ParticleTypes.PORTAL,
                        prisonX + 0.5, prisonY + 1, prisonZ + 5.5,
                        40, 0.3, 0.5, 0.3, 0.5);

                level.playSound(null, new BlockPos(prisonX, prisonY, prisonZ + 5),
                        SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1.5f, 0.7f);

                player.sendSystemMessage(Component.literal(">> Visite de la prison. ")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .append(Component.literal("Appuyez sur K pour remonter a la surface.")
                                .withStyle(ChatFormatting.YELLOW)));
            });
        });
        return true;
    }
}
