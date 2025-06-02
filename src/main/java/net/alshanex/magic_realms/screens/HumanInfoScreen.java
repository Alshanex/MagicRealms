package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.util.humans.DynamicTextureManager;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class HumanInfoScreen extends AbstractContainerScreen<HumanInfoMenu> {
    private static final ResourceLocation IRON_SPELLS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_iron_spells.png");
    private static final ResourceLocation APOTHIC_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_apothic.png");

    private final EntitySnapshot snapshot;
    private Tab currentTab = Tab.IRON_SPELLS;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isScrolling = false;
    private int lastMouseY = 0;

    // Tab coordinates
    private static final int TAB_1_X = 120;
    private static final int TAB_1_Y = 3;
    private static final int TAB_2_X = 162;
    private static final int TAB_2_Y = 3;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_HEIGHT = 10;

    // Entity render area
    private static final int ENTITY_RENDER_X = 13;
    private static final int ENTITY_RENDER_Y = 30;
    private static final int ENTITY_RENDER_WIDTH = 56;
    private static final int ENTITY_RENDER_HEIGHT = 83;

    // Stats display
    private static final int HEALTH_X = 28;
    private static final int HEALTH_Y = 125;
    private static final int ARMOR_X = 28;
    private static final int ARMOR_Y = 145;
    private static final int DAMAGE_X = 85;
    private static final int DAMAGE_Y = 136;

    // Attributes area
    private static final int ATTRIBUTES_X = 125;
    private static final int ATTRIBUTES_Y = 24;
    private static final int ATTRIBUTES_WIDTH = 120;
    private static final int ATTRIBUTES_HEIGHT = 135;
    private static final int LINE_HEIGHT = 9;

    private static final int LABEL_WIDTH = 70;
    private static final int VALUE_X_OFFSET = 72;

    public HumanInfoScreen(HumanInfoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.snapshot = menu.getSnapshot();

        // Set the GUI size to match your texture
        this.imageWidth = 256;
        this.imageHeight = 250;
    }

    @Override
    protected void init() {
        super.init();

        // Hide default labels
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
        // Render background first
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Render the container (this handles slots automatically)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render additional UI elements
        renderEntityPlaceholder(guiGraphics);
        renderStats(guiGraphics);

        // Render scrollable attributes with scissor
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

        // Render tooltips last
        this.renderTooltip(guiGraphics, mouseX, mouseY);
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
        if (snapshot.entityClass == EntityClass.MAGE) {
            lines += 2;
            lines += Math.max(1, snapshot.magicSchools.size());
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

        // Background
        guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarHeight, 0x66000000);

        // Thumb
        if (maxScroll > 0) {
            int thumbHeight = Math.max(8, (scrollBarHeight * scrollBarHeight) / (scrollBarHeight + maxScroll));
            int thumbY = scrollBarY + (int)((scrollBarHeight - thumbHeight) * (scrollOffset / (float)maxScroll));
            guiGraphics.fill(scrollBarX, thumbY, scrollBarX + 3, thumbY + thumbHeight, 0xAA666666);
        }
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

    private void renderEmptyArea(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0x22000000);
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

    private void renderStats(GuiGraphics guiGraphics) {
        if (snapshot == null) return;

        CompoundTag attributes = snapshot.attributes;

        // Health
        float health = attributes.contains("health") ? (float) attributes.getDouble("health") : 20.0f;
        float maxHealth = attributes.contains("max_health") ? (float) attributes.getDouble("max_health") : 20.0f;
        Component healthComponent = Component.literal(String.format("%.0f/%.0f", health, maxHealth));
        guiGraphics.drawString(font, healthComponent, leftPos + HEALTH_X, topPos + HEALTH_Y, 0xFF5555, false);

        // Armor
        double armor = attributes.contains("armor") ? attributes.getDouble("armor") : 0.0;
        Component armorComponent = Component.literal(String.format("%.1f", armor));
        guiGraphics.drawString(font, armorComponent, leftPos + ARMOR_X, topPos + ARMOR_Y, 0xAAAAAA, false);

        // Attack Damage
        double damage = attributes.contains("attack_damage") ? attributes.getDouble("attack_damage") : 1.0;
        Component damageComponent = Component.literal(String.format("%.1f", damage));
        guiGraphics.drawString(font, damageComponent, leftPos + DAMAGE_X, topPos + DAMAGE_Y, 0xCC5555, false);
    }

    // Keep your existing attribute rendering methods...
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
            guiGraphics.drawString(font, Component.literal("Entity Schools:").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
            y += 10;

            if (snapshot.magicSchools.isEmpty()) {
                guiGraphics.drawString(font, Component.literal("None").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum Tab {
        IRON_SPELLS,
        APOTHIC
    }
}
