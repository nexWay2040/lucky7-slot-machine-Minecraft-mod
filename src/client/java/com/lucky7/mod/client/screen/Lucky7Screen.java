package com.lucky7.mod.client.screen;

import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.config.Lucky7Config;
import com.lucky7.mod.game.probability.ProbabilityCalculator;
import com.lucky7.mod.game.probability.ProbabilityProfile;
import com.lucky7.mod.network.Lucky7ActionPayload;
import com.lucky7.mod.screen.Lucky7ScreenHandler;
import com.lucky7.mod.sound.Lucky7Sounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Locale;

public final class Lucky7Screen extends AbstractContainerScreen<Lucky7ScreenHandler> {
    private static final int W = 356;
    private static final int H = 216;
    private int page = 0; // 0 = main, 1 = info, 2 = stats

    private Button startBtn;
    private Button freeSpinBtn;
    private Button returnBtn;
    private Button infoBtn;
    private Button statsBtn;

    private String errorMessage = null;
    private long errorTime = 0;
    private long lastClickTime = 0;

    public Lucky7Screen(Lucky7ScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int btnY = topPos + 94;

        startBtn = addRenderableWidget(Button.builder(Component.translatable("gui.lucky7.start"), b -> {
            if (menu.spinning()) return;
            if (menu.stakeValue() <= 0 && !menu.isFeverMode()) {
                errorMessage = "Положите руды, слитки или фишки в слоты СТАВКИ!";
                errorTime = System.currentTimeMillis();
                clickSound();
                return;
            }
            errorMessage = null;
            clickSound();
            ClientPlayNetworking.send(new Lucky7ActionPayload(menu.machinePos(), 0));
            this.onClose();
        }).bounds(leftPos + 14, btnY, 52, 18).build());

        freeSpinBtn = addRenderableWidget(Button.builder(Component.literal("🔥 БЕСПЛАТНЫЙ СПИН 🔥"), b -> {
            if (menu.spinning()) return;
            errorMessage = null;
            clickSound();
            ClientPlayNetworking.send(new Lucky7ActionPayload(menu.machinePos(), 0));
            this.onClose();
        }).bounds(leftPos + 14, btnY, 106, 18).build());

        returnBtn = addRenderableWidget(Button.builder(Component.translatable("gui.lucky7.return"), b -> {
            if (menu.spinning() || menu.stakeValue() <= 0) return;
            clickSound();
            ClientPlayNetworking.send(new Lucky7ActionPayload(menu.machinePos(), 1));
        }).bounds(leftPos + 68, btnY, 52, 18).build());

        infoBtn = addRenderableWidget(Button.builder(Component.translatable("gui.lucky7.info"), b -> {
            page = (page == 1) ? 0 : 1;
            clickSound();
        }).bounds(leftPos + 122, btnY, 46, 18).build());

        statsBtn = addRenderableWidget(Button.builder(Component.translatable("gui.lucky7.stats"), b -> {
            page = (page == 2) ? 0 : 2;
            clickSound();
        }).bounds(leftPos + 170, btnY, 68, 18).build());
    }

    private void clickSound() {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 250) return;
        lastClickTime = now;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Lucky7Sounds.BUTTON_CLICK, 1.0f, 0.85f));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        // Custom styled titles and pool are drawn in extractBackground
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (page != 0) return;
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (page != 0 && event.key() == 256) {
            clickSound();
            page = 0;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (page != 0) {
            clickSound();
            page = 0;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean fever = menu.isFeverMode();
        if (freeSpinBtn != null) {
            freeSpinBtn.visible = (page == 0 && fever);
            freeSpinBtn.active = !menu.spinning();
            if (fever) {
                freeSpinBtn.setMessage(Component.literal("🔥 БЕСПЛАТНЫЙ СПИН (" + menu.feverSpins() + ") 🔥").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }
        if (startBtn != null) {
            startBtn.visible = (page == 0 && !fever);
            startBtn.active = !menu.spinning();
        }
        if (returnBtn != null) {
            returnBtn.visible = (page == 0 && !fever);
            returnBtn.active = !menu.spinning();
        }
        if (infoBtn != null) infoBtn.visible = (page == 0);
        if (statsBtn != null) statsBtn.visible = (page == 0);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        if (page != 0) {
            graphics.nextStratum();
            if (page == 1) {
                drawInfoOverlay(graphics);
            } else {
                drawStatsOverlay(graphics);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float a) {
        super.extractBackground(ctx, mouseX, mouseY, a);
        int x = leftPos;
        int y = topPos;

        // Main Window Background (Dark Brushed Steel + Gold Border)
        ctx.fill(x, y, x + W, y + H, 0xF2101016);
        ctx.fill(x, y, x + W, y + 2, 0xFFE0A838);
        ctx.fill(x, y + H - 2, x + W, y + H, 0xFFE0A838);
        ctx.fill(x, y, x + 2, y + H, 0xFFE0A838);
        ctx.fill(x + W - 2, y, x + W, y + H, 0xFFE0A838);

        if (page != 0) {
            return;
        }

        // Header Title
        if (menu.isFeverMode()) {
            ctx.text(this.font, Component.literal("LUCKY 7 // 🔥 FEVER MODE (" + menu.feverSpins() + " СПИНА) 🔥").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), x + 12, y + 8, 0xFFFFAA00, false);
        } else {
            ctx.text(this.font, Component.literal("LUCKY 7 // ПАНЕЛЬ УПРАВЛЕНИЯ СИНДИКАТА").withStyle(ChatFormatting.GOLD), x + 12, y + 8, 0xFFF0C850, false);
        }
        String poolText = String.format(Locale.ROOT, "ПУЛ: %06d", menu.jackpotPool());
        ctx.text(this.font, Component.literal(poolText).withStyle(ChatFormatting.RED), x + W - 92, y + 8, 0xFFFF5555, false);

        // Left: Stake Panel (3x3 slots: x=14..74, y=24..92)
        ctx.fill(x + 14, y + 24, x + 74, y + 92, 0x55000000);
        ctx.text(this.font, Component.literal("СТАВКА").withStyle(ChatFormatting.YELLOW), x + 16, y + 26, 0xFFFFE060, false);
        drawSlotFrames(ctx, 17, 37, 3, 3);

        // Middle: Live Probability & Prize Tier Alignment Panel (x=76..174, y=24..92)
        ctx.fill(x + 76, y + 24, x + 174, y + 92, 0x44000000);
        ctx.text(this.font, Component.literal("УРОВЕНЬ // ШАНС").withStyle(ChatFormatting.YELLOW), x + 78, y + 26, 0xFFFFE060, false);
        drawProbabilities(ctx, x + 78, y + 42);

        // Right: Prize Showcase Panel (3x9 slots: x=176..344, y=24..92)
        ctx.fill(x + 176, y + 24, x + 342, y + 92, 0x55000000);
        ctx.text(this.font, Component.literal("ПРИЗОВОЙ ФОНД").withStyle(ChatFormatting.AQUA), x + 178, y + 26, 0xFF55FFFF, false);
        drawSlotFrames(ctx, 177, 37, 9, 3);

        // Stake Info or Warning
        if (errorMessage != null && (System.currentTimeMillis() - errorTime) < 3500) {
            ctx.text(this.font, Component.literal("⚠ " + errorMessage).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), x + 16, y + 114, 0xFFFF4444, false);
        } else {
            if (menu.isFeverMode()) {
                String valText = "🔥 СПИНОВ: " + menu.feverSpins();
                ctx.text(this.font, Component.literal(valText).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), x + 242, y + 98, 0xFFFFD700, false);
            } else {
                String valText = "СТАВКА: " + menu.stakeValue();
                ctx.text(this.font, Component.literal(valText).withStyle(ChatFormatting.WHITE), x + 252, y + 98, 0xFFFFFFFF, false);
            }
        }

        // Player Inventory Panel (Centered below: x=95..261, y=116..196)
        ctx.fill(x + 95, y + 116, x + 261, y + 196, 0x33000000);
        drawSlotFrames(ctx, 96, 119, 9, 3);
        drawSlotFrames(ctx, 96, 177, 9, 1);

        // Status or In-World Notice at bottom
        if (menu.spinning()) {
            ctx.text(this.font, Component.literal("▶ БАРАБАНЫ ВРАЩАЮТСЯ В МИРЕ...").withStyle(ChatFormatting.GOLD), x + 14, y + H - 12, 0xFFFFAA00, false);
        } else {
            ProbabilityProfile p = ProbabilityCalculator.calculate(menu.stakeValue(), Lucky7Config.INSTANCE.probabilitySettings());
            String status = "БЕЗ ПРИЗА: " + pct(p.noWin()) + "  |  ПАРА: " + pct(p.pair()) + "  |  3D-барабаны вращаются за стеклом";
            ctx.text(this.font, Component.literal(status).withStyle(ChatFormatting.GRAY), x + 14, y + H - 12, 0xFFAAAAAA, false);
        }
    }

    private void drawProbabilities(GuiGraphicsExtractor ctx, int px, int py) {
        ProbabilityProfile p = ProbabilityCalculator.calculate(menu.stakeValue(), Lucky7Config.INSTANCE.probabilitySettings());
        ctx.text(this.font, Component.literal("ДЖЕКПОТ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), px, py, 0xFFFF5555, false);
        ctx.text(this.font, Component.literal(pct(p.jackpot())).withStyle(ChatFormatting.WHITE), px + 58, py, 0xFFFFFFFF, false);

        ctx.text(this.font, Component.literal("КРУПНЫЙ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), px, py + 18, 0xFFFFAA00, false);
        ctx.text(this.font, Component.literal(pct(p.bigWin())).withStyle(ChatFormatting.WHITE), px + 58, py + 18, 0xFFFFFFFF, false);

        ctx.text(this.font, Component.literal("ОБЫЧНЫЙ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), px, py + 36, 0xFF55FF55, false);
        ctx.text(this.font, Component.literal(pct(p.smallWin() + p.mediumWin())).withStyle(ChatFormatting.WHITE), px + 58, py + 36, 0xFFFFFFFF, false);
    }

    private static String pct(double val) {
        return String.format(Locale.ROOT, "%.1f%%", Math.max(0.0, val * 100.0));
    }

    private void drawSlotFrames(GuiGraphicsExtractor ctx, int startX, int startY, int cols, int rows) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = leftPos + startX + c * 18;
                int sy = topPos + startY + r * 18;
                ctx.fill(sx, sy, sx + 18, sy + 18, 0x88181822);
                ctx.fill(sx, sy, sx + 18, sy + 1, 0xFF353545);
                ctx.fill(sx, sy, sx + 1, sy + 18, 0xFF353545);
                ctx.fill(sx, sy + 17, sx + 18, sy + 18, 0xFF555566);
                ctx.fill(sx + 17, sy, sx + 18, sy + 18, 0xFF555566);
            }
        }
    }

    private void drawInfoOverlay(GuiGraphicsExtractor ctx) {
        int ox = leftPos, oy = topPos, ow = W, oh = H;
        ctx.fill(ox, oy, ox + ow, oy + oh, 0xFF101018);
        ctx.fill(ox, oy, ox + ow, oy + 2, 0xFFE0A838);
        ctx.fill(ox, oy + oh - 2, ox + ow, oy + oh, 0xFFE0A838);
        ctx.fill(ox, oy, ox + 2, oy + oh, 0xFFE0A838);
        ctx.fill(ox + ow - 2, oy, ox + ow, oy + oh, 0xFFE0A838);

        ctx.fill(ox + 2, oy + 2, ox + ow - 2, oy + 24, 0x44000000);
        ctx.text(this.font, Component.literal("КАК РАБОТАЕТ КАЗИНО LUCKY 7 (ВЕРСИЯ 2.0)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), ox + 14, oy + 8, 0xFFF0C850, false);
        ctx.text(this.font, Component.literal("[X]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), ox + ow - 22, oy + 8, 0xFFFF5555, false);

        int textY = oy + 32;
        ctx.text(this.font, Component.literal("1. Положите ценности или Золотые Фишки в 9 слотов СТАВКИ слева.").withStyle(ChatFormatting.WHITE), ox + 14, textY, 0xFFEEEEEE, false);
        ctx.text(this.font, Component.literal("2. Поддерживаются: Фишки (64), Монеты (12), Незерит, Алмазы, Золото, Железо...").withStyle(ChatFormatting.YELLOW), ox + 14, textY + 18, 0xFFFFFF66, false);
        ctx.text(this.font, Component.literal("3. Секрет Босса: Королевский VIP Кубок даёт бонус шанса и не тратится!").withStyle(ChatFormatting.AQUA), ox + 14, textY + 36, 0xFF66FFFF, false);
        ctx.text(this.font, Component.literal("4. При выпадении 3 Клеверов или 3 Звёзд открывается кнопка БЕСПЛАТНЫЙ СПИН!").withStyle(ChatFormatting.GREEN), ox + 14, textY + 54, 0xFF66FF66, false);
        ctx.text(this.font, Component.literal("5. Выигрыши эффектно вылетают физическими предметами из нижнего лотка!").withStyle(ChatFormatting.WHITE), ox + 14, textY + 72, 0xFFFFFFFF, false);
        ctx.text(this.font, Component.literal("6. Не хватает алмазов? Активируйте Контракт Дона Криперионе!").withStyle(ChatFormatting.RED), ox + 14, textY + 90, 0xFFFF6666, false);
        ctx.text(this.font, Component.literal("7. Также испытайте 3D Колесо Риска и Стол Шулера (Игра в Напёрстки)!").withStyle(ChatFormatting.LIGHT_PURPLE), ox + 14, textY + 108, 0xFFFF88FF, false);

        ctx.fill(ox + 2, oy + oh - 22, ox + ow - 2, oy + oh - 2, 0x44000000);
        ctx.text(this.font, Component.literal("[ Кликните в любое место или нажмите ESC, чтобы вернуться в игру ]").withStyle(ChatFormatting.GRAY), ox + 14, oy + oh - 15, 0xFFAAAAAA, false);
    }

    private void drawStatsOverlay(GuiGraphicsExtractor ctx) {
        int ox = leftPos, oy = topPos, ow = W, oh = H;
        ctx.fill(ox, oy, ox + ow, oy + oh, 0xFF101018);
        ctx.fill(ox, oy, ox + ow, oy + 2, 0xFFE0A838);
        ctx.fill(ox, oy + oh - 2, ox + ow, oy + oh, 0xFFE0A838);
        ctx.fill(ox, oy, ox + 2, oy + oh, 0xFFE0A838);
        ctx.fill(ox + ow - 2, oy, ox + ow, oy + oh, 0xFFE0A838);

        ctx.fill(ox + 2, oy + 2, ox + ow - 2, oy + 24, 0x44000000);
        ctx.text(this.font, Component.literal("СТАТИСТИКА ИГРОВОГО АВТОМАТА").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), ox + 14, oy + 8, 0xFFF0C850, false);
        ctx.text(this.font, Component.literal("[X]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), ox + ow - 22, oy + 8, 0xFFFF5555, false);

        int textY = oy + 32;
        ctx.text(this.font, Component.literal("Всего запусков: " + menu.spins()).withStyle(ChatFormatting.WHITE), ox + 14, textY, 0xFFEEEEEE, false);
        ctx.text(this.font, Component.literal("Всего выигрышей: " + menu.wins()).withStyle(ChatFormatting.GREEN), ox + 14, textY + 16, 0xFF66FF66, false);
        ctx.text(this.font, Component.literal("Сорвано Джекпотов: " + menu.jackpots()).withStyle(ChatFormatting.RED), ox + 14, textY + 32, 0xFFFF5555, false);
        ctx.text(this.font, Component.literal("Рекордная выплата: " + menu.bestWin() + " очков").withStyle(ChatFormatting.YELLOW), ox + 14, textY + 48, 0xFFFFFF55, false);
        ctx.text(this.font, Component.literal("Всего разыграно ставок: " + menu.totalWagered() + " очков").withStyle(ChatFormatting.AQUA), ox + 14, textY + 64, 0xFF55FFFF, false);
        ctx.text(this.font, Component.literal("Всего выплачено призов: " + menu.totalPaid() + " очков").withStyle(ChatFormatting.GOLD), ox + 14, textY + 80, 0xFFFFAA00, false);

        Lucky7BlockEntity m = menu.machine();
        if (m != null) {
            List<String> hist = m.historyLines();
            ctx.text(this.font, Component.literal("История последних вращений:").withStyle(ChatFormatting.GRAY), ox + 14, textY + 100, 0xFFAAAAAA, false);
            for (int i = 0; i < Math.min(hist.size(), 3); i++) {
                ctx.text(this.font, Component.literal("• " + hist.get(i)).withStyle(ChatFormatting.WHITE), ox + 18, textY + 114 + i * 14, 0xFFDDDDDD, false);
            }
        }

        ctx.fill(ox + 2, oy + oh - 22, ox + ow - 2, oy + oh - 2, 0x44000000);
        ctx.text(this.font, Component.literal("[ Кликните в любое место или нажмите ESC, чтобы вернуться в игру ]").withStyle(ChatFormatting.GRAY), ox + 14, oy + oh - 15, 0xFFAAAAAA, false);
    }
}
