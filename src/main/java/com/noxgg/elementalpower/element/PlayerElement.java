package com.noxgg.elementalpower.element;

import net.minecraft.nbt.CompoundTag;

public class PlayerElement {
    private ElementType element = ElementType.NONE;
    private boolean hasChosen = false;

    public ElementType getElement() { return element; }

    public void setElement(ElementType element) {
        this.element = element;
        this.hasChosen = true;
    }

    public boolean hasChosen() { return hasChosen; }

    public void copyFrom(PlayerElement source) {
        this.element = source.element;
        this.hasChosen = source.hasChosen;
    }

    public void saveNBT(CompoundTag tag) {
        tag.putString("element", element.getId());
        tag.putBoolean("hasChosen", hasChosen);
    }

    public void loadNBT(CompoundTag tag) {
        element = ElementType.fromId(tag.getString("element"));
        hasChosen = tag.getBoolean("hasChosen");
    }
}
