package net.alshanex.magic_realms.util.humans;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class LevelingStatsManager {
    public static void applyLevelBasedAttributes(RandomHumanEntity entity, int level) {
        EntityClass entityClass = entity.getEntityClass();
        int starLevel = entity.getStarLevel();

        MagicRealms.LOGGER.debug("Applying level-based attributes for {} (Level: {}, Class: {}, Stars: {})",
                entity.getEntityName(), level, entityClass.getName(), starLevel);

        // Aplicar bonificación de vida común para todas las clases
        applyHealthBonus(entity, level, starLevel);

        switch (entityClass) {
            case MAGE -> applyMageAttributes(entity, level, starLevel);
            case WARRIOR -> applyWarriorAttributes(entity, level, starLevel);
            case ROGUE -> applyRogueAttributes(entity, level, starLevel);
        }
    }

    private static void applyHealthBonus(RandomHumanEntity entity, int level, int starLevel) {
        // Calcular bonificación de vida basada en estrellas y nivel
        double healthPerLevel = switch (starLevel) {
            case 1 -> 0.5;  // +0.5 corazones por nivel (1 HP)
            case 2 -> 0.75; // +0.75 corazones por nivel (1.5 HP)
            case 3 -> 1.0;  // +1 corazón por nivel (2 HP)
            default -> 0.5;
        };

        double totalHealthBonus = Math.min(level * healthPerLevel * 2, 300 * healthPerLevel * 2); // *2 porque cada corazón = 2 HP

        applyOrUpdateAttribute(entity, Attributes.MAX_HEALTH,
                "level_health_bonus",
                totalHealthBonus,
                AttributeModifier.Operation.ADD_VALUE);

        // Curar completamente al subir de nivel para reflejar el aumento de vida
        entity.setHealth(entity.getMaxHealth());

        MagicRealms.LOGGER.debug("Applied health bonus: +{} HP ({} hearts) to {} (Level: {})",
                String.format("%.1f", totalHealthBonus),
                String.format("%.1f", totalHealthBonus / 2),
                entity.getEntityName(), level);
    }

    private static void applyMageAttributes(RandomHumanEntity entity, int level, int starLevel) {
        // Calcular límite máximo: 50% + (10% * estrellas)
        double maxBonusPercentage = 50.0 + (10.0 * starLevel);

        double progressPercentage = Math.min(1.0, level / 300.0);
        double currentBonusPercentage = maxBonusPercentage * progressPercentage;

        applyOrUpdateAttribute(entity, AttributeRegistry.SPELL_POWER,
                "mage_level_spell_power",
                currentBonusPercentage / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        applyOrUpdateAttribute(entity, AttributeRegistry.SPELL_RESIST,
                "mage_level_spell_resist",
                currentBonusPercentage / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // Bonus adicional de mana/spell power para magos
        double manaBonus = Math.min(level * 0.5, 150.0); // Máximo 150 puntos de mana extra

        try {
            applyOrUpdateAttribute(entity, AttributeRegistry.MAX_MANA,
                    "mage_level_mana_bonus",
                    manaBonus,
                    AttributeModifier.Operation.ADD_VALUE);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not apply mana bonus: {}", e.getMessage());
        }

        MagicRealms.LOGGER.debug("Applied mage level attributes: {}% spell power and resistance, +{} mana to {} (Level: {}/300)",
                String.format("%.2f", currentBonusPercentage),
                String.format("%.1f", manaBonus),
                entity.getEntityName(), level);
    }

    private static void applyWarriorAttributes(RandomHumanEntity entity, int level, int starLevel) {
        double baseMultiplier = switch (starLevel) {
            case 1 -> 0.02; // 2% por nivel
            case 2 -> 0.025; // 2.5% por nivel
            case 3 -> 0.03; // 3% por nivel
            default -> 0.02;
        };

        double damageBonus = Math.min(level * baseMultiplier, baseMultiplier * 300);

        applyOrUpdateAttribute(entity, Attributes.ATTACK_DAMAGE,
                "warrior_level_attack_damage",
                damageBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // Bonus adicional de armadura para guerreros
        double armorBonus = Math.min(level * 0.1, 30.0); // Máximo 30 puntos de armadura

        applyOrUpdateAttribute(entity, Attributes.ARMOR,
                "warrior_level_armor_bonus",
                armorBonus,
                AttributeModifier.Operation.ADD_VALUE);

        MagicRealms.LOGGER.debug("Applied warrior level attributes: {}% attack damage, +{} armor to {} (Level: {})",
                String.format("%.2f", damageBonus * 100),
                String.format("%.1f", armorBonus),
                entity.getEntityName(), level);
    }

    private static void applyRogueAttributes(RandomHumanEntity entity, int level, int starLevel) {
        double baseMultiplier = switch (starLevel) {
            case 1 -> 0.015; // 1.5% por nivel
            case 2 -> 0.02;  // 2% por nivel
            case 3 -> 0.025; // 2.5% por nivel
            default -> 0.015;
        };

        double damageBonus = Math.min(level * baseMultiplier, baseMultiplier * 300);

        if(entity.isAssassin()){
            applyOrUpdateAttribute(entity, Attributes.ATTACK_DAMAGE,
                    "assassin_level_attack_damage",
                    damageBonus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }

        double critBonus = Math.min(level * 0.05, 15.0) / 100.0;

        try {
            applyOrUpdateAttribute(entity, ALObjects.Attributes.CRIT_CHANCE,
                    "assassin_level_crit_chance",
                    critBonus,
                    AttributeModifier.Operation.ADD_VALUE);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Apothic Attributes not available for crit chance bonus");
        }

        // Bonus adicional de velocidad para rogues
        double speedBonus = Math.min(level * 0.002, 0.6); // Máximo 60% de velocidad extra

        applyOrUpdateAttribute(entity, Attributes.MOVEMENT_SPEED,
                "rogue_level_speed_bonus",
                speedBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        MagicRealms.LOGGER.debug("Applied rogue level attributes: {}% attack damage, {}% crit chance, +{}% speed to {} (Level: {})",
                String.format("%.2f", damageBonus * 100),
                String.format("%.2f", critBonus * 100),
                String.format("%.2f", speedBonus * 100),
                entity.getEntityName(), level);
    }

    private static void applyOrUpdateAttribute(RandomHumanEntity entity, Holder<Attribute> attributeHolder,
                                               String modifierName, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attributeHolder);
        if (instance == null) {
            MagicRealms.LOGGER.warn("Attribute instance not found for {}", attributeHolder.toString());
            return;
        }

        ResourceLocation modifierLocation = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, modifierName);

        AttributeModifier existing = instance.getModifier(modifierLocation);
        if (existing != null) {
            instance.removeModifier(existing);
        }

        double roundedAmount = Math.round(amount * 100.0) / 100.0;

        AttributeModifier modifier = new AttributeModifier(modifierLocation, roundedAmount, operation);
        instance.addPermanentModifier(modifier);

        MagicRealms.LOGGER.debug("Updated attribute modifier: {} = {} ({})", modifierName, roundedAmount, operation);
    }
}
