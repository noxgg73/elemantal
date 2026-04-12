package com.noxgg.elementalpower.element;

import net.minecraft.nbt.CompoundTag;

public class PlayerElement {
    private ElementType element = ElementType.NONE;
    private boolean hasChosen = false;
    private int level = 1;
    private int xp = 0;
    private int souls = 0;
    private String subClass = ""; // "chara" or "frisk" for Undertale class
    private boolean isAlastor = false; // Demon class: reincarnated as Alastor after death
    private boolean alastorModeActive = true; // true = Alastor spells, false = normal Demon spells
    private int alastorSpellSlot = 0; // 0=Tentacules, 1=Vaudou, 2=Onde Radio
    private boolean hasPuppeteerPower = false; // Fire class: puppeteer power from killing Pure Vanilla

    // Soul Contract tracking
    private boolean hasActiveContract = false;
    private int contractQuestIndex = -1;
    private long contractDeadlineTick = -1; // world game time deadline
    private boolean contractCompleted = false;

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
    public String getSubClass() { return subClass; }
    public void setSubClass(String subClass) { this.subClass = subClass; }
    public boolean isFrisk() { return "frisk".equals(subClass); }
    public boolean isChara() { return "chara".equals(subClass); }

    public boolean isAlastor() { return isAlastor; }
    public void setAlastor(boolean alastor) { this.isAlastor = alastor; if (alastor) this.alastorModeActive = true; }
    public boolean isAlastorModeActive() { return isAlastor && alastorModeActive; }
    public void setAlastorModeActive(boolean active) { this.alastorModeActive = active; }
    public int getAlastorSpellSlot() { return alastorSpellSlot; }
    public void setAlastorSpellSlot(int slot) { this.alastorSpellSlot = slot % 3; }
    public void cycleAlastorSpell() { this.alastorSpellSlot = (alastorSpellSlot + 1) % 3; }

    public boolean hasPuppeteerPower() { return hasPuppeteerPower; }
    public void setPuppeteerPower(boolean power) { this.hasPuppeteerPower = power; }

    // Soul Contract methods
    public boolean hasActiveContract() { return hasActiveContract; }
    public int getContractQuestIndex() { return contractQuestIndex; }
    public long getContractDeadlineTick() { return contractDeadlineTick; }
    public boolean isContractCompleted() { return contractCompleted; }

    public void startContract(int questIndex, long deadlineTick) {
        this.hasActiveContract = true;
        this.contractQuestIndex = questIndex;
        this.contractDeadlineTick = deadlineTick;
        this.contractCompleted = false;
    }

    public void completeContract() {
        this.contractCompleted = true;
    }

    public void clearContract() {
        this.hasActiveContract = false;
        this.contractQuestIndex = -1;
        this.contractDeadlineTick = -1;
        this.contractCompleted = false;
    }

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
        this.subClass = source.subClass;
        this.isAlastor = source.isAlastor;
        this.alastorModeActive = source.alastorModeActive;
        this.alastorSpellSlot = source.alastorSpellSlot;
        this.hasPuppeteerPower = source.hasPuppeteerPower;
        this.hasActiveContract = source.hasActiveContract;
        this.contractQuestIndex = source.contractQuestIndex;
        this.contractDeadlineTick = source.contractDeadlineTick;
        this.contractCompleted = source.contractCompleted;
    }

    public void saveNBT(CompoundTag tag) {
        tag.putString("element", element.getId());
        tag.putBoolean("hasChosen", hasChosen);
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("souls", souls);
        tag.putString("subClass", subClass);
        tag.putBoolean("isAlastor", isAlastor);
        tag.putBoolean("alastorModeActive", alastorModeActive);
        tag.putInt("alastorSpellSlot", alastorSpellSlot);
        tag.putBoolean("hasPuppeteerPower", hasPuppeteerPower);
        tag.putBoolean("hasActiveContract", hasActiveContract);
        tag.putInt("contractQuestIndex", contractQuestIndex);
        tag.putLong("contractDeadlineTick", contractDeadlineTick);
        tag.putBoolean("contractCompleted", contractCompleted);
    }

    public void loadNBT(CompoundTag tag) {
        element = ElementType.fromId(tag.getString("element"));
        hasChosen = tag.getBoolean("hasChosen");
        level = tag.getInt("level");
        if (level < 1) level = 1;
        xp = tag.getInt("xp");
        souls = tag.getInt("souls");
        subClass = tag.getString("subClass");
        isAlastor = tag.getBoolean("isAlastor");
        alastorModeActive = tag.contains("alastorModeActive") ? tag.getBoolean("alastorModeActive") : isAlastor;
        alastorSpellSlot = tag.getInt("alastorSpellSlot");
        hasPuppeteerPower = tag.getBoolean("hasPuppeteerPower");
        hasActiveContract = tag.getBoolean("hasActiveContract");
        contractQuestIndex = tag.getInt("contractQuestIndex");
        contractDeadlineTick = tag.getLong("contractDeadlineTick");
        contractCompleted = tag.getBoolean("contractCompleted");
    }
}
