package com.noxgg.elementalpower.screen;

import com.noxgg.elementalpower.network.ModMessages;
import com.noxgg.elementalpower.network.UndertaleBattleActionC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Full Undertale-style battle screen with Gaster Blaster attacks.
 * Turn-based: Player turn (5s) -> Enemy turn with Gaster Blasters (5s) -> repeat
 */
public class UndertaleBattleScreen extends Screen {
    private final int entityId;
    private final String mobName;
    private float mobHealth;
    private final float mobMaxHealth;
    private final boolean isFrisk;

    // Battle box
    private int boxX, boxY, boxW, boxH;

    // Soul
    private double soulX, soulY;
    private static final int SOUL_SIZE = 8;

    // Turn system
    private enum TurnPhase { PLAYER_TURN, ATTACK_ANIM, ENEMY_TURN }
    private TurnPhase phase = TurnPhase.PLAYER_TURN;

    // Attack animation
    private long attackAnimStart = 0;
    private boolean attackHit = false;

    // Enemy turn (Gaster Blasters)
    private long enemyTurnStart = 0;
    private static final long ENEMY_TURN_DURATION = 5000; // 5 seconds

    // Gaster Blasters
    private static final int MAX_BLASTERS = 6;
    private double[] blasterX = new double[MAX_BLASTERS];
    private double[] blasterY = new double[MAX_BLASTERS];
    private double[] blasterAngle = new double[MAX_BLASTERS]; // angle in radians
    private long[] blasterSpawnTime = new long[MAX_BLASTERS];
    private boolean[] blasterFiring = new boolean[MAX_BLASTERS];
    private long[] blasterFireStart = new long[MAX_BLASTERS];
    private int blasterCount = 0;
    private int nextBlasterSpawn = 0; // index for staggered spawning

    // Bouncing bones (additional projectiles)
    private double[] boneX, boneY, boneDX, boneDY;
    private int boneCount = 0;

    // Damage flash
    private boolean damageFlash = false;
    private long damageFlashStart = 0;
    private long lastDamageTick = 0;

    // Dialog
    private String dialogText = "";
    private long dialogStart = 0;

    // Sub-menu
    private String currentMenu = "main";

    // Undertale HP system: 20 HP max, each hit = 3 damage, die at 0
    private static final float UT_MAX_HP = 20;
    private static final float UT_HIT_DAMAGE = 3;
    private float utPlayerHp = UT_MAX_HP;
    private int totalDamageTaken = 0; // track total damage to send to server

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
        boxW = 230;
        boxH = 140;
        boxX = (this.width - boxW) / 2;
        boxY = this.height / 2 + 10;

        soulX = boxX + boxW / 2.0;
        soulY = boxY + boxH / 2.0;

        setupMainButtons();
    }

    private void setupMainButtons() {
        this.clearWidgets();
        currentMenu = "main";
        phase = TurnPhase.PLAYER_TURN;

        int btnW = 50;
        int btnH = 20;
        int btnY = this.height - 30;
        int totalW = btnW * 4 + 15 * 3;
        int startX = (this.width - totalW) / 2;

        // FIGHT
        Button fightBtn = Button.builder(
                Component.literal("FIGHT").withStyle(isFrisk ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD, ChatFormatting.BOLD),
                button -> { if (!isFrisk) startAttackAnimation(); })
                .bounds(startX, btnY, btnW, btnH).build();
        if (isFrisk) fightBtn.active = false;
        this.addRenderableWidget(fightBtn);

        // ACT
        this.addRenderableWidget(Button.builder(
                Component.literal("ACT").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                button -> setupActMenu())
                .bounds(startX + btnW + 15, btnY, btnW, btnH).build());

        // ITEM
        this.addRenderableWidget(Button.builder(
                Component.literal("ITEM").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD),
                button -> { dialogText = "* Tu n'as rien a utiliser."; dialogStart = System.currentTimeMillis(); })
                .bounds(startX + (btnW + 15) * 2, btnY, btnW, btnH).build());

        // MERCY
        this.addRenderableWidget(Button.builder(
                Component.literal("MERCY").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                button -> setupMercyMenu())
                .bounds(startX + (btnW + 15) * 3, btnY, btnW, btnH).build());
    }

    private void setupActMenu() {
        this.clearWidgets();
        currentMenu = "act";
        int btnW = 80; int btnH = 20;
        int btnY = this.height - 50;
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(Component.literal("* Verifier").withStyle(ChatFormatting.WHITE),
                button -> {
                    dialogText = "* " + mobName + " - PV " + (int)mobHealth + "/" + (int)mobMaxHealth
                            + " - ATK " + (int)(mobMaxHealth * 0.2f) + " DEF " + (int)(mobMaxHealth * 0.1f);
                    dialogStart = System.currentTimeMillis();
                    startEnemyTurn();
                }).bounds(centerX - btnW - 5, btnY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.literal("* Parler").withStyle(ChatFormatting.WHITE),
                button -> {
                    String[] texts = { "* " + mobName + " n'a pas l'air de comprendre.",
                            "* " + mobName + " te regarde bizarrement.", "* Tu racontes une blague..." };
                    dialogText = texts[(int)(System.currentTimeMillis() % texts.length)];
                    dialogStart = System.currentTimeMillis();
                    startEnemyTurn();
                }).bounds(centerX + 5, btnY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.literal("< Retour").withStyle(ChatFormatting.GRAY),
                button -> setupMainButtons()).bounds(centerX - 30, btnY + 25, 60, 16).build());
    }

    private void setupMercyMenu() {
        this.clearWidgets();
        currentMenu = "act";
        int btnW = 80; int btnH = 20;
        int btnY = this.height - 50;
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("* Epargner").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                button -> {
                    ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId, "spare"));
                }).bounds(centerX - btnW - 5, btnY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("* Fuir").withStyle(ChatFormatting.WHITE),
                button -> {
                    ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId, "flee"));
                    this.onClose();
                }).bounds(centerX + 5, btnY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.literal("< Retour").withStyle(ChatFormatting.GRAY),
                button -> setupMainButtons()).bounds(centerX - 30, btnY + 25, 60, 16).build());
    }

    private void startAttackAnimation() {
        phase = TurnPhase.ATTACK_ANIM;
        attackAnimStart = System.currentTimeMillis();
        attackHit = false;
        this.clearWidgets();
        currentMenu = "attack";
    }

    private void startEnemyTurn() {
        phase = TurnPhase.ENEMY_TURN;
        enemyTurnStart = System.currentTimeMillis();
        this.clearWidgets();
        currentMenu = "enemy";

        // Reset soul to center
        soulX = boxX + boxW / 2.0;
        soulY = boxY + boxH / 2.0;

        // Spawn Gaster Blasters staggered over time
        blasterCount = 3 + (int)(Math.random() * 4); // 3-6 blasters
        if (blasterCount > MAX_BLASTERS) blasterCount = MAX_BLASTERS;
        nextBlasterSpawn = 0;
        for (int i = 0; i < blasterCount; i++) {
            blasterFiring[i] = false;
            blasterSpawnTime[i] = enemyTurnStart + i * 700L; // stagger by 700ms
        }

        // Also spawn bones
        boneCount = 4 + (int)(Math.random() * 4);
        boneX = new double[boneCount];
        boneY = new double[boneCount];
        boneDX = new double[boneCount];
        boneDY = new double[boneCount];
        for (int i = 0; i < boneCount; i++) {
            int side = (int)(Math.random() * 4);
            switch (side) {
                case 0 -> { boneX[i] = boxX + Math.random() * boxW; boneY[i] = boxY + 3;
                    boneDX[i] = (Math.random() - 0.5) * 2; boneDY[i] = 1.0 + Math.random(); }
                case 1 -> { boneX[i] = boxX + Math.random() * boxW; boneY[i] = boxY + boxH - 3;
                    boneDX[i] = (Math.random() - 0.5) * 2; boneDY[i] = -(1.0 + Math.random()); }
                case 2 -> { boneX[i] = boxX + 3; boneY[i] = boxY + Math.random() * boxH;
                    boneDX[i] = 1.0 + Math.random(); boneDY[i] = (Math.random() - 0.5) * 2; }
                case 3 -> { boneX[i] = boxX + boxW - 3; boneY[i] = boxY + Math.random() * boxH;
                    boneDX[i] = -(1.0 + Math.random()); boneDY[i] = (Math.random() - 0.5) * 2; }
            }
        }

        dialogText = "* " + mobName + " invoque des Gaster Blasters!";
        dialogStart = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF000000);
        long now = System.currentTimeMillis();

        // Check death
        if (utPlayerHp <= 0) {
            utPlayerHp = 0;
            ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId, "playerdeath"));
            this.onClose();
            return;
        }

        renderEnemyArea(g);
        renderHealthBars(g);
        renderBattleBox(g, now);
        renderDialog(g, now);

        if (phase == TurnPhase.ATTACK_ANIM) renderAttackAnimation(g, now);
        if (phase == TurnPhase.ENEMY_TURN) updateEnemyTurn(g, now);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderEnemyArea(GuiGraphics g) {
        int centerX = this.width / 2;
        int enemyY = 20;
        g.drawCenteredString(this.font, mobName, centerX, enemyY, 0xFFFFFF);

        int barW = 100; int barH = 10;
        int barX = centerX - barW / 2; int barY = enemyY + 14;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        float hpRatio = Math.max(0, mobHealth / mobMaxHealth);
        int hpColor = hpRatio > 0.5f ? 0xFF00FF00 : hpRatio > 0.25f ? 0xFFFFFF00 : 0xFFFF0000;
        g.fill(barX, barY, barX + (int)(barW * hpRatio), barY + barH, hpColor);
        g.renderOutline(barX, barY, barW, barH, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "HP " + (int)mobHealth + " / " + (int)mobMaxHealth,
                centerX, barY + barH + 4, 0xFFFFFF);

        int artY = barY + barH + 18;
        String[] lines = getMobAsciiArt().split("\n");
        for (int i = 0; i < lines.length; i++)
            g.drawCenteredString(this.font, lines[i], centerX, artY + i * 10, 0xFFFFFF);
    }

    private String getMobAsciiArt() {
        String lower = mobName.toLowerCase();
        if (lower.contains("zombie")) return "   ___\n  |x x|\n  |___|  \n  /| |\\\n / | | \\";
        if (lower.contains("skeleton") || lower.contains("squelette")) return "   ___\n  |o o|\n  |___|  \n  X| |X\n  /   \\";
        if (lower.contains("creeper")) return "   ___\n  |0 0|\n  |vvv|  \n  |   |\n  |___|";
        if (lower.contains("spider") || lower.contains("araign")) return " \\ _ /\n  (o.o)  \n / \\_/ \\";
        if (lower.contains("enderman")) return "   ___\n  |. .|\n  |   |  \n  |   |\n  |___|";
        if (lower.contains("slime")) return "  _____\n /     \\\n| o   o |\n \\_____/";
        return "   ___\n  |? ?|\n  |___|  \n  /| |\\\n /   \\";
    }

    private void renderHealthBars(GuiGraphics g) {
        int y = this.height / 2 - 2;
        String playerName = isFrisk ? "FRISK" : "CHARA";
        int nameColor = isFrisk ? 0xFFFF00 : 0xFF0000;
        g.drawString(this.font, playerName, boxX, y, nameColor);

        int barW = 80;
        int barX = boxX + this.font.width(playerName) + 10;
        g.drawString(this.font, "HP", barX, y, 0xFFFFFF);
        barX += 14;
        // Red background = damage taken, yellow = remaining HP
        g.fill(barX, y, barX + barW, y + 10, 0xFFCC0000);
        float ratio = utPlayerHp / UT_MAX_HP;
        g.fill(barX, y, barX + (int)(barW * Math.max(0, ratio)), y + 10, 0xFFFFFF00);
        g.drawString(this.font, (int)utPlayerHp + " / " + (int)UT_MAX_HP, barX + barW + 5, y, 0xFFFFFF);
    }

    private void renderBattleBox(GuiGraphics g, long now) {
        g.renderOutline(boxX, boxY, boxW, boxH, 0xFFFFFFFF);
        g.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1, 0xFF000000);

        // Soul
        if (phase != TurnPhase.ATTACK_ANIM) {
            int soulColor = isFrisk ? 0xFFFFFF00 : 0xFFFF0000;
            drawHeart(g, (int)soulX, (int)soulY, soulColor);
        }

        // Damage flash
        if (damageFlash && now - damageFlashStart < 150) {
            g.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1,
                    0x60FF0000);
        } else {
            damageFlash = false;
        }
    }

    private void drawHeart(GuiGraphics g, int cx, int cy, int color) {
        int s = SOUL_SIZE / 2;
        // Top bumps
        g.fill(cx - s, cy - s + 1, cx, cy - s + 3, color);
        g.fill(cx, cy - s + 1, cx + s, cy - s + 3, color);
        // Middle
        g.fill(cx - s, cy - s + 2, cx + s, cy, color);
        // Bottom point
        g.fill(cx - s + 1, cy, cx + s - 1, cy + 1, color);
        g.fill(cx - s + 2, cy + 1, cx + s - 2, cy + 2, color);
        g.fill(cx - 1, cy + 2, cx + 1, cy + 3, color);
    }

    private void renderDialog(GuiGraphics g, long now) {
        if (dialogText.isEmpty()) return;
        int dialogY = boxY - 16;
        long elapsed = now - dialogStart;
        int chars = Math.min(dialogText.length(), (int)(elapsed / 25));
        g.drawString(this.font, dialogText.substring(0, chars), boxX + 5, dialogY, 0xFFFFFF);
    }

    private void renderAttackAnimation(GuiGraphics g, long now) {
        long elapsed = now - attackAnimStart;
        int barX = boxX + 10; int barY = boxY + boxH / 2 - 25;
        int barW = boxW - 20; int barH = 50;

        g.fill(barX, barY, barX + barW, barY + barH, 0xFF000000);
        g.renderOutline(barX, barY, barW, barH, 0xFFFFFFFF);

        int centerX = barX + barW / 2;
        g.fill(centerX - 10, barY + 1, centerX + 10, barY + barH - 1, 0x4400FF00);

        double progress = (elapsed % 1500) / 1500.0;
        int sliderX = barX + (int)(progress * barW);
        g.fill(sliderX - 1, barY, sliderX + 1, barY + barH, 0xFFFFFFFF);

        g.drawCenteredString(this.font, "ESPACE pour frapper!", this.width / 2, barY - 12, 0xFFFFFF);

        if (elapsed > 3000 && !attackHit) {
            dialogText = "* MISS!";
            dialogStart = System.currentTimeMillis();
            startEnemyTurn();
        }
    }

    private void updateEnemyTurn(GuiGraphics g, long now) {
        long elapsed = now - enemyTurnStart;

        // Check if enemy turn is over -> back to player turn
        if (elapsed > ENEMY_TURN_DURATION) {
            phase = TurnPhase.PLAYER_TURN;
            // Send damage taken to server
            ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId,
                    "endturn:" + totalDamageTaken));
            totalDamageTaken = 0;

            // Check if player died during dodge
            if (utPlayerHp <= 0) {
                ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId, "playerdeath"));
                this.onClose();
                return;
            }

            setupMainButtons();
            dialogText = "* C'est ton tour.";
            dialogStart = System.currentTimeMillis();
            return;
        }

        // === GASTER BLASTERS ===
        for (int i = 0; i < blasterCount; i++) {
            if (now < blasterSpawnTime[i]) continue; // Not spawned yet

            long blasterAge = now - blasterSpawnTime[i];

            if (!blasterFiring[i]) {
                // Blaster is aiming: calculate position and angle
                if (blasterAge < 800) {
                    // Spawn at random edge, aim at soul
                    if (blasterAge < 50) {
                        // Initialize position on first frame
                        int side = (int)(Math.random() * 4);
                        switch (side) {
                            case 0 -> { blasterX[i] = boxX + Math.random() * boxW; blasterY[i] = boxY + 5; }
                            case 1 -> { blasterX[i] = boxX + Math.random() * boxW; blasterY[i] = boxY + boxH - 5; }
                            case 2 -> { blasterX[i] = boxX + 5; blasterY[i] = boxY + Math.random() * boxH; }
                            case 3 -> { blasterX[i] = boxX + boxW - 5; blasterY[i] = boxY + Math.random() * boxH; }
                        }
                    }

                    // Aim at current soul position
                    blasterAngle[i] = Math.atan2(soulY - blasterY[i], soulX - blasterX[i]);

                    // Draw Gaster Blaster head (skull shape)
                    drawGasterBlaster(g, (int)blasterX[i], (int)blasterY[i], blasterAngle[i], false);

                    // Warning line (red dashed)
                    if (blasterAge > 400) {
                        double lineLen = 200;
                        for (int d = 0; d < lineLen; d += 6) {
                            int lx = (int)(blasterX[i] + Math.cos(blasterAngle[i]) * d);
                            int ly = (int)(blasterY[i] + Math.sin(blasterAngle[i]) * d);
                            if (lx > boxX && lx < boxX + boxW && ly > boxY && ly < boxY + boxH) {
                                g.fill(lx - 1, ly - 1, lx + 1, ly + 1, 0x80FF0000);
                            }
                        }
                    }
                } else {
                    // Start firing
                    blasterFiring[i] = true;
                    blasterFireStart[i] = now;
                }
            }

            if (blasterFiring[i]) {
                long fireAge = now - blasterFireStart[i];
                if (fireAge > 600) {
                    blasterFiring[i] = false;
                    blasterSpawnTime[i] = Long.MAX_VALUE; // Done
                    continue;
                }

                // Draw firing blaster
                drawGasterBlaster(g, (int)blasterX[i], (int)blasterY[i], blasterAngle[i], true);

                // Draw beam
                double beamLen = 250;
                int beamWidth = 8;
                double cos = Math.cos(blasterAngle[i]);
                double sin = Math.sin(blasterAngle[i]);

                for (int d = 5; d < beamLen; d += 2) {
                    int bx = (int)(blasterX[i] + cos * d);
                    int by = (int)(blasterY[i] + sin * d);
                    if (bx < boxX + 1 || bx > boxX + boxW - 1 || by < boxY + 1 || by > boxY + boxH - 1)
                        continue;

                    // White beam with slight glow
                    int bw2 = beamWidth / 2;
                    int perpX = (int)(-sin * bw2);
                    int perpY = (int)(cos * bw2);
                    g.fill(bx - Math.abs(perpX), by - Math.abs(perpY),
                            bx + Math.abs(perpX) + 1, by + Math.abs(perpY) + 1, 0xFFFFFFFF);

                    // Check collision with soul - each hit = 3 damage
                    double dx = bx - soulX;
                    double dy = by - soulY;
                    if (dx * dx + dy * dy < 100 && now - lastDamageTick > 400) {
                        damageFlash = true;
                        damageFlashStart = now;
                        lastDamageTick = now;
                        utPlayerHp -= UT_HIT_DAMAGE;
                        totalDamageTaken += (int)UT_HIT_DAMAGE;
                    }
                }
            }
        }

        // === BONES ===
        for (int i = 0; i < boneCount; i++) {
            boneX[i] += boneDX[i];
            boneY[i] += boneDY[i];
            if (boneX[i] <= boxX + 3 || boneX[i] >= boxX + boxW - 3) boneDX[i] = -boneDX[i];
            if (boneY[i] <= boxY + 3 || boneY[i] >= boxY + boxH - 3) boneDY[i] = -boneDY[i];

            // Draw bone (white rectangle)
            int bx = (int)boneX[i]; int by = (int)boneY[i];
            g.fill(bx - 1, by - 5, bx + 1, by + 5, 0xFFFFFFFF);
            g.fill(bx - 3, by - 5, bx + 3, by - 3, 0xFFFFFFFF);
            g.fill(bx - 3, by + 3, bx + 3, by + 5, 0xFFFFFFFF);

            double dx = boneX[i] - soulX;
            double dy = boneY[i] - soulY;
            if (dx * dx + dy * dy < 64 && now - lastDamageTick > 400) {
                damageFlash = true;
                damageFlashStart = now;
                lastDamageTick = now;
                utPlayerHp -= UT_HIT_DAMAGE;
                totalDamageTaken += (int)UT_HIT_DAMAGE;
            }
        }

        // Timer bar
        float timeLeft = 1.0f - (float)elapsed / ENEMY_TURN_DURATION;
        int timerX = boxX + 10; int timerY = boxY + boxH + 5;
        int timerW = boxW - 20;
        g.fill(timerX, timerY, timerX + timerW, timerY + 4, 0xFF333333);
        g.fill(timerX, timerY, timerX + (int)(timerW * timeLeft), timerY + 4, 0xFFFFFF00);

        // "DODGE!" text
        g.drawCenteredString(this.font, "ESQUIVE! (WASD)", this.width / 2, boxY + boxH + 12, 0xFFFF4444);
    }

    private void drawGasterBlaster(GuiGraphics g, int x, int y, double angle, boolean firing) {
        // Gaster Blaster skull shape (simplified)
        int color = firing ? 0xFFFFFFFF : 0xFFCCCCCC;
        int eyeColor = firing ? 0xFF00FFFF : 0xFFFF8800;

        // Skull body
        g.fill(x - 6, y - 5, x + 6, y + 5, color);
        g.fill(x - 7, y - 3, x + 7, y + 3, color);
        // Eyes
        g.fill(x - 4, y - 3, x - 1, y, eyeColor);
        g.fill(x + 1, y - 3, x + 4, y, eyeColor);
        // Jaw (open when firing)
        if (firing) {
            g.fill(x - 5, y + 4, x + 5, y + 8, color);
            // Beam source glow
            g.fill(x - 3, y + 5, x + 3, y + 7, 0xFF00FFFF);
        } else {
            g.fill(x - 5, y + 4, x + 5, y + 6, color);
        }
        // Teeth
        g.fill(x - 3, y + 3, x - 2, y + 5, 0xFF000000);
        g.fill(x, y + 3, x + 1, y + 5, 0xFF000000);
        g.fill(x + 2, y + 3, x + 3, y + 5, 0xFF000000);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // SPACE during attack animation
        if (keyCode == 32 && phase == TurnPhase.ATTACK_ANIM && !attackHit) {
            attackHit = true;
            ModMessages.sendToServer(new UndertaleBattleActionC2SPacket(entityId, "attack"));
            // After attack, start enemy turn with Gaster Blasters
            startEnemyTurn();
            return true;
        }

        // Move soul during enemy turn
        if (phase == TurnPhase.ENEMY_TURN) {
            double speed = 3.5;
            switch (keyCode) {
                case 87, 265 -> soulY = Math.max(boxY + SOUL_SIZE, soulY - speed);
                case 83, 264 -> soulY = Math.min(boxY + boxH - SOUL_SIZE, soulY + speed);
                case 65, 263 -> soulX = Math.max(boxX + SOUL_SIZE, soulX - speed);
                case 68, 262 -> soulX = Math.min(boxX + boxW - SOUL_SIZE, soulX + speed);
            }
            return true;
        }

        if (keyCode == 256) return true; // Block ESC
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }
}
