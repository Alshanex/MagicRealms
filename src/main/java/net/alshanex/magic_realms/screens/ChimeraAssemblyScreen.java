package net.alshanex.magic_realms.screens;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ChimeraBlueprint;
import net.alshanex.magic_realms.data.ChimeraMobDefinition;
import net.alshanex.magic_realms.data.ChimeraSlot;
import net.alshanex.magic_realms.entity.chimera.ChimeraEntity;
import net.alshanex.magic_realms.network.SaveBlueprintPayload;
import net.alshanex.magic_realms.registry.ChimeraPartRegistry;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.chimera.ChimeraAssembly;
import net.alshanex.magic_realms.util.chimera.EntityAttributeHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Reworked Chimera Assembly GUI Screen.
 * <p>
 * Layout:
 * - Left side: 3D preview of the chimera being assembled, 6 slot buttons arranged
 *   around the preview in humanoid layout, custom name text field below the preview.
 * - Right side: Scrollable list of available entities for the selected slot, each
 *   entry shows a 3D render of the entity and its translated name.
 * - Bottom: Save and Cancel buttons spanning the full width.
 */
public class ChimeraAssemblyScreen extends Screen {

    // Overall GUI dimensions
    private static final int GUI_WIDTH = 440;
    private static final int GUI_HEIGHT = 280;

    // Left panel (chimera preview + slots)
    private static final int LEFT_PANEL_WIDTH = 240;

    // 3D preview area (relative to guiLeft) - centered in the left panel with room for slot buttons on each side
    private static final int PREVIEW_X = 60;
    private static final int PREVIEW_Y = 50;
    private static final int PREVIEW_WIDTH = 120;
    private static final int PREVIEW_HEIGHT = 140;

    // Slot button dimensions
    private static final int SLOT_BTN_WIDTH = 48;
    private static final int SLOT_BTN_HEIGHT = 20;

    // Right panel (entity list)
    private static final int RIGHT_PANEL_X = 245;
    private static final int ENTITY_ENTRY_HEIGHT = 50;
    private static final int ENTITY_LIST_PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 8;

    // Bottom buttons
    private static final int BOTTOM_BTN_HEIGHT = 20;
    private static final int BOTTOM_BTN_Y_OFFSET = 30; // from bottom of GUI

    // State
    private final ItemStack catalystStack;
    private final boolean isMainHand;
    private final ChimeraAssembly assembly;
    private ChimeraSlot selectedSlot = ChimeraSlot.HEAD;
    private String customName = "";

    // Mob list state
    private List<ChimeraMobDefinition> availableMobs = new ArrayList<>();
    private double mobListScrollOffset = 0;

    // Cached entity instances for 3D rendering
    private final Map<ResourceLocation, LivingEntity> entityCache = new HashMap<>();
    private ChimeraEntity previewChimera;
    private float previewRotation = 0f;

    // Widgets
    private final Map<ChimeraSlot, Button> slotButtons = new EnumMap<>(ChimeraSlot.class);
    private EditBox nameField;
    private Button saveButton;

    // GUI position
    private int guiLeft;
    private int guiTop;

    // Dragging for chimera preview rotation
    private boolean isDraggingPreview = false;
    private double lastDragX = 0;

    public ChimeraAssemblyScreen(ItemStack catalystStack, boolean isMainHand, @Nullable ChimeraBlueprint existing) {
        super(Component.translatable("screen.chimera.assembly"));
        this.catalystStack = catalystStack;
        this.isMainHand = isMainHand;

        if (existing != null && existing.assembly().isPresent()) {
            this.assembly = existing.assembly().get();
        } else {
            this.assembly = new ChimeraAssembly();
        }
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;

        // Slot Buttons (arranged around the preview)
        int previewLeftX = guiLeft + PREVIEW_X;
        int previewRightX = guiLeft + PREVIEW_X + PREVIEW_WIDTH;
        int previewTopY = guiTop + PREVIEW_Y;
        int previewBottomY = guiTop + PREVIEW_Y + PREVIEW_HEIGHT;
        int previewCenterX = guiLeft + PREVIEW_X + PREVIEW_WIDTH / 2;

        // Head: above preview, left-aligned
        addSlotButton(ChimeraSlot.HEAD,
                previewCenterX - SLOT_BTN_WIDTH - 4, previewTopY - SLOT_BTN_HEIGHT - 4,
                SLOT_BTN_WIDTH, SLOT_BTN_HEIGHT);

        // Torso: above preview, right-aligned
        addSlotButton(ChimeraSlot.TORSO,
                previewCenterX + 4, previewTopY - SLOT_BTN_HEIGHT - 4,
                SLOT_BTN_WIDTH, SLOT_BTN_HEIGHT);

        // R. Arm: left side of preview, vertically centered
        addSlotButton(ChimeraSlot.RIGHT_ARM,
                previewLeftX - SLOT_BTN_WIDTH - 6, previewTopY + (PREVIEW_HEIGHT / 3) - SLOT_BTN_HEIGHT - 4,
                SLOT_BTN_WIDTH, SLOT_BTN_HEIGHT);

        // L. Arm: right side of preview, vertically centered
        addSlotButton(ChimeraSlot.LEFT_ARM,
                previewRightX + 6, previewTopY + (PREVIEW_HEIGHT / 3) - SLOT_BTN_HEIGHT - 4,
                SLOT_BTN_WIDTH, SLOT_BTN_HEIGHT);

        // R. Leg: below preview area, left side
        addSlotButton(ChimeraSlot.RIGHT_LEG,
                previewLeftX - SLOT_BTN_WIDTH - 6, previewTopY + (PREVIEW_HEIGHT / 2) + (PREVIEW_HEIGHT / 3) + 4,
                SLOT_BTN_WIDTH, SLOT_BTN_HEIGHT);

        // L. Leg: below preview area, right side
        addSlotButton(ChimeraSlot.LEFT_LEG,
                previewRightX + 6, previewTopY + (PREVIEW_HEIGHT / 2) + (PREVIEW_HEIGHT / 3) + 4,
                SLOT_BTN_WIDTH, SLOT_BTN_HEIGHT);

        // Custom Name Field
        int nameFieldY = previewBottomY + 20;
        nameField = new EditBox(this.font, guiLeft + 16, nameFieldY, LEFT_PANEL_WIDTH - 32, 18,
                Component.translatable("screen.magic_realms.assembly.name_field"));
        nameField.setMaxLength(50);
        nameField.setValue(customName);
        nameField.setHint(Component.literal("Custom name...").withStyle(ChatFormatting.DARK_GRAY));
        nameField.setResponder(s -> customName = s);
        addRenderableWidget(nameField);

        // Save Button
        int bottomY = guiTop + GUI_HEIGHT - BOTTOM_BTN_Y_OFFSET;
        int btnWidth = (GUI_WIDTH - 20) / 2;

        saveButton = Button.builder(
                Component.translatable("screen.magic_realms.assembly.save").withStyle(ChatFormatting.GREEN),
                b -> onSave()
        ).bounds(guiLeft + (GUI_WIDTH / 2) - (btnWidth / 2), bottomY, btnWidth, BOTTOM_BTN_HEIGHT).build();
        addRenderableWidget(saveButton);

        // Initial state
        refreshAvailableMobs();
        updateSlotButtonStyles();
        rebuildPreviewChimera();
    }

    private void addSlotButton(ChimeraSlot slot, int x, int y, int w, int h) {
        Button btn = Button.builder(getSlotLabel(slot), b -> onSlotSelected(slot))
                .bounds(x, y, w, h)
                .tooltip(Tooltip.create(getSlotTooltip(slot)))
                .build();
        slotButtons.put(slot, btn);
        addRenderableWidget(btn);
    }

    //  Slot Selection

    private void onSlotSelected(ChimeraSlot slot) {
        this.selectedSlot = slot;
        this.mobListScrollOffset = 0;
        refreshAvailableMobs();
        updateSlotButtonStyles();
    }

    private void updateSlotButtonStyles() {
        for (Map.Entry<ChimeraSlot, Button> entry : slotButtons.entrySet()) {
            ChimeraSlot slot = entry.getKey();
            Button btn = entry.getValue();
            btn.setMessage(getSlotLabel(slot));
            btn.setTooltip(Tooltip.create(getSlotTooltip(slot)));
        }
        saveButton.active = assembly.isComplete();
    }

    private Component getSlotLabel(ChimeraSlot slot) {
        Optional<ResourceLocation> assigned = assembly.getEntityForSlot(slot);
        ChatFormatting color;
        if (slot == selectedSlot) {
            color = ChatFormatting.YELLOW;
        } else if (assigned.isPresent()) {
            color = ChatFormatting.GREEN;
        } else {
            color = ChatFormatting.GRAY;
        }

        String shortName = switch (slot) {
            case HEAD -> "Head";
            case LEFT_ARM -> "L. Arm";
            case RIGHT_ARM -> "R. Arm";
            case TORSO -> "Torso";
            case LEFT_LEG -> "L. Leg";
            case RIGHT_LEG -> "R. Leg";
        };

        return Component.literal(shortName).withStyle(color);
    }

    private Component getSlotTooltip(ChimeraSlot slot) {
        Optional<ResourceLocation> assigned = assembly.getEntityForSlot(slot);
        if (assigned.isPresent()) {
            ResourceLocation entityId = assigned.get();
            String translationKey = "entity." + entityId.getNamespace() + "." + entityId.getPath();
            return Component.translatable(translationKey).withStyle(ChatFormatting.GREEN);
        }
        return Component.literal("Empty - Click to select").withStyle(ChatFormatting.GRAY);
    }

    //  Mob List

    private void refreshAvailableMobs() {
        ChimeraPartRegistry registry = ChimeraPartRegistry.getInstance();
        if (registry == null) {
            availableMobs = Collections.emptyList();
            return;
        }
        availableMobs = registry.getEntitiesWithSlot(selectedSlot);
    }

    private void onMobSelected(ResourceLocation entityId) {
        Optional<ResourceLocation> current = assembly.getEntityForSlot(selectedSlot);
        if (current.isPresent() && current.get().equals(entityId)) {
            assembly.clearSlot(selectedSlot);
        } else {
            assembly.setSlot(selectedSlot, entityId);
        }
        rebuildPreviewChimera();
        updateSlotButtonStyles();
    }

    //  Preview Chimera

    private void rebuildPreviewChimera() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (previewChimera == null) {
            previewChimera = new ChimeraEntity(MREntityRegistry.CHIMERA_ENTITY.get(), mc.level);
        }
        previewChimera.setAssembly(assembly);
    }

    @Nullable
    private LivingEntity getOrCreateEntity(ResourceLocation entityId) {
        if (entityCache.containsKey(entityId)) {
            return entityCache.get(entityId);
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return null;

        Optional<EntityType<? extends LivingEntity>> typeOpt = EntityAttributeHelper.resolveEntityType(entityId);
        if (typeOpt.isEmpty()) {
            entityCache.put(entityId, null);
            return null;
        }

        try {
            LivingEntity living = typeOpt.get().create(level);
            entityCache.put(entityId, living);
            return living;
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Could not create preview entity for {}: {}", entityId, e.getMessage());
        }

        entityCache.put(entityId, null);
        return null;
    }

    //  Save

    private void onSave() {
        if (!assembly.isComplete()) return;

        PacketDistributor.sendToServer(new SaveBlueprintPayload(isMainHand, assembly.toNbt(), customName));
        onClose();
    }

    //  Input Handling

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Check if mouse is over the entity list area
        int listX = guiLeft + RIGHT_PANEL_X;
        int listY = guiTop + ENTITY_LIST_PADDING + 18; // below the header
        int listWidth = GUI_WIDTH - RIGHT_PANEL_X - 5;
        int listHeight = getEntityListHeight();

        if (mouseX >= listX && mouseX <= listX + listWidth
                && mouseY >= listY && mouseY <= listY + listHeight) {
            double maxScroll = getMaxScrollOffset();
            mobListScrollOffset = Math.max(0, Math.min(mobListScrollOffset - scrollY * 20, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check for clicks on entity list entries
        if (button == 0) {
            int listX = guiLeft + RIGHT_PANEL_X;
            int listY = guiTop + ENTITY_LIST_PADDING + 18;
            int listWidth = GUI_WIDTH - RIGHT_PANEL_X - 5 - SCROLLBAR_WIDTH;
            int listHeight = getEntityListHeight();

            if (mouseX >= listX && mouseX <= listX + listWidth
                    && mouseY >= listY && mouseY <= listY + listHeight) {
                // Determine which entry was clicked
                double relativeY = mouseY - listY + mobListScrollOffset;
                int index = (int) (relativeY / ENTITY_ENTRY_HEIGHT);
                if (index >= 0 && index < availableMobs.size()) {
                    onMobSelected(availableMobs.get(index).entity());
                    return true;
                }
            }

            // Check for preview dragging
            int previewScreenX = guiLeft + PREVIEW_X;
            int previewScreenY = guiTop + PREVIEW_Y;
            if (mouseX >= previewScreenX && mouseX <= previewScreenX + PREVIEW_WIDTH
                    && mouseY >= previewScreenY && mouseY <= previewScreenY + PREVIEW_HEIGHT) {
                isDraggingPreview = true;
                lastDragX = mouseX;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingPreview && button == 0) {
            previewRotation += (float) (mouseX - lastDragX) * 1.0f;
            lastDragX = mouseX;
            return true;
        }

        // Scrollbar dragging
        int listX = guiLeft + RIGHT_PANEL_X;
        int listWidth = GUI_WIDTH - RIGHT_PANEL_X - 5;
        int scrollbarX = listX + listWidth - SCROLLBAR_WIDTH;
        int listY = guiTop + ENTITY_LIST_PADDING + 18;
        int listHeight = getEntityListHeight();

        if (button == 0 && mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH
                && mouseY >= listY && mouseY <= listY + listHeight) {
            double maxScroll = getMaxScrollOffset();
            if (maxScroll > 0) {
                double ratio = (mouseY - listY) / listHeight;
                mobListScrollOffset = ratio * maxScroll;
                mobListScrollOffset = Math.max(0, Math.min(mobListScrollOffset, maxScroll));
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingPreview = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Let the name field consume key presses when focused
        if (nameField != null && nameField.isFocused()) {
            if (keyCode == 256) { // Escape
                nameField.setFocused(false);
                return true;
            }
            return nameField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //  Rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Darken background
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // Main panel background
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xE0101010);

        // Left panel background
        graphics.fill(guiLeft + 2, guiTop + 2,
                guiLeft + LEFT_PANEL_WIDTH, guiTop + GUI_HEIGHT - BOTTOM_BTN_Y_OFFSET - 5,
                0xC0202020);

        // Left panel border
        drawBorder(graphics, guiLeft + 2, guiTop + 2,
                LEFT_PANEL_WIDTH - 2, GUI_HEIGHT - BOTTOM_BTN_Y_OFFSET - 7, 0xFF555555);

        // Preview area border
        int previewScreenX = guiLeft + PREVIEW_X;
        int previewScreenY = guiTop + PREVIEW_Y;
        drawBorder(graphics, previewScreenX - 1, previewScreenY - 1,
                PREVIEW_WIDTH + 2, PREVIEW_HEIGHT + 2, 0xFF444444);
        graphics.fill(previewScreenX, previewScreenY,
                previewScreenX + PREVIEW_WIDTH, previewScreenY + PREVIEW_HEIGHT,
                0xFF1A1A1A);

        // Render 3D Chimera Preview
        renderChimeraPreview(graphics, previewScreenX, previewScreenY, mouseX, mouseY, partialTick);

        // Slot completion indicator
        int filled = 0;
        for (ChimeraSlot slot : ChimeraSlot.values()) {
            if (assembly.getEntityForSlot(slot).isPresent()) filled++;
        }
        graphics.drawCenteredString(this.font,
                Component.literal(filled + "/6").withStyle(filled == 6 ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                previewScreenX + PREVIEW_WIDTH / 2, previewScreenY + PREVIEW_HEIGHT + 6, 0xFFFFFF);

        // Right panel
        renderEntityList(graphics, mouseX, mouseY, partialTick);

        // Selected slot indicator
        String slotName = formatSlotName(selectedSlot);
        graphics.drawCenteredString(this.font,
                Component.literal("Editing: " + slotName).withStyle(ChatFormatting.YELLOW),
                guiLeft + RIGHT_PANEL_X + (GUI_WIDTH - RIGHT_PANEL_X - 5) / 2,
                guiTop + ENTITY_LIST_PADDING + 4, 0xFFFFFF);

        // Render all widgets (buttons, text field)
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderChimeraPreview(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        if (previewChimera == null) return;

        // Check if at least one slot is filled
        boolean hasAnySlot = false;
        for (ChimeraSlot slot : ChimeraSlot.values()) {
            if (assembly.getEntityForSlot(slot).isPresent()) {
                hasAnySlot = true;
                break;
            }
        }
        if (!hasAnySlot) {
            graphics.drawCenteredString(this.font,
                    Component.literal("No parts assigned").withStyle(ChatFormatting.DARK_GRAY),
                    x + PREVIEW_WIDTH / 2, y + PREVIEW_HEIGHT / 2 - 4, 0x888888);
            return;
        }

        int scale = 30;

        // Use scissor to clip rendering to the preview area
        graphics.enableScissor(x, y, x + PREVIEW_WIDTH, y + PREVIEW_HEIGHT);

        try {
            float rotation = previewRotation;

            // Set all rotation angles to our controlled rotation
            // so the entity doesn't try to look at anything
            float oldYBodyRot = previewChimera.yBodyRot;
            float oldYRot = previewChimera.getYRot();
            float oldXRot = previewChimera.getXRot();
            float oldYHeadRot = previewChimera.yHeadRot;
            float oldYHeadRotO = previewChimera.yHeadRotO;

            previewChimera.yBodyRot = rotation;
            previewChimera.setYRot(rotation);
            previewChimera.setXRot(0);
            previewChimera.yHeadRot = rotation;
            previewChimera.yHeadRotO = rotation;

            // Use renderEntityInInventoryFollowsMouse but with fixed "mouse" coordinates at the center of the preview area so the entity looks straight ahead
            float fixedMouseX = x + PREVIEW_WIDTH / 2.0f;
            float fixedMouseY = y + PREVIEW_HEIGHT / 3.0f; // slightly above center for natural look
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, x, y, x + PREVIEW_WIDTH, y + PREVIEW_HEIGHT,
                    scale, 0.0625F, fixedMouseX, fixedMouseY, previewChimera);

            previewChimera.yBodyRot = oldYBodyRot;
            previewChimera.setYRot(oldYRot);
            previewChimera.setXRot(oldXRot);
            previewChimera.yHeadRot = oldYHeadRot;
            previewChimera.yHeadRotO = oldYHeadRotO;
        } catch (Exception e) {
            // Silently fail if rendering has issues
        }

        graphics.disableScissor();
    }

    private void renderEntityList(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int listX = guiLeft + RIGHT_PANEL_X;
        int headerY = guiTop + ENTITY_LIST_PADDING;
        int listY = headerY + 18;
        int listWidth = GUI_WIDTH - RIGHT_PANEL_X - 5;
        int listHeight = getEntityListHeight();

        // Right panel background
        graphics.fill(listX, guiTop + 2, guiLeft + GUI_WIDTH - 2,
                guiTop + GUI_HEIGHT - BOTTOM_BTN_Y_OFFSET - 5, 0xC0202020);
        drawBorder(graphics, listX, guiTop + 2,
                GUI_WIDTH - RIGHT_PANEL_X - 4, GUI_HEIGHT - BOTTOM_BTN_Y_OFFSET - 7, 0xFF555555);

        if (availableMobs.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.literal("No entities available").withStyle(ChatFormatting.DARK_GRAY),
                    listX + listWidth / 2, listY + listHeight / 2 - 4, 0x888888);
            return;
        }

        // Scissor for the list area
        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        int totalContentHeight = availableMobs.size() * ENTITY_ENTRY_HEIGHT;

        for (int i = 0; i < availableMobs.size(); i++) {
            ChimeraMobDefinition def = availableMobs.get(i);
            ResourceLocation entityId = def.entity();

            int entryY = (int) (listY + i * ENTITY_ENTRY_HEIGHT - mobListScrollOffset);

            // Skip entries that are fully outside the visible area
            if (entryY + ENTITY_ENTRY_HEIGHT < listY || entryY > listY + listHeight) continue;

            boolean isAssigned = assembly.getEntityForSlot(selectedSlot)
                    .map(rl -> rl.equals(entityId))
                    .orElse(false);

            boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth - SCROLLBAR_WIDTH
                    && mouseY >= Math.max(listY, entryY)
                    && mouseY <= Math.min(listY + listHeight, entryY + ENTITY_ENTRY_HEIGHT);

            // Entry background
            int bgColor = isAssigned ? 0x60306030 : (isHovered ? 0x40404040 : 0x30282828);
            graphics.fill(listX + 2, entryY, listX + listWidth - SCROLLBAR_WIDTH - 2,
                    entryY + ENTITY_ENTRY_HEIGHT - 2, bgColor);

            // Entry border if assigned
            if (isAssigned) {
                drawBorder(graphics, listX + 2, entryY,
                        listWidth - SCROLLBAR_WIDTH - 4, ENTITY_ENTRY_HEIGHT - 2, 0xFF55FF55);
            }

            // 3D Entity render
            LivingEntity entityInstance = getOrCreateEntity(entityId);
            if (entityInstance != null) {
                int entityRenderX = listX + 26;
                int entityRenderY = entryY + ENTITY_ENTRY_HEIGHT - 4;
                int entityScale = getEntityRenderScale(entityInstance);

                graphics.enableScissor(listX + 4, Math.max(listY, entryY),
                        listX + 48, Math.min(listY + listHeight, entryY + ENTITY_ENTRY_HEIGHT - 2));
                try {
                    InventoryScreen.renderEntityInInventoryFollowsMouse(
                            graphics, listX + 4, Math.max(listY, entryY),
                            listX + 48, Math.min(listY + listHeight, entryY + ENTITY_ENTRY_HEIGHT - 2),
                            entityScale, 0.0625F, (float) listX + 26, (float) entryY + 10, entityInstance);
                } catch (Exception e) {
                    // Fallback: draw a placeholder
                    graphics.drawCenteredString(this.font, "?", entityRenderX, entryY + 18, 0x888888);
                }
                graphics.disableScissor();
            }

            // Entity name
            String translationKey = "entity." + entityId.getNamespace() + "." + entityId.getPath();
            Component name = Component.translatable(translationKey);
            ChatFormatting nameColor = isAssigned ? ChatFormatting.GREEN : ChatFormatting.WHITE;
            graphics.drawString(this.font, name.copy().withStyle(nameColor),
                    listX + 52, entryY + 8, 0xFFFFFF, true);

            // Attribute info below the name
            String attrInfo = getAttributeInfo(entityId);
            if (!attrInfo.isEmpty()) {
                graphics.drawString(this.font,
                        Component.literal(attrInfo).withStyle(ChatFormatting.DARK_GRAY),
                        listX + 52, entryY + 22, 0xAAAAAA, false);
            }

            // Separator line
            graphics.fill(listX + 4, entryY + ENTITY_ENTRY_HEIGHT - 2,
                    listX + listWidth - SCROLLBAR_WIDTH - 4, entryY + ENTITY_ENTRY_HEIGHT - 1,
                    0x40FFFFFF);
        }

        graphics.disableScissor();

        // Scrollbar
        if (totalContentHeight > listHeight) {
            int scrollbarX = listX + listWidth - SCROLLBAR_WIDTH;
            // Track background
            graphics.fill(scrollbarX, listY, scrollbarX + SCROLLBAR_WIDTH, listY + listHeight, 0x40FFFFFF);

            // Thumb
            double maxScroll = getMaxScrollOffset();
            double scrollRatio = maxScroll > 0 ? mobListScrollOffset / maxScroll : 0;
            int thumbHeight = Math.max(20, (int) ((double) listHeight * listHeight / totalContentHeight));
            int thumbY = listY + (int) (scrollRatio * (listHeight - thumbHeight));

            graphics.fill(scrollbarX + 1, thumbY, scrollbarX + SCROLLBAR_WIDTH - 1,
                    thumbY + thumbHeight, 0xC0888888);
        }
    }

    private String getAttributeInfo(ResourceLocation entityId) {
        if (selectedSlot.isMainArm()) {
            return "ATK: %.1f  ATKSPD: %.1f".formatted(
                    EntityAttributeHelper.getAttackDamage(entityId),
                    EntityAttributeHelper.getAttackSpeed(entityId));
        } else if (selectedSlot.isTorso()) {
            return "HP: %.0f  ARM: %.0f".formatted(
                    Math.min(EntityAttributeHelper.getMaxHealth(entityId), 100.0),
                    EntityAttributeHelper.getArmor(entityId));
        } else if (selectedSlot.isLeg()) {
            return "SPD: %.3f".formatted(EntityAttributeHelper.getMovementSpeed(entityId));
        }
        return "Cosmetic";
    }

    private int getEntityRenderScale(LivingEntity entity) {
        float height = entity.getBbHeight();
        if (height <= 0.5f) return 28;
        if (height <= 1.0f) return 22;
        if (height <= 2.0f) return 16;
        if (height <= 3.0f) return 12;
        return 8;
    }

    private int getEntityListHeight() {
        return GUI_HEIGHT - BOTTOM_BTN_Y_OFFSET - 5 - ENTITY_LIST_PADDING - 18 - 4;
    }

    private double getMaxScrollOffset() {
        int totalContentHeight = availableMobs.size() * ENTITY_ENTRY_HEIGHT;
        int listHeight = getEntityListHeight();
        return Math.max(0, totalContentHeight - listHeight);
    }

    //  Utility

    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        // Top
        graphics.fill(x, y, x + width, y + 1, color);
        // Bottom
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        // Left
        graphics.fill(x, y, x + 1, y + height, color);
        // Right
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private String formatSlotName(ChimeraSlot slot) {
        String name = slot.getSerializedName().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        entityCache.clear();
    }
}