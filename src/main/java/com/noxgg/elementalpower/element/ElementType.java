package com.noxgg.elementalpower.element;

import net.minecraft.ChatFormatting;

public enum ElementType {
    NONE("none", "Aucun", ChatFormatting.GRAY, 0x808080),
    FIRE("fire", "Feu", ChatFormatting.RED, 0xFF3300),
    WATER("water", "Eau", ChatFormatting.BLUE, 0x3366FF),
    EARTH("earth", "Terre", ChatFormatting.GOLD, 0x8B5A2B),
    AIR("air", "Air", ChatFormatting.WHITE, 0xCCEEFF),
    SPACE("space", "Espace", ChatFormatting.DARK_PURPLE, 0x6600CC),
    TIME("time", "Temps", ChatFormatting.YELLOW, 0xFFD700),
    POISON("poison", "Poison", ChatFormatting.DARK_GREEN, 0x33CC00),
    DARKNESS("darkness", "Tenebres", ChatFormatting.DARK_GRAY, 0x1A1A2E),
    LIGHT("light", "Lumiere", ChatFormatting.AQUA, 0xFFFFAA),
    DEMON("demon", "Demon", ChatFormatting.DARK_RED, 0x8B0000),
    NATURE("nature", "Nature", ChatFormatting.GREEN, 0x228B22),
    LIGHTNING("lightning", "Foudre", ChatFormatting.YELLOW, 0xFFFF00);

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final int rgbColor;

    ElementType(String id, String displayName, ChatFormatting color, int rgbColor) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.rgbColor = rgbColor;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ChatFormatting getColor() { return color; }
    public int getRgbColor() { return rgbColor; }

    public String getDescription() {
        return switch (this) {
            case FIRE -> "Maitrise du feu et des flammes. Immunite au feu, boules de feu.";
            case WATER -> "Maitrise de l'eau. Respiration aquatique, regeneration, soin.";
            case EARTH -> "Maitrise de la terre. Resistance aux degats, murs de pierre.";
            case AIR -> "Maitrise de l'air. Vitesse, saut, chute lente.";
            case SPACE -> "Maitrise de l'espace. Teleportation, enderpearl infini.";
            case TIME -> "Maitrise du temps. Ralentir les ennemis, vitesse extreme.";
            case POISON -> "Maitrise du poison. Empoisonne les ennemis, immunite au poison.";
            case DARKNESS -> "Maitrise des tenebres. Invisibilite, aveuglement des ennemis.";
            case LIGHT -> "Maitrise de la lumiere. Regeneration, degats aux morts-vivants.";
            case DEMON -> "Puissance demoniaque. Force surhumaine, aura de feu.";
            case NATURE -> "Maitrise de la nature. Regeneration rapide, epines.";
            case LIGHTNING -> "Maitrise de la foudre. Invoque la foudre, vitesse eclair.";
            case NONE -> "Aucun element selectionne.";
        };
    }

    public static ElementType fromId(String id) {
        for (ElementType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return NONE;
    }
}
