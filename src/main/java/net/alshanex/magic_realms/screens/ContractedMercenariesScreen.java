package net.alshanex.magic_realms.screens;

import net.alshanex.magic_realms.data.ContractedMercenaryEntry;
import net.alshanex.magic_realms.network.TeleportMercenaryPacket;
import net.alshanex.magic_realms.util.humans.mercenaries.ClientContractedMercenaries;
import net.alshanex.magic_realms.setup.KeyMappings;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenaryFaceDrawer;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenaryMessageFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lists the player's contracted mercenaries <b>in their current dimension</b> as cards (face, name, class, health) and lets
 * them teleport the selected one.
 */
@OnlyIn(Dist.CLIENT)
public class ContractedMercenariesScreen extends Screen {

    private static final int PANEL_W = 240;
    private static final int PAD = 8;
    private static final int HEADER_H = 16;
    private static final int CARD_H = 40;
    private static final int CARD_GAP = 4;
    private static final int VISIBLE_CARDS = 5;
    private static final int FACE_SIZE = 32;
    private static final int BUTTON_H = 20;
    private static final int SCROLLBAR_W = 4;
    private static final int HEALTH_BAR_W = 70;
    private static final int STRIDE = CARD_H + CARD_GAP;

    private static final int COLOR_PANEL = 0xE0101018;
    private static final int COLOR_PANEL_BORDER = 0xFF5A4A2A;
    private static final int COLOR_ACCENT = 0xFFE0C060;

    private int left, top, panelH;
    private int listX, listY, listW, listH;
    private double scrollOffset;
    @Nullable private UUID selected;
    private Button teleportButton;

    public ContractedMercenariesScreen() {
        super(Component.translatable("gui.magic_realms.mercenaries.title"));
    }

    @Override
    protected void init() {
        listH = VISIBLE_CARDS * STRIDE - CARD_GAP;
        panelH = PAD + HEADER_H + listH + PAD + BUTTON_H + PAD;
        left = (this.width - PANEL_W) / 2;
        top = (this.height - panelH) / 2;

        listX = left + PAD;
        listY = top + PAD + HEADER_H;
        listW = PANEL_W - PAD * 2;

        teleportButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.magic_realms.mercenaries.teleport"), b -> teleportSelected())
                .bounds(left + PAD, listY + listH + PAD, PANEL_W - PAD * 2, BUTTON_H)
                .build());
        updateButtonState();
    }

    // Data

    private List<ContractedMercenaryEntry> entries() {
        LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) return List.of();
        long now = player.level().getGameTime();
        return ClientContractedMercenaries.get().stream()
                .filter(e -> !e.isExpired(now)) // hide instantly; the server prunes within a few seconds
                .toList();
    }

    private void updateButtonState() {
        if (selected != null && entries().stream().noneMatch(e -> e.uuid().equals(selected))) {
            selected = null; // selected mercenary died / expired while the screen was open
        }
        teleportButton.active = selected != null;
    }

    private void teleportSelected() {
        if (selected == null) return;
        PacketDistributor.sendToServer(new TeleportMercenaryPacket(selected));
        onClose();
    }

    @Override
    public void tick() {
        super.tick();
        updateButtonState();
    }

    // Rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + PANEL_W, top + panelH, COLOR_PANEL);
        g.renderOutline(left, top, PANEL_W, panelH, COLOR_PANEL_BORDER);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // background + button

        g.drawCenteredString(font, this.title, left + PANEL_W / 2, top + PAD, COLOR_ACCENT);

        List<ContractedMercenaryEntry> list = entries();
        clampScroll(list.size());

        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.magic_realms.mercenaries.empty"),
                    left + PANEL_W / 2, listY + listH / 2 - 4, 0xFF888888);
            return;
        }

        boolean scrollable = contentHeight(list.size()) > listH;
        int cardW = listW - (scrollable ? SCROLLBAR_W + 2 : 0);
        long now = this.minecraft.player.level().getGameTime();

        g.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < list.size(); i++) {
            int y = listY + i * STRIDE - (int) scrollOffset;
            if (y + CARD_H < listY || y > listY + listH) continue;

            boolean hovered = mouseX >= listX && mouseX < listX + cardW
                    && mouseY >= Math.max(y, listY) && mouseY < Math.min(y + CARD_H, listY + listH);
            ContractedMercenaryEntry e = list.get(i);
            renderCard(g, e, listX, y, cardW, hovered, e.uuid().equals(selected), now);
        }
        g.disableScissor();

        if (scrollable) renderScrollbar(g, list.size());
    }

    private void renderCard(GuiGraphics g, ContractedMercenaryEntry e, int x, int y, int w, boolean hovered, boolean isSelected, long now) {
        int bg = isSelected ? 0xFF3B3424 : hovered ? 0xFF2C2C38 : 0xFF22222C;
        g.fill(x, y, x + w, y + CARD_H, bg);
        if (isSelected) g.renderOutline(x, y, w, CARD_H, COLOR_ACCENT);

        // Face
        int faceX = x + 4;
        int faceY = y + (CARD_H - FACE_SIZE) / 2;
        g.fill(faceX - 1, faceY - 1, faceX + FACE_SIZE + 1, faceY + FACE_SIZE + 1, 0xFF000000);
        MercenaryFaceDrawer.draw(g, e.face(), faceX, faceY, FACE_SIZE);

        int tx = faceX + FACE_SIZE + 6;
        int right = x + w - 4;

        // Contract tag (top-right)
        Component tag = e.permanent()
                ? Component.translatable("gui.magic_realms.mercenaries.permanent").withStyle(ChatFormatting.GOLD)
                : Component.translatable("gui.magic_realms.mercenaries.remaining", remainingMinutes(e, now))
                .withStyle(ChatFormatting.GRAY);
        int tagW = font.width(tag);
        g.drawString(font, tag, right - tagW, y + 4, 0xFFFFFFFF, false);

        // Name (same per-mercenary color as chat)
        Integer rgb = MercenaryMessageFormatter.colorFor(e.uuid()).getColor();
        String name = font.plainSubstrByWidth(e.name(), Math.max(0, right - tagW - 4 - tx));
        g.drawString(font, name, tx, y + 4, (rgb == null ? 0xFFFFFF : rgb) | 0xFF000000, true);

        // Combat class
        List<FormattedCharSequence> classLines = font.split(e.combatClass(), Math.max(1, right - tx));
        if (!classLines.isEmpty()) g.drawString(font, classLines.get(0), tx, y + 15, 0xFFAAAAAA, false);

        // Health bar + numbers
        float ratio = e.maxHealth() > 0 ? Mth.clamp(e.health() / e.maxHealth(), 0f, 1f) : 0f;
        int barY = y + 29;
        g.fill(tx, barY, tx + HEALTH_BAR_W, barY + 5, 0xFF000000);
        g.fill(tx + 1, barY + 1, tx + 1 + (int) ((HEALTH_BAR_W - 2) * ratio), barY + 4, healthColor(ratio));
        g.drawString(font, formatHp(e.health()) + " / " + formatHp(e.maxHealth()),
                tx + HEALTH_BAR_W + 4, y + 27, 0xFFDDDDDD, false);
    }

    private void renderScrollbar(GuiGraphics g, int count) {
        int x = listX + listW - SCROLLBAR_W;
        g.fill(x, listY, x + SCROLLBAR_W, listY + listH, 0xFF15151C);

        int content = contentHeight(count);
        int thumbH = Math.max(12, listH * listH / content);
        double maxScroll = content - listH;
        int thumbY = listY + (int) ((listH - thumbH) * (scrollOffset / maxScroll));
        g.fill(x, thumbY, x + SCROLLBAR_W, thumbY + thumbH, 0xFF6A6A7A);
    }

    // Input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (button == 0 && mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            int rel = (int) (mouseY - listY + scrollOffset);
            if (rel % STRIDE < CARD_H) {
                int index = rel / STRIDE;
                List<ContractedMercenaryEntry> list = entries();
                if (index >= 0 && index < list.size()) {
                    selected = list.get(index).uuid();
                    updateButtonState();
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= scrollY * 16;
        clampScroll(entries().size());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Helpers

    private int contentHeight(int count) {
        return Math.max(0, count * STRIDE - CARD_GAP);
    }

    private void clampScroll(int count) {
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, contentHeight(count) - listH));
    }

    private static long remainingMinutes(ContractedMercenaryEntry e, long now) {
        long ticks = Math.max(0, e.expiresAt() - now);
        return (ticks + 1199) / 1200; // round up so "0 min" never shows on a live contract
    }

    private static int healthColor(float ratio) {
        if (ratio > 0.5f) return 0xFF4CC24C;
        if (ratio > 0.25f) return 0xFFE0C040;
        return 0xFFD04040;
    }

    private static String formatHp(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}