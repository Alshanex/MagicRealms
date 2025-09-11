package net.alshanex.magic_realms.util.humans;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
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
        KillTrackerData killData = entity.getData(MRDataAttachments.KILL_TRACKER);

        MagicRealms.LOGGER.debug("Applying level-based attributes for {} (Level: {}, Class: {}, Stars: {})",
                entity.getEntityName(), level, entityClass.getName(), starLevel);

        applyHealthBonus(entity, level);

        applyBossKillBonuses(entity, killData.getBossKills());

        switch (entityClass) {
            case MAGE -> applyMageAttributes(entity, level);
            case WARRIOR -> applyWarriorAttributes(entity, level, starLevel);
            case ROGUE -> {
                if (entity.isArcher()) {
                    applyArcherAttributes(entity, level);
                } else {
                    applyAssassinAttributes(entity, level);
                }
            }
        }
    }

    private static void applyHealthBonus(RandomHumanEntity entity, int level) {
        int healthPerLevel = Config.healthAmount;

        int totalHealthBonus = Math.min(level * healthPerLevel, Config.maxLevel * healthPerLevel);

        applyOrUpdateAttribute(entity, Attributes.MAX_HEALTH,
                "level_health_bonus",
                totalHealthBonus,
                AttributeModifier.Operation.ADD_VALUE);

        entity.setHealth(entity.getMaxHealth());
    }

    private static void applyBossKillBonuses(RandomHumanEntity entity, int bossKills) {
        if (bossKills == 0) return;

        int bossKillHealthMultiplier = Config.healthAmountBossKills;
        int bossKillDamageMultiplier = Config.damageAmountBossKills;
        int bossKillArmorMultiplier = Config.armorAmountBossKills;

        // Max health bonus from boss kills (0.5 hearts per boss kill, scaled by star level)
        int bossHealthBonus = Math.min(bossKills * bossKillHealthMultiplier, Config.healthAmountBossKillsTimes * bossKillHealthMultiplier);
        applyOrUpdateAttribute(entity, Attributes.MAX_HEALTH,
                "boss_kill_health_bonus",
                bossHealthBonus,
                AttributeModifier.Operation.ADD_VALUE);

        // Attack damage bonus from boss kills
        int bossDamageBonus = Math.min(bossKills * bossKillDamageMultiplier, Config.damageAmountBossKillsTimes * bossKillDamageMultiplier);
        applyOrUpdateAttribute(entity, Attributes.ATTACK_DAMAGE,
                "boss_kill_damage_bonus",
                bossDamageBonus,
                AttributeModifier.Operation.ADD_VALUE);

        // Armor bonus from boss kills
        double bossArmorBonus = Math.min(bossKills * bossKillArmorMultiplier, Config.armorAmountBossKillsTimes * bossKillArmorMultiplier);
        applyOrUpdateAttribute(entity, Attributes.ARMOR,
                "boss_kill_armor_bonus",
                bossArmorBonus,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyMageAttributes(RandomHumanEntity entity, int level) {

        double progressPercentage = Math.min(1.0, (double) level / Config.maxLevel);
        double currentSpellPowerBonusPercentage = Config.maxSpellPowerPercentage * progressPercentage;
        double currentSpellResistBonusPercentage = Config.maxSpellResistancePercentage * progressPercentage;

        applyOrUpdateAttribute(entity, AttributeRegistry.SPELL_POWER,
                "mage_level_spell_power",
                currentSpellPowerBonusPercentage / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        applyOrUpdateAttribute(entity, AttributeRegistry.SPELL_RESIST,
                "mage_level_spell_resist",
                currentSpellResistBonusPercentage / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void applyWarriorAttributes(RandomHumanEntity entity, int level, int starLevel) {
        int damageBonus = Math.min(level * Config.damageAmountWarriors, Config.damageAmountWarriorsTimes * Config.damageAmountWarriors);

        applyOrUpdateAttribute(entity, Attributes.ATTACK_DAMAGE,
                "warrior_level_attack_damage",
                damageBonus,
                AttributeModifier.Operation.ADD_VALUE);

        // Bonus adicional de armadura para guerreros
        int armorBonus = Math.min(level * Config.armorAmountWarriors, Config.armorAmountWarriorsTimes * Config.armorAmountWarriors);

        applyOrUpdateAttribute(entity, Attributes.ARMOR,
                "warrior_level_armor_bonus",
                armorBonus,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyArcherAttributes(RandomHumanEntity entity, int level) {
        double progressPercentage = Math.min(1.0, (double) level / Config.maxLevel);

        double currentArrowDamageBonusPercentage = Config.maxArrowDamagePercentage * progressPercentage;
        try {
            applyOrUpdateAttribute(entity, ALObjects.Attributes.ARROW_DAMAGE,
                    "archer_level_arrow_damage",
                    currentArrowDamageBonusPercentage / 100.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Apothic Attributes not available for arrow damage bonus: {}", e.getMessage());
        }

        double currentArrowVelocityBonusPercentage = Config.maxArrowVelocityPercentage * progressPercentage;
        try {
            applyOrUpdateAttribute(entity, ALObjects.Attributes.ARROW_VELOCITY,
                    "archer_level_arrow_velocity",
                    currentArrowVelocityBonusPercentage / 100.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Apothic Attributes not available for arrow velocity bonus: {}", e.getMessage());
        }

        double currentDrawSpeedBonusPercentage = Config.maxDrawSpeedPercentage * progressPercentage;
        try {
            applyOrUpdateAttribute(entity, ALObjects.Attributes.DRAW_SPEED,
                    "archer_level_draw_speed",
                    currentDrawSpeedBonusPercentage / 100.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Apothic Attributes not available for draw speed bonus: {}", e.getMessage());
        }
    }

    private static void applyAssassinAttributes(RandomHumanEntity entity, int level) {
        int damageBonus = Math.min(level * Config.damageAmountRogues, Config.damageAmountRoguesTimes * Config.damageAmountRogues);

        applyOrUpdateAttribute(entity, Attributes.ATTACK_DAMAGE,
                "assassin_level_attack_damage",
                damageBonus,
                AttributeModifier.Operation.ADD_VALUE);

        double currentCritChance = entity.getAttribute(ALObjects.Attributes.CRIT_CHANCE).getBaseValue();
        double maxCritChanceGain = 100.0 - currentCritChance;
        double progressPercentage = Math.min(1.0, (double) level / (Config.maxLevel / 2.0));
        double currentCritBonusPercentage = maxCritChanceGain * progressPercentage;

        try {
            applyOrUpdateAttribute(entity, ALObjects.Attributes.CRIT_CHANCE,
                    "assassin_level_crit_chance",
                    currentCritBonusPercentage / 100,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Apothic Attributes not available for crit chance bonus");
        }

        double progressPercentageSpeed = Math.min(1.0, (double) level / (Config.maxLevel / 2.0));
        double currentSpeedBonusPercentage = Config.maxSpeedPercentage * progressPercentageSpeed;

        applyOrUpdateAttribute(entity, Attributes.MOVEMENT_SPEED,
                "rogue_level_speed_bonus",
                currentSpeedBonusPercentage / 100,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
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
