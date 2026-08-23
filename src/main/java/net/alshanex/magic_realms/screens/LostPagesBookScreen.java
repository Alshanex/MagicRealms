package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.alshanex.magic_realms.story.BossStory;
import net.alshanex.magic_realms.util.chimera.EntityAttributeHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The Lost Pages Book GUI screen.
 * <p>
 * Uses an open-book background with two "pages" visible at once.
 * Layout is margin-based: the left page is inset from the book's left edge,
 * the right page is inset from the book's right edge, and both stop at the
 * central spine. This makes positioning resilient to texture size changes
 * and GUI scale.
 * <p>
 * Navigation:
 * - Click the left outer margin to go to the previous page spread.
 * - Click the right outer margin to go to the next page spread.
 * - Click the bookmark at the top center (spine) to go back one navigation level.
 * <p>
 * Three navigation levels:
 * <ol>
 *   <li><b>Story List</b> — Two stories at a time, one per page.</li>
 *   <li><b>Chapter View</b> — Two chapters at a time, one per page.</li>
 *   <li><b>Page View</b> — Content entries two at a time (text or image).</li>
 * </ol>
 */
public class LostPagesBookScreen extends Screen {
    private static final ResourceLocation BOOK_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/book_background.png");

    // Book texture dimensions
    private static final int BOOK_WIDTH = 420;
    private static final int BOOK_HEIGHT = 261;

    // Margins (from the page's own outer edge inward)
    private static final int OUTER_MARGIN = 40; // margin from the book's outer edge to usable content
    private static final int SPINE_MARGIN = 25; // margin from the center spine to usable content
    private static final int TOP_MARGIN = 25; // top content margin
    private static final int BOTTOM_MARGIN = 25; // bottom margin

    // Bookmark hit area
    // The bookmark graphic spans ~14px to each side of the spine, at the very top of the book
    private static final int BOOKMARK_HALF_WIDTH = 14;
    private static final int BOOKMARK_HEIGHT = 30; // clickable height from top of book

    // Button dimensions
    private static final int BTN_WIDTH = 60;
    private static final int BTN_HEIGHT = 16;

    // Entity preview for story list
    private static final int ENTITY_PREVIEW_SIZE = 120;

    private final ItemStack bookStack;
    private final PlayerLoreProgress progress;
    private final List<StoryEntry> stories;

    // View Modes
    private enum ViewMode { STORY_LIST, CHAPTER_VIEW, PAGE_VIEW }
    private ViewMode viewMode = ViewMode.STORY_LIST;

    @Nullable private StoryEntry selectedStory;
    private int selectedChapterIndex = 0;

    // For story/chapter view: index of the first item on the current two-page spread (always even)
    private int spreadStartIndex = 0;
    // For page view: index of the first content entry on the current spread (always even)
    private int contentSpreadStart = 0;

    // Text scroll per visible page side (0=left, 1=right)
    private final int[] textScroll = {0, 0};
    private final int[] maxTextScroll = {0, 0};

    // Cached entity instances for 3D rendering
    private final Map<ResourceLocation, LivingEntity> entityCache = new HashMap<>();

    // Computed layout (set in init)
    private int bookLeft, bookTop;
    // Left page usable content area (absolute screen coords)
    private int leftPageX, leftPageRight;
    // Right page usable content area (absolute screen coords)
    private int rightPageX, rightPageRight;
    // Vertical content area (same for both pages)
    private int pageContentTop, pageContentBottom;

    // Navigation margin click areas (absolute screen coords)
    private int leftMarginLeft, leftMarginRight, marginTop, marginBottom;
    private int rightMarginLeft, rightMarginRight;

    // Bookmark click area (absolute screen coords)
    private int bookmarkLeft, bookmarkRight, bookmarkTop, bookmarkBottom;

    // "Open" button hit areas (up to 2 on screen, set per frame)
    private final int[] openBtnX = new int[2];
    private final int[] openBtnY = new int[2];
    private final boolean[] openBtnVisible = {false, false};

    private record StoryEntry(ResourceLocation id, BossStory story, int collected, boolean complete) {}

    public LostPagesBookScreen(ItemStack bookStack) {
        super(Component.translatable("screen.magic_realms.book"));
        this.bookStack = bookStack;

        Player player = Minecraft.getInstance().player;
        this.progress = player != null ? player.getData(MRDataAttachments.PLAYER_LORE) : PlayerLoreProgress.EMPTY;

        this.stories = new ArrayList<>();
        for (var entry : MRRegistries.BOSS_STORIES.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            BossStory story = entry.getValue();
            int collected = progress.getCollectedCount(id);
            boolean complete = progress.isStoryComplete(id);
            stories.add(new StoryEntry(id, story, collected, complete));
        }
    }

    @Override
    protected void init() {
        super.init();
        bookLeft = (this.width - BOOK_WIDTH) / 2;
        bookTop = (this.height - BOOK_HEIGHT) / 2;

        int spineX = bookLeft + BOOK_WIDTH / 2; // center of the book = spine

        // Left page: from (bookLeft + OUTER_MARGIN) to (spineX - SPINE_MARGIN)
        leftPageX = bookLeft + OUTER_MARGIN;
        leftPageRight = spineX - SPINE_MARGIN;

        // Right page: from (spineX + SPINE_MARGIN) to (bookLeft + BOOK_WIDTH - OUTER_MARGIN)
        rightPageX = spineX + SPINE_MARGIN;
        rightPageRight = bookLeft + BOOK_WIDTH - OUTER_MARGIN;

        // Vertical bounds (shared)
        pageContentTop = bookTop + TOP_MARGIN;
        pageContentBottom = bookTop + BOOK_HEIGHT - BOTTOM_MARGIN;

        // Left outer margin clickable area (the strip between book edge and content)
        leftMarginLeft = bookLeft;
        leftMarginRight = leftPageX;
        marginTop = bookTop;
        marginBottom = bookTop + BOOK_HEIGHT;

        // Right outer margin clickable area
        rightMarginLeft = rightPageRight;
        rightMarginRight = bookLeft + BOOK_WIDTH;

        // Bookmark clickable area at the top center
        bookmarkLeft = spineX - BOOKMARK_HALF_WIDTH;
        bookmarkRight = spineX + BOOKMARK_HALF_WIDTH;
        bookmarkTop = bookTop;
        bookmarkBottom = bookTop + BOOKMARK_HEIGHT;
    }

    //  Page geometry helpers

    /** Returns the left edge X of the usable content area for a page side (0=left, 1=right). */
    private int pageX(int side) { return side == 0 ? leftPageX : rightPageX; }

    /** Returns the right edge X of the usable content area for a page side. */
    private int pageRight(int side) { return side == 0 ? leftPageRight : rightPageRight; }

    /** Returns the usable width for a page side. */
    private int pageWidth(int side) { return pageRight(side) - pageX(side); }

    /** Returns the horizontal center of a page side. */
    private int pageCenterX(int side) { return pageX(side) + pageWidth(side) / 2; }

    //  No-shadow centered text helpers

    private void drawCenteredNoShadow(GuiGraphics graphics, Component text, int cx, int y, int color) {
        graphics.drawString(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    private void drawCenteredNoShadow(GuiGraphics graphics, FormattedCharSequence text, int cx, int y, int color) {
        graphics.drawString(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    private void drawCenteredNoShadow(GuiGraphics graphics, String text, int cx, int y, int color) {
        graphics.drawString(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    //  Rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BOOK_BACKGROUND, bookLeft, bookTop, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);

        switch (viewMode) {
            case STORY_LIST -> renderStoryList(graphics, mouseX, mouseY);
            case CHAPTER_VIEW -> renderChapterView(graphics, mouseX, mouseY);
            case PAGE_VIEW -> renderPageView(graphics, mouseX, mouseY);
        }

        // Render navigation hover indicators on top of content
        renderNavigationHints(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderNavigationHints(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hasPrev = hasPreviousPage();
        boolean hasNext = hasNextPage();

        boolean leftHovered = mouseX >= leftMarginLeft && mouseX < leftMarginRight
                && mouseY >= marginTop && mouseY < marginBottom;
        boolean rightHovered = mouseX >= rightMarginLeft && mouseX < rightMarginRight
                && mouseY >= marginTop && mouseY < marginBottom;

        // Left margin hover - show a subtle "‹" arrow indicator
        if (leftHovered && hasPrev) {
            int arrowX = leftMarginLeft + (leftMarginRight - leftMarginLeft) / 2;
            int arrowY = bookTop + BOOK_HEIGHT / 2;
            drawCenteredNoShadow(graphics, "\u2039", arrowX, arrowY - font.lineHeight / 2, 0x8B7355);
        }

        // Right margin hover - show a subtle "›" arrow indicator
        if (rightHovered && hasNext) {
            int arrowX = rightMarginLeft + (rightMarginRight - rightMarginLeft) / 2;
            int arrowY = bookTop + BOOK_HEIGHT / 2;
            drawCenteredNoShadow(graphics, "\u203A", arrowX, arrowY - font.lineHeight / 2, 0x8B7355);
        }
    }

    private boolean hasPreviousPage() {
        return switch (viewMode) {
            case STORY_LIST, CHAPTER_VIEW -> spreadStartIndex > 0;
            case PAGE_VIEW -> contentSpreadStart > 0;
        };
    }

    private boolean hasNextPage() {
        return switch (viewMode) {
            case STORY_LIST -> spreadStartIndex + 2 < stories.size();
            case CHAPTER_VIEW -> selectedStory != null && spreadStartIndex + 2 < selectedStory.story().totalChapters();
            case PAGE_VIEW -> {
                if (selectedStory == null) yield false;
                BossStory.Chapter chapter = selectedStory.story().getChapter(selectedChapterIndex);
                yield contentSpreadStart + 2 < chapter.contentCount();
            }
        };
    }

    //  Story List - two stories per spread

    private void renderStoryList(GuiGraphics graphics, int mouseX, int mouseY) {
        if (stories.isEmpty()) {
            drawCenteredNoShadow(graphics,
                    Component.translatable("screen.magic_realms.book.no_stories").withStyle(ChatFormatting.GRAY),
                    bookLeft + BOOK_WIDTH / 2, bookTop + BOOK_HEIGHT / 2, 0x6B5B4A);
            return;
        }

        openBtnVisible[0] = false;
        openBtnVisible[1] = false;

        if (spreadStartIndex < stories.size()) {
            renderStoryOnPage(graphics, 0, stories.get(spreadStartIndex), mouseX, mouseY);
        }
        if (spreadStartIndex + 1 < stories.size()) {
            renderStoryOnPage(graphics, 1, stories.get(spreadStartIndex + 1), mouseX, mouseY);
        }
    }

    private void renderStoryOnPage(GuiGraphics graphics, int side, StoryEntry entry, int mouseX, int mouseY) {
        int cx = pageCenterX(side);
        int pw = pageWidth(side);
        int y = pageContentTop;

        // Story title
        MutableComponent title = BossStory.titleComponent(entry.id());
        Component styledTitle = title.withStyle(Style.EMPTY.withBold(true));
        int textWidth = pageWidth(side);
        List<FormattedCharSequence> titleLines = this.font.split(styledTitle, textWidth);
        for (FormattedCharSequence line : titleLines) {
            int lineWidth = this.font.width(line);
            graphics.drawString(this.font, line, cx - lineWidth / 2, y, 0x4A3728, false);
            y += this.font.lineHeight + 1;
        }
        y += 28;

        // 3D Entity render centered on the page
        ResourceLocation bossEntityId = entry.story().bossEntity();
        LivingEntity entityInstance = getOrCreateEntity(bossEntityId);

        int previewSize = Math.min(ENTITY_PREVIEW_SIZE, pw - 4);
        int previewLeft = cx - previewSize / 2;
        int previewRight = cx + previewSize / 2;
        int previewTop = (this.height / 2) - (previewSize / 2);
        int previewBottom = (this.height / 2) + (previewSize / 2);

        if (entityInstance != null) {
            int entityScale = getEntityRenderScale(entityInstance) * 2;
            graphics.enableScissor(previewLeft, previewTop, previewRight, previewBottom);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        graphics, previewLeft, previewTop, previewRight, previewBottom,
                        entityScale, 0.0625F, (float) cx, previewTop + previewSize / 3.0f,
                        entityInstance);
            } catch (Exception e) {
                drawCenteredNoShadow(graphics, "?", cx, previewTop + previewSize / 2 - 4, 0x888888);
            }
            graphics.disableScissor();
        } else {
            drawCenteredNoShadow(graphics, "?", cx, previewTop + previewSize / 2 - 4, 0x888888);
        }
        y = previewBottom + 6;

        // "Open" button
        int btnX = cx - BTN_WIDTH / 2;
        openBtnX[side] = btnX;
        openBtnY[side] = y;
        openBtnVisible[side] = true;

        boolean hovered = mouseX >= btnX && mouseX <= btnX + BTN_WIDTH && mouseY >= y && mouseY <= y + BTN_HEIGHT;
        graphics.fill(btnX, y, btnX + BTN_WIDTH, y + BTN_HEIGHT, hovered ? 0xC0404040 : 0xA0303030);
        drawBorder(graphics, btnX, y, BTN_WIDTH, BTN_HEIGHT, 0xFF555555);
        drawCenteredNoShadow(graphics,
                Component.translatable("screen.magic_realms.book.open_story"), cx, y + 4, 0xFFFFFF);

        y += 28;

        // Progress indicator
        int mainCollected = progress.getCollectedMainCount(entry.id());
        int totalMain = entry.story().totalMainChapters();
        Component progressText = Component.literal("[" + mainCollected + "/" + totalMain + "]")
                .withStyle(entry.complete() ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY);
        drawCenteredNoShadow(graphics, progressText, cx, y, 0x6B5B4A);
    }

    //  Chapter View — two chapters per spread

    private void renderChapterView(GuiGraphics graphics, int mouseX, int mouseY) {
        if (selectedStory == null) return;

        openBtnVisible[0] = false;
        openBtnVisible[1] = false;

        int totalChapters = selectedStory.story().totalChapters();

        if (spreadStartIndex < totalChapters) {
            renderChapterOnPage(graphics, 0, spreadStartIndex, mouseX, mouseY);
        }
        if (spreadStartIndex + 1 < totalChapters) {
            renderChapterOnPage(graphics, 1, spreadStartIndex + 1, mouseX, mouseY);
        }
    }

    private void renderChapterOnPage(GuiGraphics graphics, int side, int chapterIdx, int mouseX, int mouseY) {
        int cx = pageCenterX(side);
        int pw = pageWidth(side);
        int y = pageContentTop;

        boolean unlocked = progress.hasPage(selectedStory.id(), chapterIdx);

        BossStory.Chapter chapter = selectedStory.story().getChapter(chapterIdx);

        // Chapter title
        Component chapterTitle;
        if (unlocked) {
            chapterTitle = BossStory.chapterTitleComponent(selectedStory.id(), chapterIdx);
        } else {
            if (chapter.isSubstory()) {
                chapterTitle = Component.literal("Substory: ???");
            } else {
                chapterTitle = Component.literal("Ch. " + (chapterIdx + 1) + ": ???");
            }
        }
        drawCenteredNoShadow(graphics, chapterTitle.copy().withStyle(Style.EMPTY.withBold(true)), cx, y, 0x4A3728);
        y += 16;

        // Cover image area - sized to fit the page width with some padding
        int imgW = pw - 8; // leave 4px padding on each side
        int imgH = (int) (imgW * 0.7f); // ~70% aspect ratio
        int imgX = cx - imgW / 2;

        if (!unlocked) {
            // Locked overlay
            graphics.fill(imgX, y, imgX + imgW, y + imgH, 0x80000000);

            // Draw a large centered "?"
            String questionMark = "?";
            float scale = 3.0f;
            int textW = this.font.width(questionMark);
            float drawX = cx - (textW * scale) / 2f;
            float drawY = y + imgH / 2f - (font.lineHeight * scale) / 2f;

            graphics.pose().pushPose();
            graphics.pose().translate(drawX, drawY, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.drawString(this.font, questionMark, 0, 0, 0x8B0000, false);
            graphics.pose().popPose();
        } else if (chapter.coverImage() != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(chapter.coverImage(), imgX, y, 0, 0, imgW, imgH, imgW, imgH);
        } else {
            // No cover - show title as placeholder
            graphics.fill(imgX, y, imgX + imgW, y + imgH, 0x30000000);
            drawCenteredNoShadow(graphics,
                    BossStory.chapterTitleComponent(selectedStory.id(), chapterIdx)
                            .withStyle(Style.EMPTY.withItalic(true)),
                    cx, y + imgH / 2 - 4, 0x4A3728);
        }
        y += imgH + 6;

        // "Open" button (only if unlocked)
        if (unlocked) {
            int btnX = cx - BTN_WIDTH / 2;
            openBtnX[side] = btnX;
            openBtnY[side] = y;
            openBtnVisible[side] = true;

            boolean hovered = mouseX >= btnX && mouseX <= btnX + BTN_WIDTH
                    && mouseY >= y && mouseY <= y + BTN_HEIGHT;
            graphics.fill(btnX, y, btnX + BTN_WIDTH, y + BTN_HEIGHT, hovered ? 0xC0404040 : 0xA0303030);
            drawBorder(graphics, btnX, y, BTN_WIDTH, BTN_HEIGHT, 0xFF555555);
            drawCenteredNoShadow(graphics,
                    Component.translatable("screen.magic_realms.book.open_story"), cx, y + 4, 0xFFFFFF);
        }
    }

    //  Page View - content entries, two per spread

    private void renderPageView(GuiGraphics graphics, int mouseX, int mouseY) {
        if (selectedStory == null) return;

        BossStory.Chapter chapter = selectedStory.story().getChapter(selectedChapterIndex);
        int totalContent = chapter.contentCount();

        openBtnVisible[0] = false;
        openBtnVisible[1] = false;

        // Render left side content
        if (contentSpreadStart < totalContent) {
            renderContentEntry(graphics, 0, chapter, contentSpreadStart);
        }
        // Render right side content
        if (contentSpreadStart + 1 < totalContent) {
            renderContentEntry(graphics, 1, chapter, contentSpreadStart + 1);
        }

        // Page indicator at spine bottom
        int currentPair = contentSpreadStart / 2 + 1;
        int totalPairs = (totalContent + 1) / 2;
        Component pageInfo = Component.literal(currentPair + " / " + totalPairs).withStyle(ChatFormatting.GRAY);
        drawCenteredNoShadow(graphics, pageInfo,
                bookLeft + BOOK_WIDTH / 2, bookTop + BOOK_HEIGHT - 10, 0x6B5B4A);
    }

    private void renderContentEntry(GuiGraphics graphics, int side, BossStory.Chapter chapter, int contentIdx) {
        if (!chapter.isValidContent(contentIdx)) return;

        BossStory.ContentEntry entry = chapter.getContent(contentIdx);

        if (entry.isImage()) {
            renderImageContent(graphics, side, entry);
        } else {
            renderTextContent(graphics, side, entry);
        }
    }

    private void renderTextContent(GuiGraphics graphics, int side, BossStory.ContentEntry entry) {
        int px = pageX(side);
        int pw = pageWidth(side);
        int top = pageContentTop;
        int bottom = pageContentBottom;

        Component text = BossStory.pageTextComponent(selectedStory.id(), selectedChapterIndex, entry.textIndex());

        int textWidth = pw - 6; // small inner padding
        List<FormattedCharSequence> lines = this.font.split(text, textWidth);

        int availableHeight = bottom - top;
        int visibleLines = Math.max(1, availableHeight / (font.lineHeight + 1));
        maxTextScroll[side] = Math.max(0, lines.size() - visibleLines);
        textScroll[side] = Math.min(textScroll[side], maxTextScroll[side]);

        // Render with scissor to clip to the page area
        graphics.enableScissor(px, top, px + pw, bottom);

        int textX = px + 3;
        for (int i = textScroll[side]; i < Math.min(lines.size(), textScroll[side] + visibleLines); i++) {
            int lineY = top + (i - textScroll[side]) * (font.lineHeight + 1);
            graphics.drawString(this.font, lines.get(i), textX, lineY, 0x4A3728, false);
        }

        graphics.disableScissor();

        // Scroll indicators
        if (maxTextScroll[side] > 0) {
            int indicatorX = px + pw - 8;
            if (textScroll[side] < maxTextScroll[side]) {
                graphics.drawString(this.font, "\u25BC", indicatorX, bottom - 9, 0x8B7355, false);
            }
            if (textScroll[side] > 0) {
                graphics.drawString(this.font, "\u25B2", indicatorX, top, 0x8B7355, false);
            }
        }
    }

    private void renderImageContent(GuiGraphics graphics, int side, BossStory.ContentEntry entry) {
        if (entry.image() == null) return;

        int px = pageX(side);
        int pw = pageWidth(side);
        int top = pageContentTop;
        int bottom = pageContentBottom;
        int cx = pageCenterX(side);
        int availableHeight = bottom - top;

        // Fit image within the page area, preserving reasonable proportions
        int imgW = pw - 4;
        int imgH = Math.min(availableHeight - 4, (int) (imgW * 0.85f));
        int imgX = cx - imgW / 2;
        int imgY = top + (availableHeight - imgH) / 2;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(entry.image(), imgX, imgY, 0, 0, imgW, imgH, imgW, imgH);
    }

    //  Input Handling

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Check "Open" buttons first
        for (int side = 0; side < 2; side++) {
            if (openBtnVisible[side] && mx >= openBtnX[side] && mx <= openBtnX[side] + BTN_WIDTH
                    && my >= openBtnY[side] && my <= openBtnY[side] + BTN_HEIGHT) {
                handleOpenButton(side);
                return true;
            }
        }

        // Check bookmark (go back)
        if (viewMode != ViewMode.STORY_LIST
                && mx >= bookmarkLeft && mx < bookmarkRight
                && my >= bookmarkTop && my < bookmarkBottom) {
            handleBackButton();
            return true;
        }

        // Check left margin (previous page)
        if (mx >= leftMarginLeft && mx < leftMarginRight
                && my >= marginTop && my < marginBottom) {
            if (hasPreviousPage()) {
                handlePrevPage();
                return true;
            }
        }

        // Check right margin (next page)
        if (mx >= rightMarginLeft && mx < rightMarginRight
                && my >= marginTop && my < marginBottom) {
            if (hasNextPage()) {
                handleNextPage();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleOpenButton(int side) {
        switch (viewMode) {
            case STORY_LIST -> {
                int storyIdx = spreadStartIndex + side;
                if (storyIdx < stories.size()) {
                    selectedStory = stories.get(storyIdx);
                    spreadStartIndex = 0;
                    viewMode = ViewMode.CHAPTER_VIEW;
                }
            }
            case CHAPTER_VIEW -> {
                int chapterIdx = spreadStartIndex + side;
                if (selectedStory != null && selectedStory.story().isValidChapter(chapterIdx)) {
                    if (progress.hasPage(selectedStory.id(), chapterIdx)) {
                        selectedChapterIndex = chapterIdx;
                        contentSpreadStart = 0;
                        textScroll[0] = 0;
                        textScroll[1] = 0;
                        viewMode = ViewMode.PAGE_VIEW;
                    }
                }
            }
        }
    }

    private void handleBackButton() {
        switch (viewMode) {
            case CHAPTER_VIEW -> {
                viewMode = ViewMode.STORY_LIST;
                selectedStory = null;
                spreadStartIndex = 0;
            }
            case PAGE_VIEW -> {
                viewMode = ViewMode.CHAPTER_VIEW;
                spreadStartIndex = (selectedChapterIndex / 2) * 2;
                contentSpreadStart = 0;
                textScroll[0] = 0;
                textScroll[1] = 0;
            }
        }
    }

    private void handlePrevPage() {
        switch (viewMode) {
            case STORY_LIST, CHAPTER_VIEW -> {
                if (spreadStartIndex >= 2) spreadStartIndex -= 2;
            }
            case PAGE_VIEW -> {
                if (contentSpreadStart >= 2) {
                    contentSpreadStart -= 2;
                    textScroll[0] = 0;
                    textScroll[1] = 0;
                }
            }
        }
    }

    private void handleNextPage() {
        switch (viewMode) {
            case STORY_LIST -> {
                if (spreadStartIndex + 2 < stories.size()) spreadStartIndex += 2;
            }
            case CHAPTER_VIEW -> {
                if (selectedStory != null && spreadStartIndex + 2 < selectedStory.story().totalChapters())
                    spreadStartIndex += 2;
            }
            case PAGE_VIEW -> {
                if (selectedStory != null) {
                    BossStory.Chapter chapter = selectedStory.story().getChapter(selectedChapterIndex);
                    if (contentSpreadStart + 2 < chapter.contentCount()) {
                        contentSpreadStart += 2;
                        textScroll[0] = 0;
                        textScroll[1] = 0;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (viewMode == ViewMode.PAGE_VIEW) {
            int mx = (int) mouseX;
            // Determine which page the mouse is over using the computed areas
            int side = -1;
            if (mx >= leftPageX && mx <= leftPageRight) {
                side = 0;
            } else if (mx >= rightPageX && mx <= rightPageRight) {
                side = 1;
            }

            if (side >= 0 && selectedStory != null) {
                BossStory.Chapter chapter = selectedStory.story().getChapter(selectedChapterIndex);
                int contentIdx = contentSpreadStart + side;
                if (chapter.isValidContent(contentIdx) && chapter.getContent(contentIdx).isText()) {
                    if (scrollY > 0 && textScroll[side] > 0) {
                        textScroll[side]--;
                        return true;
                    } else if (scrollY < 0 && textScroll[side] < maxTextScroll[side]) {
                        textScroll[side]++;
                        return true;
                    }
                }
            }
        } else {
            if (scrollY > 0) { handlePrevPage(); return true; }
            if (scrollY < 0) { handleNextPage(); return true; }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    //  Entity Cache

    @Nullable
    private LivingEntity getOrCreateEntity(ResourceLocation entityId) {
        if (entityCache.containsKey(entityId)) return entityCache.get(entityId);

        Level level = Minecraft.getInstance().level;
        if (level == null) { entityCache.put(entityId, null); return null; }

        Optional<EntityType<? extends LivingEntity>> typeOpt = EntityAttributeHelper.resolveEntityType(entityId);
        if (typeOpt.isEmpty()) { entityCache.put(entityId, null); return null; }

        try {
            LivingEntity living = typeOpt.get().create(level);
            entityCache.put(entityId, living);
            return living;
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Could not create preview entity for {}: {}", entityId, e.getMessage());
            entityCache.put(entityId, null);
            return null;
        }
    }

    private int getEntityRenderScale(LivingEntity entity) {
        float height = entity.getBbHeight();
        if (height <= 0.5f) return 40;
        if (height <= 1.0f) return 32;
        if (height <= 2.0f) return 24;
        if (height <= 3.0f) return 18;
        if (height <= 4.0f) return 14;
        return 10;
    }

    //  Utilities

    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void removed() {
        super.removed();
        entityCache.clear();
    }
}