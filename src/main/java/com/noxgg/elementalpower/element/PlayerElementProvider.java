package com.noxgg.elementalpower.element;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerElementProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<PlayerElement> PLAYER_ELEMENT =
            CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerElement element = null;
    private final LazyOptional<PlayerElement> optional = LazyOptional.of(this::createPlayerElement);

    private PlayerElement createPlayerElement() {
        if (element == null) {
            element = new PlayerElement();
        }
        return element;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_ELEMENT) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        createPlayerElement().saveNBT(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        createPlayerElement().loadNBT(tag);
    }
}
