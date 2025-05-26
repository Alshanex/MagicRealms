package net.alshanex.magic_realms.util.humans;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class HumanStatsManager {

    public static void applyClassAttributes(RandomHumanEntity entity) {
        EntityClass entityClass = entity.getEntityClass();
        int starLevel = entity.getStarLevel();
        RandomSource random = entity.level().getRandom();

        MagicRealms.LOGGER.debug("Applying attributes for {} with {} stars", entityClass.getName(), starLevel);

        // Aplicar atributos base según la clase
        switch (entityClass) {
            case MAGE -> applyMageAttributes(entity, starLevel, random);
            case WARRIOR -> applyWarriorAttributes(entity, starLevel, random);
            case ROGUE -> applyRogueAttributes(entity, starLevel, random);
        }

        // Aplicar atributos comunes a todas las clases
        applyCommonAttributes(entity, starLevel, random);
    }

    private static void applyMageAttributes(RandomHumanEntity entity, int starLevel, RandomSource random) {
        // Health base: 15
        addAttributeModifier(entity, Attributes.MAX_HEALTH,
                "mage_health", 15.0, AttributeModifier.Operation.ADD_VALUE);

        entity.heal(entity.getMaxHealth());

        // Bonus mana según estrella
        double bonusMana = getStarBasedValue(starLevel, 10, 50, 30, 70, 50, 100, random);
        addAttributeModifier(entity, AttributeRegistry.MAX_MANA,
                "mage_bonus_mana", bonusMana, AttributeModifier.Operation.ADD_VALUE);

        // Mana regen bonus según estrella
        double manaRegenBonus = roundToTwoDecimals(getStarBasedValue(starLevel, 0, 10, 5, 15, 10, 20, random) / 100.0);
        addAttributeModifier(entity, AttributeRegistry.MANA_REGEN,
                "mage_mana_regen", manaRegenBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // Cooldown reduction según estrella
        double cooldownReduction = roundToTwoDecimals(getStarBasedValue(starLevel, 0, 10, 5, 15, 10, 20, random) / 100.0);
        addAttributeModifier(entity, AttributeRegistry.COOLDOWN_REDUCTION,
                "mage_cooldown_reduction", cooldownReduction, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // Casting movespeed según estrella
        double castingMovespeed = roundToTwoDecimals(getStarBasedValue(starLevel, 0, 10, 5, 15, 10, 20, random) / 100.0);
        addAttributeModifier(entity, AttributeRegistry.CASTING_MOVESPEED,
                "mage_casting_movespeed", castingMovespeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void applyWarriorAttributes(RandomHumanEntity entity, int starLevel, RandomSource random) {
        // Health base: 20
        addAttributeModifier(entity, Attributes.MAX_HEALTH,
                "warrior_health", 20.0, AttributeModifier.Operation.ADD_VALUE);

        entity.heal(entity.getMaxHealth());

        // Ghost health
        double ghostHealth = 0.1;
        addAttributeModifier(entity, ALObjects.Attributes.GHOST_HEALTH,
                "warrior_ghost_health", ghostHealth, AttributeModifier.Operation.ADD_VALUE);

        // Lifesteal
        double lifesteal = roundToTwoDecimals(getRandomValue(0, 10, random) / 100.0);
        addAttributeModifier(entity, ALObjects.Attributes.LIFE_STEAL,
                "warrior_lifesteal", lifesteal, AttributeModifier.Operation.ADD_VALUE);

        // Overheal
        double overheal = roundToTwoDecimals(getRandomValue(5, 15, random) / 100.0);
        addAttributeModifier(entity, ALObjects.Attributes.OVERHEAL,
                "warrior_overheal", overheal, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyRogueAttributes(RandomHumanEntity entity, int starLevel, RandomSource random) {
        // Health base: 10
        addAttributeModifier(entity, Attributes.MAX_HEALTH,
                "rogue_health", 10.0, AttributeModifier.Operation.ADD_VALUE);

        entity.heal(entity.getMaxHealth());

        // Aplicar atributos de archer solo si es archer
        if (entity.isArcher()) {
            // Arrow attributes solo para archer
            double arrowDamage = roundToTwoDecimals(getRandomValue(100, 200, random) / 100.0);
            addAttributeModifier(entity, ALObjects.Attributes.ARROW_DAMAGE,
                    "archer_arrow_damage", arrowDamage - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            double arrowVelocity = roundToTwoDecimals(getRandomValue(100, 200, random) / 100.0);
            addAttributeModifier(entity, ALObjects.Attributes.ARROW_VELOCITY,
                    "archer_arrow_velocity", arrowVelocity - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            double drawSpeed = roundToTwoDecimals(getRandomValue(100, 200, random) / 100.0);
            addAttributeModifier(entity, ALObjects.Attributes.DRAW_SPEED,
                    "archer_draw_speed", drawSpeed - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            double projectileDamage = roundToTwoDecimals(getRandomValue(100, 200, random) / 100.0);
            addAttributeModifier(entity, ALObjects.Attributes.PROJECTILE_DAMAGE,
                    "archer_projectile_damage", projectileDamage - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }

        // Lifesteal para rogues
        double lifesteal = roundToTwoDecimals(getRandomValue(0, 10, random) / 100.0);
        addAttributeModifier(entity, ALObjects.Attributes.LIFE_STEAL,
                "rogue_lifesteal", lifesteal, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyCommonAttributes(RandomHumanEntity entity, int starLevel, RandomSource random) {
        // Crit chance (común para todas las clases)
        double critChance = roundToTwoDecimals(getClassSpecificCritChance(entity.getEntityClass(), random));
        addAttributeModifier(entity, ALObjects.Attributes.CRIT_CHANCE,
                "class_crit_chance", critChance / 100.0, AttributeModifier.Operation.ADD_VALUE);

        // Crit damage (común para todas las clases)
        double critDamage = roundToTwoDecimals(getClassSpecificCritDamage(entity.getEntityClass(), random));
        addAttributeModifier(entity, ALObjects.Attributes.CRIT_DAMAGE,
                "class_crit_damage", roundToTwoDecimals((critDamage / 100.0) - 1.5), AttributeModifier.Operation.ADD_VALUE);

        // Dodge chance (común para todas las clases)
        double dodgeChance = roundToTwoDecimals(getClassSpecificDodgeChance(entity.getEntityClass(), random));
        addAttributeModifier(entity, ALObjects.Attributes.DODGE_CHANCE,
                "class_dodge_chance", dodgeChance / 100.0, AttributeModifier.Operation.ADD_VALUE);

        // Armor penetration
        double armorShred = roundToTwoDecimals(getClassSpecificArmorShred(entity.getEntityClass(), random));
        if (armorShred > 0) {
            addAttributeModifier(entity, ALObjects.Attributes.ARMOR_SHRED,
                    "class_armor_shred", armorShred / 100.0, AttributeModifier.Operation.ADD_VALUE);
        }

        double armorPierce = roundToTwoDecimals(getClassSpecificArmorPierce(entity.getEntityClass(), random));
        if (armorPierce > 0) {
            addAttributeModifier(entity, ALObjects.Attributes.ARMOR_PIERCE,
                    "class_armor_pierce", armorPierce, AttributeModifier.Operation.ADD_VALUE);
        }

        // Protection penetration
        double protShred = roundToTwoDecimals(getClassSpecificProtShred(entity.getEntityClass(), random));
        if (protShred > 0) {
            addAttributeModifier(entity, ALObjects.Attributes.PROT_SHRED,
                    "class_prot_shred", protShred / 100.0, AttributeModifier.Operation.ADD_VALUE);
        }

        double protPierce = roundToTwoDecimals(getClassSpecificProtPierce(entity.getEntityClass(), random));
        if (protPierce > 0) {
            addAttributeModifier(entity, ALObjects.Attributes.PROT_PIERCE,
                    "class_prot_pierce", protPierce, AttributeModifier.Operation.ADD_VALUE);
        }

        // Summon damage (solo para mage)
        if (entity.getEntityClass() == EntityClass.MAGE) {
            double summonDamage = roundToTwoDecimals(getRandomValue(0, 20, random) / 100.0);
            addAttributeModifier(entity, AttributeRegistry.SUMMON_DAMAGE,
                    "mage_summon_damage", summonDamage, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
    }

    // Métodos helper para obtener valores específicos por clase
    private static double getClassSpecificCritChance(EntityClass entityClass, RandomSource random) {
        return switch (entityClass) {
            case MAGE -> getRandomValue(5, 100, random);
            case WARRIOR -> getRandomValue(5, 100, random);
            case ROGUE -> getRandomValue(50, 100, random);
        };
    }

    private static double getClassSpecificCritDamage(EntityClass entityClass, RandomSource random) {
        return switch (entityClass) {
            case MAGE -> getRandomValue(150, 250, random);
            case WARRIOR -> getRandomValue(150, 200, random);
            case ROGUE -> getRandomValue(200, 250, random);
        };
    }

    private static double getClassSpecificDodgeChance(EntityClass entityClass, RandomSource random) {
        return switch (entityClass) {
            case MAGE -> getRandomValue(0, 5, random);
            case WARRIOR -> getRandomValue(5, 10, random);
            case ROGUE -> getRandomValue(10, 25, random);
        };
    }

    private static double getClassSpecificArmorShred(EntityClass entityClass, RandomSource random) {
        return switch (entityClass) {
            case MAGE -> getRandomValue(5, 25, random);
            case WARRIOR -> getRandomValue(5, 10, random);
            case ROGUE -> 0.0; // Rogues no tienen armor shred
        };
    }

    private static double getClassSpecificArmorPierce(EntityClass entityClass, RandomSource random) {
        return switch (entityClass) {
            case MAGE -> 0.0; // Mages no tienen armor pierce
            case WARRIOR -> getRandomValue(5, 10, random);
            case ROGUE -> getRandomValue(5, 25, random);
        };
    }

    private static double getClassSpecificProtShred(EntityClass entityClass, RandomSource random) {
        return switch (entityClass) {
            case MAGE -> getRandomValue(5, 25, random);
            case WARRIOR -> getRandomValue(5, 10, random);
            case ROGUE -> 0.0; // Rogues no tienen prot shred por defecto
        };
    }

    private static double getClassSpecificProtPierce(EntityClass entityClass, RandomSource random) {
        return switch (entityClass) {
            case MAGE -> 0.0; // Mages no tienen prot pierce
            case WARRIOR -> getRandomValue(5, 10, random);
            case ROGUE -> getRandomValue(5, 25, random);
        };
    }

    // Métodos helper para valores aleatorios
    private static double getRandomValue(double min, double max, RandomSource random) {
        double value = min + (max - min) * random.nextDouble();
        return roundToTwoDecimals(value);
    }

    private static double getStarBasedValue(int starLevel, double min1, double max1,
                                            double min2, double max2, double min3, double max3,
                                            RandomSource random) {
        double value = switch (starLevel) {
            case 1 -> getRandomValueRaw(min1, max1, random);
            case 2 -> getRandomValueRaw(min2, max2, random);
            case 3 -> getRandomValueRaw(min3, max3, random);
            default -> getRandomValueRaw(min1, max1, random);
        };
        return roundToTwoDecimals(value);
    }

    private static double getRandomValueRaw(double min, double max, RandomSource random) {
        return min + (max - min) * random.nextDouble();
    }

    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void addAttributeModifier(RandomHumanEntity entity, Holder<Attribute> attributeHolder,
                                             String modifierName, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attributeHolder);
        if (instance != null) {
            net.minecraft.resources.ResourceLocation modifierLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, modifierName.toLowerCase().replace(" ", "_"));

            // Remover modificador existente si existe
            AttributeModifier existing = instance.getModifier(modifierLocation);
            if (existing != null) {
                instance.removeModifier(existing);
            }

            // Redondear el amount antes de crear el modificador
            double roundedAmount = roundToTwoDecimals(amount);

            // Añadir nuevo modificador
            AttributeModifier modifier = new AttributeModifier(modifierLocation, roundedAmount, operation);
            instance.addPermanentModifier(modifier);

            MagicRealms.LOGGER.debug("Applied attribute modifier: {} = {} ({})", modifierName, roundedAmount, operation);
        } else {
            MagicRealms.LOGGER.warn("Failed to apply attribute modifier: {} - attribute instance is null", modifierName);
        }
    }
}
