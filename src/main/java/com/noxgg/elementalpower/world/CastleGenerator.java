package com.noxgg.elementalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates a medieval castle structure at a given position.
 * Castle includes: outer walls, 4 corner towers, gate, courtyard,
 * main keep/donjon, battlements, windows, and a throne room.
 */
public class CastleGenerator {

    private static final BlockState STONE_BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState MOSSY_BRICK = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED_BRICK = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    private static final BlockState COBBLE_WALL = Blocks.COBBLESTONE_WALL.defaultBlockState();
    private static final BlockState STONE_SLAB = Blocks.STONE_BRICK_SLAB.defaultBlockState();
    private static final BlockState GOLD_BLOCK = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState IRON_BARS = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState TORCH = Blocks.WALL_TORCH.defaultBlockState();
    private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    private static final BlockState OAK_PLANKS = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState OAK_STAIRS = Blocks.OAK_STAIRS.defaultBlockState();
    private static final BlockState RED_CARPET = Blocks.RED_CARPET.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState BANNER = Blocks.RED_BANNER.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState GLASS_PANE = Blocks.GLASS_PANE.defaultBlockState();
    private static final BlockState DARK_OAK_DOOR = Blocks.DARK_OAK_DOOR.defaultBlockState();
    private static final BlockState CHEST = Blocks.CHEST.defaultBlockState();

    /**
     * Generate a castle at the given center position.
     * The castle faces the direction the player is looking.
     */
    public static void generate(ServerLevel level, BlockPos center, int groundY) {
        // Castle dimensions (grand royal castle)
        int wallSize = 30; // half-width of outer walls
        int wallHeight = 16;
        int towerHeight = 28;
        int towerRadius = 6;
        int keepSize = 14; // half-width of central keep
        int keepHeight = 24;

        // Clear and flatten area
        flattenArea(level, center, wallSize + 5, groundY);

        // 1. Foundation
        buildFoundation(level, center, wallSize, groundY);

        // 2. Outer walls (4 sides)
        buildOuterWalls(level, center, wallSize, wallHeight, groundY);

        // 3. Corner towers (4)
        buildTower(level, center.offset(wallSize, 0, wallSize), towerHeight, towerRadius, groundY);
        buildTower(level, center.offset(-wallSize, 0, wallSize), towerHeight, towerRadius, groundY);
        buildTower(level, center.offset(wallSize, 0, -wallSize), towerHeight, towerRadius, groundY);
        buildTower(level, center.offset(-wallSize, 0, -wallSize), towerHeight, towerRadius, groundY);

        // 4. Gate (front wall, south side)
        buildGate(level, center.offset(0, 0, wallSize), wallHeight, groundY);

        // 5. Central keep / donjon
        buildKeep(level, center, keepSize, keepHeight, groundY);

        // 6. Courtyard decorations
        buildCourtyard(level, center, wallSize, groundY);

        // 7. Throne room inside keep
        buildThroneRoom(level, center, keepSize, groundY);
    }

    private static void flattenArea(ServerLevel level, BlockPos center, int radius, int groundY) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Set ground level
                level.setBlock(center.offset(x, 0, z).atY(groundY), STONE_BRICK, 2);
                // Clear above
                for (int y = 1; y <= 40; y++) {
                    level.setBlock(center.offset(x, 0, z).atY(groundY + y), AIR, 2);
                }
            }
        }
    }

    private static void buildFoundation(ServerLevel level, BlockPos center, int size, int groundY) {
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                // Floor of courtyard
                BlockState floor = (Math.abs(x) + Math.abs(z)) % 7 == 0 ? MOSSY_BRICK : STONE_BRICK;
                level.setBlock(center.offset(x, 0, z).atY(groundY), floor, 2);
                // Underground foundation
                level.setBlock(center.offset(x, 0, z).atY(groundY - 1), DEEPSLATE, 2);
            }
        }
    }

    private static void buildOuterWalls(ServerLevel level, BlockPos center, int size, int height, int groundY) {
        for (int y = 1; y <= height; y++) {
            for (int i = -size; i <= size; i++) {
                // Vary stone type for weathered look
                BlockState wall = y < 3 ? MOSSY_BRICK : (Math.abs(i * y) % 11 == 0 ? CRACKED_BRICK : STONE_BRICK);

                // North wall
                level.setBlock(center.offset(i, 0, -size).atY(groundY + y), wall, 2);
                // South wall
                level.setBlock(center.offset(i, 0, size).atY(groundY + y), wall, 2);
                // East wall
                level.setBlock(center.offset(size, 0, i).atY(groundY + y), wall, 2);
                // West wall
                level.setBlock(center.offset(-size, 0, i).atY(groundY + y), wall, 2);
            }
        }

        // Battlements (crenellations) on top
        for (int i = -size; i <= size; i++) {
            if (i % 2 == 0) {
                level.setBlock(center.offset(i, 0, -size).atY(groundY + height + 1), STONE_BRICK, 2);
                level.setBlock(center.offset(i, 0, size).atY(groundY + height + 1), STONE_BRICK, 2);
                level.setBlock(center.offset(size, 0, i).atY(groundY + height + 1), STONE_BRICK, 2);
                level.setBlock(center.offset(-size, 0, i).atY(groundY + height + 1), STONE_BRICK, 2);
            }
        }

        // Arrow slits (windows) every 4 blocks
        for (int i = -size + 2; i <= size - 2; i += 4) {
            for (int wy = 4; wy <= 5; wy++) {
                level.setBlock(center.offset(i, 0, -size).atY(groundY + wy), IRON_BARS, 2);
                level.setBlock(center.offset(i, 0, size).atY(groundY + wy), IRON_BARS, 2);
                level.setBlock(center.offset(size, 0, i).atY(groundY + wy), IRON_BARS, 2);
                level.setBlock(center.offset(-size, 0, i).atY(groundY + wy), IRON_BARS, 2);
            }
        }
    }

    private static void buildTower(ServerLevel level, BlockPos base, int height, int radius, int groundY) {
        for (int y = 1; y <= height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist <= radius) {
                        // Walls (hollow inside)
                        if (dist > radius - 1.2) {
                            BlockState tBlock = y < 3 ? MOSSY_BRICK : STONE_BRICK;
                            level.setBlock(base.offset(x, 0, z).atY(groundY + y), tBlock, 2);
                        } else if (y == 1) {
                            // Floor
                            level.setBlock(base.offset(x, 0, z).atY(groundY + y), OAK_PLANKS, 2);
                        } else {
                            // Interior air
                            level.setBlock(base.offset(x, 0, z).atY(groundY + y), AIR, 2);
                        }
                    }
                }
            }
        }

        // Pointed roof (cone)
        for (int y = 0; y <= radius + 2; y++) {
            double roofR = radius - y * 0.8;
            if (roofR < 0) roofR = 0;
            for (int x = -(int) Math.ceil(roofR); x <= (int) Math.ceil(roofR); x++) {
                for (int z = -(int) Math.ceil(roofR); z <= (int) Math.ceil(roofR); z++) {
                    if (Math.sqrt(x * x + z * z) <= roofR + 0.5) {
                        level.setBlock(base.offset(x, 0, z).atY(groundY + height + 1 + y),
                                Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Battlements on tower
        for (int i = 0; i < 12; i++) {
            double a = (Math.PI * 2.0 / 12) * i;
            if (i % 2 == 0) {
                int bx = (int) Math.round(Math.cos(a) * radius);
                int bz = (int) Math.round(Math.sin(a) * radius);
                level.setBlock(base.offset(bx, 0, bz).atY(groundY + height + 1), STONE_BRICK, 2);
            }
        }

        // Torch on top
        level.setBlock(base.atY(groundY + height + 1), Blocks.TORCH.defaultBlockState(), 2);
    }

    private static void buildGate(ServerLevel level, BlockPos gateCenter, int wallHeight, int groundY) {
        // Gate opening (5 wide, 8 tall)
        for (int x = -2; x <= 2; x++) {
            for (int y = 1; y <= 8; y++) {
                level.setBlock(gateCenter.offset(x, 0, 0).atY(groundY + y), AIR, 2);
            }
        }

        // Gate arch
        for (int x = -3; x <= 3; x++) {
            level.setBlock(gateCenter.offset(x, 0, 0).atY(groundY + 9), STONE_BRICK, 2);
            level.setBlock(gateCenter.offset(x, 0, 0).atY(groundY + 10), GOLD_BLOCK, 2);
        }

        // Iron bars portcullis
        for (int x = -2; x <= 2; x++) {
            level.setBlock(gateCenter.offset(x, 0, 0).atY(groundY + 7), IRON_BARS, 2);
            level.setBlock(gateCenter.offset(x, 0, 0).atY(groundY + 8), IRON_BARS, 2);
        }

        // Gold accents on gate pillars
        for (int y = 1; y <= 8; y++) {
            level.setBlock(gateCenter.offset(-3, 0, 0).atY(groundY + y), GOLD_BLOCK, 2);
            level.setBlock(gateCenter.offset(3, 0, 0).atY(groundY + y), GOLD_BLOCK, 2);
        }

        // Lanterns at gate
        level.setBlock(gateCenter.offset(-3, 0, 1).atY(groundY + 7), LANTERN, 2);
        level.setBlock(gateCenter.offset(3, 0, 1).atY(groundY + 7), LANTERN, 2);
        level.setBlock(gateCenter.offset(-3, 0, -1).atY(groundY + 7), LANTERN, 2);
        level.setBlock(gateCenter.offset(3, 0, -1).atY(groundY + 7), LANTERN, 2);
    }

    private static void buildKeep(ServerLevel level, BlockPos center, int size, int height, int groundY) {
        for (int y = 1; y <= height; y++) {
            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    boolean isWall = Math.abs(x) == size || Math.abs(z) == size;
                    if (isWall) {
                        BlockState kBlock = y < 2 ? DEEPSLATE : STONE_BRICK;
                        level.setBlock(center.offset(x, 0, z).atY(groundY + y), kBlock, 2);
                    } else {
                        // Interior
                        if (y == 1 || y == 8 || y == 15) {
                            // Floors (ground, 2nd, 3rd level)
                            level.setBlock(center.offset(x, 0, z).atY(groundY + y), OAK_PLANKS, 2);
                        } else {
                            level.setBlock(center.offset(x, 0, z).atY(groundY + y), AIR, 2);
                        }
                    }
                }
            }
        }

        // Keep roof (flat with battlements)
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                level.setBlock(center.offset(x, 0, z).atY(groundY + height + 1), STONE_BRICK, 2);
            }
        }
        // Battlements
        for (int i = -size; i <= size; i++) {
            if (i % 2 == 0) {
                level.setBlock(center.offset(i, 0, -size).atY(groundY + height + 2), STONE_BRICK, 2);
                level.setBlock(center.offset(i, 0, size).atY(groundY + height + 2), STONE_BRICK, 2);
                level.setBlock(center.offset(size, 0, i).atY(groundY + height + 2), STONE_BRICK, 2);
                level.setBlock(center.offset(-size, 0, i).atY(groundY + height + 2), STONE_BRICK, 2);
            }
        }

        // Keep windows
        for (int wy = 3; wy <= 4; wy++) {
            for (int i = -size + 2; i <= size - 2; i += 3) {
                level.setBlock(center.offset(i, 0, -size).atY(groundY + wy), GLASS_PANE, 2);
                level.setBlock(center.offset(i, 0, size).atY(groundY + wy), GLASS_PANE, 2);
                level.setBlock(center.offset(size, 0, i).atY(groundY + wy), GLASS_PANE, 2);
                level.setBlock(center.offset(-size, 0, i).atY(groundY + wy), GLASS_PANE, 2);
            }
        }

        // Keep entrance (south side, bigger)
        for (int y = 1; y <= 6; y++) {
            for (int x = -2; x <= 2; x++) {
                level.setBlock(center.offset(x, 0, size).atY(groundY + y), AIR, 2);
            }
        }

        // Royal banners above keep entrance
        level.setBlock(center.offset(0, 0, size).atY(groundY + 7), BANNER, 2);
        level.setBlock(center.offset(-3, 0, size).atY(groundY + 7), BANNER, 2);
        level.setBlock(center.offset(3, 0, size).atY(groundY + 7), BANNER, 2);

        // Gold trim on keep
        for (int i = -size; i <= size; i++) {
            level.setBlock(center.offset(i, 0, -size).atY(groundY + height), GOLD_BLOCK, 2);
            level.setBlock(center.offset(i, 0, size).atY(groundY + height), GOLD_BLOCK, 2);
            level.setBlock(center.offset(size, 0, i).atY(groundY + height), GOLD_BLOCK, 2);
            level.setBlock(center.offset(-size, 0, i).atY(groundY + height), GOLD_BLOCK, 2);
        }
    }

    private static void buildCourtyard(ServerLevel level, BlockPos center, int wallSize, int groundY) {
        // Red carpet path from gate to keep
        for (int z = 14; z <= wallSize; z++) {
            level.setBlock(center.offset(0, 0, z).atY(groundY + 1), RED_CARPET, 2);
            level.setBlock(center.offset(-1, 0, z).atY(groundY + 1), RED_CARPET, 2);
            level.setBlock(center.offset(1, 0, z).atY(groundY + 1), RED_CARPET, 2);
        }

        // Lantern posts along the path
        for (int z = 16; z <= wallSize - 2; z += 4) {
            level.setBlock(center.offset(-5, 0, z).atY(groundY + 1), Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
            level.setBlock(center.offset(-5, 0, z).atY(groundY + 2), Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
            level.setBlock(center.offset(-5, 0, z).atY(groundY + 3), LANTERN, 2);

            level.setBlock(center.offset(5, 0, z).atY(groundY + 1), Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
            level.setBlock(center.offset(5, 0, z).atY(groundY + 2), Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
            level.setBlock(center.offset(5, 0, z).atY(groundY + 3), LANTERN, 2);
        }

        // Water fountain in courtyard (left side)
        int fz = wallSize - 10;
        level.setBlock(center.offset(16, 0, fz).atY(groundY + 1), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        level.setBlock(center.offset(16, 0, fz).atY(groundY + 2), Blocks.WATER.defaultBlockState(), 2);
        for (int fx = 14; fx <= 18; fx++) {
            for (int ffz = fz - 2; ffz <= fz + 2; ffz++) {
                level.setBlock(center.offset(fx, 0, ffz).atY(groundY + 1), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2);
            }
        }

        // Water fountain in courtyard (right side)
        level.setBlock(center.offset(-16, 0, fz).atY(groundY + 1), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        level.setBlock(center.offset(-16, 0, fz).atY(groundY + 2), Blocks.WATER.defaultBlockState(), 2);
        for (int fx = -18; fx <= -14; fx++) {
            for (int ffz = fz - 2; ffz <= fz + 2; ffz++) {
                level.setBlock(center.offset(fx, 0, ffz).atY(groundY + 1), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2);
            }
        }
    }

    private static void buildThroneRoom(ServerLevel level, BlockPos center, int keepSize, int groundY) {
        // Elevated throne platform (3 steps)
        for (int step = 0; step < 3; step++) {
            int platSize = 4 - step;
            for (int x = -platSize; x <= platSize; x++) {
                for (int z = -keepSize + 2; z <= -keepSize + 2 + platSize; z++) {
                    level.setBlock(center.offset(x, 0, z).atY(groundY + 2 + step), Blocks.QUARTZ_BLOCK.defaultBlockState(), 2);
                }
            }
        }

        // Throne (stairs facing south)
        level.setBlock(center.offset(0, 0, -keepSize + 3).atY(groundY + 5), Blocks.QUARTZ_STAIRS.defaultBlockState(), 2);
        level.setBlock(center.offset(0, 0, -keepSize + 3).atY(groundY + 6), Blocks.QUARTZ_SLAB.defaultBlockState(), 2);

        // Gold armrests
        level.setBlock(center.offset(-1, 0, -keepSize + 3).atY(groundY + 5), GOLD_BLOCK, 2);
        level.setBlock(center.offset(1, 0, -keepSize + 3).atY(groundY + 5), GOLD_BLOCK, 2);

        // Throne back (taller)
        for (int y = 5; y <= 8; y++) {
            level.setBlock(center.offset(0, 0, -keepSize + 2).atY(groundY + y), Blocks.QUARTZ_BLOCK.defaultBlockState(), 2);
        }
        level.setBlock(center.offset(0, 0, -keepSize + 2).atY(groundY + 9), GOLD_BLOCK, 2);

        // Red carpet to throne (wide)
        for (int z = -keepSize + 5; z <= keepSize; z++) {
            level.setBlock(center.offset(0, 0, z).atY(groundY + 2), RED_CARPET, 2);
            level.setBlock(center.offset(-1, 0, z).atY(groundY + 2), RED_CARPET, 2);
            level.setBlock(center.offset(1, 0, z).atY(groundY + 2), RED_CARPET, 2);
        }

        // Chests on sides (multiple)
        for (int i = 0; i < 3; i++) {
            level.setBlock(center.offset(-keepSize + 2 + i * 2, 0, -keepSize + 2).atY(groundY + 2), CHEST, 2);
            level.setBlock(center.offset(keepSize - 2 - i * 2, 0, -keepSize + 2).atY(groundY + 2), CHEST, 2);
        }

        // Lanterns in throne room (more of them)
        for (int z = -keepSize + 3; z <= keepSize - 3; z += 5) {
            level.setBlock(center.offset(-6, 0, z).atY(groundY + 4), LANTERN, 2);
            level.setBlock(center.offset(6, 0, z).atY(groundY + 4), LANTERN, 2);
        }

        // Gold pillar columns
        for (int z = -keepSize + 4; z <= keepSize - 2; z += 6) {
            for (int y = 2; y <= 5; y++) {
                level.setBlock(center.offset(-5, 0, z).atY(groundY + y), GOLD_BLOCK, 2);
                level.setBlock(center.offset(5, 0, z).atY(groundY + y), GOLD_BLOCK, 2);
            }
        }
    }
}
