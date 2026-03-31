package com.noxgg.elementalpower.item;

import com.noxgg.elementalpower.ElementalPowerMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class RoyalCrownItem extends ArmorItem {

    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(
            new Vector3f(1.0f, 0.84f, 0.0f), 1.2f);

    public RoyalCrownItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Only works when worn on head
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() != this) return;

        // Every 5 ticks
        if (player.tickCount % 5 != 0) return;

        Vec3 lookDir = player.getLookAngle();
        Vec3 flatLook = new Vec3(lookDir.x, 0, lookDir.z).normalize();
        Vec3 playerPos = player.position();

        // Check entities in a 5-block cone in front of the player
        serverLevel.getEntities(player, player.getBoundingBox().inflate(6), e -> {
            if (e == player) return false;
            if (!(e instanceof LivingEntity)) return false;

            // Vector from player to entity
            Vec3 toEntity = e.position().subtract(playerPos);
            Vec3 flatToEntity = new Vec3(toEntity.x, 0, toEntity.z);
            double dist = flatToEntity.length();

            // Must be within 5 blocks
            if (dist > 5.0 || dist < 0.5) return false;

            // Must be in front (dot product > 0.5 = ~60 degree cone)
            Vec3 flatNorm = flatToEntity.normalize();
            double dot = flatLook.dot(flatNorm);
            return dot > 0.5;
        }).forEach(e -> {
            if (e instanceof LivingEntity living) {
                // Force to kneel: slowness + weakness
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5, false, false));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 3, false, false));

                // Push entity down slightly (bowing)
                if (living.getDeltaMovement().y >= 0) {
                    living.setDeltaMovement(living.getDeltaMovement().x * 0.3, -0.1, living.getDeltaMovement().z * 0.3);
                    living.hurtMarked = true;
                }

                // Golden particles falling on the kneeling entity
                if (player.tickCount % 10 == 0) {
                    serverLevel.sendParticles(GOLD_DUST,
                            living.getX(), living.getY() + living.getBbHeight() + 0.5, living.getZ(),
                            5, 0.2, 0.1, 0.2, 0.02);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            living.getX(), living.getY() + 0.5, living.getZ(),
                            2, 0.1, 0.2, 0.1, 0.01);
                }
            }
        });

        // Crown golden particle aura around player head
        if (player.tickCount % 10 == 0) {
            double headY = player.getY() + 2.2;
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 / 5) * i + player.tickCount * 0.05;
                double crX = player.getX() + Math.cos(angle) * 0.5;
                double crZ = player.getZ() + Math.sin(angle) * 0.5;
                serverLevel.sendParticles(GOLD_DUST, crX, headY, crZ, 1, 0.02, 0.02, 0.02, 0.001);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Couronne du Roi").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Les entites devant vous").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("s'inclinent en votre presence.").withStyle(ChatFormatting.GRAY));
    }
}
