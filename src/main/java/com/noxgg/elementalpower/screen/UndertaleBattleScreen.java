package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.UndertaleBattleActionC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Full Undertale-style battle screen.
 * Black background, white battle box, enemy display area,
 * FIGHT / ACT / ITEM / MERCY buttons at bottom.
 */
public class UndertaleBattleScreen extends Screen {
    private final int entityId;
    private final String mobName;
    private final float mobHealth;
    private final float mobMaxHealth;
    private final boolean isFrisk;

    // Battle box dimensions
    private int boxX, boxY, boxW, boxH;

    // Soul position (red/yellow heart the player controls)
    private double soulX, soulY;
    private static final int SOUL_SIZE = 8;

    // Attack animation state
    private boolean inAttackAnim = false;
    private long attackAnimStart = 0;
    private int attackSliderX = 0;
    private boolean attackHit = false;

    // Enemy attack phase
    private boolean inEnemyAttack = false;
    private long enemyAttackStart = 0;
    private static final long ENEMY_ATTACK_DURATION = 4000; // 4 seconds

    // Projectiles for enemy attack
    private double[] projX, projY, projDX, projDY;
    private int projCount = 0;

    // Damage flash
    private boolean damageFlash = false;
    private long damageFlashStart = 0;

    // Dialog text
    private String dialogText = "";
    private long dialogStart = 0;

    // Sub-menu state
    private String currentMenu = "main"; // "main", "act", "attack"

    public UndertaleBattleScreen(int entityId, String mobName, float mobHealth, float mobMaxHealth, boolean isFrisk) {
        super(Component.literal("Undertale Battle"));
        this.entityId = entityId;
        this.mobName = mobName;
        this.mobHealth = mobHealth;
        this.mobMaxHealth = mobMaxHealth;
        this.isFrisk = isFrisk;
        this.dialogText = "* " + mobName + " se dresse devant toi!";
        this.dialogStart = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        // Battle box: centered, lower portion of screen
        boxW = 230;
        boxH = 120;
        boxX = (this.width - boxW) / 2;
        boxY = this.height / 2 + 20;

        // Soul starts in center of battle box
        soulX = boxX + boxW / 2.0;
        soulY = boxY + boxH / 2.0;

        setupMainButtons();
    }

    private void setupMainButtons() {
        this.clearWidgets();
        currentMenu = "main";

        int btnW = 50;
        int btnH = 20;
        int btnY = this.height - 30;
        int totalW = btnW * 4 + 15 * 3;
        int startX = (this.width - totalW) / 2;

        // FIGHT button (orange) - grayed for Frisk
        Button fightBtn = Button.builder(
                Component.literal("FIGHT").withStyle(isFrisk ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD, ChatFormatting.BOLD),
                button -> {
                    if (!isFrisk) {
                        startAttackAnimation();
                    }
                })
                .bounds(startX, btnY, btnW, btnH)
                .build();
        if (isFrisk) fightBtn.active = false;
        this.addRenderableWidget(fightBtn);

        // ACT button (blue)
        this.addRenderableWidget(Button.builder(
                Component.literal("ACT").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                button -> setupActMenu())
                .bounds(startX + btnW + 15, btnY, btnW, btnH)
                .build());

        // ITEM button (dark, not implemented)
        Button itemBtn = Button.builder(
                Component.literal("ITEM").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD),
                button -> {
                    dialogText = "* Tu n'as rien a utiliser.";
                    dialogStart = System.currentTimeMillis();
                })
                .bounds(startX + (btnW + 15) * 2, btnY, btnW, btnH)
                .build();
        this.addRenderableWidget(itemBtn);

        // MERCY button (yellow)
        this.addRenderableWidget(Button.builder(
                Component.literal("MERCY").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                button -> setupMercyMenu())
                .bounds(startX + (btnW + 15) * 3, btnY, btnW, btnH)
                .build());
    }

    private void setupActMenu() {
        this.clearWidgets();
        currentMenu = "act";

        int btnW = 80;
        int btnH = 20;
        int btnY = this.height - 50;
        int centerX = this.width / 2;

        // Check
        this.addRenderableWidget(Button.builder(
                Component.literal("* Verifier").withStyle(ChatFormatting.WHITE),
                button -> {
                    dialogText = "* " + mobName + " - PV " + (int)mobHealth + "/" + (int)mobMaxHealth
                            + " - ATK " + (int)(mobMaxHealth * 0.2f) + " DEF " + (int)(mobMaxHealth * 0.1f);
                    dialogStart = System.currentTimeMillis();
                    setupMainButtons();
                    startEnemyAttack();
                })
                .bounds(centerX - btnW - 5, btnY, btnW, btnH)
                .build());

        // Talk
        this.addRenderableWidget(Button.builder(
                Component.literal("* Parler").withStyle(ChatFormatting.WHITE),
                button -> {
                    String[] talkTexts = {
                            "* Tu essaies de parler a " + mobName + "...",
                            "* " + mobName + " n'a pas l'air de comprendre.",
                            "* " + mobName + " te regarde bizarrement.",
                            "* Tu racontes une blague. " + mobName + " ne rit pas."
                    };
                    dialogText = talkTexts[(int)(System.currentTimeMillis() % talkTexts.length)];
                    dialogStart = System.currentTimeMillis();
                    setupMainButtons();
                    startEnemyAttack();
                })
                .bounds(centerX + 5, btnY, btnW, btnH)
                .build());

        // Back
        this.addRenderableWidget(Button.builder(
                Component.literal("< Retour").withStyle(ChatFormatting.GRAY),
                button -> setupMainButtons())
                .bounds(centerX - 30, btnY + 25, 60, 16)
                .build());
    }

    private void setupMercyMenu() {
        this.clearWidgets();
        currentMenu = "act";

        int btnW = 80;
        int btnH = 20;
        int btnY = this.height - 50;
        int centerX = this.width / 2;

        // Spare
        this.addRenderableWidget(Button.builder(
                Component.literal("* Epargner").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                button -> {
                    ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId, "spare"));
                    // Don't close yet - server will reopen with updated state or end battle
                })
                .bounds(centerX - btnW - 5, btnY, btnW, btnH)
                .build());

        // Flee
        this.addRenderableWidget(Button.builder(
                Component.literal("* Fuir").withStyle(ChatFormatting.WHITE),
                button -> {
                    ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId, "flee"));
                    this.onClose();
                })
                .bounds(centerX + 5, btnY, btnW, btnH)
                .build());

        // Back
        this.addRenderableWidget(Button.builder(
                Component.literal("< Retour").withStyle(ChatFormatting.GRAY),
                button -> setupMainButtons())
                .bounds(centerX - 30, btnY + 25, 60, 16)
                .build());
    }

    private void startAttackAnimation() {
        inAttackAnim = true;
        attackAnimStart = System.currentTimeMillis();
        attackSliderX = 0;
        attackHit = false;
        this.clearWidgets();
        currentMenu = "attack";
    }

    private void startEnemyAttack() {
        inEnemyAttack = true;
        enemyAttackStart = System.currentTimeMillis();
        spawnProjectiles();
    }

    private void spawnProjectiles() {
        // Spawn 6-10 projectiles from random edges of the battle box
        projCount = 6 + (int)(Math.random() * 5);
        projX = new double[projCount];
        projY = new double[projCount];
        projDX = new double[projCount];
        projDY = new double[projCount];

        for (int i = 0; i < projCount; i++) {
            int side = (int)(Math.random() * 4);
            switch (side) {
                case 0 -> { // top
                    projX[i] = boxX + Math.random() * boxW;
                    projY[i] = boxY;
                    projDX[i] = (Math.random() - 0.5) * 1.5;
                    projDY[i] = 1.0 + Math.random() * 1.5;
                }
                case 1 -> { // bottom
                    projX[i] = boxX + Math.random() * boxW;
                    projY[i] = boxY + boxH;
                    projDX[i] = (Math.random() - 0.5) * 1.5;
                    projDY[i] = -(1.0 + Math.random() * 1.5);
                }
                case 2 -> { // left
                    projX[i] = boxX;
                    projY[i] = boxY + Math.random() * boxH;
                    projDX[i] = 1.0 + Math.random() * 1.5;
                    projDY[i] = (Math.random() - 0.5) * 1.5;
                }
                case 3 -> { // right
                    projX[i] = boxX + boxW;
                    projY[i] = boxY + Math.random() * boxH;
                    projDX[i] = -(1.0 + Math.random() * 1.5);
                    projDY[i] = (Math.random() - 0.5) * 1.5;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Full black background
        g.fill(0, 0, this.width, this.height, 0xFF000000);

        long now = System.currentTimeMillis();

        // === ENEMY DISPLAY AREA (top half) ===
        renderEnemyArea(g, now);

        // === HEALTH BAR ===
        renderHealthBars(g);

        // === BATTLE BOX ===
        renderBattleBox(g, now);

        // === DIALOG TEXT ===
        renderDialog(g, now);

        // === ATTACK ANIMATION ===
        if (inAttackAnim) {
            renderAttackAnimation(g, now);
        }

        // === ENEMY ATTACK PHASE ===
        if (inEnemyAttack) {
            updateEnemyAttack(g, now);
        }

        // === BUTTONS ===
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderEnemyArea(GuiGraphics g, long now) {
        int centerX = this.width / 2;
        int enemyY = 30;

        // Enemy name
        g.drawCenteredString(this.font, mobName,
                centerX, enemyY, 0xFFFFFF);

        // Enemy HP bar
        int barW = 100;
        int barH = 10;
        int barX = centerX - barW / 2;
        int barY = enemyY + 14;

        // Background
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        // Health fill (green to yellow to red)
        float hpRatio = mobHealth / mobMaxHealth;
        int hpColor;
        if (hpRatio > 0.5f) hpColor = 0xFF00FF00;
        else if (hpRatio > 0.25f) hpColor = 0xFFFFFF00;
        else hpColor = 0xFFFF0000;
        g.fill(barX, barY, barX + (int)(barW * hpRatio), barY + barH, hpColor);
        // Border
        g.renderOutline(barX, barY, barW, barH, 0xFFFFFFFF);

        // HP text
        g.drawCenteredString(this.font, "HP " + (int)mobHealth + " / " + (int)mobMaxHealth,
                centerX, barY + barH + 4, 0xFFFFFF);

        // Simple mob ASCII art representation
        int artY = barY + barH + 20;
        String mobArt = getMobAsciiArt();
        String[] lines = mobArt.split("\n");
        for (int i = 0; i < lines.length; i++) {
            g.drawCenteredString(this.font, lines[i], centerX, artY + i * 10, 0xFFFFFF);
        }
    }

    private String getMobAsciiArt() {
        // Simple representations based on mob name
        String lower = mobName.toLowerCase();
        if (lower.contains("zombie")) return "   ___\n  |x x|\n  |___|  \n  /| |\\\n / | | \\";
        if (lower.contains("skeleton") || lower.contains("squelette")) return "   ___\n  |o o|\n  |___|  \n  X| |X\n  /   \\";
        if (lower.contains("creeper")) return "   ___\n  |0 0|\n  |vvv|  \n  |   |\n  |___|";
        if (lower.contains("spider") || lower.contains("araign")) return " \\ _ /\n  (o.o)  \n / \\_/ \\";
        if (lower.contains("enderman")) return "   ___\n  |. .|\n  |   |  \n  |   |\n  |   |\n  |___|";
        if (lower.contains("slime")) return "   _____\n  /     \\\n | o   o |\n |  ___  |\n  \\_____/";
        if (lower.contains("blaze")) return "  * * *\n   ___\n  |# #|  \n  |___|  \n  * * *";
        if (lower.contains("wither")) return " ___  ___  ___\n|x x||x x||x x|\n|___||___||___|";
        if (lower.contains("dragon")) return "   /\\_/\\\n  ( o.o )\n   > ^ <  \n /|   |\\\n/_|   |_\\";
        // Default
        return "   ___\n  |? ?|\n  |___|  \n  /| |\\\n /   \\";
    }

    private void renderHealthBars(GuiGraphics g) {
        int centerX = this.width / 2;
        int y = this.height / 2 + 5;

        // Player name
        String playerName = isFrisk ? "FRISK" : "CHARA";
        int nameColor = isFrisk ? 0xFFFF00 : 0xFF0000;
        g.drawString(this.font, playerName, boxX, y, nameColor);

        // Player HP bar
        if (minecraft != null && minecraft.player != null) {
            float playerHp = minecraft.player.getHealth();
            float playerMaxHp = minecraft.player.getMaxHealth();
            int barW = 80;
            int barX = boxX + this.font.width(playerName) + 10;
            g.drawString(this.font, "HP", barX, y, 0xFFFFFF);
            barX += 14;

            // Yellow bar background
            g.fill(barX, y, barX + barW, y + 10, 0xFF333300);
            // Health fill
            float ratio = playerHp / playerMaxHp;
            g.fill(barX, y, barX + (int)(barW * ratio), y + 10, 0xFFFFFF00);

            g.drawString(this.font, (int)playerHp + " / " + (int)playerMaxHp,
                    barX + barW + 5, y, 0xFFFFFF);
        }
    }

    private void renderBattleBox(GuiGraphics g, long now) {
        // White border box
        g.renderOutline(boxX, boxY, boxW, boxH, 0xFFFFFFFF);
        // Inner black
        g.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1, 0xFF000000);

        // Draw the soul (heart) if in enemy attack or idle
        if (inEnemyAttack || !inAttackAnim) {
            int soulColor = isFrisk ? 0xFFFFFF00 : 0xFFFF0000;
            int sx = (int)soulX - SOUL_SIZE / 2;
            int sy = (int)soulY - SOUL_SIZE / 2;
            // Draw heart shape
            g.fill(sx + 1, sy, sx + SOUL_SIZE - 1, sy + 1, soulColor);
            g.fill(sx, sy + 1, sx + SOUL_SIZE, sy + 2, soulColor);
            g.fill(sx, sy + 2, sx + SOUL_SIZE, sy + 3, soulColor);
            g.fill(sx + 1, sy + 3, sx + SOUL_SIZE - 1, sy + 4, soulColor);
            g.fill(sx + 2, sy + 4, sx + SOUL_SIZE - 2, sy + 5, soulColor);
            g.fill(sx + 3, sy + 5, sx + SOUL_SIZE - 3, sy + 6, soulColor);
        }

        // Damage flash
        if (damageFlash && now - damageFlashStart < 200) {
            int alpha = (int)(200 - (now - damageFlashStart));
            g.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1,
                    (alpha << 24) | 0xFF0000);
        }
    }

    private void renderDialog(GuiGraphics g, long now) {
        if (dialogText.isEmpty()) return;

        int dialogY = boxY - 20;
        // Typewriter effect
        long elapsed = now - dialogStart;
        int charsToShow = Math.min(dialogText.length(), (int)(elapsed / 30));
        String visibleText = dialogText.substring(0, charsToShow);

        g.drawString(this.font, visibleText, boxX + 5, dialogY, 0xFFFFFF);
    }

    private void renderAttackAnimation(GuiGraphics g, long now) {
        long elapsed = now - attackAnimStart;

        int barX = boxX + 10;
        int barY = boxY + boxH / 2 - 30;
        int barW = boxW - 20;
        int barH = 60;

        // Horizontal bar
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF000000);
        g.renderOutline(barX, barY, barW, barH, 0xFFFFFFFF);

        // Center target zone
        int centerX = barX + barW / 2;
        int targetW = 20;
        g.fill(centerX - targetW / 2, barY + 1, centerX + targetW / 2, barY + barH - 1, 0x4400FF00);

        // Moving slider line
        double sliderProgress = (elapsed % 2000) / 2000.0;
        int sliderX = barX + (int)(sliderProgress * barW);

        // White vertical line
        g.fill(sliderX - 1, barY, sliderX + 1, barY + barH, 0xFFFFFFFF);

        // Instructions
        g.drawCenteredString(this.font, "Appuie sur ESPACE pour frapper!",
                this.width / 2, barY - 15, 0xFFFFFF);

        // Check if 3 seconds passed without hit
        if (elapsed > 3000 && !attackHit) {
            inAttackAnim = false;
            dialogText = "* MISS! Tu as rate ton attaque!";
            dialogStart = System.currentTimeMillis();
            setupMainButtons();
            startEnemyAttack();
        }
    }

    private void updateEnemyAttack(GuiGraphics g, long now) {
        long elapsed = now - enemyAttackStart;

        if (elapsed > ENEMY_ATTACK_DURATION) {
            inEnemyAttack = false;
            return;
        }

        // Update and render projectiles
        for (int i = 0; i < projCount; i++) {
            projX[i] += projDX[i];
            projY[i] += projDY[i];

            // Bounce off walls
            if (projX[i] <= boxX + 3 || projX[i] >= boxX + boxW - 3) projDX[i] = -projDX[i];
            if (projY[i] <= boxY + 3 || projY[i] >= boxY + boxH - 3) projDY[i] = -projDY[i];

            // Draw projectile (white circle)
            int px = (int) projX[i];
            int py = (int) projY[i];
            g.fill(px - 3, py - 3, px + 3, py + 3, 0xFFFFFFFF);
            g.fill(px - 2, py - 4, px + 2, py + 4, 0xFFFFFFFF);
            g.fill(px - 4, py - 2, px + 4, py + 2, 0xFFFFFFFF);

            // Check collision with soul
            double dx = projX[i] - soulX;
            double dy = projY[i] - soulY;
            if (dx * dx + dy * dy < 64) { // 8px radius collision
                damageFlash = true;
                damageFlashStart = now;
                // Respawn projectile from edge
                projX[i] = boxX + Math.random() * boxW;
                projY[i] = boxY;
                projDY[i] = Math.abs(projDY[i]);
            }
        }

        // Timer bar
        float timeLeft = 1.0f - (float)(elapsed) / ENEMY_ATTACK_DURATION;
        int timerW = boxW - 20;
        int timerX = boxX + 10;
        int timerY = boxY + boxH + 5;
        g.fill(timerX, timerY, timerX + timerW, timerY + 4, 0xFF333333);
        g.fill(timerX, timerY, timerX + (int)(timerW * timeLeft), timerY + 4, 0xFFFFFF00);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // SPACE key during attack animation = hit
        if (keyCode == 32 && inAttackAnim && !attackHit) {
            attackHit = true;
            inAttackAnim = false;

            // Send attack action
            ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId, "attack"));
            // Screen will be refreshed by server sending new OpenUndertaleBattleS2CPacket
            return true;
        }

        // WASD / Arrow keys to move soul during enemy attack
        if (inEnemyAttack) {
            double speed = 3.0;
            switch (keyCode) {
                case 87, 265 -> soulY = Math.max(boxY + SOUL_SIZE, soulY - speed); // W or Up
                case 83, 264 -> soulY = Math.min(boxY + boxH - SOUL_SIZE, soulY + speed); // S or Down
                case 65, 263 -> soulX = Math.max(boxX + SOUL_SIZE, soulX - speed); // A or Left
                case 68, 262 -> soulX = Math.min(boxX + boxW - SOUL_SIZE, soulX + speed); // D or Right
            }
            return true;
        }

        // Don't allow ESC during battle unless shift
        if (keyCode == 256) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
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
