package com.noxgg.elementalpower.network;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
import com.noxgg.elementalpower.world.DarkPrisonManager;
import com.noxgg.elementalpower.world.PoisonDragonManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.function.Supplier;

public class DarkPrisonC2SPacket {
    public DarkPrisonC2SPacket() {}
    public DarkPrisonC2SPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(data -> {
                ElementType element = data.getElement();
                if (element != ElementType.DARKNESS && element != ElementType.POISON
                        && element != ElementType.ROYAL && element != ElementType.SPACE
                        && element != ElementType.DEMON && element != ElementType.NATURE
                        && element != ElementType.AIR && element != ElementType.TIME
                        && element != ElementType.UNDERTALE && element != ElementType.EARTH) {
                    player.sendSystemMessage(Component.literal("Ce sort n'est pas disponible pour ta classe!")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                // Check level 10 minimum for G spells (bypass for creative and server ops)
                if (data.getLevel() < 10 && !player.isCreative() && !player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.literal(">> Sort G verrouille! Niveau 10 requis (actuel: " + data.getLevel() + ")")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                ServerLevel level = player.serverLevel();

                // EARTH: Twin Gaster Blasters
                if (element == ElementType.EARTH) {
                    Vec3 earthEye = player.getEyePosition();
                    Vec3 earthLook = player.getLookAngle();
                    LivingEntity earthTarget = null;
                    double earthClosest = 40.0;

                    for (Entity entity : level.getEntities(player,
                            player.getBoundingBox().inflate(40),
                            e -> e instanceof LivingEntity && e != player)) {
                        LivingEntity living = (LivingEntity) entity;
                        Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(earthEye);
                        double dist = toEntity.length();
                        if (dist > earthClosest) continue;
                        Vec3 toEntityNorm = toEntity.normalize();
                        if (earthLook.dot(toEntityNorm) > 0.9) {
                            earthClosest = dist;
                            earthTarget = living;
                        }
                    }

                    if (earthTarget == null) {
                        player.sendSystemMessage(Component.literal("Aucune cible en vue!")
                                .withStyle(ChatFormatting.GRAY));
                        return;
                    }

                    // Launch the Earth Gaster Blasters
                    com.noxgg.elementalpower.world.EarthBlasterManager.addBlast(
                            new com.noxgg.elementalpower.world.EarthBlasterManager.EarthBlast(
                                    level, player, earthTarget));

                    // Initial casting particles
                    Vec3 tp = earthTarget.position();
                    var brownDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.6f, 0.4f, 0.1f), 2.0f);
                    level.sendParticles(brownDust,
                            player.getX(), player.getY() + 1, player.getZ(),
                            20, 0.5, 0.5, 0.5, 0.05);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 2, player.getZ(),
                            10, 0.3, 0.3, 0.3, 0.1);

                    level.playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f);
                    level.playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);

                    player.sendSystemMessage(Component.literal(">> Gaster Blasters de Terre invoques! ")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                            .append(Component.literal("Deux canons geants convergent sur la cible!")
                                    .withStyle(ChatFormatting.YELLOW)));
                    return;
                }

                // UNDERTALE FRISK: SPARE mechanic
                if (element == ElementType.UNDERTALE && data.isFrisk()) {
                    Vec3 spareEye = player.getEyePosition();
                    Vec3 spareLook = player.getLookAngle();
                    LivingEntity spareTarget = null;
                    double spareClosest = 20.0;

                    for (Entity entity : level.getEntities(player,
                            player.getBoundingBox().inflate(20),
                            e -> e instanceof LivingEntity && e != player && !(e instanceof net.minecraft.world.entity.player.Player))) {
                        LivingEntity living = (LivingEntity) entity;
                        Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(spareEye);
                        double dist = toEntity.length();
                        if (dist > spareClosest) continue;
                        Vec3 toEntityNorm = toEntity.normalize();
                        if (spareLook.dot(toEntityNorm) > 0.9) {
                            spareClosest = dist;
                            spareTarget = living;
                        }
                    }

                    if (spareTarget == null) {
                        player.sendSystemMessage(Component.literal("")
                                .append(Component.literal("* ").withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal("Aucun ennemi en vue a SPARE.").withStyle(ChatFormatting.GRAY)));
                        return;
                    }

                    // === SPARE THE MOB ===
                    LivingEntity target = spareTarget;

                    // Make it peaceful: remove AI hostility, add hearts
                    if (target instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setTarget(null);
                        mob.setNoAi(true); // Stop AI temporarily

                        // After 2 seconds, re-enable AI but peaceful
                        // The mob will no longer attack because its target is cleared
                        mob.setPersistenceRequired();

                        // Schedule re-enable AI after a short delay via the mob's persistent data
                        mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.REGENERATION, 600, 2, false, true));
                        mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.GLOWING, 200, 0, false, false));

                        // Give it a friendly name
                        String mobName = mob.getType().getDescription().getString();
                        mob.setCustomName(Component.literal("\u00A7a\u00A7l[Spare] \u00A72" + mobName));
                        mob.setCustomNameVisible(true);
                    }

                    // Yellow heart particles from player to target
                    Vec3 targetPos = target.position();
                    var yellowDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(1.0f, 1.0f, 0.0f), 2.0f);
                    for (int i = 0; i < 15; i++) {
                        double t = i / 15.0;
                        double px = player.getX() + (targetPos.x - player.getX()) * t;
                        double py = player.getEyeY() + (targetPos.y + 1 - player.getEyeY()) * t;
                        double pz = player.getZ() + (targetPos.z - player.getZ()) * t;
                        level.sendParticles(yellowDust, px, py, pz, 2, 0.05, 0.05, 0.05, 0.01);
                    }

                    // Heart particles above the spared mob
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                            targetPos.x, targetPos.y + target.getBbHeight() + 0.5, targetPos.z,
                            10, 0.3, 0.3, 0.3, 0.1);
                    level.sendParticles(yellowDust,
                            targetPos.x, targetPos.y + 1, targetPos.z,
                            20, 0.5, 0.5, 0.5, 0.05);

                    // Undertale spare sound
                    level.playSound(null, target.blockPosition(),
                            net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
                    level.playSound(null, target.blockPosition(),
                            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 1.2f);

                    // Undertale-style text
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("* ").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("Tu as SPARE ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                            .append(Component.literal(target.getType().getDescription().getString()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("* ").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("Il est maintenant amical avec toi.").withStyle(ChatFormatting.GREEN)));

                    // Give XP for spare
                    data.addXp(25);
                    com.noxgg.elementalpower.event.ElementEvents.syncToClient(player, data);
                    return;
                }

                // UNDERTALE CHARA: handled by regular attack (no special G)
                if (element == ElementType.UNDERTALE && data.isChara()) {
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("* ").withStyle(ChatFormatting.DARK_RED))
                            .append(Component.literal("Les vrais tueurs n'ont pas besoin de sorts. Frappe.").withStyle(ChatFormatting.RED)));
                    return;
                }

                // SPACE: Black hole doesn't need a target
                if (element == ElementType.SPACE) {
                    Vec3 look = player.getLookAngle();
                    Vec3 holePos = player.position().add(look.scale(10));

                    com.noxgg.elementalpower.world.BlackHoleManager.addBlackHole(
                            new com.noxgg.elementalpower.world.BlackHoleManager.BlackHole(
                                    holePos.x, holePos.y + 2, holePos.z, 20.0,
                                    level, player, 150)); // 7.5 seconds

                    // Creation burst
                    var voidDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.2f, 0.0f, 0.3f), 3.0f);
                    level.sendParticles(voidDust, holePos.x, holePos.y + 2, holePos.z, 60, 2, 2, 2, 0.1);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, holePos.x, holePos.y + 2, holePos.z, 40, 1, 1, 1, 0.2);
                    level.sendParticles(ParticleTypes.FLASH, holePos.x, holePos.y + 2, holePos.z, 2, 0, 0, 0, 0);

                    level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0f, 0.3f);
                    level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.3f);

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Trou Noir invoque! ")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                            .append(net.minecraft.network.chat.Component.literal("Il aspire tout pendant 7.5 secondes!")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE)));
                    return;
                }

                // AIR: Spirit Form
                if (element == ElementType.AIR) {
                    if (com.noxgg.elementalpower.world.SpiritManager.isSpirit(player.getUUID())) {
                        // Already spirit -> exit
                        com.noxgg.elementalpower.world.SpiritManager.exitSpiritForm(player);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Forme d'esprit desactivee.")
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        // Enter spirit form
                        com.noxgg.elementalpower.world.SpiritManager.enterSpiritForm(player);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Forme d'Esprit activee! ")
                                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                                .append(net.minecraft.network.chat.Component.literal("Clic droit sur un mob pour l'etrangler. G pour annuler.")
                                        .withStyle(ChatFormatting.GRAY)));
                    }
                    return;
                }

                // NATURE: Carnivorous Flower
                if (element == ElementType.NATURE) {
                    Vec3 look = player.getLookAngle();
                    Vec3 flowerPos = player.position().add(new Vec3(look.x, 0, look.z).normalize().scale(5));

                    // Place the flower block
                    BlockPos fPos = BlockPos.containing(flowerPos.x, flowerPos.y, flowerPos.z);
                    // Find ground level
                    for (int dy = 2; dy > -3; dy--) {
                        BlockPos check = fPos.above(dy);
                        if (level.getBlockState(check).isAir() && !level.getBlockState(check.below()).isAir()) {
                            fPos = check;
                            break;
                        }
                    }
                    level.setBlock(fPos, net.minecraft.world.level.block.Blocks.WITHER_ROSE.defaultBlockState(), 3);

                    com.noxgg.elementalpower.world.CarnivorousFlowerManager.addFlower(
                            new com.noxgg.elementalpower.world.CarnivorousFlowerManager.CarnivorousFlower(
                                    fPos.getX() + 0.5, fPos.getY(), fPos.getZ() + 0.5,
                                    30.0, level, player, true));

                    // Spawn particles
                    var mossGreen = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.2f, 0.6f, 0.1f), 2.0f);
                    level.sendParticles(mossGreen,
                            fPos.getX() + 0.5, fPos.getY() + 1, fPos.getZ() + 0.5,
                            40, 1, 1, 1, 0.05);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            fPos.getX() + 0.5, fPos.getY() + 1, fPos.getZ() + 0.5,
                            25, 1.5, 0.5, 1.5, 0.05);
                    level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                            fPos.getX() + 0.5, fPos.getY() + 2, fPos.getZ() + 0.5,
                            15, 2, 1, 2, 0.02);

                    level.playSound(null, fPos, SoundEvents.FLOWERING_AZALEA_PLACE, SoundSource.PLAYERS, 2.0f, 0.5f);
                    level.playSound(null, fPos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.PLAYERS, 1.5f, 0.8f);

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Fleur Carnivore invoquee! ")
                            .withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD)
                            .append(net.minecraft.network.chat.Component.literal("Elle paralyse et digere les proies. Detruisez la fleur pour tout arreter.")
                                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN)));
                    return;
                }

                // DEMON: Soul Tsunami
                if (element == ElementType.DEMON) {
                    com.noxgg.elementalpower.world.SoulTsunamiManager.addTsunami(
                            new com.noxgg.elementalpower.world.SoulTsunamiManager.SoulTsunami(level, player));

                    // Launch burst
                    var soulRed = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.6f, 0.05f, 0.0f), 2.5f);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX(), player.getY() + 1, player.getZ(), 40, 1, 1, 1, 0.15);
                    level.sendParticles(soulRed,
                            player.getX(), player.getY() + 1, player.getZ(), 30, 1.5, 0.5, 1.5, 0.1);
                    level.sendParticles(ParticleTypes.SOUL,
                            player.getX(), player.getY() + 2, player.getZ(), 20, 0.5, 1, 0.5, 0.1);

                    level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0f, 0.5f);
                    level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.5f, 0.3f);

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(">> Tsunami d'Ames invoque! ")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                            .append(net.minecraft.network.chat.Component.literal("Une vague d'ames deferle devant vous!")
                                    .withStyle(ChatFormatting.RED)));
                    return;
                }

                // TIME: Abime du Temps - target the looked-at mob
                if (element == ElementType.TIME) {
                    Vec3 timeEye = player.getEyePosition();
                    Vec3 timeLook = player.getLookAngle();
                    LivingEntity timeTarget = null;
                    double timeClosest = 30.0;

                    for (Entity entity : level.getEntities(player,
                            player.getBoundingBox().inflate(30),
                            e -> e instanceof LivingEntity && e != player)) {
                        LivingEntity living = (LivingEntity) entity;
                        Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(timeEye);
                        double dist = toEntity.length();
                        if (dist > timeClosest) continue;
                        Vec3 toEntityNorm = toEntity.normalize();
                        if (timeLook.dot(toEntityNorm) > 0.95) {
                            timeClosest = dist;
                            timeTarget = living;
                        }
                    }

                    if (timeTarget == null) {
                        player.sendSystemMessage(Component.literal("Aucune cible en vue!")
                                .withStyle(ChatFormatting.GRAY));
                        return;
                    }

                    // Launch the Time Abyss
                    com.noxgg.elementalpower.world.TimeAbyssManager.addAbyss(
                            new com.noxgg.elementalpower.world.TimeAbyssManager.TimeAbyss(
                                    level, player, timeTarget));

                    // Freeze target immediately
                    timeTarget.setDeltaMovement(0, 0, 0);
                    timeTarget.hurtMarked = true;

                    // Initial casting beam from player to target
                    Vec3 targetPos = timeTarget.position();
                    var timeDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.1f, 0.9f, 0.4f), 1.5f);
                    for (int i = 0; i < 20; i++) {
                        double t = i / 20.0;
                        double px = player.getX() + (targetPos.x - player.getX()) * t;
                        double py = player.getEyeY() + (targetPos.y + 1 - player.getEyeY()) * t;
                        double pz = player.getZ() + (targetPos.z - player.getZ()) * t;
                        level.sendParticles(timeDust, px, py, pz, 3, 0.05, 0.05, 0.05, 0.01);
                        level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.02, 0.02, 0.02, 0.005);
                    }

                    // Vortex burst at target location
                    for (int i = 0; i < 40; i++) {
                        double angle = (Math.PI * 2.0 / 40) * i;
                        double px = targetPos.x + Math.cos(angle) * 2.0;
                        double pz = targetPos.z + Math.sin(angle) * 2.0;
                        level.sendParticles(ParticleTypes.PORTAL,
                                px, targetPos.y + 0.5, pz, 5, 0.1, 0.3, 0.1, 0.5);
                    }

                    level.playSound(null, timeTarget.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.5f, 1.5f);
                    level.playSound(null, timeTarget.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0f, 0.5f);

                    player.sendSystemMessage(Component.literal(">> Abime du Temps invoque! ")
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                            .append(Component.literal("La cible est aspiree dans le vortex temporel... elle vieillira et sera bombardee!")
                                    .withStyle(ChatFormatting.DARK_GREEN)));
                    return;
                }

                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();

                // Find the mob the player is looking at (within 30 blocks)
                LivingEntity target = null;
                double closestDist = 30.0;

                for (Entity entity : level.getEntities(player,
                        player.getBoundingBox().inflate(30),
                        e -> e instanceof LivingEntity && e != player)) {

                    LivingEntity living = (LivingEntity) entity;
                    Vec3 toEntity = living.position().add(0, living.getBbHeight() / 2, 0).subtract(eye);
                    double dist = toEntity.length();
                    if (dist > closestDist) continue;

                    Vec3 toEntityNorm = toEntity.normalize();
                    double dot = look.dot(toEntityNorm);

                    // Must be looking at the mob (within ~10 degree cone)
                    if (dot > 0.95) {
                        closestDist = dist;
                        target = living;
                    }
                }

                if (target == null) {
                    player.sendSystemMessage(Component.literal("Aucune cible en vue!")
                            .withStyle(ChatFormatting.GRAY));
                    return;
                }

                if (element == ElementType.DARKNESS) {
                    // === DARK PRISON ===
                    double prisonRadius = 3.5;
                    DarkPrisonManager.addPrison(new DarkPrisonManager.DarkPrison(
                            level, player, target, prisonRadius));

                    target.setDeltaMovement(0, 0, 0);
                    target.hurtMarked = true;

                    Vec3 targetPos = target.position();
                    for (int i = 0; i < 20; i++) {
                        double t = i / 20.0;
                        double px = player.getX() + (targetPos.x - player.getX()) * t;
                        double py = player.getEyeY() + (targetPos.y + 1 - player.getEyeY()) * t;
                        double pz = player.getZ() + (targetPos.z - player.getZ()) * t;
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                px, py, pz, 3, 0.05, 0.05, 0.05, 0.01);
                        level.sendParticles(ParticleTypes.SCULK_SOUL,
                                px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                    }

                    for (int i = 0; i < 60; i++) {
                        double theta = level.random.nextDouble() * Math.PI * 2;
                        double phi = level.random.nextDouble() * Math.PI;
                        double bx = targetPos.x + Math.cos(theta) * Math.sin(phi) * prisonRadius;
                        double by = targetPos.y + Math.cos(phi) * prisonRadius;
                        double bz = targetPos.z + Math.sin(theta) * Math.sin(phi) * prisonRadius;
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                bx, by, bz, 2, 0.1, 0.1, 0.1, 0.02);
                    }

                    level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 0.4f);
                    level.playSound(null, target.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0f, 0.6f);

                    player.sendSystemMessage(Component.literal(">> Prison de Tenebres invoquee! ")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)
                            .append(Component.literal("Gardez le regard fixe sur le prisonnier!")
                                    .withStyle(ChatFormatting.GRAY)));

                } else if (element == ElementType.POISON) {
                    // === POISON DRAGON ATTACK ===
                    PoisonDragonManager.addAttack(new PoisonDragonManager.PoisonDragonAttack(
                            level, player, target));

                    // Purple casting beam from player to target
                    Vec3 targetPos = target.position();
                    var purpleDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.6f, 0.0f, 0.8f), 1.5f);
                    for (int i = 0; i < 15; i++) {
                        double t = i / 15.0;
                        double px = player.getX() + (targetPos.x - player.getX()) * t;
                        double py = player.getEyeY() + (targetPos.y + 1 - player.getEyeY()) * t;
                        double pz = player.getZ() + (targetPos.z - player.getZ()) * t;
                        level.sendParticles(purpleDust, px, py, pz, 3, 0.05, 0.05, 0.05, 0.02);
                        level.sendParticles(ParticleTypes.WITCH, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                    }

                    level.playSound(null, target.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.6f);
                    level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.0f, 0.5f);

                    player.sendSystemMessage(Component.literal(">> Dragons de Poison invoques! ")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD)
                            .append(Component.literal("Ils fondent sur la cible!")
                                    .withStyle(ChatFormatting.GREEN)));

                } else if (element == ElementType.ROYAL) {
                    // === ROYAL JUDGMENT: paralyze all in front + open choice screen ===
                    Vec3 flatLook = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
                    Vec3 playerPos = player.position();

                    var goldDust = new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(1.0f, 0.84f, 0.0f), 2.0f);

                    // Find all entities in 4 blocks in front
                    java.util.List<Integer> paralyzedIds = new java.util.ArrayList<>();

                    level.getEntities(player, player.getBoundingBox().inflate(5), e -> {
                        if (e == player || !(e instanceof LivingEntity)) return false;
                        Vec3 toE = e.position().subtract(playerPos);
                        Vec3 flatToE = new Vec3(toE.x, 0, toE.z);
                        double dist = flatToE.length();
                        if (dist > 4.0 || dist < 0.3) return false;
                        return flatLook.dot(flatToE.normalize()) > 0.5;
                    }).forEach(e -> {
                        if (e instanceof LivingEntity living) {
                            // Paralyze
                            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 127));
                            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 127));
                            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
                            living.setDeltaMovement(0, 0, 0);
                            living.hurtMarked = true;

                            // Golden chains on entity
                            level.sendParticles(goldDust,
                                    living.getX(), living.getY() + 1, living.getZ(),
                                    15, 0.3, 0.5, 0.3, 0.02);
                            level.sendParticles(ParticleTypes.END_ROD,
                                    living.getX(), living.getY() + 0.5, living.getZ(),
                                    5, 0.2, 0.3, 0.2, 0.01);

                            paralyzedIds.add(e.getId());
                        }
                    });

                    if (paralyzedIds.isEmpty()) {
                        player.sendSystemMessage(Component.literal("Aucun sujet devant vous!")
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        // Open judgment screen
                        ModMessages.sendToPlayer(new OpenRoyalScreenS2CPacket(paralyzedIds), player);

                        // Royal fanfare
                        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0f, 1.0f);
                        level.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).get(), SoundSource.PLAYERS, 2.0f, 0.8f);

                        player.sendSystemMessage(Component.literal(">> Jugement Royal! ")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                .append(Component.literal("Choisissez le sort de vos prisonniers.")
                                        .withStyle(ChatFormatting.YELLOW)));
                    }
                }
            });
        });
        return true;
    }
}
