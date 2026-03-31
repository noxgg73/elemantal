package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.ElementalPowerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalPowerMod.MOD_ID, value = Dist.CLIENT)
public class DreamOverlay {
    private static int currentPhase = -1;
    private static String currentMessage = "";
    private static long phaseStartTime = 0;
    private static String questName = "";
    private static boolean timerActive = false;
    private static long timerStartTime = 0;

    public static void setPhase(int phase, String message) {
        currentPhase = phase;
        currentMessage = message;
        phaseStartTime = System.currentTimeMillis();

        if (phase == 2) {
            questName = message;
        }
        if (phase == 3) {
            questName = message;
            timerActive = true;
            timerStartTime = System.currentTimeMillis();
            // Reset dream overlay after a moment
            currentPhase = -1;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        long elapsed = System.currentTimeMillis() - phaseStartTime;

        // === DREAM PHASES ===
        if (currentPhase == 0) {
            // Phase 0: You're dreaming
            graphics.drawCenteredString(mc.font, "Vous sombrez dans un reve profond...",
                    screenWidth / 2, screenHeight / 2 - 30, 0x8888FF);
        }

        if (currentPhase == 1) {
            // Phase 1: Moon message
            // Fade in effect
            int alpha = Math.min(255, (int)(elapsed / 5));
            int color = (alpha << 24) | 0xFFFFCC;

            graphics.drawCenteredString(mc.font, "La Lune parle...",
                    screenWidth / 2, 30, 0xFFFFAA);
            graphics.drawCenteredString(mc.font, currentMessage,
                    screenWidth / 2, screenHeight / 2, 0xFFD700);
        }

        if (currentPhase == 2) {
            // Phase 2: Quest scroll
            // Draw scroll background
            int scrollX = screenWidth / 2 - 120;
            int scrollY = screenHeight / 2 - 50;
            graphics.fill(scrollX, scrollY, scrollX + 240, scrollY + 100, 0xCC2B1D0E);
            graphics.fill(scrollX + 2, scrollY + 2, scrollX + 238, scrollY + 98, 0xCC4A3728);

            graphics.drawCenteredString(mc.font, "=== CONTRAT ===",
                    screenWidth / 2, scrollY + 8, 0xFFD700);
            graphics.drawCenteredString(mc.font, "---",
                    screenWidth / 2, scrollY + 20, 0x886644);

            graphics.drawCenteredString(mc.font, "Quete assignee:",
                    screenWidth / 2, scrollY + 35, 0xDDCCBB);
            graphics.drawCenteredString(mc.font, questName,
                    screenWidth / 2, scrollY + 50, 0xFFDD44);

            graphics.drawCenteredString(mc.font, "Delai: 3 nuits",
                    screenWidth / 2, scrollY + 68, 0xFF6644);
            graphics.drawCenteredString(mc.font, "---",
                    screenWidth / 2, scrollY + 80, 0x886644);
        }

        // === TIMER (always visible after waking up) ===
        if (timerActive && currentPhase == -1) {
            long timerElapsed = System.currentTimeMillis() - timerStartTime;
            // 3 nights = 3 * 24000 ticks = about 60 minutes real time
            // Show simplified countdown
            long totalMs = 60L * 60 * 1000; // 60 minutes
            long remaining = Math.max(0, totalMs - timerElapsed);
            int minutes = (int)(remaining / 60000);
            int seconds = (int)((remaining % 60000) / 1000);

            String timerText = String.format("Contrat: %02d:%02d", minutes, seconds);
            int timerColor = remaining < 300000 ? 0xFF4444 : 0xFFD700; // Red if < 5 min

            // Top right corner
            int textWidth = mc.font.width(timerText);
            int tx = screenWidth - textWidth - 10;
            graphics.fill(tx - 4, 4, screenWidth - 4, 18, 0x88000000);
            graphics.drawString(mc.font, timerText, tx, 6, timerColor);

            // Quest name below timer
            if (!questName.isEmpty()) {
                int qWidth = mc.font.width(questName);
                int qx = screenWidth - qWidth - 10;
                graphics.fill(qx - 4, 18, screenWidth - 4, 32, 0x88000000);
                graphics.drawString(mc.font, questName, qx, 20, 0xAAAAFF);
            }
        }
    }
}
