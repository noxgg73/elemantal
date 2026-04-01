package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.UndertaleSubClassC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UndertaleChoiceScreen extends Screen {

    private String hoveredChoice = null;

    public UndertaleChoiceScreen() {
        super(Component.literal("Undertale - Choisis ton Ame"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Chara button - red
        this.addRenderableWidget(Button.builder(
                Component.literal("\u2764 CHARA \u2764").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD),
                button -> {
                    ModMessages.sendToServer(new UndertaleSubClassC2SPacket("chara"));
                    this.onClose();
                })
                .bounds(centerX - 130, centerY - 10, 120, 30)
                .build());

        // Frisk button - yellow
        this.addRenderableWidget(Button.builder(
                Component.literal("\u2764 FRISK \u2764").withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD),
                button -> {
                    ModMessages.sendToServer(new UndertaleSubClassC2SPacket("frisk"));
                    this.onClose();
                })
                .bounds(centerX + 10, centerY - 10, 120, 30)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;

        // Title
        graphics.drawCenteredString(this.font, "\u2605 UNDERTALE \u2605",
                centerX, 30, 0xFFFF00);
        graphics.drawCenteredString(this.font, "La Determination coule en toi...",
                centerX, 45, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Choisis ton ame:",
                centerX, 60, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Descriptions
        int descY = this.height / 2 + 40;

        // Check which button is hovered
        boolean charaHover = mouseX >= centerX - 130 && mouseX <= centerX - 10
                && mouseY >= this.height / 2 - 10 && mouseY <= this.height / 2 + 20;
        boolean friskHover = mouseX >= centerX + 10 && mouseX <= centerX + 130
                && mouseY >= this.height / 2 - 10 && mouseY <= this.height / 2 + 20;

        if (charaHover) {
            graphics.drawCenteredString(this.font, "CHARA - La voie du Genocide",
                    centerX, descY, 0xFF0000);
            graphics.drawCenteredString(this.font, "Puissance destructrice maximale.",
                    centerX, descY + 14, 0xCC0000);
            graphics.drawCenteredString(this.font, "Chaque kill augmente ta DETERMINATION.",
                    centerX, descY + 28, 0xAA0000);
        } else if (friskHover) {
            graphics.drawCenteredString(this.font, "FRISK - La voie du Pacifiste",
                    centerX, descY, 0xFFFF00);
            graphics.drawCenteredString(this.font, "Tu ne peux pas attaquer.",
                    centerX, descY + 14, 0xCCCC00);
            graphics.drawCenteredString(this.font, "Appuie sur G pour SPARE un ennemi et le rendre amical.",
                    centerX, descY + 28, 0xAAAA00);
        }
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
