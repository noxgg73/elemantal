package com.noxgg.elementalpower.element;

import net.minecraft.nbt.CompoundTag;

public class PlayerElement {
    private ElementType element = ElementType.NONE;
    private boolean hasChosen = false;
    private int level = 1;
    private int xp = 0;
    private int souls = 0;

    public ElementType getElement() { return element; }

    public void setElement(ElementType element) {
        this.element = element;
        this.hasChosen = true;
        this.level = 1;
        this.xp = 0;
        this.souls = 0;
    }

    public boolean hasChosen() { return hasChosen; }

    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getSouls() { return souls; }

    public int getXpForNextLevel() {
        return level * 50 + (level * level * 10);
    }

    public boolean addXp(int amount) {
        xp += amount;
        boolean leveledUp = false;
        while (xp >= getXpForNextLevel() && level < 100) {
            xp -= getXpForNextLevel();
            level++;
            leveledUp = true;
        }
        return leveledUp;
    }

    public void addSoul() {
        souls++;
    }

    public float getDamageMultiplier() {
        return 1.0f + (level - 1) * 0.05f;
    }

    public float getDefenseMultiplier() {
        return 1.0f + (level - 1) * 0.03f;
    }

    public void copyFrom(PlayerElement source) {
        this.element = source.element;
        this.hasChosen = source.hasChosen;
        this.level = source.level;
        this.xp = source.xp;
        this.souls = source.souls;
    }

    public void saveNBT(CompoundTag tag) {
        tag.putString("element", element.getId());
        tag.putBoolean("hasChosen", hasChosen);
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("souls", souls);
    }

    public void loadNBT(CompoundTag tag) {
        element = ElementType.fromId(tag.getString("element"));
        hasChosen = tag.getBoolean("hasChosen");
        level = tag.getInt("level");
        if (level < 1) level = 1;
        xp = tag.getInt("xp");
        souls = tag.getInt("souls");
    }
}
