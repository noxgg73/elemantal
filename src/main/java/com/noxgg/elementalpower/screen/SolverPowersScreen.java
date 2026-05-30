package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.ElementalPowerMod;
import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.SolverSelectPowerC2SPacket;
import com.noxgg.elementalpower.world.AbsoluteSolverManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** R-menu listing all Absolute Solver powers. Clicking one selects it (right-click then triggers it). */
public class SolverPowersScreen extends Screen {

    private static final int N = AbsoluteSolverManager.POWER_NAMES.length;
    private static final ResourceLocation[] ICONS = new ResourceLocation[N];
    static {
        for (int i = 0; i < N; i++) {
            ICONS[i] = new ResourceLocation(ElementalPowerMod.MOD_ID, "textures/gui/solver/power_" + i + ".png");
        }
    }

    private static final int BTN_W = 200;
    private static final int BTN_H = 18;
    private static final int SPACING = 21;

    private int startX, startY;

    public SolverPowersScreen() {
        super(Component.literal("Pouvoirs du Solver"));
    }

    @Override
    protected void init() {
        startX = (this.width - BTN_W) / 2 + 12; // leave room on the left for the icon
        startY = this.height / 2 - (N * SPACING) / 2;

        for (int i = 0; i < N; i++) {
            final int index = i;
            int y = startY + i * SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal(AbsoluteSolverManager.POWER_NAMES[i]),
                    button -> {
                        ModMessages.sendToServer(new SolverSelectPowerC2SPacket(index));
                        this.onClose();
                    })
                    .bounds(startX, y, BTN_W, BTN_H)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.drawCenteredString(this.font, "--- ABSOLUTE SOLVER ---",
                this.width / 2, startY - 30, 0xD400FF);
        graphics.drawCenteredString(this.font, "Choisis un pouvoir, puis clic droit pour le declencher",
                this.width / 2, startY - 18, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Icon to the left of each power button (16x16 textures in the Solver style)
        for (int i = 0; i < N; i++) {
            int y = startY + i * SPACING + (BTN_H - 16) / 2;
            graphics.blit(ICONS[i], startX - 20, y, 0, 0, 16, 16, 16, 16);
        }

        // Description of the hovered power
        for (int i = 0; i < N; i++) {
            int y = startY + i * SPACING;
            if (mouseX >= startX && mouseX <= startX + BTN_W && mouseY >= y && mouseY <= y + BTN_H) {
                graphics.drawCenteredString(this.font, AbsoluteSolverManager.POWER_DESCS[i],
                        this.width / 2, startY + N * SPACING + 8, 0xAAAAAA);
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
