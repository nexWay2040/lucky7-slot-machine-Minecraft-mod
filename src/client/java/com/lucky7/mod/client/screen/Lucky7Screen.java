package com.lucky7.mod.client.screen;

import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.config.Lucky7Config;
import com.lucky7.mod.game.probability.ProbabilityCalculator;
import com.lucky7.mod.game.probability.ProbabilityProfile;
import com.lucky7.mod.network.Lucky7ActionPayload;
import com.lucky7.mod.screen.Lucky7ScreenHandler;
import com.lucky7.mod.sound.Lucky7Sounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;

public final class Lucky7Screen extends HandledScreen<Lucky7ScreenHandler> {
    private static final int W = 356;
    private static final int H = 216;
    private int page = 0; // 0 = main, 1 = info, 2 = stats

    private ButtonWidget startBtn;
    private ButtonWidget returnBtn;
    private ButtonWidget infoBtn;
    private ButtonWidget statsBtn;

    public Lucky7Screen(Lucky7ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
    }

    private String errorMessage = null;
    private long errorTime = 0;

    @Override
    protected void init() {
        super.init();
        int btnY = y + 94;

        startBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("gui.lucky7.start"), b -> {
            if (handler.spinning()) return;
            if (handler.stakeValue() <= 0) {
                errorMessage = "Положите руды или слитки в слоты СТАВКИ!";
                errorTime = System.currentTimeMillis();
                clickSound();
                return;
            }
            errorMessage = null;
            clickSound();
            ClientPlayNetworking.send(new Lucky7ActionPayload(handler.machinePos(), 0));
            this.close(); // Automatically close GUI on start so player watches 3D reels spin!
        }).dimensions(x + 14, btnY, 52, 18).build());

        returnBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("gui.lucky7.return"), b -> {
            if (handler.spinning() || handler.stakeValue() <= 0) return;
            clickSound();
            ClientPlayNetworking.send(new Lucky7ActionPayload(handler.machinePos(), 1));
        }).dimensions(x + 68, btnY, 52, 18).build());

        infoBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("gui.lucky7.info"), b -> {
            page = (page == 1) ? 0 : 1;
            clickSound();
        }).dimensions(x + 122, btnY, 46, 18).build());

        statsBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("gui.lucky7.stats"), b -> {
            page = (page == 2) ? 0 : 2;
            clickSound();
        }).dimensions(x + 170, btnY, 68, 18).build());
    }

    private long lastClickTime = 0;
    private void clickSound() {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 250) return; // Debounce spam
        lastClickTime = now;
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(Lucky7Sounds.BUTTON_CLICK, 0.85f));
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // Intentionally left blank: custom styled titles and pool are drawn in drawBackground,
        // preventing double title rendering and displaced inventory labels.
    }

    @Override
    protected void drawSlot(DrawContext ctx, net.minecraft.screen.slot.Slot slot) {
        if (page != 0) return; // Completely hide all slot rendering when modal dialog is active!
        super.drawSlot(ctx, slot);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (page != 0 && keyCode == 256) { // ESC key closes modal overlay
            clickSound();
            page = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (startBtn != null) {
            startBtn.visible = (page == 0);
            startBtn.active = !handler.spinning();
        }
        if (returnBtn != null) {
            returnBtn.visible = (page == 0);
            returnBtn.active = !handler.spinning();
        }
        if (infoBtn != null) infoBtn.visible = (page == 0);
        if (statsBtn != null) statsBtn.visible = (page == 0);

        super.render(ctx, mouseX, mouseY, delta);

        if (page == 0) {
            drawMouseoverTooltip(ctx, mouseX, mouseY);
        } else {
            // Render modal dialog in front of everything
            ctx.getMatrices().push();
            ctx.getMatrices().translate(0.0f, 0.0f, 300.0f);
            if (page == 1) {
                drawInfoOverlay(ctx);
            } else {
                drawStatsOverlay(ctx);
            }
            ctx.getMatrices().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page != 0) {
            int ox = x + 20, oy = y + 16, ow = W - 40, oh = H - 28;
            // Click outside or anywhere in modal closes it cleanly
            clickSound();
            page = 0;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        // Main Window Background (Dark Brushed Steel + Gold Border)
        ctx.fill(x, y, x + W, y + H, 0xF2101016);
        ctx.fill(x, y, x + W, y + 2, 0xFFE0A838);
        ctx.fill(x, y + H - 2, x + W, y + H, 0xFFE0A838);
        ctx.fill(x, y, x + 2, y + H, 0xFFE0A838);
        ctx.fill(x + W - 2, y, x + W, y + H, 0xFFE0A838);

        if (page != 0) {
            // When modal overlay is active, completely skip rendering slots, frames, and gameplay panels
            return;
        }

        // Header Title
        if (handler.isFeverMode()) {
            ctx.drawText(textRenderer, Text.literal("LUCKY 7 // 🔥 FEVER MODE (" + handler.feverSpins() + " СПИНА) 🔥").formatted(Formatting.GOLD, Formatting.BOLD), x + 12, y + 8, 0xFFFFAA00, false);
        } else {
            ctx.drawText(textRenderer, Text.literal("LUCKY 7 // ПАНЕЛЬ УПРАВЛЕНИЯ").formatted(Formatting.GOLD), x + 12, y + 8, 0xFFF0C850, false);
        }
        String poolText = String.format(Locale.ROOT, "ПУЛ: %06d", handler.jackpotPool());
        ctx.drawText(textRenderer, Text.literal(poolText).formatted(Formatting.RED), x + W - 92, y + 8, 0xFFFF5555, false);

        // Section Panels
        // Left: Stake Panel (3x3 slots: x=14..74, y=24..92)
        ctx.fill(x + 14, y + 24, x + 74, y + 92, 0x55000000);
        ctx.drawText(textRenderer, Text.literal("СТАВКА").formatted(Formatting.YELLOW), x + 16, y + 26, 0xFFFFE060, false);
        drawSlotFrames(ctx, 17, 37, 3, 3);

        // Middle: Live Probability & Prize Tier Alignment Panel (x=76..174, y=24..92)
        ctx.fill(x + 76, y + 24, x + 174, y + 92, 0x44000000);
        ctx.drawText(textRenderer, Text.literal("УРОВЕНЬ // ШАНС").formatted(Formatting.YELLOW), x + 78, y + 26, 0xFFFFE060, false);
        drawProbabilities(ctx, x + 78, y + 42);

        // Right: Prize Showcase Panel (3x9 slots: x=176..344, y=24..92)
        ctx.fill(x + 176, y + 24, x + 342, y + 92, 0x55000000);
        ctx.drawText(textRenderer, Text.literal("ПРИЗОВОЙ ФОНД").formatted(Formatting.AQUA), x + 178, y + 26, 0xFF55FFFF, false);
        drawSlotFrames(ctx, 177, 37, 9, 3);

        // Stake Info or Warning
        if (errorMessage != null && (System.currentTimeMillis() - errorTime) < 3500) {
            ctx.drawText(textRenderer, Text.literal("⚠ " + errorMessage).formatted(Formatting.RED, Formatting.BOLD), x + 16, y + 114, 0xFFFF4444, false);
        } else {
            String valText = "СТАВКА: " + handler.stakeValue();
            ctx.drawText(textRenderer, Text.literal(valText).formatted(Formatting.WHITE), x + 252, y + 98, 0xFFFFFFFF, false);
        }

        // Player Inventory Panel (Centered below: x=95..261, y=116..196)
        ctx.fill(x + 95, y + 116, x + 261, y + 196, 0x33000000);
        drawSlotFrames(ctx, 96, 119, 9, 3); // main inv
        drawSlotFrames(ctx, 96, 177, 9, 1); // hotbar

        // Status or In-World Notice at bottom
        if (handler.spinning()) {
            ctx.drawText(textRenderer, Text.literal("▶ БАРАБАНЫ ВРАЩАЮТСЯ В МИРЕ...").formatted(Formatting.GOLD), x + 14, y + H - 12, 0xFFFFAA00, false);
        } else {
            ProbabilityProfile p = ProbabilityCalculator.calculate(handler.stakeValue(), Lucky7Config.INSTANCE.probabilitySettings());
            String status = "БЕЗ ПРИЗА: " + pct(p.noWin()) + "  |  ПАРА: " + pct(p.pair()) + "  |  3D-барабаны вращаются за стеклом";
            ctx.drawText(textRenderer, Text.literal(status).formatted(Formatting.GRAY), x + 14, y + H - 12, 0xFFAAAAAA, false);
        }
    }

    private void drawProbabilities(DrawContext ctx, int px, int py) {
        ProbabilityProfile p = ProbabilityCalculator.calculate(handler.stakeValue(), Lucky7Config.INSTANCE.probabilitySettings());
        // Row 0: Aligned with Jackpot showcase row (y=38..56, text at py = y + 43)
        ctx.drawText(textRenderer, Text.literal("ДЖЕКПОТ").formatted(Formatting.RED, Formatting.BOLD), px, py, 0xFFFF5555, false);
        ctx.drawText(textRenderer, Text.literal(pct(p.jackpot())).formatted(Formatting.WHITE), px + 58, py, 0xFFFFFFFF, false);

        // Row 1: Aligned with Big Win showcase row (y=56..74, text at py + 18 = y + 61)
        ctx.drawText(textRenderer, Text.literal("КРУПНЫЙ").formatted(Formatting.GOLD, Formatting.BOLD), px, py + 18, 0xFFFFAA00, false);
        ctx.drawText(textRenderer, Text.literal(pct(p.bigWin())).formatted(Formatting.WHITE), px + 58, py + 18, 0xFFFFFFFF, false);

        // Row 2: Aligned with Medium/Utility showcase row (y=74..92, text at py + 36 = y + 79)
        ctx.drawText(textRenderer, Text.literal("ОБЫЧНЫЙ").formatted(Formatting.GREEN, Formatting.BOLD), px, py + 36, 0xFF55FF55, false);
        ctx.drawText(textRenderer, Text.literal(pct(p.smallWin() + p.mediumWin())).formatted(Formatting.WHITE), px + 58, py + 36, 0xFFFFFFFF, false);
    }

    private static String pct(double val) {
        return String.format(Locale.ROOT, "%.1f%%", Math.max(0.0, val * 100.0));
    }

    private void drawSlotFrames(DrawContext ctx, int startX, int startY, int cols, int rows) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = x + startX + c * 18;
                int sy = y + startY + r * 18;
                ctx.fill(sx, sy, sx + 18, sy + 18, 0x88181822);
                ctx.fill(sx, sy, sx + 18, sy + 1, 0xFF353545);
                ctx.fill(sx, sy, sx + 1, sy + 18, 0xFF353545);
                ctx.fill(sx, sy + 17, sx + 18, sy + 18, 0xFF555566);
                ctx.fill(sx + 17, sy, sx + 18, sy + 18, 0xFF555566);
            }
        }
    }

    private void drawInfoOverlay(DrawContext ctx) {
        int ox = x, oy = y, ow = W, oh = H;
        // Completely covers entire GUI window with dark titanium & double gold border
        ctx.fill(ox, oy, ox + ow, oy + oh, 0xFF101018);
        ctx.fill(ox, oy, ox + ow, oy + 2, 0xFFE0A838);
        ctx.fill(ox, oy + oh - 2, ox + ow, oy + oh, 0xFFE0A838);
        ctx.fill(ox, oy, ox + 2, oy + oh, 0xFFE0A838);
        ctx.fill(ox + ow - 2, oy, ox + ow, oy + oh, 0xFFE0A838);

        // Header
        ctx.fill(ox + 2, oy + 2, ox + ow - 2, oy + 24, 0x44000000);
        ctx.drawText(textRenderer, Text.literal("КАК РАБОТАЕТ ИГРОВОЙ АВТОМАТ LUCKY 7").formatted(Formatting.GOLD, Formatting.BOLD), ox + 14, oy + 8, 0xFFF0C850, false);
        ctx.drawText(textRenderer, Text.literal("[X]").formatted(Formatting.RED, Formatting.BOLD), ox + ow - 22, oy + 8, 0xFFFF5555, false);

        // Content
        int textY = oy + 32;
        ctx.drawText(textRenderer, Text.literal("1. Положите ценности в 9 слотов СТАВКИ слева (руды, слитки, блоки).").formatted(Formatting.WHITE), ox + 14, textY, 0xFFEEEEEE, false);
        ctx.drawText(textRenderer, Text.literal("2. Поддерживаются: Незерит (36-324), Алмазы (16-144), Золото (8-72), Железо (4-36)...").formatted(Formatting.YELLOW), ox + 14, textY + 18, 0xFFFFFF66, false);
        ctx.drawText(textRenderer, Text.literal("3. Нажмите СТАРТ - барабаны начнут физически вращаться за стеклом прямо в мире!").formatted(Formatting.AQUA), ox + 14, textY + 36, 0xFF66FFFF, false);
        ctx.drawText(textRenderer, Text.literal("4. Можно закрыть интерфейс клавишей ESC - барабаны продолжат крутиться в мире!").formatted(Formatting.GREEN), ox + 14, textY + 54, 0xFF66FF66, false);
        ctx.drawText(textRenderer, Text.literal("5. Выигрыш автоматически зачисляется прямо в инвентарь игрока.").formatted(Formatting.WHITE), ox + 14, textY + 72, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, Text.literal("6. Красная горизонтальная линия по центру окна барабанов - выигрышная линия!").formatted(Formatting.RED), ox + 14, textY + 90, 0xFFFF6666, false);
        ctx.drawText(textRenderer, Text.literal("7. Содержимое витрины призов справа защищено от прямого извлечения.").formatted(Formatting.LIGHT_PURPLE), ox + 14, textY + 108, 0xFFFF88FF, false);

        // Footer
        ctx.fill(ox + 2, oy + oh - 22, ox + ow - 2, oy + oh - 2, 0x44000000);
        ctx.drawText(textRenderer, Text.literal("[ Кликните в любое место или нажмите ESC, чтобы вернуться в игру ]").formatted(Formatting.GRAY), ox + 14, oy + oh - 15, 0xFFAAAAAA, false);
    }

    private void drawStatsOverlay(DrawContext ctx) {
        int ox = x, oy = y, ow = W, oh = H;
        // Completely covers entire GUI window
        ctx.fill(ox, oy, ox + ow, oy + oh, 0xFF101018);
        ctx.fill(ox, oy, ox + ow, oy + 2, 0xFFE0A838);
        ctx.fill(ox, oy + oh - 2, ox + ow, oy + oh, 0xFFE0A838);
        ctx.fill(ox, oy, ox + 2, oy + oh, 0xFFE0A838);
        ctx.fill(ox + ow - 2, oy, ox + ow, oy + oh, 0xFFE0A838);

        // Header
        ctx.fill(ox + 2, oy + 2, ox + ow - 2, oy + 24, 0x44000000);
        ctx.drawText(textRenderer, Text.literal("СТАТИСТИКА ИГРОВОГО АВТОМАТА").formatted(Formatting.GOLD, Formatting.BOLD), ox + 14, oy + 8, 0xFFF0C850, false);
        ctx.drawText(textRenderer, Text.literal("[X]").formatted(Formatting.RED, Formatting.BOLD), ox + ow - 22, oy + 8, 0xFFFF5555, false);

        // Content
        int textY = oy + 32;
        ctx.drawText(textRenderer, Text.literal("Всего запусков: " + handler.spins()).formatted(Formatting.WHITE), ox + 14, textY, 0xFFEEEEEE, false);
        ctx.drawText(textRenderer, Text.literal("Всего выигрышей: " + handler.wins()).formatted(Formatting.GREEN), ox + 14, textY + 16, 0xFF66FF66, false);
        ctx.drawText(textRenderer, Text.literal("Сорвано Джекпотов: " + handler.jackpots()).formatted(Formatting.RED), ox + 14, textY + 32, 0xFFFF5555, false);
        ctx.drawText(textRenderer, Text.literal("Рекордная выплата: " + handler.bestWin() + " очков").formatted(Formatting.YELLOW), ox + 14, textY + 48, 0xFFFFFF55, false);
        ctx.drawText(textRenderer, Text.literal("Всего разыграно ставок: " + handler.totalWagered() + " очков").formatted(Formatting.AQUA), ox + 14, textY + 64, 0xFF55FFFF, false);
        ctx.drawText(textRenderer, Text.literal("Всего выплачено призов: " + handler.totalPaid() + " очков").formatted(Formatting.GOLD), ox + 14, textY + 80, 0xFFFFAA00, false);

        Lucky7BlockEntity m = handler.machine();
        if (m != null) {
            List<String> hist = m.historyLines();
            ctx.drawText(textRenderer, Text.literal("История последних вращений:").formatted(Formatting.GRAY), ox + 14, textY + 100, 0xFFAAAAAA, false);
            for (int i = 0; i < Math.min(hist.size(), 3); i++) {
                ctx.drawText(textRenderer, Text.literal("• " + hist.get(i)).formatted(Formatting.WHITE), ox + 18, textY + 114 + i * 14, 0xFFDDDDDD, false);
            }
        }

        // Footer
        ctx.fill(ox + 2, oy + oh - 22, ox + ow - 2, oy + oh - 2, 0x44000000);
        ctx.drawText(textRenderer, Text.literal("[ Кликните в любое место или нажмите ESC, чтобы вернуться в игру ]").formatted(Formatting.GRAY), ox + 14, oy + oh - 15, 0xFFAAAAAA, false);
    }
}
