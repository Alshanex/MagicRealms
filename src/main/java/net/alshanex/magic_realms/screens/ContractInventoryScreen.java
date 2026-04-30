package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.network.SwitchAttributesMenuPacket;
import net.alshanex.magic_realms.network.TogglePatrolModePacket;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.TextureComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ContractInventoryScreen extends AbstractContainerScreen<ContractInventoryMenu> {
    private static final ResourceLocation INVENTORY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_inventory.png");

    // UI coordinates
    private static final int ENTITY_RENDER_X = 13;
    private static final int ENTITY_RENDER_Y = 30;
    private static final int ENTITY_RENDER_WIDTH = 56;
    private static final int ENTITY_RENDER_HEIGHT = 83;

    private static final int TAB_1_X = 120;
    private static final int TAB_1_Y = 3;
    private static final int TAB_3_X = 204;
    private static final int TAB_3_Y = 3;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_HEIGHT = 10;

    private static final int SYMBOL_X = 98;
    private static final int SYMBOL_Y = 20;

    private float mouseX = 0.0f;
    private float mouseY = 0.0f;
    private float targetHeadYaw = 0.0f;
    private float targetHeadPitch = 0.0f;
    private float currentHeadYaw = 0.0f;
    private float currentHeadPitch = 0.0f;

    private static final int EXP_BAR_X = 12;
    private static final int EXP_BAR_Y = 114;
    private static final int EXP_BAR_WIDTH = 96;
    private static final int EXP_BAR_HEIGHT = 3;

    private static final int ATTRIBUTES_START_X = 13;
    private static final int ATTRIBUTES_START_Y = 135;
    private static final int ATTRIBUTES_END_X = ATTRIBUTES_START_X + 85;

    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 2;

    private final EntitySnapshot snapshot;
    private final AbstractMercenaryEntity entity;
    private static final Map<String, AbstractMercenaryEntity> virtualEntityCache = new LinkedHashMap<>(8, 0.75f, true);

    private Button patrolToggleButton;

    public ContractInventoryScreen(ContractInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.snapshot = menu.getSnapshot();
        this.entity = menu.getEntity();

        this.imageWidth = 256;
        this.imageHeight = 250;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000; // Hide default labels
        this.titleLabelY = 10000;

        // Patrol/follow toggle button. Same coords in both screens so it stays in place when switching tabs.
        int buttonX = leftPos + 12;
        int buttonY = topPos;
        int buttonW = 100;
        int buttonH = 14;

        this.patrolToggleButton = Button.builder(
                getPatrolButtonLabel(),
                b -> onPatrolButtonClicked()
        ).bounds(buttonX, buttonY, buttonW, buttonH).build();
        this.addRenderableWidget(this.patrolToggleButton);
    }

    private Component getPatrolButtonLabel() {
        boolean patrolling = this.menu.isPatrolMode();
        String key = patrolling
                ? "ui.magic_realms.button.switch_to_follow"
                : "ui.magic_realms.button.switch_to_patrol";
        return Component.translatable(key);
    }

    private void onPatrolButtonClicked() {
        if (minecraft == null || minecraft.level == null) return;
        UUID entityUUID = this.menu.getSnapshot() != null ? this.menu.getSnapshot().entityUUID : null;
        if (entityUUID == null) return;

        boolean newState = !this.menu.isPatrolMode();
        this.menu.setPatrolModeLocal(newState);
        PacketDistributor.sendToServer(new TogglePatrolModePacket(entityUUID));

        if (this.patrolToggleButton != null) {
            this.patrolToggleButton.setMessage(getPatrolButtonLabel());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, INVENTORY_TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(INVENTORY_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderEntity3D(guiGraphics);
        renderStats(guiGraphics);
        renderClassSymbol(guiGraphics);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private boolean renderEntity3D(GuiGraphics guiGraphics) {
        AbstractMercenaryEntity entityToRender = entity;

        // If we don't have the real entity, use the virtual entity with cache
        if (entityToRender == null && snapshot != null) {
            try {
                entityToRender = getOrCreateVirtualEntity();
                if (entityToRender != null) {
                    // MagicRealms.LOGGER.debug("Using cached/created virtual entity for 3D rendering");
                }
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Failed to get virtual entity: {}", e.getMessage());
                return false;
            }
        }

        if (entityToRender == null) {
            MagicRealms.LOGGER.debug("Cannot render 3D: no entity available");
            return false;
        }

        try {
            // Update head rotation based on mouse position
            updateHeadRotation(0); // You can pass partialTick if available

            // Store original head rotation
            float originalHeadYaw = entityToRender.getYHeadRot();
            float originalHeadPitch = entityToRender.getXRot();

            // Apply mouse look rotation
            entityToRender.setYHeadRot(currentHeadYaw);
            entityToRender.setXRot(currentHeadPitch);

            int entityX = leftPos + ENTITY_RENDER_X;
            int entityY = topPos + ENTITY_RENDER_Y;

            float centerX = entityX + ENTITY_RENDER_WIDTH / 2.0f + 1.0f;
            float centerY = entityY + ENTITY_RENDER_HEIGHT / 2.0f - 8.0f;
            float scale = 35.0f;

            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf cameraOrientation = new Quaternionf().rotateX(0.0f);

            float frontFacingAngle = 180.0f;
            pose.rotateY(frontFacingAngle * (float) (Math.PI / 180.0));

            Vector3f translate = new Vector3f(0.0F, 1.2F, 0.0F);

            renderEntityInInventory(
                    guiGraphics, centerX, centerY, scale, translate, pose, cameraOrientation,
                    entityToRender, entityX, entityY, entityX + ENTITY_RENDER_WIDTH, entityY + ENTITY_RENDER_HEIGHT
            );

            // Restore original head rotation to avoid affecting the actual entity
            entityToRender.setYHeadRot(originalHeadYaw);
            entityToRender.setXRot(originalHeadPitch);

            return true;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error rendering entity 3D: {}", e.getMessage(), e);
            return false;
        }
    }

    private void updateMouseLookTarget() {
        // Get entity render bounds
        int entityCenterX = leftPos + ENTITY_RENDER_X + ENTITY_RENDER_WIDTH / 2;
        int entityCenterY = topPos + ENTITY_RENDER_Y + ENTITY_RENDER_HEIGHT / 2;

        // Calculate relative mouse position
        float relativeX = mouseX - entityCenterX;
        float relativeY = mouseY - entityCenterY;

        // Convert to angles (adjust sensitivity as needed)
        float sensitivity = 0.8f;
        targetHeadYaw = -relativeX * sensitivity; // Negative for correct direction
        targetHeadPitch = relativeY * sensitivity * 0.5f; // Reduced vertical sensitivity

        // Clamp angles to reasonable limits
        targetHeadYaw = Math.max(-45.0f, Math.min(45.0f, targetHeadYaw));
        targetHeadPitch = Math.max(-20.0f, Math.min(20.0f, targetHeadPitch));
    }

    private void updateHeadRotation(float partialTick) {
        float lerpSpeed = 0.15f; // Adjust for smoother/snappier movement

        currentHeadYaw = lerp(currentHeadYaw, targetHeadYaw, lerpSpeed);
        currentHeadPitch = lerp(currentHeadPitch, targetHeadPitch, lerpSpeed);
    }

    private float lerp(float start, float end, float factor) {
        return start + factor * (end - start);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        this.mouseX = (float) mouseX;
        this.mouseY = (float) mouseY;
        updateMouseLookTarget();
    }

    private AbstractMercenaryEntity getOrCreateVirtualEntity() {
        if (snapshot == null || minecraft.level == null) {
            return null;
        }

        String cacheKey = createCacheKey();
        EntityType<? extends AbstractMercenaryEntity> entityType = menu.getEntityType();

        // Check cache first
        AbstractMercenaryEntity cachedEntity = virtualEntityCache.get(cacheKey);
        if (cachedEntity != null && cachedEntity.level() == minecraft.level &&
                cachedEntity.getType() == entityType) {
            if (menu != null && menu.getEquipmentContainer() != null) {
                applyEquipmentFromContainer(cachedEntity, menu.getEquipmentContainer());
            }
            return cachedEntity;
        }

        // Create new virtual entity
        AbstractMercenaryEntity virtualEntity = createVirtualEntityForSnapshot();
        if (virtualEntity != null) {
            virtualEntityCache.put(cacheKey, virtualEntity);

            // Clean cache if too large
            if (virtualEntityCache.size() > 5) {
                String oldestKey = virtualEntityCache.keySet().iterator().next();
                virtualEntityCache.remove(oldestKey);
            }
        }

        return virtualEntity;
    }

    private AbstractMercenaryEntity createVirtualEntityForSnapshot() {
        if (snapshot == null || minecraft.level == null) {
            return null;
        }

        try {
            EntityType<? extends AbstractMercenaryEntity> entityType = menu.getEntityType();
            AbstractMercenaryEntity virtualEntity = entityType.create(minecraft.level);
            if (virtualEntity == null) {
                MagicRealms.LOGGER.error("Failed to create virtual entity of type: {}", entityType);
                return null;
            }

            // Configure basic properties
            virtualEntity.setGender(snapshot.gender);
            virtualEntity.setEntityClass(snapshot.entityClass);
            virtualEntity.setEntityName(snapshot.entityName);
            virtualEntity.setStarLevel(snapshot.starLevel);
            virtualEntity.setHasShield(snapshot.hasShield);
            virtualEntity.setIsArcher(snapshot.isArcher);
            virtualEntity.setInitialized(true);

            setEntityUUID(virtualEntity, snapshot.entityUUID);

            // Handle texture components for RandomHumanEntity
            if (virtualEntity instanceof RandomHumanEntity randomHuman && snapshot.textureComponents != null) {
                randomHuman.setTextureMetadata(snapshot.textureComponents);
                forceGenerateTextureComponents(randomHuman, snapshot.textureComponents);
            }

            // Apply equipment
            if (menu != null && menu.getEquipmentContainer() != null) {
                applyEquipmentFromContainer(virtualEntity, menu.getEquipmentContainer());
            }

            virtualEntity.setPos(0, 0, 0);
            return virtualEntity;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to create virtual entity: {}", e.getMessage(), e);
            return null;
        }
    }

    // Force texture generation for virtual entities
    private void forceGenerateTextureComponents(RandomHumanEntity randomHuman, CompoundTag textureMetadata) {
        if (!minecraft.level.isClientSide() || textureMetadata.isEmpty()) {
            return;
        }
        try {
            TextureComponents components = TextureComponents.fromMetadata(textureMetadata);
            setTextureComponentsReflective(randomHuman, components);
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to force generate texture components for virtual entity", e);
        }
    }

    private void setTextureComponentsReflective(RandomHumanEntity randomHuman, TextureComponents components) {
        try {
            Field field = RandomHumanEntity.class.getDeclaredField("textureComponents");
            field.setAccessible(true);
            field.set(randomHuman, components);
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to set texture components reflectively", e);
        }
    }

    private String createCacheKey() {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(menu.getEntityType().toString()).append("_");

        if (snapshot.textureComponents != null) {
            keyBuilder.append(snapshot.textureComponents.hashCode());
        } else {
            keyBuilder.append("null");
        }

        keyBuilder.append("_").append(snapshot.gender.name());
        keyBuilder.append("_").append(snapshot.entityClass.name());
        keyBuilder.append("_").append(snapshot.starLevel);
        keyBuilder.append("_").append(snapshot.hasShield);
        keyBuilder.append("_").append(snapshot.isArcher);
        keyBuilder.append("_").append(snapshot.entityName);
        keyBuilder.append("_").append(snapshot.entityUUID);

        if (menu != null && menu.getEquipmentContainer() != null) {
            keyBuilder.append("_eq:");
            for (int i = 0; i < 6; i++) {
                keyBuilder.append(menu.getEquipmentContainer().getItem(i).hashCode()).append(",");
            }
        }

        return keyBuilder.toString();
    }

    private void setEntityUUID(AbstractMercenaryEntity entity, UUID originalUUID) {
        try {
            if (originalUUID != null) {
                java.lang.reflect.Field uuidField = Entity.class.getDeclaredField("uuid");
                uuidField.setAccessible(true);
                uuidField.set(entity, originalUUID);
                //MagicRealms.LOGGER.debug("Set exact UUID for virtual entity: {}", originalUUID);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to set exact UUID: {}", e.getMessage());
        }
    }

    private void applyEquipmentFromContainer(AbstractMercenaryEntity entity, Container equipmentContainer) {
        try {
            entity.setItemSlot(EquipmentSlot.HEAD, equipmentContainer.getItem(0).copy());
            entity.setItemSlot(EquipmentSlot.CHEST, equipmentContainer.getItem(1).copy());
            entity.setItemSlot(EquipmentSlot.LEGS, equipmentContainer.getItem(2).copy());
            entity.setItemSlot(EquipmentSlot.FEET, equipmentContainer.getItem(3).copy());
            entity.setItemSlot(EquipmentSlot.MAINHAND, equipmentContainer.getItem(4).copy());
            entity.setItemSlot(EquipmentSlot.OFFHAND, equipmentContainer.getItem(5).copy());
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to apply equipment to virtual entity: {}", e.getMessage());
        }
    }

    public static void renderEntityInInventory(
            GuiGraphics guiGraphics,
            float x,
            float y,
            float scale,
            Vector3f translate,
            Quaternionf pose,
            @Nullable Quaternionf cameraOrientation,
            LivingEntity entity,
            int scissorX1,
            int scissorY1,
            int scissorX2,
            int scissorY2
    ) {
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((double)x, (double)y, 50.0);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().translate(translate.x, translate.y, translate.z);
        guiGraphics.pose().mulPose(pose);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        if (cameraOrientation != null) {
            entityRenderDispatcher.overrideCameraOrientation(
                    cameraOrientation.conjugate(new Quaternionf()).rotateY((float) Math.PI)
            );
        }

        entityRenderDispatcher.setRenderShadow(false);

        RenderSystem.runAsFancy(() -> {
            entityRenderDispatcher.render(
                    entity,
                    0.0,
                    0.0,
                    0.0,
                    0.0F,
                    1.0F,
                    guiGraphics.pose(),
                    guiGraphics.bufferSource(),
                    15728880
            );
        });

        guiGraphics.flush();
        entityRenderDispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();

        guiGraphics.disableScissor();
    }

    private void renderStats(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        CompoundTag attributes = snapshot.attributes;

        // Render experience bar and level
        renderExperienceBar(guiGraphics);

        int startX = leftPos + ATTRIBUTES_START_X;
        int endX = leftPos + ATTRIBUTES_END_X;
        int firstLineY = topPos + ATTRIBUTES_START_Y;
        int totalWidth = endX - startX;

        // Prepare attribute strings
        float health = attributes.contains("health") ? (float) attributes.getDouble("health") : 20.0f;
        float maxHealth = attributes.contains("max_health") ? (float) attributes.getDouble("max_health") : 20.0f;
        String healthText = String.format("%.0f/%.0f", health, maxHealth);

        double armor = attributes.contains("armor") ? attributes.getDouble("armor") : 0.0;
        String armorText = String.format("%.1f", armor);

        double damage = attributes.contains("attack_damage") ? attributes.getDouble("attack_damage") : 1.0;
        String damageText = String.format("%.1f", damage);

        // Calculate widths for first line (health and armor)
        int healthWidth = ICON_SIZE + ICON_SPACING + font.width(healthText);
        int armorWidth = ICON_SIZE + ICON_SPACING + font.width(armorText);
        int firstLineContentWidth = healthWidth + armorWidth;
        int firstLineSpacing = totalWidth - firstLineContentWidth;

        // Ensure spacing is not negative
        if (firstLineSpacing < 0) firstLineSpacing = 2;

        // Render first line: Health and Armor
        int currentX = startX;

        // Render Health
        currentX = renderAttributeWithIcon(guiGraphics, currentX, firstLineY,
                ResourceLocation.withDefaultNamespace("hud/heart/container"),
                ResourceLocation.withDefaultNamespace("hud/heart/full"),
                healthText, 0xFF5555);

        currentX += firstLineSpacing;

        // Render Damage
        renderAttributeWithIcon(guiGraphics, currentX, firstLineY,
                null,
                ResourceLocation.withDefaultNamespace("hud/armor_full"),
                armorText, 0xAAAAAA);
    }

    private void renderExperienceBar(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int barX = leftPos + EXP_BAR_X;
        int barY = topPos + EXP_BAR_Y;

        // Get level data
        int currentLevel = snapshot.currentLevel;
        int currentExp = snapshot.experiencePoints;

        // Calculate experience for next level (same formula as in KillTrackerData)
        int expForCurrentLevel = (int) (200 * currentLevel * (net.alshanex.magic_realms.Config.xpNeededMultiplier / 100));
        int expForNextLevel = (int) (200 * (currentLevel + 1) * (net.alshanex.magic_realms.Config.xpNeededMultiplier / 100));

        int expIntoLevel = currentExp - expForCurrentLevel;
        int expNeeded = expForNextLevel - expForCurrentLevel;

        // Calculate progress (0.0 to 1.0)
        float progress = expNeeded > 0 ? (float) expIntoLevel / expNeeded : 0.0f;
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        // Render experience bar background (dark)
        guiGraphics.fill(barX, barY, barX + EXP_BAR_WIDTH, barY + EXP_BAR_HEIGHT, 0xFF000000);

        // Render experience bar fill (green)
        int fillWidth = (int) (EXP_BAR_WIDTH * progress);
        if (fillWidth > 0) {
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + EXP_BAR_HEIGHT, 0xFF00FF00);
        }

        // Render border
        // Top border
        guiGraphics.fill(barX, barY - 1, barX + EXP_BAR_WIDTH, barY, 0xFF555555);
        // Bottom border
        guiGraphics.fill(barX, barY + EXP_BAR_HEIGHT, barX + EXP_BAR_WIDTH, barY + EXP_BAR_HEIGHT + 1, 0xFF555555);
        // Left border
        guiGraphics.fill(barX - 1, barY, barX, barY + EXP_BAR_HEIGHT, 0xFF555555);
        // Right border
        guiGraphics.fill(barX + EXP_BAR_WIDTH, barY, barX + EXP_BAR_WIDTH + 1, barY + EXP_BAR_HEIGHT, 0xFF555555);

        // Render level number centered below the bar (vanilla style)
        String levelText = String.valueOf(currentLevel);
        int textWidth = font.width(levelText);
        int textX = barX + (EXP_BAR_WIDTH / 2) - (textWidth / 2);
        int textY = barY + EXP_BAR_HEIGHT + 2;

        // Render level text with shadow (green like vanilla)
        guiGraphics.drawString(font, levelText, textX, textY, 0x80FF20, true);
    }

    private int renderAttributeWithIcon(GuiGraphics guiGraphics, int x, int y,
                                        ResourceLocation backgroundSprite,
                                        ResourceLocation iconSprite,
                                        String value, int color) {
        // Render background sprite if present (for health container)
        if (backgroundSprite != null) {
            guiGraphics.blitSprite(backgroundSprite, x, y, ICON_SIZE, ICON_SIZE);
        }

        // Render main icon sprite
        guiGraphics.blitSprite(iconSprite, x, y, ICON_SIZE, ICON_SIZE);

        // Render value text next to icon
        int textX = x + ICON_SIZE + ICON_SPACING;
        guiGraphics.drawString(font, value, textX, y + 1, color, true);

        // Return next X position
        int textWidth = font.width(value);
        return textX + textWidth + ICON_SPACING;
    }

    private void renderClassSymbol(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        ResourceLocation frame = ResourceLocation.withDefaultNamespace("advancements/challenge_frame_obtained");

        ItemStack symbolItem = getSymbolItemForClass();
        if (symbolItem.isEmpty()) return;

        int frameX = leftPos + SYMBOL_X - 4;
        int frameY = topPos + SYMBOL_Y - 4;
        int symbolX = leftPos + SYMBOL_X;
        int symbolY = topPos + SYMBOL_Y;

        guiGraphics.blitSprite(frame, frameX, frameY, 24, 24);
        guiGraphics.renderItem(symbolItem, symbolX, symbolY);
    }

    private ItemStack getSymbolItemForClass() {
        EntityClass entityClass = snapshot.entityClass;

        switch (entityClass) {
            case MAGE -> {
                try {
                    return new ItemStack(ItemRegistry.GOLD_SPELL_BOOK.get());
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Could not find gold_spell_book item: {}", e.getMessage());
                    return new ItemStack(Items.BOOK);
                }
            }
            case WARRIOR -> {
                if (snapshot.hasShield) {
                    return new ItemStack(Items.SHIELD);
                } else {
                    return new ItemStack(Items.IRON_AXE);
                }
            }
            case ROGUE -> {
                if (snapshot.isArcher) {
                    return new ItemStack(Items.ARROW);
                } else {
                    try {
                        return new ItemStack(ItemRegistry.WEAPON_PARTS.get());
                    } catch (Exception e) {
                        MagicRealms.LOGGER.warn("Could not find weapon_parts item: {}", e.getMessage());
                        return new ItemStack(Items.GOLDEN_SWORD);
                    }
                }
            }
            default -> {
                return ItemStack.EMPTY;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        // Tab clicking - switch to attributes menu for non-inventory tabs
        if (relativeX >= TAB_1_X - 2 && relativeX < TAB_1_X + (TAB_WIDTH + 2) * 2 &&
                relativeY >= TAB_1_Y - 2 && relativeY < TAB_1_Y + TAB_HEIGHT + 2) {
            switchToAttributesMenu(ContractHumanInfoMenu.Tab.IRON_SPELLS);
            return true;
        }

        // Inventory tab - stay on current screen (do nothing)
        if (relativeX >= TAB_3_X - 2 && relativeX < TAB_3_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_3_Y - 2 && relativeY < TAB_3_Y + TAB_HEIGHT + 2) {
            return true; // Already on inventory tab
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchToAttributesMenu(ContractHumanInfoMenu.Tab newTab) {
        // Send packet to server to switch to attributes menu
        if (minecraft.level.isClientSide()) {
            PacketDistributor.sendToServer(new SwitchAttributesMenuPacket(newTab));
        }
    }

    public static void clearVirtualEntityCache() {
        virtualEntityCache.clear();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
