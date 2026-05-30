package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.SolverSelectPowerC2SPacket;
import com.noxgg.elementalpower.world.AbsoluteSolverManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** R-menu listing all Absolute Solver powers. Clicking one selects it (right-click then triggers it). */
public class SolverPowersScreen extends Screen {

    public SolverPowersScreen() {
        super(Component.literal("Pouvoirs du Solver"));
    }

    @Override
    protected void init() {
        int buttonWidth = 220;
        int buttonHeight = 22;
        int spacingY = 28;
        int startX = (this.width - buttonWidth) / 2;
        int startY = this.height / 2 - (AbsoluteSolverManager.POWER_NAMES.length * spacingY) / 2;

        for (int i = 0; i < AbsoluteSolverManager.POWER_NAMES.length; i++) {
            final int index = i;
            int y = startY + i * spacingY;
            this.addRenderableWidget(Button.builder(
                    Component.literal(AbsoluteSolverManager.POWER_NAMES[i]),
                    button -> {
                        ModMessages.sendToServer(new SolverSelectPowerC2SPacket(index));
                        this.onClose();
                    })
                    .bounds(startX, y, buttonWidth, buttonHeight)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.drawCenteredString(this.font, "--- ABSOLUTE SOLVER ---",
                this.width / 2, this.height / 2 - (AbsoluteSolverManager.POWER_NAMES.length * 28) / 2 - 34, 0xD400FF);
        graphics.drawCenteredString(this.font, "Choisis un pouvoir, puis clic droit pour le declencher",
                this.width / 2, this.height / 2 - (AbsoluteSolverManager.POWER_NAMES.length * 28) / 2 - 20, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Description of the hovered power
        int buttonWidth = 220;
        int spacingY = 28;
        int startX = (this.width - buttonWidth) / 2;
        int startY = this.height / 2 - (AbsoluteSolverManager.POWER_NAMES.length * spacingY) / 2;
        for (int i = 0; i < AbsoluteSolverManager.POWER_NAMES.length; i++) {
            int y = startY + i * spacingY;
            if (mouseX >= startX && mouseX <= startX + buttonWidth && mouseY >= y && mouseY <= y + 22) {
                graphics.drawCenteredString(this.font, AbsoluteSolverManager.POWER_DESCS[i],
                        this.width / 2, startY + AbsoluteSolverManager.POWER_NAMES.length * spacingY + 8, 0xAAAAAA);
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
