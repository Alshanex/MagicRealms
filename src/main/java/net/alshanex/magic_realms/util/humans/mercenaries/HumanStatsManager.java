package net.alshanex.magic_realms.util.humans.mercenaries;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class HumanStatsManager {

    public static void applyClassAttributes(AbstractMercenaryEntity entity) {
        int starLevel = entity.getStarLevel();
        RandomSource random = entity.level().getRandom();

        entity.getCombatClass().applyAttributes(entity, starLevel, random);
    }

    public static double getStarBasedValue(int starLevel, double min1, double max1,
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

    public static void addAttributeModifier(AbstractMercenaryEntity entity, Holder<Attribute> attributeHolder,
                                            String modifierName, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attributeHolder);
        if (instance != null) {
            ResourceLocation modifierLocation = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, modifierName.toLowerCase().replace(" ", "_"));

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
