package net.alshanex.magic_realms.util.humans.stats;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class HumanStatsManager {

    public static void applyClassAttributes(AbstractMercenaryEntity entity) {
        EntityClass entityClass = entity.getEntityClass();
        int starLevel = entity.getStarLevel();
        RandomSource random = entity.level().getRandom();

        //MagicRealms.LOGGER.debug("Applying attributes for {} with {} stars", entityClass.getName(), starLevel);

        // Aplicar atributos base según la clase
        switch (entityClass) {
            case MAGE -> applyMageAttributes(entity, starLevel, random);
            case WARRIOR -> applyWarriorAttributes(entity, starLevel, random);
            case ROGUE -> applyRogueAttributes(entity, starLevel, random);
        }

        // Aplicar atributos comunes a todas las clases
        applyCommonAttributes(entity, starLevel, random);
    }

    private static void applyMageAttributes(AbstractMercenaryEntity entity, int starLevel, RandomSource random) {
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

        applyMageSchoolBonuses(entity, random);
    }

    private static void applyMageSchoolBonuses(AbstractMercenaryEntity entity, RandomSource random) {
        if (entity.getEntityClass() != EntityClass.MAGE) {
            return;
        }

        List<SchoolType> schools = entity.getMagicSchools();

        for (SchoolType school : schools) {
            double bonusPercentage = 5.0 + (random.nextDouble() * 5.0);
            double roundedBonus = roundToTwoDecimals(bonusPercentage);

            ResourceLocation powerAttributeId = ResourceLocation.fromNamespaceAndPath(
                    school.getId().getNamespace(),
                    school.getId().getPath() + "_spell_power"
            );

            var attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(powerAttributeId).orElse(null);

            if (attributeHolder != null) {
                addAttributeModifier(entity, attributeHolder,
                        "mage_initial_" + school.getId().getPath() + "_power",
                        roundedBonus / 100.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            } else {
                MagicRealms.LOGGER.warn("Could not find power attribute for school: {}", school.getId());
            }
        }
    }

    private static void applyWarriorAttributes(AbstractMercenaryEntity entity, int starLevel, RandomSource random) {
        // Health base: 20
        addAttributeModifier(entity, Attributes.MAX_HEALTH,
                "warrior_health", 20.0, AttributeModifier.Operation.ADD_VALUE);

        entity.heal(entity.getMaxHealth());
    }

    private static void applyRogueAttributes(AbstractMercenaryEntity entity, int starLevel, RandomSource random) {
        // Health base: 10
        addAttributeModifier(entity, Attributes.MAX_HEALTH,
                "rogue_health", 10.0, AttributeModifier.Operation.ADD_VALUE);

        entity.heal(entity.getMaxHealth());
    }

    private static void applyCommonAttributes(AbstractMercenaryEntity entity, int starLevel, RandomSource random) {
        // Summon damage (solo para mage)
        if (entity.getEntityClass() == EntityClass.MAGE) {
            double summonDamage = roundToTwoDecimals(getRandomValue(0, 20, random) / 100.0);
            addAttributeModifier(entity, AttributeRegistry.SUMMON_DAMAGE,
                    "mage_summon_damage", summonDamage, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
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

    private static void addAttributeModifier(AbstractMercenaryEntity entity, Holder<Attribute> attributeHolder,
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

            //MagicRealms.LOGGER.debug("Applied attribute modifier: {} = {} ({})", modifierName, roundedAmount, operation);
        } else {
            MagicRealms.LOGGER.warn("Failed to apply attribute modifier: {} - attribute instance is null", modifierName);
        }
    }
}
