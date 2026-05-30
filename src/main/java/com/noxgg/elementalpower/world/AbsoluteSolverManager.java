package com.noxgg.elementalpower.world;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.element.PlayerElementProvider;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Self-contained powers for the ABSOLUTE_SOLVER class.
 * Inspired by the Murder Drones "Absolute Solver" program: matter/reality
 * manipulation, disassembly tendrils, planet-eating singularities and a
 * winged "Solver form". All visuals use the Solver's own palette (white core,
 * magenta accretion, yellow drone-eye highlights) so it does not reuse other
 * classes' managers.
 */
public class AbsoluteSolverManager {

    // === Solver palette ===
    private static final DustParticleOptions SOLVER_WHITE = new DustParticleOptions(
            new Vector3f(0.95f, 0.95f, 1.0f), 2.0f);
    private static final DustParticleOptions SOLVER_MAGENTA = new DustParticleOptions(
            new Vector3f(0.83f, 0.0f, 1.0f), 2.0f);
    private static final DustParticleOptions SOLVER_YELLOW = new DustParticleOptions(
            new Vector3f(1.0f, 0.9f, 0.1f), 1.5f);
    private static final DustParticleOptions SOLVER_VOID = new DustParticleOptions(
            new Vector3f(0.03f, 0.0f, 0.06f), 3.5f);

    private static final List<Singularity> singularities = new ArrayList<>();
    private static final Map<UUID, SolverForm> forms = new HashMap<>();

    // ===================================================================
    //  R KEY — DISASSEMBLY TENDRILS
    // ===================================================================
    public static void castTendrils(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        final int[] lvl = {1};
        player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(d -> lvl[0] = d.getLevel());
        float damage = 18.0f + lvl[0] * 0.6f;

        Vec3 origin = player.position().add(0, player.getBbHeight() * 0.6, 0);

        // Gather up to 5 living targets in range
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(24),
                e -> e instanceof LivingEntity && e != player && !(e instanceof Player))) {
            targets.add((LivingEntity) e);
            if (targets.size() >= 5) break;
        }

        if (targets.isEmpty()) {
            // No prey: still emit a threatening burst of tendrils around the caster
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2.0;
                Vec3 tip = origin.add(Math.cos(angle) * 4, 1.5, Math.sin(angle) * 4);
                drawTendril(level, origin, tip);
            }
            player.sendSystemMessage(Component.literal(">> Aucune cible a desassembler.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            LivingEntity puppet = null;
            for (LivingEntity t : targets) {
                Vec3 tip = t.position().add(0, t.getBbHeight() * 0.5, 0);
                drawTendril(level, origin, tip);

                t.hurt(level.damageSources().magic(), damage);
                // "Nanite acid" injection
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2));
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));

                // Drag the target toward the Solver
                Vec3 pull = player.position().subtract(t.position()).normalize().scale(0.8);
                t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.25, pull.z));
                t.hurtMarked = true;

                // Solver symbol flash on the victim
                level.sendParticles(SOLVER_MAGENTA, tip.x, tip.y, tip.z, 18, 0.4, 0.5, 0.4, 0.04);
                level.sendParticles(ParticleTypes.SQUID_INK, tip.x, tip.y, tip.z, 10, 0.3, 0.4, 0.3, 0.02);
                level.sendParticles(ParticleTypes.SCULK_SOUL, tip.x, tip.y, tip.z, 8, 0.3, 0.4, 0.3, 0.03);

                if (puppet == null) puppet = t;
            }

            // Possession flavour: turn the first victim against the others
            if (puppet instanceof Mob mob && targets.size() > 1) {
                for (LivingEntity other : targets) {
                    if (other != puppet) { mob.setTarget(other); break; }
                }
            }

            player.heal(targets.size() * 2.0f);
            player.sendSystemMessage(Component.literal(">> Membres de Desassemblage! ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal(targets.size() + " cible(s) lacerees (" + (int) damage + " degats).")
                            .withStyle(ChatFormatting.DARK_PURPLE)));
        }

        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    private static void drawTendril(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        int steps = (int) Math.max(6, len * 3);
        Vec3 perp = new Vec3(-delta.z, 0, delta.x).normalize();
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            double wobble = Math.sin(t * Math.PI * 4 + len) * 0.4 * (1 - t);
            double px = from.x + delta.x * t + perp.x * wobble;
            double py = from.y + delta.y * t + Math.sin(t * Math.PI) * 0.6;
            double pz = from.z + delta.z * t + perp.z * wobble;
            level.sendParticles(ParticleTypes.SCULK_SOUL, px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);
            level.sendParticles(SOLVER_VOID, px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);
            if (s % 3 == 0) {
                level.sendParticles(SOLVER_MAGENTA, px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    // ===================================================================
    //  G KEY — ABSOLUTE SINGULARITY
    // ===================================================================
    public static class Singularity {
        final double x, y, z;
        final double radius;
        final ServerLevel level;
        final ServerPlayer caster;
        int ticksRemaining;

        Singularity(double x, double y, double z, double radius, ServerLevel level, ServerPlayer caster, int duration) {
            this.x = x; this.y = y; this.z = z;
            this.radius = radius; this.level = level; this.caster = caster;
            this.ticksRemaining = duration;
        }
    }

    public static void castSingularity(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.scale(12)).add(0, 2, 0);

        singularities.add(new Singularity(pos.x, pos.y, pos.z, 22.0, level, player, 180));

        // Birth flash
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 3, 0, 0, 0, 0);
        level.sendParticles(SOLVER_WHITE, pos.x, pos.y, pos.z, 80, 2, 2, 2, 0.15);
        level.sendParticles(SOLVER_MAGENTA, pos.x, pos.y, pos.z, 60, 2, 2, 2, 0.2);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 50, 1.5, 1.5, 1.5, 0.25);

        level.playSound(null, BlockPos.containing(pos), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0f, 0.4f);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 0.3f);

        player.sendSystemMessage(Component.literal(">> Singularite Absolue! ")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("La realite s'effondre pendant 9 secondes.")
                        .withStyle(ChatFormatting.DARK_PURPLE)));
    }

    private static void tickSingularities() {
        Iterator<Singularity> it = singularities.iterator();
        while (it.hasNext()) {
            Singularity s = it.next();
            s.ticksRemaining--;
            if (s.level == null || s.ticksRemaining <= 0) {
                if (s.level != null) collapseSingularity(s);
                it.remove();
                continue;
            }

            // Pull everything except the caster and other Solvers
            s.level.getEntities((Entity) null, new AABB(
                    s.x - s.radius, s.y - s.radius, s.z - s.radius,
                    s.x + s.radius, s.y + s.radius, s.z + s.radius), e -> true).forEach(e -> {
                if (e == s.caster) return;
                if (e instanceof Player p) {
                    boolean[] skip = {false};
                    p.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(d -> {
                        if (d.getElement() == ElementType.ABSOLUTE_SOLVER) skip[0] = true;
                    });
                    if (skip[0]) return;
                }
                double dx = s.x - e.getX(), dy = s.y - e.getY(), dz = s.z - e.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > s.radius || dist < 0.5) return;
                double strength = (1.0 - dist / s.radius) * 0.9;
                e.setDeltaMovement(e.getDeltaMovement().add(dx / dist * strength, dy / dist * strength * 0.6, dz / dist * strength));
                e.hurtMarked = true;
                if (dist < 3.5 && e instanceof LivingEntity living && s.ticksRemaining % 8 == 0) {
                    living.hurt(s.level.damageSources().magic(), 7.0f);
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 2, false, false));
                }
            });

            if (s.ticksRemaining % 4 == 0) absorbBlocks(s);
            if (s.ticksRemaining % 2 == 0) renderSingularity(s);
            if (s.ticksRemaining % 16 == 0) {
                s.level.playSound(null, BlockPos.containing(s.x, s.y, s.z),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.5f, 0.4f);
            }
        }
    }

    private static void absorbBlocks(Singularity s) {
        int elapsed = 180 - s.ticksRemaining;
        double blockRadius = Math.min(9.0, 2.0 + elapsed * 0.07);
        int budget = 3 + elapsed / 18;
        int cx = (int) Math.round(s.x), cy = (int) Math.round(s.y), cz = (int) Math.round(s.z);
        int destroyed = 0;
        for (double r = blockRadius; r >= 1.5 && destroyed < budget; r -= 1.0) {
            for (int attempt = 0; attempt < 18 && destroyed < budget; attempt++) {
                double theta = s.level.random.nextDouble() * Math.PI * 2;
                double phi = s.level.random.nextDouble() * Math.PI;
                int bx = cx + (int) (Math.cos(theta) * Math.sin(phi) * r);
                int by = cy + (int) (Math.cos(phi) * r);
                int bz = cz + (int) (Math.sin(theta) * Math.sin(phi) * r);
                BlockPos pos = new BlockPos(bx, by, bz);
                BlockState state = s.level.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.getDestroySpeed(s.level, pos) < 0) continue;
                if (state.liquid()) continue;
                double px = bx + 0.5, py = by + 0.5, pz = bz + 0.5;
                for (double t = 0; t < 1; t += 0.34) {
                    s.level.sendParticles(ParticleTypes.PORTAL,
                            px + (s.x - px) * t, py + (s.y - py) * t, pz + (s.z - pz) * t, 1, 0.03, 0.03, 0.03, 0.01);
                }
                s.level.sendParticles(SOLVER_MAGENTA, px, py, pz, 2, 0.1, 0.1, 0.1, 0.05);
                s.level.destroyBlock(pos, false);
                destroyed++;
            }
        }
    }

    private static void renderSingularity(Singularity s) {
        int tick = 180 - s.ticksRemaining;
        // White-hot core
        for (int i = 0; i < 24; i++) {
            double theta = Math.random() * Math.PI * 2, phi = Math.random() * Math.PI, r = 0.8 + Math.random() * 0.5;
            s.level.sendParticles(SOLVER_WHITE,
                    s.x + Math.cos(theta) * Math.sin(phi) * r,
                    s.y + Math.cos(phi) * r,
                    s.z + Math.sin(theta) * Math.sin(phi) * r, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 14; i++) {
            double theta = Math.random() * Math.PI * 2, phi = Math.random() * Math.PI, r = 1.4 + Math.random() * 0.4;
            s.level.sendParticles(SOLVER_VOID,
                    s.x + Math.cos(theta) * Math.sin(phi) * r,
                    s.y + Math.cos(phi) * r,
                    s.z + Math.sin(theta) * Math.sin(phi) * r, 1, 0, 0, 0, 0);
        }
        // Magenta accretion rings
        for (int ring = 0; ring < 3; ring++) {
            double ringR = 2.6 + ring * 1.4;
            int points = (int) (ringR * 7);
            for (int p = 0; p < points; p++) {
                double a = (Math.PI * 2 / points) * p + tick * (0.16 - ring * 0.03);
                double px = s.x + Math.cos(a) * ringR;
                double pz = s.z + Math.sin(a) * ringR;
                double py = s.y + Math.sin(a * 3 + tick * 0.2) * 0.4;
                s.level.sendParticles(SOLVER_MAGENTA, px, py, pz, 1, 0, 0, 0, 0.001);
            }
        }
        // Yellow drone-eye streaks
        for (int streak = 0; streak < 5; streak++) {
            double sa = (Math.PI * 2 / 5) * streak + tick * 0.06;
            for (double d = 2.5; d < 6; d += 0.4) {
                s.level.sendParticles(SOLVER_YELLOW,
                        s.x + Math.cos(sa) * d, s.y + Math.sin(d * 0.7 + tick * 0.2) * 0.3, s.z + Math.sin(sa) * d, 1, 0, 0, 0, 0.001);
            }
        }
    }

    private static void collapseSingularity(Singularity s) {
        for (int i = 0; i < 70; i++) {
            double theta = Math.random() * Math.PI * 2, phi = Math.random() * Math.PI, r = s.radius * 0.7;
            s.level.sendParticles(SOLVER_VOID,
                    s.x + Math.cos(theta) * Math.sin(phi) * r,
                    s.y + Math.cos(phi) * r,
                    s.z + Math.sin(theta) * Math.sin(phi) * r, 2, 0, 0, 0, 0.3);
        }
        s.level.sendParticles(ParticleTypes.FLASH, s.x, s.y, s.z, 4, 0, 0, 0, 0);
        s.level.sendParticles(SOLVER_WHITE, s.x, s.y, s.z, 60, 0.5, 0.5, 0.5, 0.4);
        s.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y, s.z, 1, 0, 0, 0, 0);
        s.level.playSound(null, BlockPos.containing(s.x, s.y, s.z), SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 2.0f, 0.3f);
    }

    // ===================================================================
    //  L KEY — SOLVER FORM (winged transformation, toggle)
    // ===================================================================
    public static class SolverForm {
        final ServerPlayer player;
        final boolean prevMayFly;
        int tick = 0;
        SolverForm(ServerPlayer player) {
            this.player = player;
            this.prevMayFly = player.getAbilities().mayfly;
        }
    }

    public static boolean isInSolverForm(UUID id) {
        return forms.containsKey(id);
    }

    public static void toggleSolverForm(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID id = player.getUUID();

        if (forms.containsKey(id)) {
            exitSolverForm(player);
            player.sendSystemMessage(Component.literal(">> Forme du Solver desactivee.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.6f);
            return;
        }

        SolverForm form = new SolverForm(player);
        forms.put(id, form);

        // Grant flight (the Solver's wings)
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();

        // Reality-altering buffs
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));

        // Activation shockwave
        level.getEntities(player, player.getBoundingBox().inflate(8),
                e -> e instanceof LivingEntity && e != player).forEach(e -> {
            LivingEntity living = (LivingEntity) e;
            living.hurt(level.damageSources().magic(), 8.0f);
            Vec3 away = e.position().subtract(player.position()).normalize().scale(1.4);
            living.setDeltaMovement(away.x, 0.6, away.z);
            living.hurtMarked = true;
        });
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1, player.getZ(), 2, 0, 0, 0, 0);
        level.sendParticles(SOLVER_MAGENTA, player.getX(), player.getY() + 1, player.getZ(), 60, 1, 1.5, 1, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.7f);

        player.sendSystemMessage(Component.literal(">> FORME DU SOLVER! ")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("Ailes deployees, regeneration et vol. L pour revenir.")
                        .withStyle(ChatFormatting.DARK_PURPLE)));
    }

    public static void exitSolverForm(ServerPlayer player) {
        SolverForm form = forms.remove(player.getUUID());
        restoreFromForm(player, form);
    }

    /** Removes the form's buffs/flight from the player without touching the map. */
    private static void restoreFromForm(ServerPlayer player, SolverForm form) {
        player.removeEffect(MobEffects.REGENERATION);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.GLOWING);

        if (!player.isCreative() && !player.isSpectator()) {
            boolean keepFly = form != null && form.prevMayFly;
            player.getAbilities().mayfly = keepFly;
            if (!keepFly) player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static void tickForms() {
        Iterator<Map.Entry<UUID, SolverForm>> it = forms.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, SolverForm> entry = it.next();
            SolverForm form = entry.getValue();
            ServerPlayer player = form.player;

            if (player == null || player.isRemoved()) {
                it.remove();
                continue;
            }
            // Sanity: if the player is no longer a Solver, drop the form.
            boolean[] stillSolver = {false};
            player.getCapability(PlayerElementProvider.PLAYER_ELEMENT).ifPresent(d -> {
                if (d.getElement() == ElementType.ABSOLUTE_SOLVER) stillSolver[0] = true;
            });
            if (!stillSolver[0]) {
                restoreFromForm(player, form);
                it.remove();
                continue;
            }

            form.tick++;
            ServerLevel level = player.serverLevel();
            double cx = player.getX(), cy = player.getY(), cz = player.getZ();

            // Wing arcs behind the player
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
            double perpX = -flat.z, perpZ = flat.x;
            for (int side = -1; side <= 1; side += 2) {
                for (double w = 0.4; w <= 2.4; w += 0.3) {
                    double sweep = Math.sin(form.tick * 0.25) * 0.4;
                    double wx = cx + perpX * w * side - flat.x * (0.4 + sweep);
                    double wz = cz + perpZ * w * side - flat.z * (0.4 + sweep);
                    double wy = cy + 1.2 + w * 0.4;
                    level.sendParticles(SOLVER_MAGENTA, wx, wy, wz, 1, 0.02, 0.02, 0.02, 0.0);
                    if (w > 1.5) level.sendParticles(ParticleTypes.SCULK_SOUL, wx, wy, wz, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
            // Floating Solver symbol above the head
            if (form.tick % 3 == 0) {
                level.sendParticles(SOLVER_WHITE, cx, cy + 2.4, cz, 2, 0.15, 0.1, 0.15, 0.0);
                level.sendParticles(SOLVER_YELLOW, cx, cy + 2.4, cz, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    // ===================================================================
    public static void tick() {
        tickSingularities();
        tickForms();
    }

    public static void onPlayerLogout(UUID id) {
        forms.remove(id);
    }
}
