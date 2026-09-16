package net.alshanex.magic_realms.util.humans.titles;

import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.network.SetDisplayedTitlePacket;
import net.alshanex.magic_realms.util.humans.mercenaries.EntitySnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Compact "&lt; Title &gt;" picker for the contract screens. Sits where the old experience bar used to be.
 *
 * <p>Cycles through the mercenary's earned, non-hidden titles plus a "no title" entry, and sends
 * {@link SetDisplayedTitlePacket} on each change. The server re-validates that the sender is the contractor and that the title was actually earned, so nothing here is trusted.
 */
@OnlyIn(Dist.CLIENT)
public class TitleSelectorWidget extends AbstractWidget {

    private static final int ARROW_WIDTH = 10;
    private static final int BACKGROUND = 0x66000000;
    private static final int BACKGROUND_HOVER = 0x99000000;
    private static final int BORDER = 0x66FFFFFF;
    private static final int ARROW_COLOR = 0xFFE0E0E0;
    private static final int ARROW_COLOR_DISABLED = 0xFF606060;
    private static final int NO_TITLE_COLOR = 0xFF909090;

    private final UUID entityUUID;
    private final List<Title> options;

    /** -1 means "no title shown". */
    private int index;

    private TitleSelectorWidget(int x, int y, int width, int height, UUID entityUUID,
                                List<Title> options, int initialIndex) {
        super(x, y, width, height, Component.translatable("ui.magic_realms.title_selector"));
        this.entityUUID = entityUUID;
        this.options = options;
        this.index = initialIndex;
    }

    /**
     * @return a ready widget, or null when the mercenary has no selectable titles (in which case the screen should
     *         render nothing at all rather than an empty control).
     */
    public static TitleSelectorWidget create(int x, int y, int width, int height,
                                             EntitySnapshot snapshot, AbstractMercenaryEntity entity) {
        if (snapshot == null) return null;

        List<Title> options = new ArrayList<>();
        String displayedId = null;

        if (entity != null) {
            // Live entity: read straight from the synced attachment, which is always current.
            options.addAll(TitleManager.selectableTitles(entity));
            Title shown = TitleManager.displayedTitle(entity);
            if (shown != null) displayedId = shown.id().toString();
        } else {
            // Snapshot-only (client with no entity resolved): fall back to the captured ids.
            for (Title t : snapshot.resolveTitles(true)) {
                if (!t.hidden()) options.add(t);
            }
            displayedId = snapshot.displayedTitle;
        }

        if (options.isEmpty()) return null;

        int initialIndex = -1;
        if (displayedId != null) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).id().toString().equals(displayedId)) {
                    initialIndex = i;
                    break;
                }
            }
        }

        return new TitleSelectorWidget(x, y, width, height, snapshot.entityUUID, options, initialIndex);
    }

    // Selection

    private Title current() {
        return (index >= 0 && index < options.size()) ? options.get(index) : null;
    }

    private boolean canGoBack() {
        return index > -1;
    }

    private boolean canGoForward() {
        return index < options.size() - 1;
    }

    private void cycle(int direction) {
        int next = index + direction;
        if (next < -1 || next >= options.size()) return;

        index = next;

        Title selected = current();
        ResourceLocation id = selected == null ? null : selected.id();
        PacketDistributor.sendToServer(new SetDisplayedTitlePacket(entityUUID, id));

        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }

    // Rendering

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;

        int right = getX() + width;
        int bottom = getY() + height;

        guiGraphics.fill(getX(), getY(), right, bottom, isHovered() ? BACKGROUND_HOVER : BACKGROUND);
        guiGraphics.renderOutline(getX(), getY(), width, height, BORDER);

        int textY = getY() + (height - 8) / 2;

        // Arrows
        guiGraphics.drawString(font, "<", getX() + 3, textY,
                canGoBack() ? ARROW_COLOR : ARROW_COLOR_DISABLED, false);
        guiGraphics.drawString(font, ">", right - 7, textY,
                canGoForward() ? ARROW_COLOR : ARROW_COLOR_DISABLED, false);

        // Label
        Title selected = current();
        Component label;
        int color;
        if (selected == null) {
            label = Component.translatable("ui.magic_realms.title_none");
            color = NO_TITLE_COLOR;
        } else {
            label = Component.translatable(selected.displayKey());
            color = 0xFF000000 | selected.resolvedColor();
        }

        int labelAreaX = getX() + ARROW_WIDTH;
        int labelAreaWidth = width - (ARROW_WIDTH * 2);

        // Trim with an ellipsis rather than letting a long title run under the arrows.
        String text = label.getString();
        if (font.width(text) > labelAreaWidth) {
            text = font.plainSubstrByWidth(text, labelAreaWidth - font.width("...")) + "...";
        }

        int labelX = labelAreaX + (labelAreaWidth - font.width(text)) / 2;
        guiGraphics.drawString(font, text, labelX, textY, color, false);

        // Tooltip: full name plus description, since the label may be trimmed.
        if (isHovered() && selected != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(selected.displayComponent());
            if (selected.descriptionKey() != null && !selected.descriptionKey().isEmpty()) {
                lines.add(selected.descriptionComponent()
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        boolean leftHalf = mouseX < getX() + (width / 2.0);
        cycle(leftHalf ? -1 : 1);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        Title selected = current();
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                selected == null
                        ? Component.translatable("ui.magic_realms.title_none")
                        : selected.displayComponent());
    }
}