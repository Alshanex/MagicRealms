package net.alshanex.magic_realms.screens;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.util.humans.DynamicTextureManager;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;


@OnlyIn(Dist.CLIENT)
public class HumanInfoScreen extends Screen {
    private static final ResourceLocation IRON_SPELLS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_iron_spells.png");
    private static final ResourceLocation APOTHIC_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_apothic.png");
    private static final ResourceLocation TRAITS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_traits.png");

    private final EntitySnapshot snapshot;
    private Tab currentTab = Tab.IRON_SPELLS;

    private final int imageWidth = 256;
    private final int imageHeight = 166;
    private int leftPos;
    private int topPos;

    // Tab positions
    private static final int TAB_1_X = 120;
    private static final int TAB_1_Y = 3;
    private static final int TAB_2_X = 162;
    private static final int TAB_2_Y = 3;
    private static final int TAB_3_X = 204;
    private static final int TAB_3_Y = 3;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_HEIGHT = 10;

    // Entity rendering area
    private static final int ENTITY_RENDER_X = 13;
    private static final int ENTITY_RENDER_Y = 30;
    private static final int ENTITY_RENDER_WIDTH = 56;
    private static final int ENTITY_RENDER_HEIGHT = 83;

    // Equipment slots
    private static final int ARMOR_SLOT_X = 73;
    private static final int ARMOR_SLOT_Y = 42;
    private static final int MAINHAND_SLOT_X = 91;
    private static final int MAINHAND_SLOT_Y = 78;
    private static final int OFFHAND_SLOT_X = 91;
    private static final int OFFHAND_SLOT_Y = 96;

    // Info display areas
    private static final int HEALTH_X = 28;
    private static final int HEALTH_Y = 125;
    private static final int ARMOR_X = 28;
    private static final int ARMOR_Y = 145;
    private static final int DAMAGE_X = 85;
    private static final int DAMAGE_Y = 136;

    // Attributes area
    private static final int ATTRIBUTES_X = 125;
    private static final int ATTRIBUTES_Y = 24;

    public HumanInfoScreen(EntitySnapshot snapshot) {
        super(Component.translatable("gui.magic_realms.human_info.title"));
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render main GUI texture
        ResourceLocation texture = getCurrentTexture();
        guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Render all content
        renderEntityPlaceholder(guiGraphics);
        renderEquipmentSlots(guiGraphics);
        renderStats(guiGraphics);

        // Render attributes based on current tab
        switch (currentTab) {
            case IRON_SPELLS -> renderIronSpellsAttributes(guiGraphics);
            case APOTHIC -> renderApothicAttributes(guiGraphics);
            case TRAITS -> renderTraits(guiGraphics);
        }
    }

    private ResourceLocation getCurrentTexture() {
        return switch (currentTab) {
            case IRON_SPELLS -> IRON_SPELLS_TEXTURE;
            case APOTHIC -> APOTHIC_TEXTURE;
            case TRAITS -> TRAITS_TEXTURE;
        };
    }

    private void renderEntityPlaceholder(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int entityX = leftPos + ENTITY_RENDER_X;
        int entityY = topPos + ENTITY_RENDER_Y;

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
        // Configuración de tamaños más apropiada para el área disponible
        int scale = Math.max(1, Math.min(width / 16, height / 32));

        // Tamaños de cada parte
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

        // 1. Cabeza
        int headX = startX + (totalWidth - headSize) / 2;
        guiGraphics.blit(texture, headX, currentY, headSize, headSize, 8, 8, 8, 8, 64, 64);
        // Overlay de la cabeza
        guiGraphics.blit(texture, headX, currentY, headSize, headSize, 40, 8, 8, 8, 64, 64);
        currentY += headSize;

        // 2. Cuerpo y brazos
        int bodyX = startX + (totalWidth - bodyWidth) / 2;

        // Torso
        guiGraphics.blit(texture, bodyX, currentY, bodyWidth, bodyHeight, 20, 20, 8, 12, 64, 64);
        // Overlay del torso
        guiGraphics.blit(texture, bodyX, currentY, bodyWidth, bodyHeight, 20, 36, 8, 12, 64, 64);

        // Brazo derecho (a la izquierda del torso desde nuestra perspectiva)
        int rightArmX = bodyX - armWidth;
        guiGraphics.blit(texture, rightArmX, currentY, armWidth, armHeight, 44, 20, 4, 12, 64, 64);
        // Overlay brazo derecho
        guiGraphics.blit(texture, rightArmX, currentY, armWidth, armHeight, 44, 36, 4, 12, 64, 64);

        // Brazo izquierdo (a la derecha del torso desde nuestra perspectiva)
        int leftArmX = bodyX + bodyWidth;
        guiGraphics.blit(texture, leftArmX, currentY, armWidth, armHeight, 36, 52, 4, 12, 64, 64);
        // Overlay brazo izquierdo
        guiGraphics.blit(texture, leftArmX, currentY, armWidth, armHeight, 52, 52, 4, 12, 64, 64);

        currentY += bodyHeight;

        // 3. Piernas
        int legsX = bodyX + (bodyWidth - (legWidth * 2)) / 2;

        // Pierna derecha
        guiGraphics.blit(texture, legsX, currentY, legWidth, legHeight, 4, 20, 4, 12, 64, 64);
        // Overlay pierna derecha
        guiGraphics.blit(texture, legsX, currentY, legWidth, legHeight, 4, 36, 4, 12, 64, 64);

        // Pierna izquierda
        guiGraphics.blit(texture, legsX + legWidth, currentY, legWidth, legHeight, 20, 52, 4, 12, 64, 64);
        // Overlay pierna izquierda
        guiGraphics.blit(texture, legsX + legWidth, currentY, legWidth, legHeight, 4, 52, 4, 12, 64, 64);
    }

    private void renderEmptyArea(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0x22000000);
    }

    private ResourceLocation getEntityTexture() {
        // Primero intentar obtener la textura dinámica existente
        if (snapshot.textureUUID != null) {
            try {
                // Verificar si la textura ya está registrada dinámicamente
                ResourceLocation dynamicTexture = getDynamicTextureIfExists(snapshot.textureUUID);
                if (dynamicTexture != null) {
                    return dynamicTexture;
                }

                // Si hay una textura guardada, intentar cargarla
                if (snapshot.savedTexturePath != null) {
                    ResourceLocation savedTexture = loadSavedTexture(snapshot.savedTexturePath);
                    if (savedTexture != null) {
                        return savedTexture;
                    }
                }

                // Como último recurso, generar una textura básica para esta entidad virtual
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
                // Cargar la imagen guardada
                BufferedImage image = ImageIO.read(path.toFile());
                if (image != null) {
                    // Registrar como textura dinámica
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

    private void renderEquipmentSlots(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        try {
            CompoundTag equipment = snapshot.equipment;

            // Armor
            CompoundTag chestTag = equipment.getCompound("chest");
            if (!chestTag.isEmpty()) {
                ItemStack armorItem = ItemStack.parseOptional(net.minecraft.client.Minecraft.getInstance().level.registryAccess(), chestTag);
                if (!armorItem.isEmpty()) {
                    guiGraphics.renderItem(armorItem, leftPos + ARMOR_SLOT_X, topPos + ARMOR_SLOT_Y);
                }
            }

            // Main hand
            CompoundTag mainHandTag = equipment.getCompound("main_hand");
            if (!mainHandTag.isEmpty()) {
                ItemStack mainHandItem = ItemStack.parseOptional(net.minecraft.client.Minecraft.getInstance().level.registryAccess(), mainHandTag);
                if (!mainHandItem.isEmpty()) {
                    guiGraphics.renderItem(mainHandItem, leftPos + MAINHAND_SLOT_X, topPos + MAINHAND_SLOT_Y);
                }
            }

            // Off hand
            CompoundTag offHandTag = equipment.getCompound("off_hand");
            if (!offHandTag.isEmpty()) {
                ItemStack offHandItem = ItemStack.parseOptional(net.minecraft.client.Minecraft.getInstance().level.registryAccess(), offHandTag);
                if (!offHandItem.isEmpty()) {
                    guiGraphics.renderItem(offHandItem, leftPos + OFFHAND_SLOT_X, topPos + OFFHAND_SLOT_Y);
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not render equipment: {}", e.getMessage());
        }
    }

    private void renderStats(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        CompoundTag attributes = snapshot.attributes;

        // Health
        if (attributes.contains("health")) {
            float health = (float) attributes.getDouble("health");
            Component healthComponent = Component.literal(String.format("%.0f", health));
            guiGraphics.drawString(font, healthComponent, leftPos + HEALTH_X, topPos + HEALTH_Y, 0xFF5555, false);
        }

        // Armor
        if (attributes.contains("armor")) {
            double armor = attributes.getDouble("armor");
            Component armorComponent = Component.literal(String.format("%.0f", armor));
            guiGraphics.drawString(font, armorComponent, leftPos + ARMOR_X, topPos + ARMOR_Y, 0xAAAAAA, false);
        }

        // Damage
        if (attributes.contains("attack_damage")) {
            double damage = attributes.getDouble("attack_damage");
            Component damageComponent = Component.literal(String.format("%.1f", damage));
            guiGraphics.drawString(font, damageComponent, leftPos + DAMAGE_X, topPos + DAMAGE_Y, 0xCC5555, false);
        }
    }

    private void renderIronSpellsAttributes(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y;

        guiGraphics.drawString(font, Component.literal("Iron's Spells").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 12;

        CompoundTag attributes = snapshot.attributes;

        // Mana
        if (attributes.contains("max_mana")) {
            renderAttribute(guiGraphics, "Max Mana", String.format("%.0f", attributes.getDouble("max_mana")), x, y, ChatFormatting.BLUE);
            y += 10;
        }

        // Mana Regen
        if (attributes.contains("mana_regen")) {
            renderAttribute(guiGraphics, "Mana Regen", String.format("%.2f", attributes.getDouble("mana_regen")), x, y, ChatFormatting.AQUA);
            y += 10;
        }

        // Spell Power
        if (attributes.contains("spell_power")) {
            renderAttribute(guiGraphics, "Spell Power", String.format("%.2f", attributes.getDouble("spell_power")), x, y, ChatFormatting.RED);
            y += 10;
        }

        // Spell Resistance
        if (attributes.contains("spell_resist")) {
            renderAttribute(guiGraphics, "Spell Resist", String.format("%.2f", attributes.getDouble("spell_resist")), x, y, ChatFormatting.LIGHT_PURPLE);
            y += 10;
        }

        // Cooldown Reduction
        if (attributes.contains("cooldown_reduction")) {
            renderAttribute(guiGraphics, "Cooldown Red.", String.format("%.1f%%", attributes.getDouble("cooldown_reduction") * 100), x, y, ChatFormatting.GREEN);
            y += 10;
        }

        // Casting Movespeed
        if (attributes.contains("casting_movespeed")) {
            renderAttribute(guiGraphics, "Cast Speed", String.format("%.1f%%", attributes.getDouble("casting_movespeed") * 100), x, y, ChatFormatting.YELLOW);
            y += 10;
        }

        // Summon Damage (para mages)
        if (snapshot.entityClass == EntityClass.MAGE && attributes.contains("summon_damage")) {
            renderAttribute(guiGraphics, "Summon Dmg", String.format("%.1f%%", attributes.getDouble("summon_damage") * 100), x, y, ChatFormatting.DARK_PURPLE);
            y += 10;
        }

        // Magic Schools para mages
        if (snapshot.entityClass == EntityClass.MAGE) {
            y += 3;
            guiGraphics.drawString(font, Component.literal("Schools:").withStyle(ChatFormatting.YELLOW), x, y, 0xFFFFFF, false);
            y += 10;

            if (snapshot.magicSchools.isEmpty()) {
                guiGraphics.drawString(font, Component.literal("None").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
            } else {
                for (String schoolId : snapshot.magicSchools) {
                    String schoolName = extractSchoolName(schoolId);
                    schoolName = schoolName.substring(0, 1).toUpperCase() + schoolName.substring(1);
                    guiGraphics.drawString(font, Component.literal("• " + schoolName).withStyle(ChatFormatting.WHITE), x, y, 0xFFFFFF, false);
                    y += 9;
                }
            }
        }
    }

    private void renderApothicAttributes(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y;

        guiGraphics.drawString(font, Component.literal("Combat Stats").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 12;

        CompoundTag attributes = snapshot.attributes;

        // Crit Chance
        if (attributes.contains("crit_chance")) {
            renderAttribute(guiGraphics, "Crit Chance", String.format("%.1f%%", attributes.getDouble("crit_chance") * 100), x, y, ChatFormatting.YELLOW);
            y += 10;
        }

        // Crit Damage
        if (attributes.contains("crit_damage")) {
            renderAttribute(guiGraphics, "Crit Damage", String.format("%.0f%%", (attributes.getDouble("crit_damage") + 1.5) * 100), x, y, ChatFormatting.GOLD);
            y += 10;
        }

        // Dodge Chance
        if (attributes.contains("dodge_chance")) {
            renderAttribute(guiGraphics, "Dodge", String.format("%.1f%%", attributes.getDouble("dodge_chance") * 100), x, y, ChatFormatting.GREEN);
            y += 10;
        }

        // Armor Shred
        if (attributes.contains("armor_shred") && attributes.getDouble("armor_shred") > 0) {
            renderAttribute(guiGraphics, "Armor Shred", String.format("%.1f%%", attributes.getDouble("armor_shred") * 100), x, y, ChatFormatting.DARK_RED);
            y += 10;
        }

        // Armor Pierce
        if (attributes.contains("armor_pierce") && attributes.getDouble("armor_pierce") > 0) {
            renderAttribute(guiGraphics, "Armor Pierce", String.format("%.1f", attributes.getDouble("armor_pierce")), x, y, ChatFormatting.RED);
            y += 10;
        }

        // Class-specific attributes
        if (snapshot.entityClass == EntityClass.ROGUE) {
            if (!snapshot.isArcher) {
                // Life Steal for assassins
                if (attributes.contains("life_steal")) {
                    renderAttribute(guiGraphics, "Life Steal", String.format("%.1f%%", attributes.getDouble("life_steal") * 100), x, y, ChatFormatting.DARK_RED);
                    y += 10;
                }
            } else {
                // Arrow attributes for archers
                if (attributes.contains("arrow_damage")) {
                    renderAttribute(guiGraphics, "Arrow Dmg", String.format("%.0f%%", (attributes.getDouble("arrow_damage") + 1.0) * 100), x, y, ChatFormatting.DARK_GREEN);
                    y += 10;
                }

                if (attributes.contains("arrow_velocity")) {
                    renderAttribute(guiGraphics, "Arrow Vel", String.format("%.0f%%", (attributes.getDouble("arrow_velocity") + 1.0) * 100), x, y, ChatFormatting.BLUE);
                    y += 10;
                }

                if (attributes.contains("draw_speed")) {
                    renderAttribute(guiGraphics, "Draw Speed", String.format("%.0f%%", (attributes.getDouble("draw_speed") + 1.0) * 100), x, y, ChatFormatting.AQUA);
                    y += 10;
                }
            }
        } else if (snapshot.entityClass == EntityClass.WARRIOR) {
            // Ghost Health for warriors
            if (attributes.contains("ghost_health")) {
                renderAttribute(guiGraphics, "Ghost Health", String.format("%.1f", attributes.getDouble("ghost_health")), x, y, ChatFormatting.GRAY);
                y += 10;
            }

            // Overheal
            if (attributes.contains("overheal")) {
                renderAttribute(guiGraphics, "Overheal", String.format("%.1f%%", attributes.getDouble("overheal") * 100), x, y, ChatFormatting.LIGHT_PURPLE);
                y += 10;
            }
        }

        // Si no hay atributos de Apothic disponibles
        if (!hasApothicAttributes(attributes)) {
            guiGraphics.drawString(font, Component.literal("Apothic Attributes").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
            y += 10;
            guiGraphics.drawString(font, Component.literal("not available").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
        }
    }

    private void renderTraits(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y;

        guiGraphics.drawString(font, Component.literal("Traits").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 15;

        CompoundTag traits = snapshot.traits;
        if (traits.contains("trait_list")) {
            ListTag traitList = traits.getList("trait_list", 10); // 10 = COMPOUND_TAG

            if (traitList.isEmpty()) {
                guiGraphics.drawString(font, Component.literal("No traits").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
            } else {
                for (int i = 0; i < traitList.size(); i++) {
                    CompoundTag traitTag = traitList.getCompound(i);
                    String traitName = traitTag.getString("name");
                    int traitLevel = traitTag.getInt("level");

                    String displayText = "• " + traitName;
                    if (traitLevel > 1) {
                        displayText += " " + traitLevel;
                    }

                    Component traitComponent = Component.literal(displayText).withStyle(ChatFormatting.WHITE);
                    guiGraphics.drawString(font, traitComponent, x, y, 0xFFFFFF, false);
                    y += 10;
                }
            }
        } else {
            guiGraphics.drawString(font, Component.literal("No trait data").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
        }
    }

    private void renderAttribute(GuiGraphics guiGraphics, String name, String value, int x, int y, ChatFormatting color) {
        guiGraphics.drawString(font, Component.literal(name + ":").withStyle(ChatFormatting.WHITE), x, y, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal(value).withStyle(color), x + 90, y, 0xFFFFFF, false);
    }

    private boolean hasApothicAttributes(CompoundTag attributes) {
        return attributes.contains("crit_chance") || attributes.contains("crit_damage") ||
                attributes.contains("dodge_chance") || attributes.contains("armor_shred") ||
                attributes.contains("armor_pierce") || attributes.contains("life_steal") ||
                attributes.contains("arrow_damage") || attributes.contains("ghost_health") ||
                attributes.contains("overheal");
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        MagicRealms.LOGGER.info("Click at relative: {}, {} | Current tab: {}", relativeX, relativeY, currentTab);

        // Tab 1 - Iron Spells
        if (relativeX >= TAB_1_X - 2 && relativeX < TAB_1_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_1_Y - 2 && relativeY < TAB_1_Y + TAB_HEIGHT + 2) {
            if (currentTab != Tab.IRON_SPELLS) {
                currentTab = Tab.IRON_SPELLS;
                MagicRealms.LOGGER.info("Switched to Iron Spells tab");
            }
            return true;
        }

        // Tab 2 - Apothic
        if (relativeX >= TAB_2_X - 2 && relativeX < TAB_2_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_2_Y - 2 && relativeY < TAB_2_Y + TAB_HEIGHT + 2) {
            if (currentTab != Tab.APOTHIC) {
                currentTab = Tab.APOTHIC;
                MagicRealms.LOGGER.info("Switched to Apothic tab");
            }
            return true;
        }

        // Tab 3 - Traits
        if (relativeX >= TAB_3_X - 2 && relativeX < TAB_3_X + TAB_WIDTH + 2 &&
                relativeY >= TAB_3_Y - 2 && relativeY < TAB_3_Y + TAB_HEIGHT + 2) {
            if (currentTab != Tab.TRAITS) {
                currentTab = Tab.TRAITS;
                MagicRealms.LOGGER.info("Switched to Traits tab");
            }
            return true;
        }

        MagicRealms.LOGGER.info("Click not in any tab area");
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ChatFormatting getColorForStarLevel(int starLevel) {
        return switch (starLevel) {
            case 2 -> ChatFormatting.AQUA;
            case 3 -> ChatFormatting.GOLD;
            default -> ChatFormatting.WHITE;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum Tab {
        IRON_SPELLS,
        APOTHIC,
        TRAITS
    }
}
