package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.network.DemonPortalC2SPacket;
import com.noxgg.elementalpower.network.ModMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DemonPortalScreen extends Screen {

    public DemonPortalScreen() {
        super(Component.literal("Portail Demoniaque"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Nether button
        this.addRenderableWidget(Button.builder(
                Component.literal("Monde des Demons (Nether)").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                button -> {
                    ModMessages.sendToServer(new DemonPortalC2SPacket(false));
                    this.onClose();
                })
                .bounds(centerX - 130, centerY - 15, 260, 25)
                .build());

        // End button
        this.addRenderableWidget(Button.builder(
                Component.literal("The End").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                button -> {
                    ModMessages.sendToServer(new DemonPortalC2SPacket(true));
                    this.onClose();
                })
                .bounds(centerX - 130, centerY + 20, 260, 25)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.drawCenteredString(this.font, "--- PORTAIL DEMONIAQUE ---",
                this.width / 2, this.height / 2 - 55, 0xFF3300);
        graphics.drawCenteredString(this.font, "Choisissez votre destination",
                this.width / 2, this.height / 2 - 40, 0xFFAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
