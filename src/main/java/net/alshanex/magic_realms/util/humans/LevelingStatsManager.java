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

        switch (entityClass) {
            case MAGE -> applyMageAttributes(entity, level, starLevel);
            case WARRIOR -> applyWarriorAttributes(entity, level, starLevel);
            case ROGUE -> applyRogueAttributes(entity, level, starLevel);
        }
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

        MagicRealms.LOGGER.debug("Applied mage level attributes: {}% spell power and resistance to {} (Level: {}/300)",
                String.format("%.2f", currentBonusPercentage), entity.getEntityName(), level);
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

        MagicRealms.LOGGER.debug("Applied warrior level attributes: {}% attack damage to {} (Level: {})",
                String.format("%.2f", damageBonus * 100), entity.getEntityName(), level);
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

        MagicRealms.LOGGER.debug("Applied rogue level attributes: {}% attack damage and {}% crit chance to {} (Level: {})",
                String.format("%.2f", damageBonus * 100), String.format("%.2f", critBonus * 100),
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
