package com.noxgg.elementalpower.world;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shadow Milk's Domain - spawns in villages where Alastor has killed.
 * Contains Shadow Milk on a throne and Pure Vanilla in a torture room.
 * Killing Pure Vanilla grants Fire class the Puppeteer power.
 */
public class ShadowMilkDomainManager {

    // Track kill counts per village area (blockpos rounded to chunk-ish coords)
    private static final Map<String, Integer> villageKillCounts = new ConcurrentHashMap<>();

    // Villages already transformed into domains
    private static final Set<String> transformedVillages = ConcurrentHashMap.newKeySet();

    // Kills needed to trigger domain creation
    private static final int KILLS_THRESHOLD = 5;

    // Domain positions for ambient effects
    private static final List<DomainData> activeDomains = Collections.synchronizedList(new ArrayList<>());

    public static class DomainData {
        public final BlockPos center;
        public final ServerLevel level;
        public final BlockPos thronePos;
        public final BlockPos torturePos;

        public DomainData(BlockPos center, ServerLevel level, BlockPos thronePos, BlockPos torturePos) {
            this.center = center;
            this.level = level;
            this.thronePos = thronePos;
            this.torturePos = torturePos;
        }
    }

    /**
     * Called when Alastor kills a mob. Checks if near a village and tracks kills.
     */
    public static void onAlastorKill(ServerPlayer player, ServerLevel level, BlockPos killPos) {
        // Check if near a village (look for villagers or village POIs nearby)
        boolean nearVillage = false;
        BlockPos villageCenter = killPos;

        // Check for villagers in a 50 block radius
        var villagers = level.getEntitiesOfClass(Villager.class,
                new AABB(killPos).inflate(50));
        if (!villagers.isEmpty()) {
            nearVillage = true;
            // Use average villager position as village center
            double avgX = 0, avgZ = 0;
            for (Villager v : villagers) {
                avgX += v.getX();
                avgZ += v.getZ();
            }
            villageCenter = new BlockPos((int) (avgX / villagers.size()), killPos.getY(),
                    (int) (avgZ / villagers.size()));
        }

        // Also check village POIs
        if (!nearVillage) {
            var poiManager = level.getPoiManager();
            var pois = poiManager.findAll(
                    holder -> true,
                    pos -> true,
                    killPos, 50, PoiManager.Occupancy.ANY);
            if (pois.count() > 3) {
                nearVillage = true;
            }
        }

        if (!nearVillage) return;

        // Create a key based on village area (rounded to 64-block grid)
        String villageKey = level.dimension().location() + "_" +
                (villageCenter.getX() >> 6) + "_" + (villageCenter.getZ() >> 6);

        if (transformedVillages.contains(villageKey)) return;

        int kills = villageKillCounts.merge(villageKey, 1, Integer::sum);

        // Notify player of dark energy building
        if (kills >= 2 && kills < KILLS_THRESHOLD) {
            player.sendSystemMessage(Component.literal("")
                    .append(Component.literal(">> ").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal("L'energie sombre s'accumule dans ce village... (" + kills + "/" + KILLS_THRESHOLD + ")")
                            .withStyle(ChatFormatting.LIGHT_PURPLE)));
        }

        if (kills >= KILLS_THRESHOLD) {
            transformedVillages.add(villageKey);
            // Transform village into Shadow Milk's domain!
            createDomain(level, villageCenter, player);
        }
    }

    /**
     * Create Shadow Milk's domain at the given village center.
     */
    private static void createDomain(ServerLevel level, BlockPos villageCenter, ServerPlayer player) {
        // Find ground level
        int groundY = villageCenter.getY();
        for (int y = villageCenter.getY() + 10; y > villageCenter.getY() - 10; y--) {
            if (!level.getBlockState(villageCenter.atY(y)).isAir()
                    && level.getBlockState(villageCenter.atY(y + 1)).isAir()) {
                groundY = y;
                break;
            }
        }

        // Corrupt the area - darken blocks in radius
        int domainRadius = 25;
        corruptArea(level, villageCenter, groundY, domainRadius);

        // Build throne room structure
        BlockPos throneRoomPos = villageCenter.offset(0, 0, -10).atY(groundY);
        buildThroneRoom(level, throneRoomPos, groundY);

        // Build torture room structure
        BlockPos tortureRoomPos = villageCenter.offset(0, 0, 10).atY(groundY);
        buildTortureRoom(level, tortureRoomPos, groundY);

        // Spawn Shadow Milk on the throne
        spawnShadowMilk(level, throneRoomPos, groundY);

        // Spawn Pure Vanilla in the torture room
        spawnPureVanilla(level, tortureRoomPos, groundY);

        // Register domain
        activeDomains.add(new DomainData(villageCenter.atY(groundY), level, throneRoomPos, tortureRoomPos));

        // Dramatic effects
        DustParticleOptions purpleDust = new DustParticleOptions(new Vector3f(0.4f, 0.0f, 0.6f), 3.0f);
        for (int i = 0; i < 100; i++) {
            double ox = (level.random.nextDouble() - 0.5) * domainRadius * 2;
            double oz = (level.random.nextDouble() - 0.5) * domainRadius * 2;
            level.sendParticles(purpleDust,
                    villageCenter.getX() + ox, groundY + level.random.nextDouble() * 10,
                    villageCenter.getZ() + oz, 1, 0, 0.1, 0, 0);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                villageCenter.getX(), groundY + 5, villageCenter.getZ(), 3, 2, 2, 2, 0);

        level.playSound(null, villageCenter.atY(groundY), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0f, 0.5f);
        level.playSound(null, villageCenter.atY(groundY), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5f, 0.3f);

        // Announce
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal(">> ").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal("LE DOMAINE DE SHADOW MILK S'EST EVEILLE!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("   Shadow Milk").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                .append(Component.literal(" trone dans la salle du trone...").withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("   Pure Vanilla").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal(" est prisonnier dans la salle de torture!").withStyle(ChatFormatting.GRAY)));
        player.sendSystemMessage(Component.literal(""));
    }

    /**
     * Corrupt the village area with dark blocks.
     */
    private static void corruptArea(ServerLevel level, BlockPos center, int groundY, int radius) {
        Random rand = new Random();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;

                BlockPos groundPos = center.offset(x, 0, z).atY(groundY);

                // Replace ground with dark blocks
                BlockState current = level.getBlockState(groundPos);
                if (!current.isAir()) {
                    if (rand.nextFloat() < 0.6f) {
                        level.setBlock(groundPos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                    } else if (rand.nextFloat() < 0.3f) {
                        level.setBlock(groundPos, Blocks.BLACKSTONE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(groundPos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
                    }
                }

                // Dark carpet on top
                if (rand.nextFloat() < 0.2f && level.getBlockState(groundPos.above()).isAir()) {
                    level.setBlock(groundPos.above(), Blocks.BLACK_CARPET.defaultBlockState(), 2);
                }

                // Soul fire torches
                if (rand.nextFloat() < 0.03f && level.getBlockState(groundPos.above()).isAir()) {
                    level.setBlock(groundPos.above(), Blocks.SOUL_TORCH.defaultBlockState(), 2);
                }
            }
        }
    }

    /**
     * Build Shadow Milk's throne room.
     */
    private static void buildThroneRoom(ServerLevel level, BlockPos pos, int groundY) {
        int sizeX = 8, sizeZ = 10, height = 8;

        // Floor
        for (int x = -sizeX; x <= sizeX; x++) {
            for (int z = -sizeZ; z <= sizeZ; z++) {
                level.setBlock(pos.offset(x, 0, z).atY(groundY), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
            }
        }

        // Walls
        for (int x = -sizeX; x <= sizeX; x++) {
            for (int y = 1; y <= height; y++) {
                level.setBlock(pos.offset(x, 0, -sizeZ).atY(groundY + y), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(), 2);
                level.setBlock(pos.offset(x, 0, sizeZ).atY(groundY + y), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(), 2);
            }
        }
        for (int z = -sizeZ; z <= sizeZ; z++) {
            for (int y = 1; y <= height; y++) {
                level.setBlock(pos.offset(-sizeX, 0, z).atY(groundY + y), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                level.setBlock(pos.offset(sizeX, 0, z).atY(groundY + y), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }

        // Ceiling
        for (int x = -sizeX; x <= sizeX; x++) {
            for (int z = -sizeZ; z <= sizeZ; z++) {
                level.setBlock(pos.offset(x, 0, z).atY(groundY + height + 1), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }

        // Clear interior
        for (int x = -sizeX + 1; x < sizeX; x++) {
            for (int z = -sizeZ + 1; z < sizeZ; z++) {
                for (int y = 1; y <= height; y++) {
                    level.setBlock(pos.offset(x, 0, z).atY(groundY + y), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Entrance
        for (int y = 1; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                level.setBlock(pos.offset(x, 0, sizeZ).atY(groundY + y), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        // Throne platform (3 steps, blackstone)
        for (int step = 0; step < 3; step++) {
            int platSize = 3 - step;
            for (int x = -platSize; x <= platSize; x++) {
                for (int z = -sizeZ + 1; z <= -sizeZ + 1 + platSize; z++) {
                    level.setBlock(pos.offset(x, 0, z).atY(groundY + 1 + step), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
                }
            }
        }

        // Throne chair
        level.setBlock(pos.offset(0, 0, -sizeZ + 2).atY(groundY + 4), Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState(), 2);
        level.setBlock(pos.offset(-1, 0, -sizeZ + 2).atY(groundY + 4), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
        level.setBlock(pos.offset(1, 0, -sizeZ + 2).atY(groundY + 4), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);

        // Throne back
        for (int y = 4; y <= 7; y++) {
            level.setBlock(pos.offset(0, 0, -sizeZ + 1).atY(groundY + y), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        }
        level.setBlock(pos.offset(0, 0, -sizeZ + 1).atY(groundY + 8), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);

        // Purple carpet to throne
        for (int z = -sizeZ + 3; z <= sizeZ - 1; z++) {
            level.setBlock(pos.offset(0, 0, z).atY(groundY + 1), Blocks.PURPLE_CARPET.defaultBlockState(), 2);
        }

        // Soul lanterns
        for (int z = -sizeZ + 2; z <= sizeZ - 2; z += 4) {
            level.setBlock(pos.offset(-sizeX + 1, 0, z).atY(groundY + 4), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
            level.setBlock(pos.offset(sizeX - 1, 0, z).atY(groundY + 4), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        }

        // Amethyst pillars
        for (int z = -sizeZ + 3; z <= sizeZ - 3; z += 5) {
            for (int y = 1; y <= 5; y++) {
                level.setBlock(pos.offset(-sizeX + 2, 0, z).atY(groundY + y), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
                level.setBlock(pos.offset(sizeX - 2, 0, z).atY(groundY + y), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
            }
        }
    }

    /**
     * Build the torture room for Pure Vanilla.
     */
    private static void buildTortureRoom(ServerLevel level, BlockPos pos, int groundY) {
        int sizeX = 6, sizeZ = 6, height = 5;

        // Floor
        for (int x = -sizeX; x <= sizeX; x++) {
            for (int z = -sizeZ; z <= sizeZ; z++) {
                level.setBlock(pos.offset(x, 0, z).atY(groundY), Blocks.BLACKSTONE.defaultBlockState(), 2);
            }
        }

        // Walls
        for (int x = -sizeX; x <= sizeX; x++) {
            for (int y = 1; y <= height; y++) {
                level.setBlock(pos.offset(x, 0, -sizeZ).atY(groundY + y), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                level.setBlock(pos.offset(x, 0, sizeZ).atY(groundY + y), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }
        for (int z = -sizeZ; z <= sizeZ; z++) {
            for (int y = 1; y <= height; y++) {
                level.setBlock(pos.offset(-sizeX, 0, z).atY(groundY + y), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                level.setBlock(pos.offset(sizeX, 0, z).atY(groundY + y), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }

        // Ceiling
        for (int x = -sizeX; x <= sizeX; x++) {
            for (int z = -sizeZ; z <= sizeZ; z++) {
                level.setBlock(pos.offset(x, 0, z).atY(groundY + height + 1), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }

        // Clear interior
        for (int x = -sizeX + 1; x < sizeX; x++) {
            for (int z = -sizeZ + 1; z < sizeZ; z++) {
                for (int y = 1; y <= height; y++) {
                    level.setBlock(pos.offset(x, 0, z).atY(groundY + y), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Entrance
        for (int y = 1; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                level.setBlock(pos.offset(x, 0, -sizeZ).atY(groundY + y), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        // Chains hanging from ceiling
        for (int x = -3; x <= 3; x += 3) {
            for (int z = -3; z <= 3; z += 3) {
                for (int y = height; y >= height - 2; y--) {
                    level.setBlock(pos.offset(x, 0, z).atY(groundY + y), Blocks.CHAIN.defaultBlockState(), 2);
                }
            }
        }

        // Iron bars (cage around center)
        for (int x = -2; x <= 2; x++) {
            level.setBlock(pos.offset(x, 0, -2).atY(groundY + 1), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(pos.offset(x, 0, -2).atY(groundY + 2), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(pos.offset(x, 0, 2).atY(groundY + 1), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(pos.offset(x, 0, 2).atY(groundY + 2), Blocks.IRON_BARS.defaultBlockState(), 2);
        }
        for (int z = -2; z <= 2; z++) {
            level.setBlock(pos.offset(-2, 0, z).atY(groundY + 1), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(pos.offset(-2, 0, z).atY(groundY + 2), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(pos.offset(2, 0, z).atY(groundY + 1), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(pos.offset(2, 0, z).atY(groundY + 2), Blocks.IRON_BARS.defaultBlockState(), 2);
        }

        // Redstone torches for sinister lighting
        level.setBlock(pos.offset(-sizeX + 1, 0, -sizeZ + 1).atY(groundY + 3), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
        level.setBlock(pos.offset(sizeX - 1, 0, -sizeZ + 1).atY(groundY + 3), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
        level.setBlock(pos.offset(-sizeX + 1, 0, sizeZ - 1).atY(groundY + 3), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
        level.setBlock(pos.offset(sizeX - 1, 0, sizeZ - 1).atY(groundY + 3), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);

        // Skulls on walls
        level.setBlock(pos.offset(0, 0, sizeZ - 1).atY(groundY + 3), Blocks.SKELETON_SKULL.defaultBlockState(), 2);
        level.setBlock(pos.offset(0, 0, -sizeZ + 1).atY(groundY + 3), Blocks.WITHER_SKELETON_SKULL.defaultBlockState(), 2);
    }

    /**
     * Spawn Shadow Milk as a powerful Evoker on the throne.
     */
    private static void spawnShadowMilk(ServerLevel level, BlockPos throneRoomPos, int groundY) {
        Vindicator shadowMilk = EntityType.VINDICATOR.create(level);
        if (shadowMilk != null) {
            int sizeZ = 10;
            shadowMilk.moveTo(throneRoomPos.getX() + 0.5, groundY + 4.5,
                    throneRoomPos.getZ() - sizeZ + 2 + 0.5, 180, 0);

            shadowMilk.setCustomName(Component.literal("Shadow Milk")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            shadowMilk.setCustomNameVisible(true);

            // Equipment
            shadowMilk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            shadowMilk.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            shadowMilk.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
            shadowMilk.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));

            // Powerful boss
            shadowMilk.setInvulnerable(true); // Shadow Milk can't be killed
            shadowMilk.setNoAi(true);
            shadowMilk.setPersistenceRequired();
            shadowMilk.addTag("shadow_milk_boss");

            // Boss effects
            shadowMilk.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
            shadowMilk.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

            level.addFreshEntity(shadowMilk);
        }
    }

    /**
     * Spawn Pure Vanilla as a trapped Villager in the torture room.
     */
    private static void spawnPureVanilla(ServerLevel level, BlockPos tortureRoomPos, int groundY) {
        Villager pureVanilla = EntityType.VILLAGER.create(level);
        if (pureVanilla != null) {
            pureVanilla.moveTo(tortureRoomPos.getX() + 0.5, groundY + 1.0,
                    tortureRoomPos.getZ() + 0.5, 0, 0);

            pureVanilla.setCustomName(Component.literal("Pure Vanilla")
                    .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            pureVanilla.setCustomNameVisible(true);

            // White robes appearance
            pureVanilla.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));

            pureVanilla.setNoAi(true);
            pureVanilla.setPersistenceRequired();
            pureVanilla.addTag("pure_vanilla_prisoner");

            // Weakness effects (tortured)
            pureVanilla.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Integer.MAX_VALUE, 2, false, true));
            pureVanilla.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));

            level.addFreshEntity(pureVanilla);
        }
    }

    /**
     * Ambient effects in active domains.
     */
    public static void tick() {
        for (DomainData domain : activeDomains) {
            if (domain.level.getGameTime() % 40 != 0) continue;

            // Purple particles in the domain
            DustParticleOptions purpleDust = new DustParticleOptions(new Vector3f(0.3f, 0.0f, 0.5f), 1.5f);
            for (int i = 0; i < 5; i++) {
                double ox = (domain.level.random.nextDouble() - 0.5) * 30;
                double oz = (domain.level.random.nextDouble() - 0.5) * 30;
                domain.level.sendParticles(purpleDust,
                        domain.center.getX() + ox, domain.center.getY() + 1 + domain.level.random.nextDouble() * 5,
                        domain.center.getZ() + oz, 1, 0, 0.05, 0, 0);
            }

            // Soul particles near throne
            domain.level.sendParticles(ParticleTypes.SCULK_SOUL,
                    domain.thronePos.getX() + 0.5, domain.thronePos.getY() + 3,
                    domain.thronePos.getZ() + 0.5, 2, 1, 1, 1, 0.01);
        }
    }
}
