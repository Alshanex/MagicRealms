package net.alshanex.magic_realms.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HumanInfoScreen extends Screen {
    private static final ResourceLocation IRON_SPELLS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_iron_spells.png");
    private static final ResourceLocation APOTHIC_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_apothic.png");
    private static final ResourceLocation TRAITS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/gui/human_info_traits.png");

    private final RandomHumanEntity entity;
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
    private static final int ENTITY_RENDER_Y = 35;
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

    // Attributes area (lado derecho)
    private static final int ATTRIBUTES_X = 125;
    private static final int ATTRIBUTES_Y = 24;

    public HumanInfoScreen(RandomHumanEntity entity) {
        super(Component.translatable("gui.magic_realms.human_info.title"));
        this.entity = entity;
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
        renderEntity(guiGraphics);
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

    private void renderEntity(GuiGraphics guiGraphics) {
        if (entity != null) {
            int entityX = leftPos + ENTITY_RENDER_X;
            int entityY = topPos + ENTITY_RENDER_Y;
            int centerX = entityX + ENTITY_RENDER_WIDTH / 2;
            int centerY = entityY + ENTITY_RENDER_HEIGHT;

            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        guiGraphics,
                        entityX,
                        entityY,
                        entityX + ENTITY_RENDER_WIDTH,
                        entityY + ENTITY_RENDER_HEIGHT,
                        25,
                        0.0625F,
                        centerX,
                        centerY - 15,
                        entity
                );
            } catch (Exception e) {
                // Fallback
                guiGraphics.drawString(font, Component.literal(entity.getEntityName()),
                        centerX - font.width(entity.getEntityName()) / 2,
                        centerY - 30, 0xFFFFFF, false);
            }
        }
    }

    private void renderEquipmentSlots(GuiGraphics guiGraphics) {
        if (entity == null) return;

        // Armor
        ItemStack armorItem = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!armorItem.isEmpty()) {
            guiGraphics.renderItem(armorItem, leftPos + ARMOR_SLOT_X, topPos + ARMOR_SLOT_Y);
        }

        // Main hand
        ItemStack mainHandItem = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainHandItem.isEmpty()) {
            guiGraphics.renderItem(mainHandItem, leftPos + MAINHAND_SLOT_X, topPos + MAINHAND_SLOT_Y);
        }

        // Off hand
        ItemStack offHandItem = entity.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!offHandItem.isEmpty()) {
            guiGraphics.renderItem(offHandItem, leftPos + OFFHAND_SLOT_X, topPos + OFFHAND_SLOT_Y);
        }
    }

    private void renderStats(GuiGraphics guiGraphics) {
        if (entity == null) return;

        // Health (junto al icono de corazón)
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        Component healthComponent = Component.literal(String.format("%.0f", health)); // Solo valor actual, más compacto
        guiGraphics.drawString(font, healthComponent, leftPos + HEALTH_X, topPos + HEALTH_Y, 0xFF5555, false);

        // Armor (junto al icono de armadura)
        double armor = entity.getAttributeValue(Attributes.ARMOR);
        Component armorComponent = Component.literal(String.format("%.0f", armor));
        guiGraphics.drawString(font, armorComponent, leftPos + ARMOR_X, topPos + ARMOR_Y, 0xAAAAAA, false);

        // Damage (junto al icono de espada) - si está visible
        if (topPos + DAMAGE_Y < topPos + imageHeight - 10) {
            double damage = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            Component damageComponent = Component.literal(String.format("%.1f", damage));
            guiGraphics.drawString(font, damageComponent, leftPos + DAMAGE_X, topPos + DAMAGE_Y, 0xCC5555, false);
        }
    }

    private void renderIronSpellsAttributes(GuiGraphics guiGraphics) {
        if (entity == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y;

        guiGraphics.drawString(font, Component.literal("Iron's Spells").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 12;

        // Mana
        AttributeInstance manaAttr = entity.getAttribute(AttributeRegistry.MAX_MANA);
        if (manaAttr != null) {
            renderAttribute(guiGraphics, "Max Mana", String.format("%.0f", manaAttr.getValue()), x, y, ChatFormatting.BLUE);
            y += 10;
        }

        // Mana Regen
        AttributeInstance manaRegenAttr = entity.getAttribute(AttributeRegistry.MANA_REGEN);
        if (manaRegenAttr != null) {
            renderAttribute(guiGraphics, "Mana Regen", String.format("%.2f", manaRegenAttr.getValue()), x, y, ChatFormatting.AQUA);
            y += 10;
        }

        // Spell Power
        AttributeInstance spellPowerAttr = entity.getAttribute(AttributeRegistry.SPELL_POWER);
        if (spellPowerAttr != null) {
            renderAttribute(guiGraphics, "Spell Power", String.format("%.2f", spellPowerAttr.getValue()), x, y, ChatFormatting.RED);
            y += 10;
        }

        // Spell Resistance
        AttributeInstance spellResistAttr = entity.getAttribute(AttributeRegistry.SPELL_RESIST);
        if (spellResistAttr != null) {
            renderAttribute(guiGraphics, "Spell Resist", String.format("%.2f", spellResistAttr.getValue()), x, y, ChatFormatting.LIGHT_PURPLE);
            y += 10;
        }

        // Cooldown Reduction
        AttributeInstance cooldownAttr = entity.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION);
        if (cooldownAttr != null) {
            renderAttribute(guiGraphics, "Cooldown Red.", String.format("%.1f%%", cooldownAttr.getValue() * 100), x, y, ChatFormatting.GREEN);
            y += 10;
        }

        // Casting Movespeed
        AttributeInstance castingMovespeedAttr = entity.getAttribute(AttributeRegistry.CASTING_MOVESPEED);
        if (castingMovespeedAttr != null) {
            renderAttribute(guiGraphics, "Cast Speed", String.format("%.1f%%", castingMovespeedAttr.getValue() * 100), x, y, ChatFormatting.YELLOW);
            y += 10;
        }

        // Summon Damage (para mages)
        if (entity.getEntityClass() == net.alshanex.magic_realms.util.humans.EntityClass.MAGE) {
            AttributeInstance summonDamageAttr = entity.getAttribute(AttributeRegistry.SUMMON_DAMAGE);
            if (summonDamageAttr != null) {
                renderAttribute(guiGraphics, "Summon Dmg", String.format("%.1f%%", summonDamageAttr.getValue() * 100), x, y, ChatFormatting.DARK_PURPLE);
                y += 10;
            }
        }

        // Magic Schools para mages
        if (entity.getEntityClass() == net.alshanex.magic_realms.util.humans.EntityClass.MAGE) {
            y += 3;
            guiGraphics.drawString(font, Component.literal("Schools:").withStyle(ChatFormatting.YELLOW), x, y, 0xFFFFFF, false);
            y += 10;

            List<SchoolType> schools = entity.getMagicSchools();
            if (schools.isEmpty()) {
                guiGraphics.drawString(font, Component.literal("None").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
            } else {
                for (SchoolType school : schools) {
                    String schoolName = school.getId().getPath();
                    schoolName = schoolName.substring(0, 1).toUpperCase() + schoolName.substring(1);
                    guiGraphics.drawString(font, Component.literal("• " + schoolName).withStyle(ChatFormatting.WHITE), x, y, 0xFFFFFF, false);
                    y += 9;
                }
            }
        }
    }

    private void renderApothicAttributes(GuiGraphics guiGraphics) {
        if (entity == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y;

        guiGraphics.drawString(font, Component.literal("Combat Stats").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 12;

        try {
            // Crit Chance
            AttributeInstance critChanceAttr = entity.getAttribute(ALObjects.Attributes.CRIT_CHANCE);
            if (critChanceAttr != null) {
                renderAttribute(guiGraphics, "Crit Chance", String.format("%.1f%%", critChanceAttr.getValue() * 100), x, y, ChatFormatting.YELLOW);
                y += 10;
            }

            // Crit Damage
            AttributeInstance critDamageAttr = entity.getAttribute(ALObjects.Attributes.CRIT_DAMAGE);
            if (critDamageAttr != null) {
                renderAttribute(guiGraphics, "Crit Damage", String.format("%.0f%%", (critDamageAttr.getValue() + 1.5) * 100), x, y, ChatFormatting.GOLD);
                y += 10;
            }

            // Dodge Chance
            AttributeInstance dodgeAttr = entity.getAttribute(ALObjects.Attributes.DODGE_CHANCE);
            if (dodgeAttr != null) {
                renderAttribute(guiGraphics, "Dodge", String.format("%.1f%%", dodgeAttr.getValue() * 100), x, y, ChatFormatting.GREEN);
                y += 10;
            }

            // Armor Shred
            AttributeInstance armorShredAttr = entity.getAttribute(ALObjects.Attributes.ARMOR_SHRED);
            if (armorShredAttr != null && armorShredAttr.getValue() > 0) {
                renderAttribute(guiGraphics, "Armor Shred", String.format("%.1f%%", armorShredAttr.getValue() * 100), x, y, ChatFormatting.DARK_RED);
                y += 10;
            }

            // Armor Pierce
            AttributeInstance armorPierceAttr = entity.getAttribute(ALObjects.Attributes.ARMOR_PIERCE);
            if (armorPierceAttr != null && armorPierceAttr.getValue() > 0) {
                renderAttribute(guiGraphics, "Armor Pierce", String.format("%.1f", armorPierceAttr.getValue()), x, y, ChatFormatting.RED);
                y += 10;
            }

            // Class-specific attributes
            if (entity.getEntityClass() == net.alshanex.magic_realms.util.humans.EntityClass.ROGUE) {
                if (entity.isAssassin()) {
                    // Life Steal for assassins
                    AttributeInstance lifeStealAttr = entity.getAttribute(ALObjects.Attributes.LIFE_STEAL);
                    if (lifeStealAttr != null) {
                        renderAttribute(guiGraphics, "Life Steal", String.format("%.1f%%", lifeStealAttr.getValue() * 100), x, y, ChatFormatting.DARK_RED);
                        y += 10;
                    }
                } else {
                    // Arrow attributes for archers
                    AttributeInstance arrowDamageAttr = entity.getAttribute(ALObjects.Attributes.ARROW_DAMAGE);
                    if (arrowDamageAttr != null) {
                        renderAttribute(guiGraphics, "Arrow Dmg", String.format("%.0f%%", (arrowDamageAttr.getValue() + 1.0) * 100), x, y, ChatFormatting.DARK_GREEN);
                        y += 10;
                    }

                    AttributeInstance arrowVelocityAttr = entity.getAttribute(ALObjects.Attributes.ARROW_VELOCITY);
                    if (arrowVelocityAttr != null) {
                        renderAttribute(guiGraphics, "Arrow Vel", String.format("%.0f%%", (arrowVelocityAttr.getValue() + 1.0) * 100), x, y, ChatFormatting.BLUE);
                        y += 10;
                    }

                    AttributeInstance drawSpeedAttr = entity.getAttribute(ALObjects.Attributes.DRAW_SPEED);
                    if (drawSpeedAttr != null) {
                        renderAttribute(guiGraphics, "Draw Speed", String.format("%.0f%%", (drawSpeedAttr.getValue() + 1.0) * 100), x, y, ChatFormatting.AQUA);
                        y += 10;
                    }
                }
            } else if (entity.getEntityClass() == net.alshanex.magic_realms.util.humans.EntityClass.WARRIOR) {
                // Ghost Health for warriors
                AttributeInstance ghostHealthAttr = entity.getAttribute(ALObjects.Attributes.GHOST_HEALTH);
                if (ghostHealthAttr != null) {
                    renderAttribute(guiGraphics, "Ghost Health", String.format("%.1f", ghostHealthAttr.getValue()), x, y, ChatFormatting.GRAY);
                    y += 10;
                }

                // Overheal
                AttributeInstance overhealAttr = entity.getAttribute(ALObjects.Attributes.OVERHEAL);
                if (overhealAttr != null) {
                    renderAttribute(guiGraphics, "Overheal", String.format("%.1f%%", overhealAttr.getValue() * 100), x, y, ChatFormatting.LIGHT_PURPLE);
                    y += 10;
                }
            }

        } catch (Exception e) {
            // Apothic not available
            guiGraphics.drawString(font, Component.literal("Apothic Attributes").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
            y += 10;
            guiGraphics.drawString(font, Component.literal("not available").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
        }
    }

    private void renderTraits(GuiGraphics guiGraphics) {
        if (entity == null) return;

        int x = leftPos + ATTRIBUTES_X;
        int y = topPos + ATTRIBUTES_Y;

        guiGraphics.drawString(font, Component.literal("Traits").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), x, y, 0xFFFFFF, false);
        y += 15;

        MobTraitCap cap = LHMiscs.MOB.type().getOrCreate(entity);
        if (cap != null) {
            List<Component> traitList = new ArrayList<>();

            cap.traitEvent((trait, level) -> {
                String traitName = trait.getClass().getSimpleName();
                String displayText = "• " + traitName;
                if (level > 1) {
                    displayText += " " + level;
                }
                Component traitComponent = Component.literal(displayText).withStyle(ChatFormatting.WHITE);
                traitList.add(traitComponent);
            });

            if (traitList.isEmpty()) {
                guiGraphics.drawString(font, Component.literal("No traits").withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
            } else {
                for (Component traitComponent : traitList) {
                    guiGraphics.drawString(font, traitComponent, x, y, 0xFFFFFF, false);
                    y += 10;
                }
            }
        }
    }

    private void renderAttribute(GuiGraphics guiGraphics, String name, String value, int x, int y, ChatFormatting color) {
        guiGraphics.drawString(font, Component.literal(name + ":").withStyle(ChatFormatting.WHITE), x, y, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal(value).withStyle(color), x + 90, y, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;

        // Debug para ver dónde está clickeando
        MagicRealms.LOGGER.info("Click at relative: {}, {} | Current tab: {}", relativeX, relativeY, currentTab);

        // Tab 1 - Iron Spells (área expandida para mejor detección)
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
