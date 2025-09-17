package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.util.humans.DynamicTextureManager;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.EntityTextureConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ContractHumanInfoScreen extends AbstractContainerScreen<ContractHumanInfoMenu> {
    private static final ResourceLocation IRON_SPELLS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_iron_spells.png");
    private static final ResourceLocation APOTHIC_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_apothic.png");

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
    private final RandomHumanEntity entity;
    private Tab currentTab = Tab.IRON_SPELLS;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isScrolling = false;
    private int lastMouseY = 0;
    private int attributeRowCounter = 0; // For alternating row colors

    // Coordenadas y dimensiones para el área de renderizado 3D
    private static final int ENTITY_RENDER_X = 13;
    private static final int ENTITY_RENDER_Y = 30;
    private static final int ENTITY_RENDER_WIDTH = 56;
    private static final int ENTITY_RENDER_HEIGHT = 83;

    // Otras coordenadas existentes...
    private static final int TAB_1_X = 120;
    private static final int TAB_1_Y = 3;
    private static final int TAB_2_X = 162;
    private static final int TAB_2_Y = 3;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_HEIGHT = 10;

    private static final int HEALTH_X = 28;
    private static final int HEALTH_Y = 125;
    private static final int ARMOR_X = 28;
    private static final int ARMOR_Y = 145;
    private static final int DAMAGE_X = 85;
    private static final int DAMAGE_Y = 136;

    private static final int ATTRIBUTES_X = 132;
    private static final int ATTRIBUTES_Y = 25;
    private static final int ATTRIBUTES_WIDTH = 112;
    private static final int ATTRIBUTES_HEIGHT = 124;
    private static final int TEXT_HEIGHT = 9;
    private static final int LINE_SPACING = 3;
    private static final int LINE_HEIGHT = TEXT_HEIGHT + LINE_SPACING;

    private static final int LABEL_WIDTH = 80;
    private static final int VALUE_X_OFFSET = 76; // Moved left to avoid scrollbar collision

    private static final int SYMBOL_X = 98;
    private static final int SYMBOL_Y = 20;

    public ContractHumanInfoScreen(ContractHumanInfoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.snapshot = menu.getSnapshot();
        this.entity = menu.getEntity();

        this.imageWidth = 256;
        this.imageHeight = 250;

        // Debug logging
        MagicRealms.LOGGER.info("ContractHumanInfoScreen created:");
        MagicRealms.LOGGER.info("  - Snapshot: {}", snapshot != null ? "present" : "null");
        MagicRealms.LOGGER.info("  - Entity: {}", entity != null ? entity.getEntityName() : "null");
        if (entity != null) {
            MagicRealms.LOGGER.info("  - Entity UUID: {}", entity.getUUID());
            MagicRealms.LOGGER.info("  - Entity Class: {}", entity.getEntityClass());
        }
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
        if (!renderEntity3D(guiGraphics)) {
            renderEntityFallback(guiGraphics);
        }
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
        switch (currentTab) {
            case IRON_SPELLS -> renderIronSpellsAttributesScrollable(guiGraphics);
            case APOTHIC -> renderApothicAttributesScrollable(guiGraphics);
        }

        guiGraphics.disableScissor();

        // Render scroll indicator if needed
        if (maxScroll > 0) {
            renderScrollIndicator(guiGraphics);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // Helper methods for improved visual rendering
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
        // Draw a subtle line separator
        guiGraphics.fill(x, y, x + width - 10, y + 1, SEPARATOR_COLOR);
        // Add some spacing
        return y + 4;
    }

    private void renderAttributeBackground(GuiGraphics guiGraphics, int x, int y, int width, boolean alternate) {
        if (alternate) {
            // Background should only cover the text height plus 1 pixel padding above and below
            guiGraphics.fill(x - 1, y - 1, x + width + 1, y + TEXT_HEIGHT + 1, ALTERNATE_ROW);
        }
    }

    private int getValueColor(double value, boolean isPercentage, String attributeKey) {
        // Color coding based on value type
        if (attributeKey.contains("resist") || attributeKey.contains("armor") || attributeKey.contains("health")) {
            return value > 0 ? POSITIVE_COLOR : NEUTRAL_COLOR;
        } else if (attributeKey.contains("damage") || attributeKey.contains("power")) {
            return value > (isPercentage ? 100 : 1) ? POSITIVE_COLOR : NEUTRAL_COLOR;
        } else if (attributeKey.contains("regen") || attributeKey.contains("speed")) {
            return value > (isPercentage ? 100 : 1) ? POSITIVE_COLOR : NEUTRAL_COLOR;
        }
        return VALUE_COLOR;
    }

    private ChatFormatting getImprovedSchoolColor(String schoolName) {
        return switch (schoolName.toLowerCase()) {
            case "fire" -> ChatFormatting.GOLD;
            case "ice" -> ChatFormatting.AQUA;
            case "lightning" -> ChatFormatting.BLUE;
            case "holy" -> ChatFormatting.YELLOW;
            case "ender" -> ChatFormatting.LIGHT_PURPLE;
            case "blood" -> ChatFormatting.RED;
            case "evocation" -> ChatFormatting.WHITE;
            case "nature" -> ChatFormatting.GREEN;
            case "eldritch" -> ChatFormatting.DARK_AQUA;
            default -> ChatFormatting.WHITE;
        };
    }

    private void renderClassSymbol(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        ItemStack symbolItem = getSymbolItemForClass();
        if (symbolItem.isEmpty()) return;

        int symbolX = leftPos + SYMBOL_X;
        int symbolY = topPos + SYMBOL_Y;

        // Renderizar el ítem
        guiGraphics.renderItem(symbolItem, symbolX, symbolY);
    }

    private ItemStack getSymbolItemForClass() {
        EntityClass entityClass = snapshot.entityClass;

        switch (entityClass) {
            case MAGE -> {
                try {
                    return new ItemStack(
                            BuiltInRegistries.ITEM.get(
                                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "gold_spell_book")
                            )
                    );
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Could not find gold_spell_book item: {}", e.getMessage());
                    return new ItemStack(Items.BOOK);
                }
            }
            case WARRIOR -> {
                if (snapshot.hasShield) {
                    return new ItemStack(Items.SHIELD);
                } else {
                    return new ItemStack(Items.IRON_SWORD);
                }
            }
            case ROGUE -> {
                if (snapshot.isArcher) {
                    return new ItemStack(Items.BOW);
                } else {
                    try {
                        return new ItemStack(
                                BuiltInRegistries.ITEM.get(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "weapon_parts")
                                )
                        );
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

    private boolean renderEntity3D(GuiGraphics guiGraphics) {
        RandomHumanEntity entityToRender = entity;

        // Si no tenemos la entidad real, usar la entidad virtual con cache
        if (entityToRender == null && snapshot != null) {
            try {
                entityToRender = getOrCreateVirtualEntity();
                if (entityToRender != null) {
                    //MagicRealms.LOGGER.debug("Using cached/created virtual entity for 3D rendering");
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

            return true;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error rendering entity 3D: {}", e.getMessage(), e);
            return false;
        }
    }

    private static final Map<String, RandomHumanEntity> virtualEntityCache = new HashMap<>();

    private RandomHumanEntity getOrCreateVirtualEntity() {
        if (snapshot == null || minecraft.level == null) {
            return null;
        }

        String cacheKey = createCacheKey();

        // Verificar si ya tenemos una entidad virtual en cache
        RandomHumanEntity cachedEntity = virtualEntityCache.get(cacheKey);
        if (cachedEntity != null && cachedEntity.level() == minecraft.level) {
            if (menu != null && menu.getEquipmentContainer() != null) {
                applyEquipmentFromContainer(cachedEntity, menu.getEquipmentContainer());
            }
            //MagicRealms.LOGGER.debug("Using cached virtual entity");
            return cachedEntity;
        }

        // Crear nueva entidad virtual
        RandomHumanEntity virtualEntity = createVirtualEntityForRendering();
        if (virtualEntity != null) {
            virtualEntityCache.put(cacheKey, virtualEntity);
            //MagicRealms.LOGGER.debug("Created and cached new virtual entity");

            // Limpiar cache viejo para evitar memory leaks
            if (virtualEntityCache.size() > 5) { // Reducir tamaño de cache
                String oldestKey = virtualEntityCache.keySet().iterator().next();
                virtualEntityCache.remove(oldestKey);
                MagicRealms.LOGGER.debug("Cleaned old cache entry");
            }
        }

        return virtualEntity;
    }

    private String createCacheKey() {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(snapshot.textureUUID != null ? snapshot.textureUUID : "null");
        keyBuilder.append("_").append(snapshot.gender.name());
        keyBuilder.append("_").append(snapshot.entityClass.name());
        keyBuilder.append("_").append(snapshot.starLevel);
        keyBuilder.append("_").append(snapshot.hasShield);
        keyBuilder.append("_").append(snapshot.isArcher);
        keyBuilder.append("_").append(snapshot.entityName);
        keyBuilder.append("_").append(snapshot.entityUUID);

        return keyBuilder.toString();
    }

    private RandomHumanEntity createVirtualEntityForRendering() {
        if (snapshot == null || minecraft.level == null) {
            return null;
        }

        try {
            // Crear una entidad temporal solo para renderizado
            RandomHumanEntity virtualEntity = new RandomHumanEntity(MREntityRegistry.HUMAN.get(), minecraft.level);

            // Configurar EXACTAMENTE los mismos datos del snapshot
            virtualEntity.setGender(snapshot.gender);
            virtualEntity.setEntityClass(snapshot.entityClass);
            virtualEntity.setEntityName(snapshot.entityName);
            virtualEntity.setStarLevel(snapshot.starLevel);
            virtualEntity.setHasShield(snapshot.hasShield);
            virtualEntity.setIsArcher(snapshot.isArcher);

            setEntityUUID(virtualEntity, snapshot.entityUUID);

            // IMPORTANTE: Marcar como inicializada y configurada ANTES de cualquier generación
            virtualEntity.setInitialized(true);
            setAppearanceGenerated(virtualEntity, true);

            // Crear la configuración de textura usando los datos exactos del snapshot
            if (snapshot.textureUUID != null) {
                EntityTextureConfig textureConfig = new EntityTextureConfig(
                        snapshot.textureUUID,
                        snapshot.gender,
                        snapshot.entityClass
                );
                setTextureConfigDirectly(virtualEntity, textureConfig);
            }

            // Aplicar equipamiento visual DESPUÉS de configurar la textura
            if (menu != null && menu.getEquipmentContainer() != null) {
                applyEquipmentFromContainer(virtualEntity, menu.getEquipmentContainer());
            }

            // Configurar posición para evitar warnings
            virtualEntity.setPos(0, 0, 0);

            return virtualEntity;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to create virtual entity: {}", e.getMessage(), e);
            return null;
        }
    }

    private void setEntityUUID(RandomHumanEntity entity, UUID originalUUID) {
        try {
            if (originalUUID != null) {

                // Usar reflection para establecer el UUID exacto
                java.lang.reflect.Field uuidField = Entity.class.getDeclaredField("uuid");
                uuidField.setAccessible(true);
                uuidField.set(entity, originalUUID);

                MagicRealms.LOGGER.debug("Set exact UUID for virtual entity: {}", originalUUID);
            }
        } catch (IllegalArgumentException e) {
            MagicRealms.LOGGER.warn("Invalid UUID format: {}, error: {}", originalUUID, e.getMessage());
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to set exact UUID: {}", e.getMessage());
        }
    }

    private void setTextureConfigDirectly(RandomHumanEntity entity, EntityTextureConfig textureConfig) {
        try {
            // Usar reflection para acceder al campo privado textureConfig
            java.lang.reflect.Field textureConfigField = RandomHumanEntity.class.getDeclaredField("textureConfig");
            textureConfigField.setAccessible(true);
            textureConfigField.set(entity, textureConfig);

            //MagicRealms.LOGGER.debug("Set texture config directly for virtual entity");

        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to set texture config directly: {}", e.getMessage());
        }
    }

    private void setAppearanceGenerated(RandomHumanEntity entity, boolean generated) {
        try {
            java.lang.reflect.Field appearanceGeneratedField = RandomHumanEntity.class.getDeclaredField("appearanceGenerated");
            appearanceGeneratedField.setAccessible(true);
            appearanceGeneratedField.set(entity, generated);

            MagicRealms.LOGGER.debug("Set appearanceGenerated to {} for virtual entity", generated);

        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to set appearanceGenerated: {}", e.getMessage());
        }
    }

    private void applyEquipmentFromContainer(RandomHumanEntity entity, Container equipmentContainer) {
        try {
            // Aplicar equipamiento de armadura
            entity.setItemSlot(EquipmentSlot.HEAD, equipmentContainer.getItem(0).copy());
            entity.setItemSlot(EquipmentSlot.CHEST, equipmentContainer.getItem(1).copy());
            entity.setItemSlot(EquipmentSlot.LEGS, equipmentContainer.getItem(2).copy());
            entity.setItemSlot(EquipmentSlot.FEET, equipmentContainer.getItem(3).copy());

            // Aplicar equipamiento de manos
            entity.setItemSlot(EquipmentSlot.MAINHAND, equipmentContainer.getItem(4).copy());
            entity.setItemSlot(EquipmentSlot.OFFHAND, equipmentContainer.getItem(5).copy());

            //MagicRealms.LOGGER.debug("Applied equipment to virtual entity without texture regeneration");

        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to apply equipment to virtual entity: {}", e.getMessage());
        }
    }


    private void renderEntityFallback(GuiGraphics guiGraphics) {
        if (snapshot == null) {
            renderEmptyArea(guiGraphics,
                    leftPos + ENTITY_RENDER_X,
                    topPos + ENTITY_RENDER_Y,
                    ENTITY_RENDER_WIDTH,
                    ENTITY_RENDER_HEIGHT);
            return;
        }

        int entityX = leftPos + ENTITY_RENDER_X;
        int entityY = topPos + ENTITY_RENDER_Y;

        MagicRealms.LOGGER.debug("Using 2D fallback rendering");

        if (!render2DEntityTexture(guiGraphics, entityX, entityY, ENTITY_RENDER_WIDTH, ENTITY_RENDER_HEIGHT)) {
            renderEmptyArea(guiGraphics, entityX, entityY, ENTITY_RENDER_WIDTH, ENTITY_RENDER_HEIGHT);
        }
    }

    private boolean render2DEntityTexture(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        try {
            ResourceLocation textureLocation = getEntityTexture();
            if (textureLocation != null) {
                renderFullMinecraftSkin(guiGraphics, textureLocation, x, y, width, height);
                return true;
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not render 2D entity texture: {}", e.getMessage());
        }
        return false;
    }

    private void renderFullMinecraftSkin(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height) {
        int scale = Math.max(1, Math.min(width / 16, height / 32));

        int headSize = 8 * scale;
        int bodyWidth = 8 * scale;
        int bodyHeight = 12 * scale;
        int armWidth = 4 * scale;
        int armHeight = 12 * scale;
        int legWidth = 4 * scale;
        int legHeight = 12 * scale;

        int totalHeight = headSize + bodyHeight + legHeight;
        int totalWidth = Math.max(headSize, bodyWidth + (armWidth * 2));

        int startX = x + (width - totalWidth) / 2;
        int startY = y + (height - totalHeight) / 2;

        int currentY = startY;

        // Head
        int headX = startX + (totalWidth - headSize) / 2;
        guiGraphics.blit(texture, headX, currentY, headSize, headSize, 8, 8, 8, 8, 64, 64);
        guiGraphics.blit(texture, headX, currentY, headSize, headSize, 40, 8, 8, 8, 64, 64);
        currentY += headSize;

        // Body and arms
        int bodyX = startX + (totalWidth - bodyWidth) / 2;

        guiGraphics.blit(texture, bodyX, currentY, bodyWidth, bodyHeight, 20, 20, 8, 12, 64, 64);
        guiGraphics.blit(texture, bodyX, currentY, bodyWidth, bodyHeight, 20, 36, 8, 12, 64, 64);

        int rightArmX = bodyX - armWidth;
        guiGraphics.blit(texture, rightArmX, currentY, armWidth, armHeight, 44, 20, 4, 12, 64, 64);
        guiGraphics.blit(texture, rightArmX, currentY, armWidth, armHeight, 44, 36, 4, 12, 64, 64);

        int leftArmX = bodyX + bodyWidth;
        guiGraphics.blit(texture, leftArmX, currentY, armWidth, armHeight, 36, 52, 4, 12, 64, 64);
        guiGraphics.blit(texture, leftArmX, currentY, armWidth, armHeight, 52, 52, 4, 12, 64, 64);

        currentY += bodyHeight;

        // Legs
        int legsX = bodyX + (bodyWidth - (legWidth * 2)) / 2;

        guiGraphics.blit(texture, legsX, currentY, legWidth, legHeight, 4, 20, 4, 12, 64, 64);
        guiGraphics.blit(texture, legsX, currentY, legWidth, legHeight, 4, 36, 4, 12, 64, 64);

        guiGraphics.blit(texture, legsX + legWidth, currentY, legWidth, legHeight, 20, 52, 4, 12, 64, 64);
        guiGraphics.blit(texture, legsX + legWidth, currentY, legWidth, legHeight, 4, 52, 4, 12, 64, 64);
    }

    private ResourceLocation getEntityTexture() {
        if (snapshot.textureUUID != null) {
            try {
                ResourceLocation dynamicTexture = getDynamicTextureIfExists(snapshot.textureUUID);
                if (dynamicTexture != null) {
                    return dynamicTexture;
                }

                if (snapshot.savedTexturePath != null) {
                    ResourceLocation savedTexture = loadSavedTexture(snapshot.savedTexturePath);
                    if (savedTexture != null) {
                        return savedTexture;
                    }
                }

                if (snapshot.entityUUID != null) {
                    ResourceLocation generatedTexture = generateVirtualEntityTexture();
                    if (generatedTexture != null) {
                        return generatedTexture;
                    }
                }
            } catch (Exception e) {
                MagicRealms.LOGGER.debug("Error getting entity texture: {}", e.getMessage());
            }
        }

        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    }

    private ResourceLocation getDynamicTextureIfExists(String textureUUID) {
        try {
            return DynamicTextureManager.getTexture(textureUUID);
        } catch (Exception e) {
            return null;
        }
    }

    private ResourceLocation loadSavedTexture(String texturePath) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(texturePath);
            if (java.nio.file.Files.exists(path)) {
                BufferedImage image = ImageIO.read(path.toFile());
                if (image != null) {
                    return DynamicTextureManager.registerDynamicTexture(
                            snapshot.textureUUID, image);
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not load saved texture from {}: {}", texturePath, e.getMessage());
        }
        return null;
    }

    private ResourceLocation generateVirtualEntityTexture() {
        try {
            return CombinedTextureManager.getCombinedTextureWithHair(
                    snapshot.textureUUID,
                    snapshot.gender,
                    snapshot.entityClass,
                    0
            );
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not generate virtual texture: {}", e.getMessage());
            return null;
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
        // Habilitar scissor para limitar el área de renderizado
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

        // Deshabilitar scissor
        guiGraphics.disableScissor();
    }

    private void renderEmptyArea(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0x22000000);

        // Información de debug más útil
        String debugText = "No Entity";
        if (entity == null) {
            debugText = "Entity: null";
        } else if (!entity.isAlive()) {
            debugText = "Entity: dead";
        } else if (snapshot == null) {
            debugText = "Snapshot: null";
        }

        Component noEntityText = Component.literal(debugText).withStyle(ChatFormatting.GRAY);
        int textWidth = font.width(noEntityText);
        int textX = x + (width - textWidth) / 2;
        int textY = y + height / 2 - font.lineHeight / 2;
        guiGraphics.drawString(font, noEntityText, textX, textY, 0xAAAAAA, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        // Tab clicking (existing code...)
        if (relativeX >= TAB_1_X - 2 && relativeX < TAB_1_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_1_Y - 2 && relativeY < TAB_1_Y + TAB_HEIGHT + 2) {
            if (currentTab != Tab.IRON_SPELLS) {
                currentTab = Tab.IRON_SPELLS;
                scrollOffset = 0;
                calculateMaxScroll();
            }
            return true;
        }

        if (relativeX >= TAB_2_X - 2 && relativeX < TAB_2_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_2_Y - 2 && relativeY < TAB_2_Y + TAB_HEIGHT + 2) {
            if (currentTab != Tab.APOTHIC) {
                currentTab = Tab.APOTHIC;
                scrollOffset = 0;
                calculateMaxScroll();
            }
            return true;
        }

        if (relativeX >= ATTRIBUTES_X && relativeX < ATTRIBUTES_X + ATTRIBUTES_WIDTH &&
                relativeY >= ATTRIBUTES_Y && relativeY < ATTRIBUTES_Y + ATTRIBUTES_HEIGHT) {
            isScrolling = true;
            lastMouseY = (int) mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
        if (isScrolling && button == 0) {
            int currentMouseY = (int) mouseY;
            int deltaScrollY = lastMouseY - currentMouseY;

            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + deltaScrollY));
            lastMouseY = currentMouseY;

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        if (relativeX >= ATTRIBUTES_X && relativeX < ATTRIBUTES_X + ATTRIBUTES_WIDTH &&
                relativeY >= ATTRIBUTES_Y && relativeY < ATTRIBUTES_Y + ATTRIBUTES_HEIGHT) {

            int scrollAmount = (int) (deltaY * -10);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + scrollAmount));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private ResourceLocation getCurrentTexture() {
        return switch (currentTab) {
            case IRON_SPELLS -> IRON_SPELLS_TEXTURE;
            case APOTHIC -> APOTHIC_TEXTURE;
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
                return getIronSpellsAttributeLines();
            }
            case APOTHIC -> {
                return getApothicAttributeLines();
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
        // Añadir líneas para los spells
        lines += 2; // Header
        if (snapshot != null) {
            lines += Math.max(1, snapshot.entitySpells.size()); // Spells list
        }
        return lines;
    }

    private int getApothicAttributeLines() {
        int lines = 2;
        lines += 3;
        lines += 2;
        lines += 4;
        lines += 2;
        lines += 3;
        lines += 2;
        lines += 4;
        lines += 2;
        lines += 4;
        lines += 2;
        lines += 3;
        return lines;
    }

    private void renderScrollIndicator(GuiGraphics guiGraphics) {
        int scrollBarX = leftPos + ATTRIBUTES_X + ATTRIBUTES_WIDTH - 6; // Wider bar
        int scrollBarY = topPos + ATTRIBUTES_Y;
        int scrollBarHeight = ATTRIBUTES_HEIGHT;

        // More visible background track
        guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 5, scrollBarY + scrollBarHeight, 0x88000000);

        if (maxScroll > 0) {
            int thumbHeight = Math.max(12, (scrollBarHeight * scrollBarHeight) / (scrollBarHeight + maxScroll));
            int thumbY = scrollBarY + (int)((scrollBarHeight - thumbHeight) * (scrollOffset / (float)maxScroll));

            // More visible thumb with border
            guiGraphics.fill(scrollBarX + 1, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0xFFAAAAAA);
            guiGraphics.fill(scrollBarX, thumbY, scrollBarX + 5, thumbY + 1, 0xFFFFFFFF); // Top border
            guiGraphics.fill(scrollBarX, thumbY + thumbHeight - 1, scrollBarX + 5, thumbY + thumbHeight, 0xFFFFFFFF); // Bottom border
        }
    }

    private void renderStats(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        CompoundTag attributes = snapshot.attributes;

        float health = attributes.contains("health") ? (float) attributes.getDouble("health") : 20.0f;
        float maxHealth = attributes.contains("max_health") ? (float) attributes.getDouble("max_health") : 20.0f;
        Component healthComponent = Component.literal(String.format("%.0f/%.0f", health, maxHealth));
        guiGraphics.drawString(font, healthComponent, leftPos + HEALTH_X, topPos + HEALTH_Y, 0xFF5555, false); // No shadow

        double armor = attributes.contains("armor") ? attributes.getDouble("armor") : 0.0;
        Component armorComponent = Component.literal(String.format("%.1f", armor));
        guiGraphics.drawString(font, armorComponent, leftPos + ARMOR_X, topPos + ARMOR_Y, 0xAAAAAA, false); // No shadow

        double damage = attributes.contains("attack_damage") ? attributes.getDouble("attack_damage") : 1.0;
        Component damageComponent = Component.literal(String.format("%.1f", damage));
        guiGraphics.drawString(font, damageComponent, leftPos + DAMAGE_X, topPos + DAMAGE_Y, 0xCC5555, false); // No shadow
    }

    private void renderIronSpellsAttributesScrollable(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y - scrollOffset;

        // Section separator
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        // Main header
        y = renderSectionHeader(guiGraphics, "Iron's Spells", x, y, ChatFormatting.AQUA);

        y+=2;

        CompoundTag attributes = snapshot.attributes;

        // Basic attributes section
        y = renderAttributeWithTruncation(guiGraphics, "Max Mana", attributes, "max_mana", 100.0, "%.0f", x, y, ChatFormatting.BLUE);
        y = renderAttributeWithTruncation(guiGraphics, "Mana Regen", attributes, "mana_regen", 1.0, "%.2f", x, y, ChatFormatting.AQUA);
        y = renderAttributeWithTruncation(guiGraphics, "Spell Power", attributes, "spell_power", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Spell Resist", attributes, "spell_resist", 1.0, "%.0f%%", x, y, ChatFormatting.LIGHT_PURPLE, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cooldown Red.", attributes, "cooldown_reduction", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cast Time Red.", attributes, "cast_time_reduction", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cast Speed", attributes, "casting_movespeed", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Summon Dmg", attributes, "summon_damage", 1.0, "%.0f%%", x, y, ChatFormatting.DARK_PURPLE, true, 1.0);

        // Section separator
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        // Magic Resist section
        y = renderSectionHeader(guiGraphics, "Magic Resist", x, y, ChatFormatting.LIGHT_PURPLE);

        y+=2;

        try {
            List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
            for (SchoolType school : schools) {
                String resistKey = school.getId().getPath() + "_magic_resist";
                String schoolName = capitalizeFirst(school.getId().getPath());
                ChatFormatting color = getImprovedSchoolColor(school.getId().getPath());
                y = renderAttributeWithTruncation(guiGraphics, schoolName, attributes, resistKey, 1.0, "%.0f%%", x, y, color, true, 1.0);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error rendering school resistances: {}", e.getMessage());
        }

        // Section separator
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        // School Power section
        y = renderSectionHeader(guiGraphics, "School Power", x, y, ChatFormatting.RED);

        y+=2;

        try {
            List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
            for (SchoolType school : schools) {
                String powerKey = school.getId().getPath() + "_spell_power";
                String schoolName = capitalizeFirst(school.getId().getPath());
                ChatFormatting color = getImprovedSchoolColor(school.getId().getPath());
                y = renderAttributeWithTruncation(guiGraphics, schoolName, attributes, powerKey, 1.0, "%.0f%%", x, y, color, true, 1.0);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error rendering school powers: {}", e.getMessage());
        }

        // Magic Schools section (for mages only)
        if (snapshot.entityClass == EntityClass.MAGE) {
            y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
            y = renderSectionHeader(guiGraphics, "Schools", x, y, ChatFormatting.GOLD);

            y+=2;

            if (snapshot.magicSchools.isEmpty()) {
                renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);
                guiGraphics.drawString(font, Component.literal("None").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, true);
                y += LINE_HEIGHT;
                attributeRowCounter++;
            } else {
                for (String schoolId : snapshot.magicSchools) {
                    String schoolName = extractSchoolName(schoolId);
                    schoolName = capitalizeFirst(schoolName);
                    ChatFormatting color = getImprovedSchoolColor(extractSchoolName(schoolId));

                    renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);
                    guiGraphics.drawString(font, Component.literal("• " + schoolName).withStyle(color), x, y, 0xFFFFFF, true);
                    y += LINE_HEIGHT;
                    attributeRowCounter++;
                }
            }
        }

        // Spells section
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);
        y = renderSectionHeader(guiGraphics, "Spells", x, y, ChatFormatting.YELLOW);

        y+=2;

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

                    renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);
                    guiGraphics.drawString(font, Component.literal("• " + displayName).withStyle(ChatFormatting.WHITE), x, y, 0xFFFFFF, true);
                    y += LINE_HEIGHT;
                    attributeRowCounter++;
                }
            }
        }
    }

    private void renderApothicAttributesScrollable(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y - scrollOffset;

        // Section separator
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        // Main header
        y = renderSectionHeader(guiGraphics, "Combat Stats", x, y, ChatFormatting.DARK_RED);

        y+=2;

        CompoundTag attributes = snapshot.attributes;

        // Combat stats
        y = renderAttributeWithTruncation(guiGraphics, "Crit Chance", attributes, "crit_chance", 0.05, "%.1f%%", x, y, ChatFormatting.YELLOW, true);
        y = renderAttributeWithTruncation(guiGraphics, "Crit Damage", attributes, "crit_damage", 1.5, "%.0f%%", x, y, ChatFormatting.RED, true);
        y = renderAttributeWithTruncation(guiGraphics, "Dodge", attributes, "dodge_chance", 0.0, "%.1f%%", x, y, ChatFormatting.AQUA, true);

        // Penetration section
        y = renderAttributeWithTruncation(guiGraphics, "Armor Pierce", attributes, "armor_pierce", 0.0, "%.1f", x, y, ChatFormatting.GOLD);
        y = renderAttributeWithTruncation(guiGraphics, "Armor Shred", attributes, "armor_shred", 0.0, "%.1f%%", x, y, ChatFormatting.GOLD, true);
        y = renderAttributeWithTruncation(guiGraphics, "Prot Pierce", attributes, "prot_pierce", 0.0, "%.1f", x, y, ChatFormatting.GOLD);
        y = renderAttributeWithTruncation(guiGraphics, "Prot Shred", attributes, "prot_shred", 0.0, "%.1f%%", x, y, ChatFormatting.GOLD, true);

        // Section separator
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        // Survivability section
        y = renderSectionHeader(guiGraphics, "Survivability", x, y, ChatFormatting.GREEN);

        y+=2;

        y = renderAttributeWithTruncation(guiGraphics, "Life Steal", attributes, "life_steal", 0.0, "%.1f%%", x, y, ChatFormatting.RED, true);
        y = renderAttributeWithTruncation(guiGraphics, "Ghost Health", attributes, "ghost_health", 0.0, "%.1f", x, y, ChatFormatting.GRAY);
        y = renderAttributeWithTruncation(guiGraphics, "Overheal", attributes, "overheal", 0.0, "%.1f%%", x, y, ChatFormatting.YELLOW, true);

        // Section separator
        y = renderSectionSeparator(guiGraphics, x, y, ATTRIBUTES_WIDTH);

        // Ranged Combat section
        y = renderSectionHeader(guiGraphics, "Ranged Combat", x, y, ChatFormatting.GOLD);

        y+=2;

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
                                              int x, int y, ChatFormatting color, boolean isPercentage) {
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

        // Render alternating background
        renderAttributeBackground(guiGraphics, x, y, ATTRIBUTES_WIDTH, attributeRowCounter % 2 == 1);

        // Use improved colors and add text shadow for better readability
        Component labelComponent = Component.literal(displayName + ":").withStyle(ChatFormatting.WHITE); // Changed to WHITE
        guiGraphics.drawString(font, labelComponent, x, y, LABEL_COLOR, true); // true for shadow

        // Color-code values based on their meaning
        int valueColor = getValueColor(value, isPercentage, attributeKey);
        Component valueComponent = Component.literal(displayValue).withStyle(color);
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

    private ChatFormatting getSchoolColor(String schoolName) {
        return switch (schoolName.toLowerCase()) {
            case "fire" -> ChatFormatting.RED;
            case "ice" -> ChatFormatting.AQUA;
            case "lightning" -> ChatFormatting.BLUE;
            case "holy" -> ChatFormatting.YELLOW;
            case "ender" -> ChatFormatting.DARK_PURPLE;
            case "blood" -> ChatFormatting.DARK_RED;
            case "evocation" -> ChatFormatting.GRAY;
            case "nature" -> ChatFormatting.GREEN;
            case "eldritch" -> ChatFormatting.DARK_AQUA;
            default -> ChatFormatting.WHITE;
        };
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

    public enum Tab {
        IRON_SPELLS,
        APOTHIC
    }
}