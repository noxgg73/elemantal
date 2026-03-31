package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.RoyalJudgmentC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class RoyalJudgmentScreen extends Screen {
    private final List<Integer> entityIds;
    private final List<EntityEntry> entries = new ArrayList<>();

    private record EntityEntry(int id, String name) {}

    public RoyalJudgmentScreen(List<Integer> entityIds) {
        super(Component.literal("Jugement Royal"));
        this.entityIds = entityIds;
    }

    @Override
    protected void init() {
        entries.clear();

        if (Minecraft.getInstance().level != null) {
            for (int id : entityIds) {
                Entity e = Minecraft.getInstance().level.getEntity(id);
                if (e instanceof LivingEntity living) {
                    entries.add(new EntityEntry(id, living.getName().getString()));
                }
            }
        }

        int startY = this.height / 2 - (entries.size() * 35) / 2 + 10;

        for (int i = 0; i < entries.size(); i++) {
            EntityEntry entry = entries.get(i);
            int y = startY + i * 35;

            // Imprison button
            this.addRenderableWidget(Button.builder(
                    Component.literal("Emprisonner").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    button -> {
                        ModMessages.sendToServer(new RoyalJudgmentC2SPacket(entry.id, true));
                        this.onClose();
                    })
                    .bounds(this.width / 2 - 155, y, 145, 20)
                    .build());

            // Release button
            this.addRenderableWidget(Button.builder(
                    Component.literal("Laisser partir").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    button -> {
                        ModMessages.sendToServer(new RoyalJudgmentC2SPacket(entry.id, false));
                        this.onClose();
                    })
                    .bounds(this.width / 2 + 10, y, 145, 20)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.drawCenteredString(this.font, "--- JUGEMENT ROYAL ---",
                this.width / 2, 20, 0xFFD700);
        graphics.drawCenteredString(this.font, "Choisissez le sort de vos prisonniers",
                this.width / 2, 35, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Emprisonner = Prison de Bedrock | Laisser partir = Liberte",
                this.width / 2, 48, 0xAAAAAA);

        int startY = this.height / 2 - (entries.size() * 35) / 2 + 10;
        for (int i = 0; i < entries.size(); i++) {
            EntityEntry entry = entries.get(i);
            int y = startY + i * 35;
            graphics.drawCenteredString(this.font, entry.name, this.width / 2, y - 12, 0xFFD700);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
