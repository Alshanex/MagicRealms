package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class LoreArtifactScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/stone_tablet.png");

    private static final ResourceLocation ANTIQUE_FONT =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "antique");

    private static final int BG_WIDTH = 210;
    private static final int BG_HEIGHT = 130;

    private static final float SCALE = 2.2F;
    private static final int RENDER_WIDTH = (int) (BG_WIDTH * SCALE);
    private static final int RENDER_HEIGHT = (int) (BG_HEIGHT * SCALE);

    private static final int TEXT_X_START = 70;
    private static final int TEXT_X_END = 390;
    private static final int TEXT_Y_START = 35;
    private static final int TEXT_Y_END = 280;

    private static final int LINE_SPACING = 4;

    private final Component loreText;
    private final Component loreTitle;
    private Component styledLoreTitle;
    private List<FormattedCharSequence> cachedLines;

    private int bgLeft, bgTop;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public LoreArtifactScreen(Component loreText, Component loreTitle) {
        super(Component.translatable("screen.magic_realms.stone_tablet"));
        this.loreText = loreText;
        this.loreTitle = loreTitle;
    }

    @Override
    protected void init() {
        super.init();
        this.bgLeft = (this.width - RENDER_WIDTH) / 2;
        this.bgTop = (this.height - RENDER_HEIGHT) / 2;

        this.styledLoreTitle = this.loreTitle.copy()
                .withStyle(style -> style.withFont(ANTIQUE_FONT));

        Component styledLoreText = this.loreText.copy()
                .withStyle(style -> style.withFont(ANTIQUE_FONT));

        int textWidth = TEXT_X_END - TEXT_X_START;
        this.cachedLines = this.font.split(styledLoreText, textWidth);

        int textHeight = TEXT_Y_END - TEXT_Y_START;
        int totalLineHeight = this.font.lineHeight + LINE_SPACING;

        int visibleLines = textHeight / totalLineHeight;

        this.maxScroll = Math.max(0, this.cachedLines.size() - visibleLines);
        this.scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Render the background image
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().pushPose();

        // Move to our top-left starting position
        graphics.pose().translate(this.bgLeft, this.bgTop, 0);

        // Scale everything drawn next
        graphics.pose().scale(SCALE, SCALE, 1.0F);

        graphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        graphics.pose().popPose();

        // Calculate absolute text area coordinates
        int textLeft = this.bgLeft + TEXT_X_START;
        int textTop = this.bgTop + TEXT_Y_START;
        int textRight = this.bgLeft + TEXT_X_END;
        int textBottom = this.bgTop + TEXT_Y_END;

        int availableHeight = textBottom - textTop;
        int totalLineHeight = this.font.lineHeight + LINE_SPACING;
        int visibleLines = availableHeight / totalLineHeight;

        // Render text with scissor to ensure it doesn't bleed out of bounds
        graphics.enableScissor(textLeft, textTop, textRight, textBottom);

        int y = textTop;

        // Update the centering math if scrolling isn't needed
        if (this.maxScroll == 0) {
            int actualTextHeight = this.cachedLines.size() * totalLineHeight;
            y += (availableHeight - actualTextHeight) / 2;
        }

        // Add title
        graphics.drawString(this.font, this.styledLoreTitle, textLeft, y - 20, 0x333333, false);

        // Draw the text using the spacing
        for (int i = this.scrollOffset; i < Math.min(this.cachedLines.size(), this.scrollOffset + visibleLines); i++) {
            graphics.drawString(this.font, this.cachedLines.get(i), textLeft, y, 0x333333, false);
            y += totalLineHeight;
        }

        graphics.disableScissor();

        // Render scroll indicators if necessary
        if (this.maxScroll > 0) {
            int indicatorX = textRight + 5;
            if (this.scrollOffset > 0) {
                graphics.drawString(this.font, "▲", indicatorX, textTop, 0x555555, false);
            }
            if (this.scrollOffset < this.maxScroll) {
                graphics.drawString(this.font, "▼", indicatorX, textBottom - this.font.lineHeight, 0x555555, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll > 0) {
            if (scrollY > 0 && this.scrollOffset > 0) {
                this.scrollOffset--;
                return true;
            } else if (scrollY < 0 && this.scrollOffset < this.maxScroll) {
                this.scrollOffset++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
