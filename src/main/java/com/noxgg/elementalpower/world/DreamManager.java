package com.noxgg.elementalpower.world;

import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.item.ModItems;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.DreamSequenceS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

import java.util.*;

public class DreamManager {
    private static final Map<UUID, DreamState> dreamingPlayers = new HashMap<>();
    private static final Map<UUID, DeathSequence> deathSequences = new HashMap<>();

    // 3 in-game days = 3 * 24000 = 72000 ticks
    private static final long CONTRACT_DURATION_TICKS = 72000L;

    private static final DustParticleOptions MOON_WHITE = new DustParticleOptions(
            new Vector3f(0.9f, 0.9f, 1.0f), 3.0f);
    private static final DustParticleOptions DREAM_BLUE = new DustParticleOptions(
            new Vector3f(0.3f, 0.4f, 0.9f), 2.0f);
    private static final DustParticleOptions DEATH_RED = new DustParticleOptions(
            new Vector3f(0.8f, 0.1f, 0.1f), 3.0f);

    // Quest definitions
    public static final String[] QUEST_NAMES = {
            "Tuer 10 monstres",
            "Miner 32 diamants",
            "Explorer 1000 blocs",
            "Tuer un Wither",
            "Recolter 64 blaze rods",
            "Trouver une forteresse",
            "Crafter une etoile du Nether",
            "Tuer 50 mobs avec ton sort R",
            "Atteindre le niveau 15",
            "Visiter le Nether et l'End"
    };

    public static class DreamState {
        public final ServerPlayer player;
        public final double savedX, savedY, savedZ;
        public final float savedYaw, savedPitch;
        public int tick = 0;
        public int phase = 0; // 0=build hand, 1=moon appears, 2=quest scroll, 3=wake up
        public final int dreamX, dreamZ;
        public boolean questAssigned = false;
        public int assignedQuest = -1;

        public DreamState(ServerPlayer player) {
            this.player = player;
            this.savedX = player.getX();
            this.savedY = player.getY();
            this.savedZ = player.getZ();
            this.savedYaw = player.getYRot();
            this.savedPitch = player.getXRot();
            this.dreamX = (int) player.getX() + 5000;
            this.dreamZ = (int) player.getZ();
        }
    }

    // Death sequence when contract expires
    public static class DeathSequence {
        public final ServerPlayer player;
        public final int moonX, moonY, moonZ;
        public int tick = 0;
        public int phase = 0; // 0=tp+build moon, 1=moon talks, 2=kill

        public DeathSequence(ServerPlayer player) {
            this.player = player;
            this.moonX = (int) player.getX() + 5000;
            this.moonY = 130;
            this.moonZ = (int) player.getZ() + 60;
        }
    }

    public static void startDream(ServerPlayer player) {
        if (dreamingPlayers.containsKey(player.getUUID())) return;
        if (deathSequences.containsKey(player.getUUID())) return;

        // Check if player has an active contract
        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
            if (data.hasActiveContract() && !data.isContractCompleted()) {
                // Player has an unfulfilled contract - just sleep normally, don't start dream
                return;
            }
        });

        // Check again synchronously - if player has active contract, skip dream
        boolean[] hasContract = {false};
        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
            hasContract[0] = data.hasActiveContract() && !data.isContractCompleted();
        });
        if (hasContract[0]) return;

        DreamState state = new DreamState(player);
        dreamingPlayers.put(player.getUUID(), state);

        // Build the giant hand platform
        buildGiantHand(player.serverLevel(), state.dreamX, 100, state.dreamZ);

        // Teleport player to the hand
        player.teleportTo(state.dreamX, 105, state.dreamZ);
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;

        // Dream effects
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 6000, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));

        // Set night time for ambiance
        player.serverLevel().setDayTime(18000);

        // Notify client to start dream sequence
        ModMessages.sendToPlayer(new DreamSequenceS2CPacket(0, ""), player);

        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.AMBIENT_CAVE.get(), SoundSource.AMBIENT, 2.0f, 0.3f);
    }

    // Start the death sequence when contract expires
    public static void startDeathSequence(ServerPlayer player) {
        if (deathSequences.containsKey(player.getUUID())) return;
        if (dreamingPlayers.containsKey(player.getUUID())) return;

        DeathSequence seq = new DeathSequence(player);
        deathSequences.put(player.getUUID(), seq);

        ServerLevel level = player.serverLevel();

        // Build moon face
        buildMoonFace(level, seq.moonX, seq.moonY, seq.moonZ);

        // Build a small platform for the player
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlock(new BlockPos(seq.moonX + x, 99, seq.moonZ - 60 + z),
                        Blocks.SOUL_SAND.defaultBlockState(), 3);
            }
        }

        // TP player in front of the moon
        player.teleportTo(seq.moonX, 100, seq.moonZ - 60);
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;

        // Lock the player in place
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 255, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, false, false));

        // Set night
        level.setDayTime(18000);

        player.sendSystemMessage(Component.literal(">> La Lune vous convoque...")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

        level.playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0f, 0.3f);
    }

    public static void tick() {
        // Tick dream sequences
        tickDreams();
        // Tick death sequences
        tickDeathSequences();
        // Check contract expirations
        tickContractChecks();
    }

    private static void tickDreams() {
        Iterator<Map.Entry<UUID, DreamState>> it = dreamingPlayers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DreamState> entry = it.next();
            DreamState state = entry.getValue();
            ServerPlayer player = state.player;

            if (player == null || player.isRemoved()) {
                it.remove();
                continue;
            }

            state.tick++;
            ServerLevel level = player.serverLevel();

            // Keep player on the hand
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, false));

            // Ambient dream particles
            if (state.tick % 5 == 0) {
                for (int i = 0; i < 10; i++) {
                    double px = player.getX() + (Math.random() - 0.5) * 20;
                    double py = player.getY() + Math.random() * 15;
                    double pz = player.getZ() + (Math.random() - 0.5) * 20;
                    level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0, -0.05, 0, 0.005);
                }
            }

            // === PHASE 0: On the hand, wait 3 seconds ===
            if (state.phase == 0 && state.tick > 60) {
                state.phase = 1;
                state.tick = 0;

                // Build moon face in front of player
                buildMoonFace(level, state.dreamX, 130, state.dreamZ + 60);

                // Notify client: moon message
                ModMessages.sendToPlayer(new DreamSequenceS2CPacket(1,
                        "Mes vaillants soldats, voici un contrat. Reglez-le avant la date limite."), player);

                level.playSound(null, player.blockPosition(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.AMBIENT, 2.0f, 0.5f);
            }

            // === PHASE 1: Moon talking, wait 5 seconds ===
            if (state.phase == 1) {
                // Moon glow particles
                if (state.tick % 3 == 0) {
                    level.sendParticles(MOON_WHITE,
                            state.dreamX, 140, state.dreamZ + 60,
                            20, 8, 8, 2, 0.02);
                    level.sendParticles(ParticleTypes.END_ROD,
                            state.dreamX, 135, state.dreamZ + 60,
                            5, 5, 5, 1, 0.01);
                }

                if (state.tick > 100) {
                    state.phase = 2;
                    state.tick = 0;

                    // Pick a random quest
                    Random rand = new Random();
                    state.assignedQuest = rand.nextInt(QUEST_NAMES.length);
                    String questName = QUEST_NAMES[state.assignedQuest];

                    // Notify client: quest scroll
                    ModMessages.sendToPlayer(new DreamSequenceS2CPacket(2, questName), player);

                    level.playSound(null, player.blockPosition(),
                            SoundEvents.BOOK_PAGE_TURN, SoundSource.AMBIENT, 2.0f, 0.8f);
                }
            }

            // === PHASE 2: Quest scroll visible, wait 5 seconds then wake up ===
            if (state.phase == 2 && state.tick > 100) {
                state.phase = 3;
                wakeUp(state);
                it.remove();
            }
        }
    }

    private static void tickDeathSequences() {
        Iterator<Map.Entry<UUID, DeathSequence>> it = deathSequences.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DeathSequence> entry = it.next();
            DeathSequence seq = entry.getValue();
            ServerPlayer player = seq.player;

            if (player == null || player.isRemoved()) {
                it.remove();
                continue;
            }

            seq.tick++;
            ServerLevel level = player.serverLevel();

            // Lock player
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 255, false, false));

            // Red ominous particles around
            if (seq.tick % 3 == 0) {
                level.sendParticles(DEATH_RED,
                        player.getX(), player.getY() + 1, player.getZ(),
                        10, 1.5, 1.5, 1.5, 0.02);
                level.sendParticles(ParticleTypes.SOUL,
                        seq.moonX, seq.moonY, seq.moonZ,
                        15, 8, 8, 2, 0.03);
            }

            // Phase 0: Moon appears, wait 2 seconds
            if (seq.phase == 0 && seq.tick > 40) {
                seq.phase = 1;
                seq.tick = 0;

                player.sendSystemMessage(Component.literal(">> La Lune: \"Tu n'as pas rempli ton contrat...\"")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));

                level.playSound(null, player.blockPosition(),
                        SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0f, 0.5f);
            }

            // Phase 1: Moon threatens, wait 3 seconds
            if (seq.phase == 1 && seq.tick > 60) {
                seq.phase = 2;
                seq.tick = 0;

                player.sendSystemMessage(Component.literal(">> La Lune: \"Ton ame m'appartient desormais.\"")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

                level.playSound(null, player.blockPosition(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.3f);
            }

            // Phase 2: Kill the player after 1 second
            if (seq.phase == 2 && seq.tick > 20) {
                // Death explosion particles
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        50, 2, 2, 2, 0.1);
                level.sendParticles(DEATH_RED,
                        player.getX(), player.getY() + 1, player.getZ(),
                        30, 1, 2, 1, 0.05);

                // Remove contract from player data
                player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                    data.clearContract();
                });

                // Remove the soul contract item from inventory
                removeSoulContract(player);

                // Kill the player
                player.kill();

                level.playSound(null, player.blockPosition(),
                        SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 2.0f, 0.5f);

                it.remove();
            }
        }
    }

    private static void tickContractChecks() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (dreamingPlayers.containsKey(player.getUUID())) continue;
            if (deathSequences.containsKey(player.getUUID())) continue;

            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                if (!data.hasActiveContract()) return;

                long currentTime = player.serverLevel().getGameTime();

                if (data.isContractCompleted()) {
                    // Contract was completed! Give rewards
                    giveContractRewards(player);
                    data.clearContract();
                    removeSoulContract(player);
                } else if (currentTime >= data.getContractDeadlineTick()) {
                    // Contract expired! Start death sequence
                    startDeathSequence(player);
                }
            });
        }
    }

    private static void giveContractRewards(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // Give the 3 reward items (infinite durability = no durability set, unbreakable)
        ItemStack explosive = new ItemStack(ModItems.EXPLOSIVE_CONCENTRATION.get());
        explosive.getOrCreateTag().putBoolean("Unbreakable", true);
        explosive.setHoverName(Component.literal("Explosive Man Concentration")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        player.getInventory().add(explosive);

        ItemStack gauntlet = new ItemStack(ModItems.PROTECTION_GAUNTLET.get());
        gauntlet.getOrCreateTag().putBoolean("Unbreakable", true);
        gauntlet.setHoverName(Component.literal("Protection Gauntlet")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.getInventory().add(gauntlet);

        ItemStack morganSword = new ItemStack(ModItems.MORGAN_SWORD.get());
        morganSword.getOrCreateTag().putBoolean("Unbreakable", true);
        morganSword.setHoverName(Component.literal("Epee Morgan")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.getInventory().add(morganSword);

        // Effects
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1, player.getZ(),
                100, 2, 2, 2, 0.5);

        level.playSound(null, player.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 2.0f, 1.5f);

        player.sendSystemMessage(Component.literal(">> CONTRAT REMPLI! La Lune vous recompense!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal(">> Vous recevez: Explosive Man Concentration, Protection Gauntlet, Epee Morgan!")
                .withStyle(ChatFormatting.GREEN));
    }

    private static void removeSoulContract(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.SOUL_CONTRACT.get())) {
                player.getInventory().removeItem(i, stack.getCount());
                break;
            }
        }
    }

    private static void wakeUp(DreamState state) {
        ServerPlayer player = state.player;
        ServerLevel level = player.serverLevel();

        // Teleport back to bed
        player.teleportTo(state.savedX, state.savedY, state.savedZ);
        player.setYRot(state.savedYaw);
        player.setXRot(state.savedPitch);

        // Remove dream effects
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        // Give contract item
        ItemStack contract = new ItemStack(ModItems.SOUL_CONTRACT.get());
        if (state.assignedQuest >= 0 && state.assignedQuest < QUEST_NAMES.length) {
            contract.setHoverName(Component.literal("Contrat: " + QUEST_NAMES[state.assignedQuest])
                    .withStyle(ChatFormatting.GOLD));
        }
        player.getInventory().add(contract);

        // Save contract in player data with deadline
        long deadline = level.getGameTime() + CONTRACT_DURATION_TICKS;
        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
            data.startContract(state.assignedQuest, deadline);
        });

        // Wake up effects
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);

        level.playSound(null, player.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.5f, 1.0f);

        // Notify client: wake up with timer (3 nights)
        String questName = state.assignedQuest >= 0 ? QUEST_NAMES[state.assignedQuest] : "Quete inconnue";
        ModMessages.sendToPlayer(new DreamSequenceS2CPacket(3, questName), player);

        player.sendSystemMessage(Component.literal(">> Vous vous reveillez avec un contrat en main!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal(">> Quete: " + questName)
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal(">> Temps restant: 3 jours en jeu!")
                .withStyle(ChatFormatting.RED));
        player.sendSystemMessage(Component.literal(">> Si vous echouez, la Lune viendra chercher votre ame...")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    private static void buildGiantHand(ServerLevel level, int cx, int cy, int cz) {
        // Giant hand 100x100 - simplified as a large palm with fingers
        // Palm: 40x50 oval
        for (int x = -20; x <= 20; x++) {
            for (int z = -25; z <= 25; z++) {
                double dist = (x * x) / 400.0 + (z * z) / 625.0;
                if (dist <= 1.0) {
                    level.setBlock(new BlockPos(cx + x, cy, cz + z),
                            Blocks.BONE_BLOCK.defaultBlockState(), 3);
                    // Slightly raised center
                    if (dist < 0.3) {
                        level.setBlock(new BlockPos(cx + x, cy + 1, cz + z),
                                Blocks.BONE_BLOCK.defaultBlockState(), 3);
                    }
                }
            }
        }

        // 5 Fingers extending forward (in +Z direction)
        int[][] fingerOffsets = {{-16, 35}, {-8, 42}, {0, 45}, {8, 42}, {16, 35}};
        int[] fingerWidths = {5, 5, 5, 5, 4};
        int[] fingerLengths = {18, 22, 25, 22, 16};

        for (int f = 0; f < 5; f++) {
            int fx = fingerOffsets[f][0];
            int fzStart = fingerOffsets[f][1];
            int fw = fingerWidths[f];
            int fl = fingerLengths[f];

            for (int seg = 0; seg < fl; seg++) {
                double narrowing = 1.0 - (double) seg / fl * 0.4;
                int currentWidth = (int)(fw * narrowing);
                for (int dx = -currentWidth; dx <= currentWidth; dx++) {
                    double edgeDist = Math.abs(dx) / (double) currentWidth;
                    level.setBlock(new BlockPos(cx + fx + dx, cy, cz + fzStart + seg),
                            Blocks.BONE_BLOCK.defaultBlockState(), 3);
                }
            }

            // Fingernail at tip
            for (int dx = -2; dx <= 2; dx++) {
                level.setBlock(new BlockPos(cx + fx + dx, cy + 1, cz + fzStart + fl - 1),
                        Blocks.QUARTZ_BLOCK.defaultBlockState(), 3);
                level.setBlock(new BlockPos(cx + fx + dx, cy + 1, cz + fzStart + fl),
                        Blocks.QUARTZ_BLOCK.defaultBlockState(), 3);
            }
        }

        // Thumb (side, in -X direction)
        for (int seg = 0; seg < 15; seg++) {
            int tw = (int)(4 * (1.0 - seg / 15.0 * 0.3));
            for (int dz = -tw; dz <= tw; dz++) {
                level.setBlock(new BlockPos(cx - 20 - seg, cy, cz - 5 + dz),
                        Blocks.BONE_BLOCK.defaultBlockState(), 3);
            }
        }
        // Thumb nail
        for (int dz = -2; dz <= 2; dz++) {
            level.setBlock(new BlockPos(cx - 35, cy + 1, cz - 5 + dz),
                    Blocks.QUARTZ_BLOCK.defaultBlockState(), 3);
        }

        // Life lines on palm (soul sand grooves)
        for (int i = -15; i <= 15; i++) {
            int zOffset = (int)(Math.sin(i * 0.15) * 5);
            level.setBlock(new BlockPos(cx + i, cy, cz + zOffset),
                    Blocks.SOUL_SAND.defaultBlockState(), 3);
            level.setBlock(new BlockPos(cx + i, cy, cz + zOffset + 8),
                    Blocks.SOUL_SAND.defaultBlockState(), 3);
        }
    }

    private static void buildMoonFace(ServerLevel level, int cx, int cy, int cz) {
        // Moon: large circle of glowstone/white blocks (radius 15)
        int radius = 15;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                double dist = Math.sqrt(x * x + y * y);
                if (dist <= radius) {
                    BlockPos pos = new BlockPos(cx + x, cy + y, cz);
                    if (dist > radius - 1.5) {
                        level.setBlock(pos, Blocks.GLOWSTONE.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Eyes (dark holes)
        for (int side = -1; side <= 1; side += 2) {
            for (int ex = -2; ex <= 2; ex++) {
                for (int ey = -1; ey <= 1; ey++) {
                    level.setBlock(new BlockPos(cx + side * 5 + ex, cy + 4 + ey, cz),
                            Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
                }
            }
            // Glowing pupil
            level.setBlock(new BlockPos(cx + side * 5, cy + 4, cz - 1),
                    Blocks.SEA_LANTERN.defaultBlockState(), 3);
        }

        // Mouth (wide grin)
        for (int mx = -7; mx <= 7; mx++) {
            int my = -(int)(Math.abs(mx) * 0.3);
            level.setBlock(new BlockPos(cx + mx, cy - 3 + my, cz),
                    Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(cx + mx, cy - 4 + my, cz),
                    Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
        }
    }

    public static boolean isDreaming(UUID playerId) {
        return dreamingPlayers.containsKey(playerId);
    }

    public static boolean isInDeathSequence(UUID playerId) {
        return deathSequences.containsKey(playerId);
    }
}
