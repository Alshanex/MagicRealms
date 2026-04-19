package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.network.SwitchTabPacket;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.EntitySnapshot;
import net.alshanex.magic_realms.skins_management.TextureComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ContractHumanInfoScreen extends AbstractContainerScreen<ContractHumanInfoMenu> {
    private static final ResourceLocation IRON_SPELLS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_iron_spells.png");

    // Color constants for improved readability
    private static final int HEADER_COLOR = 0xFFD700;      // Gold for headers
    private static final int LABEL_COLOR = 0xE0E0E0;       // Light gray for labels
    private static final int VALUE_COLOR = 0xFFFFFF;       // White for values
    private static final int POSITIVE_COLOR = 0x00FF88;    // Green for positive values
    private static final int NEGATIVE_COLOR = 0xFF6B6B;    // Red for negative values
    private static final int NEUTRAL_COLOR = 0x87CEEB;     // Sky blue for neutral values
    private static final int BACKGROUND_TINT = 0x22000000; // Semi-transparent background
    private static final int SECTION_BG = 0x33000000;      // Section background
    private static final int SEPARATOR_COLOR = 0x66FFFFFF; // Line separator color
    private static final int ALTERNATE_ROW = 0x11FFFFFF;   // Alternating row background

    private final EntitySnapshot snapshot;
    private final AbstractMercenaryEntity entity;
    private ContractHumanInfoMenu.Tab currentTab = ContractHumanInfoMenu.Tab.IRON_SPELLS;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isScrolling = false;
    private int lastMouseY = 0;
    private int attributeRowCounter = 0;

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

    private static final int ATTRIBUTES_X = 132;
    private static final int ATTRIBUTES_Y = 25;
    private static final int ATTRIBUTES_WIDTH = 112;
    private static final int ATTRIBUTES_HEIGHT = 124;
    private static final int TEXT_HEIGHT = 9;
    private static final int LINE_SPACING = 3;
    private static final int LINE_HEIGHT = TEXT_HEIGHT + LINE_SPACING;

    private static final int LABEL_WIDTH = 80;
    private static final int VALUE_X_OFFSET = 76;

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

    // Virtual entity cache - now uses base class
    private static final Map<String, AbstractMercenaryEntity> virtualEntityCache = new HashMap<>();

    public ContractHumanInfoScreen(ContractHumanInfoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.snapshot = menu.getSnapshot();
        this.entity = menu.getEntity();

        this.imageWidth = 256;
        this.imageHeight = 250;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
        this.scrollOffset = 0;
        calculateMaxScroll();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ResourceLocation texture = getCurrentTexture();
        RenderSystem.setShaderTexture(0, texture);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render entity and other elements first
        renderEntity3D(guiGraphics);
        renderStats(guiGraphics);
        renderClassSymbol(guiGraphics);

        // Calculate actual screen coordinates for scissor
        int scissorLeft = leftPos + ATTRIBUTES_X;
        int scissorTop = topPos + ATTRIBUTES_Y;
        int scissorRight = scissorLeft + ATTRIBUTES_WIDTH;
        int scissorBottom = scissorTop + ATTRIBUTES_HEIGHT;

        // Enable scissor with correct coordinates
        guiGraphics.enableScissor(scissorLeft, scissorTop, scissorRight, scissorBottom);

        // Reset row counter for each render
        attributeRowCounter = 0;

        // Render scrollable content
        renderIronSpellsAttributesScrollable(guiGraphics);

        guiGraphics.disableScissor();

        // Render scroll indicator if needed
        if (maxScroll > 0) {
            renderScrollIndicator(guiGraphics);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderClassSymbolTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderClassSymbolTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (snapshot == null) return;
        if (getSymbolItemForClass().isEmpty()) return;

        int symbolX = leftPos + SYMBOL_X;
        int symbolY = topPos + SYMBOL_Y;

        boolean hovering = mouseX >= symbolX && mouseX < symbolX + 16 && mouseY >= symbolY && mouseY < symbolY + 16;
        if (!hovering) return;

        Component label = getClassDisplayName();
        guiGraphics.renderTooltip(font, label, mouseX, mouseY);
    }

    private Component getClassDisplayName() {
        EntityClass cls = snapshot.entityClass;
        if (cls == null) {
            return Component.translatable("gui.magic_realms.human_info.class.unknown");
        }
        return switch (cls) {
            case MAGE -> Component.translatable("gui.magic_realms.human_info.class.mage");
            case WARRIOR -> snapshot.hasShield
                    ? Component.translatable("gui.magic_realms.human_info.class.warrior_shield")
                    : Component.translatable("gui.magic_realms.human_info.class.warrior");
            case ROGUE -> snapshot.isArcher
                    ? Component.translatable("gui.magic_realms.human_info.class.archer")
                    : Component.translatable("gui.magic_realms.human_info.class.assassin");
        };
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
                // Set the texture metadata from snapshot
                randomHuman.setTextureMetadata(snapshot.textureComponents);

                // Force generate client-side texture components
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

    // Cache key
    private String createCacheKey() {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(menu.getEntityType().toString()).append("_");

        // Use texture components hash instead of textureUUID
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

        // Also include equipment state since that affects rendering
        if (menu != null && menu.getEquipmentContainer() != null) {
            keyBuilder.append("_eq:");
            for (int i = 0; i < 6; i++) { // 6 equipment slots
                keyBuilder.append(menu.getEquipmentContainer().getItem(i).hashCode()).append(",");
            }
        }

        return keyBuilder.toString();
    }

    // Updated to work with base class
    private void setEntityUUID(AbstractMercenaryEntity entity, UUID originalUUID) {
        try {
            if (originalUUID != null) {
                // Use reflection to set the exact UUID
                java.lang.reflect.Field uuidField = Entity.class.getDeclaredField("uuid");
                uuidField.setAccessible(true);
                uuidField.set(entity, originalUUID);
                //MagicRealms.LOGGER.debug("Set exact UUID for virtual entity: {}", originalUUID);
            }
        } catch (IllegalArgumentException e) {
            MagicRealms.LOGGER.warn("Invalid UUID format: {}, error: {}", originalUUID, e.getMessage());
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to set exact UUID: {}", e.getMessage());
        }
    }

    // Updated to work with base class
    private void applyEquipmentFromContainer(AbstractMercenaryEntity entity, Container equipmentContainer) {
        try {
            // Apply armor equipment
            entity.setItemSlot(EquipmentSlot.HEAD, equipmentContainer.getItem(0).copy());
            entity.setItemSlot(EquipmentSlot.CHEST, equipmentContainer.getItem(1).copy());
            entity.setItemSlot(EquipmentSlot.LEGS, equipmentContainer.getItem(2).copy());
            entity.setItemSlot(EquipmentSlot.FEET, equipmentContainer.getItem(3).copy());

            // Apply hand equipment
            entity.setItemSlot(EquipmentSlot.MAINHAND, equipmentContainer.getItem(4).copy());
            entity.setItemSlot(EquipmentSlot.OFFHAND, equipmentContainer.getItem(5).copy());

        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to apply equipment to virtual entity: {}", e.getMessage());
        }
    }

    // Main 3D rendering method
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

    // Helper methods for rendering UI elements
    private int renderSectionHeader(GuiGraphics guiGraphics, String title, int x, int y, ChatFormatting color) {
        int backgroundStartX = x - 4;
        int backgroundEndX = x + ATTRIBUTES_WIDTH - 10;
        int backgroundStartY = y - 2;
        int backgroundEndY = y + 11;

        guiGraphics.fill(backgroundStartX, backgroundStartY, backgroundEndX, backgroundEndY, SECTION_BG);

        Component headerComponent = Component.literal(title).withStyle(color, ChatFormatting.BOLD);
        guiGraphics.drawString(font, headerComponent, x + 2, y + 1, HEADER_COLOR, true);

        return y + 12;
    }

    private int renderSectionSeparator(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x, y, x + width - 10, y + 1, SEPARATOR_COLOR);
        return y + 4;
    }

    private void renderAttributeBackground(GuiGraphics guiGraphics, int x, int y, int width, boolean alternate) {
        if (alternate) {
            guiGraphics.fill(x - 1, y - 1, x + width + 1, y + TEXT_HEIGHT + 1, ALTERNATE_ROW);
        }
    }

    private int getValueColor(double value, boolean isPercentage, String attributeKey) {
        if (attributeKey.contains("resist") || attributeKey.contains("armor") || attributeKey.contains("health")) {
            return value > 0 ? POSITIVE_COLOR : NEUTRAL_COLOR;
        } else if (attributeKey.contains("damage") || attributeKey.contains("power")) {
            return value > (isPercentage ? 100 : 1) ? POSITIVE_COLOR : NEUTRAL_COLOR;
        } else if (attributeKey.contains("regen") || attributeKey.contains("speed")) {
            return value > (isPercentage ? 100 : 1) ? POSITIVE_COLOR : NEUTRAL_COLOR;
        }
        return VALUE_COLOR;
    }

    private int getImprovedSchoolColor(ResourceLocation schoolRL) {
        SchoolType schoolType = SchoolRegistry.getSchool(schoolRL);
        Vector3f color = schoolType.getTargetingColor();
        return ((int)(color.x * 255) << 16) | ((int)(color.y * 255) << 8) | (int)(color.z * 255);
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

    // Input handling methods
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        // Tab clicking
        if (relativeX >= TAB_1_X - 2 && relativeX < TAB_1_X + (TAB_WIDTH + 2) * 2 &&
                relativeY >= TAB_1_Y - 2 && relativeY < TAB_1_Y + TAB_HEIGHT + 2) {
            return true;
        }

        if (relativeX >= TAB_3_X - 2 && relativeX < TAB_3_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_3_Y - 2 && relativeY < TAB_3_Y + TAB_HEIGHT + 2) {
            switchToInventoryMenu();
            return true;
        }

        // Check if clicking on scroll bar
        int scrollBarX = ATTRIBUTES_X + ATTRIBUTES_WIDTH - 6;
        int scrollBarY = ATTRIBUTES_Y;
        int scrollBarHeight = ATTRIBUTES_HEIGHT;

        if (relativeX >= scrollBarX && relativeX < scrollBarX + 5 &&
                relativeY >= scrollBarY && relativeY < scrollBarY + scrollBarHeight && maxScroll > 0) {
            isScrolling = true;
            lastMouseY = (int) mouseY;

            int thumbHeight = Math.max(12, (scrollBarHeight * scrollBarHeight) / (scrollBarHeight + maxScroll));
            int clickY = (int) relativeY - scrollBarY;

            float scrollRatio = (float) clickY / scrollBarHeight;
            scrollOffset = Math.max(0, Math.min(maxScroll, (int) (scrollRatio * maxScroll)));

            return true;
        }

        if (relativeX >= ATTRIBUTES_X && relativeX < ATTRIBUTES_X + ATTRIBUTES_WIDTH &&
                relativeY >= ATTRIBUTES_Y && relativeY < ATTRIBUTES_Y + ATTRIBUTES_HEIGHT) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchToInventoryMenu() {
        // Send packet to server to switch to inventory menu
        if (minecraft.level.isClientSide()) {
            PacketDistributor.sendToServer(new SwitchTabPacket("INVENTORY")); // Special case for inventory
        }
        //MagicRealms.LOGGER.debug("Client requested switch to inventory menu");
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isScrolling && button == 0 && maxScroll > 0) {
            int currentMouseY = (int) mouseY;
            int mouseDelta = currentMouseY - lastMouseY;

            int scrollBarHeight = ATTRIBUTES_HEIGHT;
            int thumbHeight = Math.max(12, (scrollBarHeight * scrollBarHeight) / (scrollBarHeight + maxScroll));

            float scrollRatio = (float) maxScroll / (scrollBarHeight - thumbHeight);
            int scrollDelta = (int) (mouseDelta * scrollRatio);

            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + scrollDelta));
            lastMouseY = currentMouseY;

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        // Only handle scrolling for non-inventory tabs
        if (relativeX >= ATTRIBUTES_X && relativeX < ATTRIBUTES_X + ATTRIBUTES_WIDTH &&
                relativeY >= ATTRIBUTES_Y && relativeY < ATTRIBUTES_Y + ATTRIBUTES_HEIGHT) {

            int scrollAmount = (int) (deltaY * -10);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + scrollAmount));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    // Utility methods
    private ResourceLocation getCurrentTexture() {
        return switch (currentTab) {
            case IRON_SPELLS -> IRON_SPELLS_TEXTURE;
        };
    }

    private void calculateMaxScroll() {
        int totalLines = getTotalAttributeLines();
        int visibleLines = ATTRIBUTES_HEIGHT / LINE_HEIGHT;
        this.maxScroll = Math.max(0, (totalLines - visibleLines) * LINE_HEIGHT);
    }

    private int getTotalAttributeLines() {
        switch (currentTab) {
            case IRON_SPELLS -> {
                return getIronSpellsAttributeLines() + getApothicAttributeLines();
            }
            default -> {
                return 0;
            }
        }
    }

    private int getIronSpellsAttributeLines() {
        int lines = 2;
        lines += 8;
        lines += 2;
        try {
            List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
            lines += schools.size();
        } catch (Exception e) {
            lines += 9;
        }
        lines += 2;
        try {
            List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
            lines += schools.size();
        } catch (Exception e) {
            lines += 9;
        }
        if (snapshot != null && snapshot.entityClass == EntityClass.MAGE) {
            lines += 2;
            lines += Math.max(1, snapshot.magicSchools.size());
        }
        lines += 2;
        if (snapshot != null) {
            lines += Math.max(1, snapshot.entitySpells.size());
        }
        return lines;
    }

    private int getApothicAttributeLines() {
        int lines = 2;
        lines += 3;
        lines += 2;
        lines += 4;
        lines += 2;
        lines += 4;
        return lines;
    }

    private void renderScrollIndicator(GuiGraphics guiGraphics) {
        int scrollBarX = leftPos + ATTRIBUTES_X + ATTRIBUTES_WIDTH - 6;
        int scrollBarY = topPos + ATTRIBUTES_Y;
        int scrollBarHeight = ATTRIBUTES_HEIGHT;

        guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 5, scrollBarY + scrollBarHeight, 0x88000000);

        if (maxScroll > 0) {
            int thumbHeight = Math.max(12, (scrollBarHeight * scrollBarHeight) / (scrollBarHeight + maxScroll));
            int thumbY = scrollBarY + (int)((scrollBarHeight - thumbHeight) * (scrollOffset / (float)maxScroll));

            guiGraphics.fill(scrollBarX + 1, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0xFFAAAAAA);
            guiGraphics.fill(scrollBarX, thumbY, scrollBarX + 5, thumbY + 1, 0xFFFFFFFF);
            guiGraphics.fill(scrollBarX, thumbY + thumbHeight - 1, scrollBarX + 5, thumbY + thumbHeight, 0xFFFFFFFF);
        }
    }

    // Scrollable content rendering methods
    private void renderIronSpellsAttributesScrollable(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y - scrollOffset;

        if (snapshot.entityClass == EntityClass.MAGE) {
            y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
            y = renderSectionHeader(guiGraphics, "Schools", x, y, ChatFormatting.GOLD);
            y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

            if (snapshot.magicSchools.isEmpty()) {
                renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);
                guiGraphics.drawString(font, Component.literal("None").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, true);
                y += LINE_HEIGHT;
                attributeRowCounter++;
            } else {
                for (String schoolId : snapshot.magicSchools) {
                    String schoolName = extractSchoolName(schoolId);
                    schoolName = capitalizeFirst(schoolName);
                    int color = getImprovedSchoolColor(ResourceLocation.parse(schoolId));

                    renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);
                    guiGraphics.drawString(font, Component.literal("• " + schoolName).withColor(color), x, y, 0xFFFFFF, true);
                    y += LINE_HEIGHT;
                    attributeRowCounter++;
                }
            }
        }

        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
        y = renderSectionHeader(guiGraphics, "Spells", x, y, ChatFormatting.YELLOW);
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        if (snapshot.entitySpells.isEmpty()) {
            renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);
            guiGraphics.drawString(font, Component.literal("No spells").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, true);
            y += LINE_HEIGHT;
            attributeRowCounter++;
        } else {
            List<AbstractSpell> enabledSpells = SpellRegistry.getEnabledSpells();

            for (String spellName : snapshot.entitySpells) {
                AbstractSpell foundSpell = enabledSpells.stream()
                        .filter(spell -> spell.getSpellName().equals(spellName))
                        .findFirst()
                        .orElse(null);

                if (foundSpell != null && foundSpell != SpellRegistry.none()) {
                    String componentId = foundSpell.getComponentId();
                    Component displayNameComponent = Component.translatable(componentId);
                    String displayName = truncateText(displayNameComponent.getString(), ATTRIBUTES_WIDTH - 10);
                    int color = getImprovedSchoolColor(foundSpell.getSchoolType().getId());

                    renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);
                    guiGraphics.drawString(font, Component.literal("• " + displayName).withColor(color), x, y, 0xFFFFFF, true);
                    y += LINE_HEIGHT;
                    attributeRowCounter++;
                }
            }
        }

        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
        y = renderSectionHeader(guiGraphics, "Iron's Spells", x, y, ChatFormatting.AQUA);
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        CompoundTag attributes = snapshot.attributes;

        y = renderAttributeWithTruncation(guiGraphics, "Max Mana", attributes, "max_mana", 100.0, "%.0f", x, y, ChatFormatting.BLUE);
        y = renderAttributeWithTruncation(guiGraphics, "Mana Regen", attributes, "mana_regen", 1.0, "%.2f", x, y, ChatFormatting.AQUA);
        y = renderAttributeWithTruncation(guiGraphics, "Spell Power", attributes, "spell_power", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Spell Resist", attributes, "spell_resist", 1.0, "%.0f%%", x, y, ChatFormatting.LIGHT_PURPLE, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cooldown Red.", attributes, "cooldown_reduction", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cast Time Red.", attributes, "cast_time_reduction", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cast Speed", attributes, "casting_movespeed", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Summon Dmg", attributes, "summon_damage", 1.0, "%.0f%%", x, y, ChatFormatting.DARK_PURPLE, true, 1.0);

        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
        y = renderSectionHeader(guiGraphics, "School Power", x, y, ChatFormatting.RED);
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        try {
            List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
            for (SchoolType school : schools) {
                String powerKey = school.getId().getPath() + "_spell_power";
                String schoolName = capitalizeFirst(school.getId().getPath());
                int color = getImprovedSchoolColor(school.getId());
                y = renderAttributeWithTruncation(guiGraphics, schoolName, attributes, powerKey, 1.0, "%.0f%%", x, y, color, true, 1.0);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error rendering school powers: {}", e.getMessage());
        }

        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
        y = renderSectionHeader(guiGraphics, "Magic Resist", x, y, ChatFormatting.LIGHT_PURPLE);
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        try {
            List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
            for (SchoolType school : schools) {
                String resistKey = school.getId().getPath() + "_magic_resist";
                String schoolName = capitalizeFirst(school.getId().getPath());
                int color = getImprovedSchoolColor(school.getId());
                y = renderAttributeWithTruncation(guiGraphics, schoolName, attributes, resistKey, 1.0, "%.0f%%", x, y, color, true, 1.0);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error rendering school resistances: {}", e.getMessage());
        }

        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
        y = renderSectionHeader(guiGraphics, "Combat Stats", x, y, ChatFormatting.RED);
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        y = renderAttributeWithTruncation(guiGraphics, "Attack Damage", attributes, "attack_damage", 1.0, "%.1f", x, y, ChatFormatting.RED, false);
        y = renderAttributeWithTruncation(guiGraphics, "Crit Chance", attributes, "crit_chance", 0.05, "%.1f%%", x, y, ChatFormatting.YELLOW, true);
        y = renderAttributeWithTruncation(guiGraphics, "Crit Damage", attributes, "crit_damage", 1.5, "%.0f%%", x, y, ChatFormatting.RED, true);
        y = renderAttributeWithTruncation(guiGraphics, "Dodge", attributes, "dodge_chance", 0.0, "%.1f%%", x, y, ChatFormatting.AQUA, true);

        y = renderAttributeWithTruncation(guiGraphics, "Armor Pierce", attributes, "armor_pierce", 0.0, "%.1f", x, y, ChatFormatting.GOLD);
        y = renderAttributeWithTruncation(guiGraphics, "Armor Shred", attributes, "armor_shred", 0.0, "%.1f%%", x, y, ChatFormatting.GOLD, true);
        y = renderAttributeWithTruncation(guiGraphics, "Prot Pierce", attributes, "prot_pierce", 0.0, "%.1f", x, y, ChatFormatting.GOLD);
        y = renderAttributeWithTruncation(guiGraphics, "Prot Shred", attributes, "prot_shred", 0.0, "%.1f%%", x, y, ChatFormatting.GOLD, true);

        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
        y = renderSectionHeader(guiGraphics, "Survivability", x, y, ChatFormatting.GREEN);
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        y = renderAttributeWithTruncation(guiGraphics, "Life Steal", attributes, "life_steal", 0.0, "%.1f%%", x, y, ChatFormatting.RED, true);
        y = renderAttributeWithTruncation(guiGraphics, "Ghost Health", attributes, "ghost_health", 0.0, "%.1f", x, y, ChatFormatting.GRAY);
        y = renderAttributeWithTruncation(guiGraphics, "Overheal", attributes, "overheal", 0.0, "%.1f%%", x, y, ChatFormatting.YELLOW, true);

        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
        y = renderSectionHeader(guiGraphics, "Ranged Combat", x, y, ChatFormatting.GOLD);
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        y = renderAttributeWithTruncation(guiGraphics, "Arrow Damage", attributes, "arrow_damage", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Arrow Velocity", attributes, "arrow_velocity", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Draw Speed", attributes, "draw_speed", 1.0, "%.0f%%", x, y, ChatFormatting.GREEN, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Projectile Dmg", attributes, "projectile_damage", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
    }

    private int renderAttributeWithTruncation(GuiGraphics guiGraphics, String name, CompoundTag attributes,
                                              String attributeKey, double defaultValue, String format,
                                              int x, int y, ChatFormatting color) {
        return renderAttributeWithTruncation(guiGraphics, name, attributes, attributeKey, defaultValue, format, x, y, color, false, 0.0);
    }

    private int renderAttributeWithTruncation(GuiGraphics guiGraphics, String name, CompoundTag attributes,
                                              String attributeKey, double defaultValue, String format,
                                              int x, int y, int color) {
        return renderAttributeWithTruncation(guiGraphics, name, attributes, attributeKey, defaultValue, format, x, y, color, false, 0.0);
    }

    private int renderAttributeWithTruncation(GuiGraphics guiGraphics, String name, CompoundTag attributes,
                                              String attributeKey, double defaultValue, String format,
                                              int x, int y, ChatFormatting color, boolean isPercentage) {
        return renderAttributeWithTruncation(guiGraphics, name, attributes, attributeKey, defaultValue, format, x, y, color, isPercentage, 0.0);
    }

    private int renderAttributeWithTruncation(GuiGraphics guiGraphics, String name, CompoundTag attributes,
                                              String attributeKey, double defaultValue, String format,
                                              int x, int y, int color, boolean isPercentage) {
        return renderAttributeWithTruncation(guiGraphics, name, attributes, attributeKey, defaultValue, format, x, y, color, isPercentage, 0.0);
    }

    private int renderAttributeWithTruncation(GuiGraphics guiGraphics, String name, CompoundTag attributes,
                                              String attributeKey, double defaultValue, String format,
                                              int x, int y, ChatFormatting color, boolean isPercentage, double baseOffset) {
        double value = attributes.contains(attributeKey) ? attributes.getDouble(attributeKey) : defaultValue;

        if (isPercentage) {
            if (baseOffset > 0) {
                value = (value - baseOffset) * 100.0;
            } else {
                value = value * 100.0;
            }
        }

        String formattedValue = String.format(format, value);

        int totalAvailableWidth = ATTRIBUTES_WIDTH - 4;
        int maxLabelWidth = LABEL_WIDTH;
        int maxValueWidth = totalAvailableWidth - VALUE_X_OFFSET;

        String displayName = truncateText(name, maxLabelWidth);
        String displayValue = truncateText(formattedValue, maxValueWidth);

        renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);

        Component labelComponent = Component.literal(displayName + ":").withStyle(ChatFormatting.WHITE);
        guiGraphics.drawString(font, labelComponent, x, y, LABEL_COLOR, true);

        int valueColor = getValueColor(value, isPercentage, attributeKey);
        Component valueComponent = Component.literal(displayValue).withStyle(color);
        guiGraphics.drawString(font, valueComponent, x + VALUE_X_OFFSET, y, valueColor, true);

        attributeRowCounter++;
        return y + LINE_HEIGHT;
    }

    private int renderAttributeWithTruncation(GuiGraphics guiGraphics, String name, CompoundTag attributes,
                                              String attributeKey, double defaultValue, String format,
                                              int x, int y, int color, boolean isPercentage, double baseOffset) {
        double value = attributes.contains(attributeKey) ? attributes.getDouble(attributeKey) : defaultValue;

        if (isPercentage) {
            if (baseOffset > 0) {
                value = (value - baseOffset) * 100.0;
            } else {
                value = value * 100.0;
            }
        }

        String formattedValue = String.format(format, value);

        int totalAvailableWidth = ATTRIBUTES_WIDTH - 4;
        int maxLabelWidth = LABEL_WIDTH;
        int maxValueWidth = totalAvailableWidth - VALUE_X_OFFSET;

        String displayName = truncateText(name, maxLabelWidth);
        String displayValue = truncateText(formattedValue, maxValueWidth);

        renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);

        Component labelComponent = Component.literal(displayName + ":").withStyle(ChatFormatting.WHITE);
        guiGraphics.drawString(font, labelComponent, x, y, LABEL_COLOR, true);

        int valueColor = getValueColor(value, isPercentage, attributeKey);
        Component valueComponent = Component.literal(displayValue).withColor(color);
        guiGraphics.drawString(font, valueComponent, x + VALUE_X_OFFSET, y, valueColor, true);

        attributeRowCounter++;
        return y + LINE_HEIGHT;
    }

    private String truncateText(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return text;

        if (font.width(text) <= maxWidth) {
            return text;
        }

        String truncated = text;
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);

        while (font.width(truncated) + ellipsisWidth > maxWidth && truncated.length() > 1) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }

        return truncated + ellipsis;
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String extractSchoolName(String schoolId) {
        try {
            String[] parts = schoolId.split(":");
            if (parts.length > 1) {
                return parts[1];
            }
            return schoolId;
        } catch (Exception e) {
            return schoolId;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}