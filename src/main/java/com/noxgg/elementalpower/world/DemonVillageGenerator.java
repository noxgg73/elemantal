package com.noxgg.elementalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

import java.util.Random;
import java.util.UUID;

public class DemonVillageGenerator {

    private static final DustParticleOptions DEMON_RED = new DustParticleOptions(
            new Vector3f(0.7f, 0.05f, 0.0f), 2.0f);

    public static void generate(ServerLevel level, int centerX, int centerZ, UUID casterUUID) {
        Random rand = new Random();
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);

        // === CLEAR AND FLATTEN AREA ===
        for (int x = -18; x <= 18; x++) {
            for (int z = -18; z <= 18; z++) {
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX + x, centerZ + z);
                // Clear above
                for (int y = surfaceY; y < surfaceY + 10; y++) {
                    level.setBlock(new BlockPos(centerX + x, y, centerZ + z), Blocks.AIR.defaultBlockState(), 3);
                }
                // Flatten to ground level with soul sand paths
                level.setBlock(new BlockPos(centerX + x, groundY - 1, centerZ + z),
                        Blocks.SOUL_SAND.defaultBlockState(), 3);
            }
        }

        // === NETHER BRICK PATHS (cross shape) ===
        for (int i = -16; i <= 16; i++) {
            for (int w = -1; w <= 1; w++) {
                level.setBlock(new BlockPos(centerX + i, groundY - 1, centerZ + w),
                        Blocks.NETHER_BRICKS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(centerX + w, groundY - 1, centerZ + i),
                        Blocks.NETHER_BRICKS.defaultBlockState(), 3);
            }
        }

        // === BUILD HOUSES ===
        // 4 houses in each quadrant
        int[][] housePositions = {
                {6, 6}, {-6, 6}, {6, -6}, {-6, -6},
                {12, 4}, {-12, 4}, {4, -12}, {-4, 12}
        };

        for (int[] pos : housePositions) {
            buildDemonHouse(level, centerX + pos[0], groundY, centerZ + pos[1], rand);
        }

        // === CENTRAL ALTAR ===
        buildAltar(level, centerX, groundY, centerZ);

        // === LAVA POOLS (decoration) ===
        int[][] lavaPools = {{10, 10}, {-10, -10}, {10, -10}, {-10, 10}};
        for (int[] pool : lavaPools) {
            int px = centerX + pool[0];
            int pz = centerZ + pool[1];
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlock(new BlockPos(px + dx, groundY - 2, pz + dz),
                            Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(px + dx, groundY - 1, pz + dz),
                            Blocks.LAVA.defaultBlockState(), 3);
                }
            }
            // Rim
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        level.setBlock(new BlockPos(px + dx, groundY - 1, pz + dz),
                                Blocks.NETHER_BRICK_FENCE.defaultBlockState(), 3);
                    }
                }
            }
        }

        // === SOUL LANTERNS along paths ===
        for (int i = -14; i <= 14; i += 4) {
            level.setBlock(new BlockPos(centerX + i, groundY, centerZ + 2),
                    Blocks.NETHER_BRICK_FENCE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(centerX + i, groundY + 1, centerZ + 2),
                    Blocks.SOUL_LANTERN.defaultBlockState(), 3);
            level.setBlock(new BlockPos(centerX + 2, groundY, centerZ + i),
                    Blocks.NETHER_BRICK_FENCE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(centerX + 2, groundY + 1, centerZ + i),
                    Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        }

        // === SPAWN DEMON VILLAGERS (zombified piglins with custom gear) ===
        String[] demonNames = {"Azrael", "Belphegor", "Mammon", "Asmodeus", "Beelzebub",
                "Lilith", "Abaddon", "Moloch", "Azazel", "Samael", "Mephisto", "Belial"};

        for (int i = 0; i < 12; i++) {
            double spawnX = centerX + (rand.nextDouble() - 0.5) * 28;
            double spawnZ = centerZ + (rand.nextDouble() - 0.5) * 28;
            int spawnY = groundY;

            ZombifiedPiglin demon = EntityType.ZOMBIFIED_PIGLIN.create(level);
            if (demon != null) {
                demon.setPos(spawnX, spawnY, spawnZ);
                // Make them passive (won't attack unless provoked)
                demon.setPersistenceRequired();
                // Custom name
                demon.setCustomName(net.minecraft.network.chat.Component.literal(demonNames[i % demonNames.length])
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED));
                demon.setCustomNameVisible(true);
                // Give demon gear
                switch (i % 4) {
                    case 0 -> { // Merchant
                        demon.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                        demon.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                    }
                    case 1 -> { // Guard
                        demon.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
                        demon.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                        demon.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                    }
                    case 2 -> { // Priest
                        demon.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BLAZE_ROD));
                        demon.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.WITHER_SKELETON_SKULL));
                    }
                    case 3 -> { // Worker
                        demon.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
                    }
                }
                // Drop rates at 0 so they don't drop their gear
                demon.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
                demon.setDropChance(EquipmentSlot.HEAD, 0.0f);
                demon.setDropChance(EquipmentSlot.CHEST, 0.0f);

                // Permanent effects
                demon.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

                level.addFreshEntity(demon);

                // Spawn particles
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        spawnX, spawnY + 1, spawnZ, 10, 0.3, 0.5, 0.3, 0.02);
            }
        }

        // === PORTAL ARRIVAL PARTICLES ===
        level.sendParticles(DEMON_RED, centerX, groundY + 2, centerZ, 60, 3, 2, 3, 0.1);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, centerX, groundY + 1, centerZ, 40, 5, 1, 5, 0.05);
        level.sendParticles(ParticleTypes.LAVA, centerX, groundY + 3, centerZ, 30, 3, 1, 3, 0.1);

        // === SOUNDS ===
        level.playSound(null, new BlockPos(centerX, groundY, centerZ),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0f, 0.5f);
        level.playSound(null, new BlockPos(centerX, groundY, centerZ),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.5f, 0.3f);
    }

    private static void buildDemonHouse(ServerLevel level, int x, int y, int z, Random rand) {
        BlockState wall = Blocks.NETHER_BRICKS.defaultBlockState();
        BlockState floor = Blocks.CRIMSON_PLANKS.defaultBlockState();
        BlockState roof = Blocks.DARK_OAK_SLAB.defaultBlockState();

        int w = 3; // half-width
        int h = 4; // height

        // Floor
        for (int dx = -w; dx <= w; dx++) {
            for (int dz = -w; dz <= w; dz++) {
                level.setBlock(new BlockPos(x + dx, y - 1, z + dz), floor, 3);
            }
        }

        // Walls
        for (int dy = 0; dy < h; dy++) {
            for (int dx = -w; dx <= w; dx++) {
                level.setBlock(new BlockPos(x + dx, y + dy, z - w), wall, 3);
                level.setBlock(new BlockPos(x + dx, y + dy, z + w), wall, 3);
            }
            for (int dz = -w; dz <= w; dz++) {
                level.setBlock(new BlockPos(x - w, y + dy, z + dz), wall, 3);
                level.setBlock(new BlockPos(x + w, y + dy, z + dz), wall, 3);
            }
        }

        // Interior air
        for (int dx = -w + 1; dx <= w - 1; dx++) {
            for (int dz = -w + 1; dz <= w - 1; dz++) {
                for (int dy = 0; dy < h - 1; dy++) {
                    level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Door (front opening)
        level.setBlock(new BlockPos(x, y, z - w), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x, y + 1, z - w), Blocks.AIR.defaultBlockState(), 3);

        // Roof (flat)
        for (int dx = -w - 1; dx <= w + 1; dx++) {
            for (int dz = -w - 1; dz <= w + 1; dz++) {
                level.setBlock(new BlockPos(x + dx, y + h, z + dz), roof, 3);
            }
        }

        // Window (soul lantern inside)
        level.setBlock(new BlockPos(x + w, y + 1, z), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x - w, y + 1, z), Blocks.AIR.defaultBlockState(), 3);

        // Soul fire inside
        if (rand.nextBoolean()) {
            level.setBlock(new BlockPos(x, y, z), Blocks.SOUL_FIRE.defaultBlockState(), 3);
        } else {
            level.setBlock(new BlockPos(x, y, z), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        }
    }

    private static void buildAltar(ServerLevel level, int x, int y, int z) {
        // Altar base (obsidian + crying obsidian)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
        }
        // Raised center
        level.setBlock(new BlockPos(x, y + 1, z), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x + 1, y + 1, z), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x - 1, y + 1, z), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x, y + 1, z + 1), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x, y + 1, z - 1), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);

        // Wither skull on top
        level.setBlock(new BlockPos(x, y + 2, z), Blocks.WITHER_SKELETON_SKULL.defaultBlockState(), 3);

        // 4 Pillars with soul fire
        int[][] pillars = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] p : pillars) {
            for (int dy = 0; dy < 5; dy++) {
                level.setBlock(new BlockPos(x + p[0], y + dy, z + p[1]),
                        Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
            }
            level.setBlock(new BlockPos(x + p[0], y + 5, z + p[1]),
                    Blocks.SOUL_FIRE.defaultBlockState(), 3);
        }
    }
}
