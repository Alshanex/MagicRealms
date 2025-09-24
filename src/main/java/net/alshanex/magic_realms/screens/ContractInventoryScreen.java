package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.network.SwitchAttributesMenuPacket;
import net.alshanex.magic_realms.network.SwitchTabPacket;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.Gender;
import net.alshanex.magic_realms.util.humans.appearance.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.appearance.LayeredTextureManager;
import net.alshanex.magic_realms.util.humans.appearance.TextureComponents;
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
import net.minecraft.util.RandomSource;
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
import java.util.HashMap;
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
    private static final int TAB_2_X = 162;
    private static final int TAB_2_Y = 3;
    private static final int TAB_3_X = 204;
    private static final int TAB_3_Y = 3;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_HEIGHT = 10;

    private static final int HEALTH_X = 28;
    private static final int HEALTH_Y = 125;
    private static final int ARMOR_X = 28;
    private static final int ARMOR_Y = 145;
    private static final int DAMAGE_X = 85;
    private static final int DAMAGE_Y = 136;

    private static final int SYMBOL_X = 98;
    private static final int SYMBOL_Y = 20;

    private final EntitySnapshot snapshot;
    private final AbstractMercenaryEntity entity;
    private static final Map<String, AbstractMercenaryEntity> virtualEntityCache = new HashMap<>();

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

    private void renderEntity3D(GuiGraphics guiGraphics) {
        AbstractMercenaryEntity entityToRender = entity;

        // If we don't have the real entity, use the virtual entity with cache
        if (entityToRender == null && snapshot != null) {
            try {
                entityToRender = getOrCreateVirtualEntity();
                if (entityToRender != null) {
                    MagicRealms.LOGGER.debug("Using virtual entity for 3D rendering in inventory screen");
                }
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Failed to get virtual entity: {}", e.getMessage());
                return;
            }
        }

        if (entityToRender == null) {
            MagicRealms.LOGGER.debug("Cannot render 3D: no entity available in inventory screen");
            return;
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

            MagicRealms.LOGGER.debug("Successfully rendered 3D entity in inventory screen");

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error rendering entity 3D in inventory screen: {}", e.getMessage(), e);
        }
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

    private void forceGenerateTextureComponents(RandomHumanEntity randomHuman, CompoundTag textureMetadata) {
        if (!minecraft.level.isClientSide() || textureMetadata.isEmpty()) {
            return;
        }

        try {
            LayeredTextureManager.ensureTexturesLoaded();
            generateTextureComponentsFromMetadata(randomHuman, textureMetadata);
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to force generate texture components for virtual entity", e);
        }
    }

    private void generateTextureComponentsFromMetadata(RandomHumanEntity randomHuman, CompoundTag metadata) {
        try {
            Gender gender = Gender.valueOf(metadata.getString("gender").toUpperCase());
            EntityClass entityClass = EntityClass.valueOf(metadata.getString("entityClass").toUpperCase());

            TextureComponents components;

            if (metadata.getBoolean("usePreset")) {
                int presetIndex = metadata.getInt("presetIndex");
                components = generatePresetFromIndex(gender, presetIndex);
                if (components == null) {
                    components = generateLayeredFallback(gender, entityClass, presetIndex);
                }
            } else {
                components = generateLayeredFromIndices(
                        gender, entityClass,
                        metadata.getInt("skinIndex"),
                        metadata.getInt("clothesIndex"),
                        metadata.getInt("eyesIndex"),
                        metadata.getInt("hairIndex")
                );
            }

            if (components != null) {
                setTextureComponentsDirectly(randomHuman, components);
                MagicRealms.LOGGER.debug("Generated texture components for virtual entity from metadata");
            }

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to generate texture components from metadata", e);
        }
    }

    private TextureComponents generatePresetFromIndex(Gender gender, int index) {
        if (!LayeredTextureManager.hasAdditionalTextures(gender)) {
            return null;
        }

        int count = LayeredTextureManager.getAdditionalTextureCount(gender);
        if (count == 0) return null;

        int actualIndex = index % count;
        LayeredTextureManager.TextureWithName preset =
                LayeredTextureManager.getAdditionalTextureByIndex(gender, actualIndex);

        if (preset != null) {
            return new TextureComponents(
                    preset.getTexture().toString(),
                    null, null, null,
                    preset.getName(),
                    true
            );
        }
        return null;
    }

    private TextureComponents generateLayeredFromIndices(Gender gender, EntityClass entityClass,
                                                         int skinIndex, int clothesIndex,
                                                         int eyesIndex, int hairIndex) {
        String skinTexture = LayeredTextureManager.getTextureByIndex("skin", skinIndex);
        String clothesTexture = LayeredTextureManager.getClothesTextureByIndex(gender, entityClass, clothesIndex);
        String eyesTexture = LayeredTextureManager.getTextureByIndex("eyes", eyesIndex);
        String hairTexture = LayeredTextureManager.getHairTextureByIndex(gender, hairIndex);

        return new TextureComponents(skinTexture, clothesTexture, eyesTexture, hairTexture, null, false);
    }

    private TextureComponents generateLayeredFallback(Gender gender, EntityClass entityClass, int seed) {
        RandomSource fallbackRandom = RandomSource.create(seed);
        return generateLayeredFromIndices(
                gender, entityClass,
                fallbackRandom.nextInt(10000),
                fallbackRandom.nextInt(10000),
                fallbackRandom.nextInt(10000),
                fallbackRandom.nextInt(10000)
        );
    }

    private void setTextureComponentsDirectly(RandomHumanEntity randomHuman, TextureComponents components) {
        try {
            java.lang.reflect.Field textureComponentsField = RandomHumanEntity.class.getDeclaredField("textureComponents");
            textureComponentsField.setAccessible(true);
            textureComponentsField.set(randomHuman, components);
            MagicRealms.LOGGER.debug("Set texture components directly for virtual entity");
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to set texture components directly", e);
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
                MagicRealms.LOGGER.debug("Set exact UUID for virtual entity: {}", originalUUID);
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

        var attributes = snapshot.attributes;

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

    private void renderClassSymbol(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        ItemStack symbolItem = getSymbolItemForClass();
        if (symbolItem.isEmpty()) return;

        int symbolX = leftPos + SYMBOL_X;
        int symbolY = topPos + SYMBOL_Y;

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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        // Tab clicking - switch to attributes menu for non-inventory tabs
        if (relativeX >= TAB_1_X - 2 && relativeX < TAB_1_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_1_Y - 2 && relativeY < TAB_1_Y + TAB_HEIGHT + 2) {
            switchToAttributesMenu(ContractHumanInfoMenu.Tab.IRON_SPELLS);
            return true;
        }

        if (relativeX >= TAB_2_X - 2 && relativeX < TAB_2_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_2_Y - 2 && relativeY < TAB_2_Y + TAB_HEIGHT + 2) {
            switchToAttributesMenu(ContractHumanInfoMenu.Tab.APOTHIC);
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
