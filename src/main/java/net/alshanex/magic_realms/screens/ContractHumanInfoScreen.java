package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
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

    private final EntitySnapshot snapshot;
    private final RandomHumanEntity entity;
    private Tab currentTab = Tab.IRON_SPELLS;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isScrolling = false;
    private int lastMouseY = 0;

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

    private static final int ATTRIBUTES_X = 125;
    private static final int ATTRIBUTES_Y = 24;
    private static final int ATTRIBUTES_WIDTH = 120;
    private static final int ATTRIBUTES_HEIGHT = 135;
    private static final int LINE_HEIGHT = 9;

    private static final int LABEL_WIDTH = 70;
    private static final int VALUE_X_OFFSET = 72;

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

        // Intentar renderizar la entidad en 3D, con fallback a 2D
        if (!renderEntity3D(guiGraphics)) {
            renderEntityFallback(guiGraphics);
        }

        renderStats(guiGraphics);

        renderClassSymbol(guiGraphics);

        guiGraphics.enableScissor(
                leftPos + ATTRIBUTES_X,
                topPos + ATTRIBUTES_Y,
                leftPos + ATTRIBUTES_X + ATTRIBUTES_WIDTH,
                topPos + ATTRIBUTES_Y + ATTRIBUTES_HEIGHT
        );

        switch (currentTab) {
            case IRON_SPELLS -> renderIronSpellsAttributesScrollable(guiGraphics);
            case APOTHIC -> renderApothicAttributesScrollable(guiGraphics);
        }

        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            renderScrollIndicator(guiGraphics);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
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
                    MagicRealms.LOGGER.debug("Using cached/created virtual entity for 3D rendering");
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
            MagicRealms.LOGGER.debug("Using cached virtual entity");
            return cachedEntity;
        }

        // Crear nueva entidad virtual
        RandomHumanEntity virtualEntity = createVirtualEntityForRendering();
        if (virtualEntity != null) {
            virtualEntityCache.put(cacheKey, virtualEntity);
            MagicRealms.LOGGER.debug("Created and cached new virtual entity");

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

            MagicRealms.LOGGER.debug("Created virtual entity for rendering: {} ({}) with texture UUID: {}",
                    virtualEntity.getEntityName(),
                    virtualEntity.getEntityClass().getName(),
                    snapshot.textureUUID);

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

            MagicRealms.LOGGER.debug("Set texture config directly for virtual entity");

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

            MagicRealms.LOGGER.debug("Applied equipment to virtual entity without texture regeneration");

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

    // Resto de métodos sin cambios...
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        // Tab clicking
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

        // Scroll area clicking for dragging
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

        // Check if mouse is over the attributes area
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
        int scrollBarX = leftPos + ATTRIBUTES_X + ATTRIBUTES_WIDTH - 4;
        int scrollBarY = topPos + ATTRIBUTES_Y;
        int scrollBarHeight = ATTRIBUTES_HEIGHT;

        guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarHeight, 0x66000000);

        if (maxScroll > 0) {
            int thumbHeight = Math.max(8, (scrollBarHeight * scrollBarHeight) / (scrollBarHeight + maxScroll));
            int thumbY = scrollBarY + (int)((scrollBarHeight - thumbHeight) * (scrollOffset / (float)maxScroll));
            guiGraphics.fill(scrollBarX, thumbY, scrollBarX + 3, thumbY + thumbHeight, 0xAA666666);
        }
    }

    private void renderStats(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        CompoundTag attributes = snapshot.attributes;

        float health = attributes.contains("health") ? (float) attributes.getDouble("health") : 20.0f;
        float maxHealth = attributes.contains("max_health") ? (float) attributes.getDouble("max_health") : 20.0f;
        Component healthComponent = Component.literal(String.format("%.0f/%.0f", health, maxHealth));
        guiGraphics.drawString(font, healthComponent, leftPos + HEALTH_X, topPos + HEALTH_Y, 0xFF5555, false);

        double armor = attributes.contains("armor") ? attributes.getDouble("armor") : 0.0;
        Component armorComponent = Component.literal(String.format("%.1f", armor));
        guiGraphics.drawString(font, armorComponent, leftPos + ARMOR_X, topPos + ARMOR_Y, 0xAAAAAA, false);

        double damage = attributes.contains("attack_damage") ? attributes.getDouble("attack_damage") : 1.0;
        Component damageComponent = Component.literal(String.format("%.1f", damage));
        guiGraphics.drawString(font, damageComponent, leftPos + DAMAGE_X, topPos + DAMAGE_Y, 0xCC5555, false);
    }

    private void renderIronSpellsAttributesScrollable(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y - scrollOffset;

        guiGraphics.drawString(font, Component.literal("Iron's Spells").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 12;

        CompoundTag attributes = snapshot.attributes;

        // Basic attributes
        y = renderAttributeWithTruncation(guiGraphics, "Max Mana", attributes, "max_mana", 100.0, "%.0f", x, y, ChatFormatting.BLUE);
        y = renderAttributeWithTruncation(guiGraphics, "Mana Regen", attributes, "mana_regen", 1.0, "%.2f", x, y, ChatFormatting.AQUA);
        y = renderAttributeWithTruncation(guiGraphics, "Spell Power", attributes, "spell_power", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Spell Resist", attributes, "spell_resist", 1.0, "%.0f%%", x, y, ChatFormatting.LIGHT_PURPLE, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cooldown Red.", attributes, "cooldown_reduction", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cast Time Red.", attributes, "cast_time_reduction", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Cast Speed", attributes, "casting_movespeed", 1.0, "%.0f%%", x, y, ChatFormatting.YELLOW, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Summon Dmg", attributes, "summon_damage", 1.0, "%.0f%%", x, y, ChatFormatting.DARK_PURPLE, true, 1.0);

        y += 6;
        guiGraphics.drawString(font, Component.literal("Magic Resistances:").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 10;

        try {
            List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
            for (SchoolType school : schools) {
                String resistKey = school.getId().getPath() + "_magic_resist";
                String schoolName = capitalizeFirst(school.getId().getPath());
                ChatFormatting color = getSchoolColor(school.getId().getPath());
                y = renderAttributeWithTruncation(guiGraphics, schoolName + " Resist", attributes, resistKey, 1.0, "%.0f%%", x, y, color, true, 1.0);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error rendering school resistances: {}", e.getMessage());
        }

        y += 6;
        guiGraphics.drawString(font, Component.literal("School Power:").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 10;

        try {
            List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();
            for (SchoolType school : schools) {
                String powerKey = school.getId().getPath() + "_spell_power";
                String schoolName = capitalizeFirst(school.getId().getPath());
                ChatFormatting color = getSchoolColor(school.getId().getPath());
                y = renderAttributeWithTruncation(guiGraphics, schoolName + " Power", attributes, powerKey, 1.0, "%.0f%%", x, y, color, true, 1.0);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error rendering school powers: {}", e.getMessage());
        }

        if (snapshot.entityClass == EntityClass.MAGE) {
            y += 3;
            guiGraphics.drawString(font, Component.literal("Entity Schools:").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
            y += 10;

            if (snapshot.magicSchools.isEmpty()) {
                guiGraphics.drawString(font, Component.literal("None").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
                y += 9;
            } else {
                for (String schoolId : snapshot.magicSchools) {
                    String schoolName = extractSchoolName(schoolId);
                    schoolName = capitalizeFirst(schoolName);
                    ChatFormatting color = getSchoolColor(extractSchoolName(schoolId));
                    guiGraphics.drawString(font, Component.literal("• " + schoolName).withStyle(color), x, y, 0xFFFFFF, false);
                    y += 9;
                }
            }
        }

        // Nueva sección de Entity Spells
        y += 3;
        guiGraphics.drawString(font, Component.literal("Entity Spells:").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 10;

        if (snapshot.entitySpells.isEmpty()) {
            guiGraphics.drawString(font, Component.literal("No spells").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
        } else {
            for (String spellName : snapshot.entitySpells) {
                String displayName = truncateText(spellName, ATTRIBUTES_WIDTH - 10);
                guiGraphics.drawString(font, Component.literal("• " + displayName).withStyle(ChatFormatting.WHITE), x, y, 0xFFFFFF, false);
                y += 9;
            }
        }
    }

    private void renderApothicAttributesScrollable(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y - scrollOffset;

        guiGraphics.drawString(font, Component.literal("Combat Stats").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 12;

        CompoundTag attributes = snapshot.attributes;

        // Combat stats
        y = renderAttributeWithTruncation(guiGraphics, "Crit Chance", attributes, "crit_chance", 0.05, "%.1f%%", x, y, ChatFormatting.RED, true);
        y = renderAttributeWithTruncation(guiGraphics, "Crit Damage", attributes, "crit_damage", 1.5, "%.0f%%", x, y, ChatFormatting.RED, true);
        y = renderAttributeWithTruncation(guiGraphics, "Dodge", attributes, "dodge_chance", 0.0, "%.1f%%", x, y, ChatFormatting.BLUE, true);

        y += 5;
        y = renderAttributeWithTruncation(guiGraphics, "Armor Pierce", attributes, "armor_pierce", 0.0, "%.1f", x, y, ChatFormatting.RED);
        y = renderAttributeWithTruncation(guiGraphics, "Armor Shred", attributes, "armor_shred", 0.0, "%.1f%%", x, y, ChatFormatting.RED, true);
        y = renderAttributeWithTruncation(guiGraphics, "Prot Pierce", attributes, "prot_pierce", 0.0, "%.1f", x, y, ChatFormatting.RED);
        y = renderAttributeWithTruncation(guiGraphics, "Prot Shred", attributes, "prot_shred", 0.0, "%.1f%%", x, y, ChatFormatting.RED, true);

        y += 5;
        y = renderAttributeWithTruncation(guiGraphics, "Life Steal", attributes, "life_steal", 0.0, "%.1f%%", x, y, ChatFormatting.DARK_RED, true);
        y = renderAttributeWithTruncation(guiGraphics, "Ghost Health", attributes, "ghost_health", 0.0, "%.1f", x, y, ChatFormatting.GRAY);
        y = renderAttributeWithTruncation(guiGraphics, "Overheal", attributes, "overheal", 0.0, "%.1f%%", x, y, ChatFormatting.GRAY, true);
        y = renderAttributeWithTruncation(guiGraphics, "Healing Received", attributes, "healing_received", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
        y += 5;

        // Ranged Combat
        y += 3;
        guiGraphics.drawString(font, Component.literal("Ranged Combat:").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 10;
        y = renderAttributeWithTruncation(guiGraphics, "Arrow Damage", attributes, "arrow_damage", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Arrow Velocity", attributes, "arrow_velocity", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Draw Speed", attributes, "draw_speed", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
        y = renderAttributeWithTruncation(guiGraphics, "Projectile Damage", attributes, "projectile_damage", 1.0, "%.0f%%", x, y, ChatFormatting.RED, true, 1.0);
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

        String displayName = truncateText(name, LABEL_WIDTH - 5);

        Component labelComponent = Component.literal(displayName + ":").withStyle(ChatFormatting.WHITE);
        guiGraphics.drawString(font, labelComponent, x, y, 0xFFFFFF, false);

        Component valueComponent = Component.literal(formattedValue).withStyle(color);
        guiGraphics.drawString(font, valueComponent, x + VALUE_X_OFFSET, y, 0xFFFFFF, false);

        return y + LINE_HEIGHT;
    }

    private String truncateText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String truncated = text;
        while (font.width(truncated + "...") > maxWidth && truncated.length() > 1) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }

        return truncated + "...";
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