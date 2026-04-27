package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntityRenderer;
import net.alshanex.magic_realms.network.OpenSkinCustomizerPacket;
import net.alshanex.magic_realms.network.SaveSkinPartsPacket;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.*;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkinCustomizerScreen extends Screen {

    // --- layout ---
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 220;
    private static final int PREVIEW_W = 110;
    private static final int LIST_W = 190;
    private static final int TAB_H = 18;
    private static final int ROW_H = 20;

    private final UUID entityUUID;
    private final Gender gender;
    private final EntityClass entityClass;

    // pending selections
    private String pendingSkin, pendingClothes, pendingEyes, pendingHair;

    // preview state
    private RandomHumanEntity virtualEntity;
    private float currentYaw = 0f;
    private float targetYaw = 0f;

    // tab state
    private SkinCategory currentTab = SkinCategory.SKIN;
    private int scrollOffset = 0;
    private List<SkinPart> currentOptions = new ArrayList<>();

    private int panelX, panelY;

    private EditBox nameField;
    private final String initialName;

    public SkinCustomizerScreen(OpenSkinCustomizerPacket pkt) {
        super(Component.translatable("screen.magic_realms.skin_customizer"));
        this.entityUUID = pkt.entityUUID;
        this.gender = Gender.valueOf(pkt.gender.toUpperCase());
        this.entityClass = EntityClass.valueOf(pkt.entityClass.toUpperCase());
        this.initialName = pkt.currentName;
        this.pendingSkin = pkt.currentSkin;
        this.pendingClothes = pkt.currentClothes;
        this.pendingEyes = pkt.currentEyes;
        this.pendingHair = pkt.currentHair;
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;

        // tabs
        int tabW = PANEL_W / 4;
        SkinCategory[] cats = { SkinCategory.SKIN, SkinCategory.CLOTHES, SkinCategory.EYES, SkinCategory.HAIR };
        for (int i = 0; i < cats.length; i++) {
            final SkinCategory cat = cats[i];
            addRenderableWidget(Button.builder(Component.literal(capitalize(cat.getSerializedName())), b -> {
                currentTab = cat;
                scrollOffset = 0;
                refreshOptions();
            }).bounds(panelX + i * tabW, panelY, tabW, TAB_H).build());
        }

        // name edit box — sits below the tabs, above the preview/list
        nameField = new EditBox(this.font, panelX + 4, panelY + TAB_H + 4, PANEL_W - 8, 16,
                Component.translatable("screen.magic_realms.skin_customizer.name"));
        nameField.setMaxLength(32);
        nameField.setValue(initialName != null ? initialName : "");
        nameField.setHint(Component.literal("Entity name..."));
        addRenderableWidget(nameField);

        // save / cancel — save now sends the name too
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            String newName = nameField.getValue().trim();
            PacketDistributor.sendToServer(new SaveSkinPartsPacket(
                    entityUUID, newName, pendingSkin, pendingClothes, pendingEyes, pendingHair));
            onClose();
        }).bounds(panelX + PANEL_W - 130, panelY + PANEL_H + 4, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(panelX + PANEL_W - 65, panelY + PANEL_H + 4, 60, 20).build());

        buildVirtualEntity();
        refreshOptions();
    }

    private void refreshOptions() {
        // "Show everything, no filtering" per user choice
        currentOptions = new ArrayList<>();
        for (SkinPart p : SkinCatalogHolder.client().allParts()) {
            if (p.category() == currentTab) currentOptions.add(p);
        }
    }

    private void buildVirtualEntity() {
        try {
            virtualEntity = MREntityRegistry.HUMAN.get().create(this.minecraft.level);
            if (virtualEntity == null) return;
            virtualEntity.setGender(gender);
            virtualEntity.setEntityClass(entityClass);
            virtualEntity.setInitialized(true);
            applyPendingToVirtual();
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to build virtual entity for customizer", e);
        }
    }

    private void applyPendingToVirtual() {
        if (virtualEntity == null) return;
        CompoundTag md = new CompoundTag();
        md.putString("gender", gender.getName());
        md.putString("entityClass", entityClass.getName());
        md.putBoolean("usePreset", false);
        if (!pendingSkin.isEmpty()) md.putString("skinTexture", pendingSkin);
        if (!pendingClothes.isEmpty()) md.putString("clothesTexture", pendingClothes);
        if (!pendingEyes.isEmpty()) md.putString("eyesTexture", pendingEyes);
        if (!pendingHair.isEmpty()) md.putString("hairTexture", pendingHair);
        virtualEntity.setTextureMetadata(md);

        // Force regenerate textureComponents via reflection (same pattern as contract screens)
        try {
            TextureComponents tc = TextureComponents.fromMetadata(md);
            var f = RandomHumanEntity.class.getDeclaredField("textureComponents");
            f.setAccessible(true);
            f.set(virtualEntity, tc);
            var flag = RandomHumanEntity.class.getDeclaredField("clientTexturesGenerated");
            flag.setAccessible(true);
            flag.setBoolean(virtualEntity, true);
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to apply pending textures to virtual entity", e);
        }
        // Clear composite cache so new combination renders
        if (virtualEntity.getTextureComponents() != null && !virtualEntity.getTextureComponents().isPresetTexture()) {
            RandomHumanEntityRenderer.clearCompositeCacheFor(virtualEntity.getTextureComponents());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int contentH = PANEL_H - TAB_H - 8 - 20;

        // panel bg
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xCC101010);
        g.fill(panelX, panelY + TAB_H, panelX + PANEL_W, panelY + TAB_H + 1, 0xFF404040);

        // highlight active tab
        int tabW = PANEL_W / 4;
        int activeIdx = switch (currentTab) {
            case SKIN -> 0; case CLOTHES -> 1; case EYES -> 2; case HAIR -> 3;
        };
        g.fill(panelX + activeIdx * tabW, panelY, panelX + (activeIdx + 1) * tabW, panelY + TAB_H, 0x80FFFFFF);

        // preview area (left)
        int previewX = panelX + 4;
        int previewY = panelY + TAB_H + 4 + 20;
        g.fill(previewX, previewY, previewX + PREVIEW_W, previewY + contentH, 0xFF202020);
        renderPreview(g, previewX, previewY, PREVIEW_W, contentH, mouseX, mouseY, partialTick);

        // list area (right)
        int listX = previewX + PREVIEW_W + 4;
        int listY = previewY;
        int listH = contentH;
        int visibleRows = listH / ROW_H;
        g.fill(listX, listY, listX + LIST_W - 8, listY + listH, 0xFF1A1A1A);

        int startIdx = scrollOffset;
        int endIdx = Math.min(currentOptions.size(), startIdx + visibleRows);
        for (int i = startIdx; i < endIdx; i++) {
            int rowY = listY + (i - startIdx) * ROW_H;
            SkinPart part = currentOptions.get(i);
            boolean hovered = mouseX >= listX && mouseX < listX + LIST_W - 8
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            boolean selected = part.texture().toString().equals(getPending(currentTab));

            int bg = selected ? 0xFF3A5A8C : (hovered ? 0xFF2A2A2A : 0xFF1A1A1A);
            g.fill(listX, rowY, listX + LIST_W - 8, rowY + ROW_H, bg);

            ResourceLocation assetPath = ResourceLocation.fromNamespaceAndPath(
                    part.texture().getNamespace(),
                    "textures/" + part.texture().getPath() + ".png");
            drawThumbnail(g, assetPath, part.category(), listX + 2, rowY + 2);

            String label = prettyName(part.texture().getPath());
            g.drawString(this.font, label, listX + 22, rowY + 6, 0xFFFFFFFF, false);
        }

        // scrollbar
        if (currentOptions.size() > visibleRows) {
            int sbX = listX + LIST_W - 6;
            g.fill(sbX, listY, sbX + 4, listY + listH, 0xFF2A2A2A);
            int thumbH = Math.max(10, listH * visibleRows / currentOptions.size());
            int thumbY = listY + (listH - thumbH) * scrollOffset / Math.max(1, currentOptions.size() - visibleRows);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFF808080);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static final ResourceLocation NEUTRAL_SKIN_BG = ResourceLocation.fromNamespaceAndPath(
            "magic_realms", "textures/entity/human/skin/base_skin_color_11.png");

    private void drawThumbnail(GuiGraphics g, ResourceLocation asset, SkinCategory category, int x, int y) {
        try {
            switch (category) {
                case CLOTHES -> {
                    // Torso front
                    g.blit(asset, x, y, 16, 16, 20, 20, 8, 12, 64, 64);
                }
                case SKIN -> {
                    // Face of the skin itself
                    g.blit(asset, x, y, 16, 16, 8, 8, 8, 8, 64, 64);
                }
                case EYES, HAIR -> {
                    // Draw neutral skin underneath, then the layer on top
                    g.blit(NEUTRAL_SKIN_BG, x, y, 16, 16, 8, 8, 8, 8, 64, 64);
                    g.blit(asset, x, y, 16, 16, 8, 8, 8, 8, 64, 64);
                    // Also draw the hat/overlay layer for hair (the front-of-head overlay region is at x=40, y=8)
                    if (category == SkinCategory.HAIR) {
                        g.blit(asset, x, y, 16, 16, 40, 8, 8, 8, 64, 64);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private String getPending(SkinCategory cat) {
        return switch (cat) {
            case SKIN -> pendingSkin;
            case CLOTHES -> pendingClothes;
            case EYES -> pendingEyes;
            case HAIR -> pendingHair;
        };
    }

    private void setPending(SkinCategory cat, String tex) {
        switch (cat) {
            case SKIN -> pendingSkin = tex;
            case CLOTHES -> pendingClothes = tex;
            case EYES -> pendingEyes = tex;
            case HAIR -> pendingHair = tex;
        }
        applyPendingToVirtual();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // list row clicks
        int listX = panelX + 4 + PREVIEW_W + 4;
        int listY = panelY + TAB_H + 4 + 20;
        int listH = PANEL_H - TAB_H - 8 - 20;
        int visibleRows = listH / ROW_H;

        if (mouseX >= listX && mouseX < listX + LIST_W - 8 && mouseY >= listY && mouseY < listY + listH) {
            int row = ((int) mouseY - listY) / ROW_H;
            int idx = scrollOffset + row;
            if (idx >= 0 && idx < currentOptions.size()) {
                setPending(currentTab, currentOptions.get(idx).texture().toString());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int visibleRows = (PANEL_H - TAB_H - 8 - 20) / ROW_H;
        int max = Math.max(0, currentOptions.size() - visibleRows);
        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(deltaY), 0, max);
        return true;
    }

    private void renderPreview(GuiGraphics g, int x, int y, int w, int h,
                               int mouseX, int mouseY, float partialTick) {
        if (virtualEntity == null) return;

        // rotate to follow mouse horizontally
        float mouseOffset = ((x + w / 2f) - mouseX) / (w / 2f);
        targetYaw = mouseOffset * 45f;
        currentYaw += (targetYaw - currentYaw) * 0.2f;

        int cx = x + w / 2;
        int cy = y + h - 20;
        float scale = 45f;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 100);
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(currentYaw));

        RenderSystem.runAsFancy(() -> {
            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            dispatcher.render(virtualEntity, 0, 0, 0, 0, partialTick, pose, bufferSource, 0xF000F0);
            bufferSource.endBatch();
            dispatcher.setRenderShadow(true);
        });
        pose.popPose();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String prettyName(String path) {
        int slash = path.lastIndexOf('/');
        String file = slash >= 0 ? path.substring(slash + 1) : path;
        return capitalize(file.replace('_', ' '));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public boolean isPauseScreen() { return false; }
}
