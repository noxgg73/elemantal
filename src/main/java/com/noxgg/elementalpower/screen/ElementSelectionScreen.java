package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.element.ElementType;
import com.noxgg.elementalpower.network.ElementChoiceC2SPacket;
import com.noxgg.elementalpower.network.ModMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ElementSelectionScreen extends Screen {
    private static final ElementType[] ELEMENTS = {
            ElementType.FIRE, ElementType.WATER, ElementType.EARTH, ElementType.AIR,
            ElementType.SPACE, ElementType.TIME, ElementType.POISON, ElementType.DARKNESS,
            ElementType.LIGHT, ElementType.DEMON, ElementType.NATURE, ElementType.LIGHTNING
    };

    private ElementType hoveredElement = null;

    public ElementSelectionScreen() {
        super(Component.literal("Choisis ton Element"));
    }

    @Override
    protected void init() {
        int cols = 4;
        int rows = 3;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacingX = 110;
        int spacingY = 28;
        int startX = (this.width - (cols * spacingX - 10)) / 2;
        int startY = this.height / 2 - 40;

        for (int i = 0; i < ELEMENTS.length; i++) {
            ElementType elem = ELEMENTS[i];
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * spacingX;
            int y = startY + row * spacingY;

            this.addRenderableWidget(Button.builder(
                    Component.literal(elem.getDisplayName()).withStyle(elem.getColor()),
                    button -> {
                        ModMessages.sendToServer(new ElementChoiceC2SPacket(elem.getId()));
                        this.onClose();
                    })
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // Title
        graphics.drawCenteredString(this.font, "--- ELEMENTAL POWER ---",
                this.width / 2, 20, 0xFFD700);
        graphics.drawCenteredString(this.font, "Choisis ton element pour commencer",
                this.width / 2, 35, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Description of hovered element
        ElementType hovered = getHoveredElement(mouseX, mouseY);
        if (hovered != null) {
            int descY = this.height / 2 + 55;
            graphics.drawCenteredString(this.font, hovered.getDisplayName(),
                    this.width / 2, descY, hovered.getRgbColor());
            graphics.drawCenteredString(this.font, hovered.getDescription(),
                    this.width / 2, descY + 14, 0xAAAAAA);
        }
    }

    private ElementType getHoveredElement(int mouseX, int mouseY) {
        int cols = 4;
        int spacingX = 110;
        int spacingY = 28;
        int startX = (this.width - (cols * spacingX - 10)) / 2;
        int startY = this.height / 2 - 40;

        for (int i = 0; i < ELEMENTS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * spacingX;
            int y = startY + row * spacingY;
            if (mouseX >= x && mouseX <= x + 100 && mouseY >= y && mouseY <= y + 20) {
                return ELEMENTS[i];
            }
        }
        return null;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
