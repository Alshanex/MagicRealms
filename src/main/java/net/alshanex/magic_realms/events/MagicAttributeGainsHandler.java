package net.alshanex.magic_realms.events;

import dev.xkmc.l2hostility.init.entries.TraitEntry;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class MagicAttributeGainsHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof RandomHumanEntity humanEntity) || humanEntity.level().isClientSide) {
            return;
        }

        DamageSource damageSource = event.getSource();

        SchoolType school = getSchoolFromDamageSource(damageSource);
        if (school != null) {
            handleSpellDamageReceived(humanEntity, school);
        }
    }

    public static void handleSpellCast(RandomHumanEntity entity, SchoolType school) {
        if (school == null) return;

        if (RANDOM.nextFloat() < 0.05f) {
            applySpellPowerBonus(entity, school);
        }
    }

    private static void handleSpellDamageReceived(RandomHumanEntity entity, SchoolType school) {
        if (RANDOM.nextFloat() < 0.05f) {
            applySpellResistanceBonus(entity, school);
        }
    }

    private static void applySpellPowerBonus(RandomHumanEntity entity, SchoolType school) {
        EntityClass entityClass = entity.getEntityClass();
        int starLevel = entity.getStarLevel();

        if (!canGainSpellPower(entityClass)) {
            MagicRealms.LOGGER.debug("Entity {} (class: {}) cannot gain spell power bonuses",
                    entity.getEntityName(), entityClass.getName());
            return;
        }

        if (hasReachedAttributeLimit(entity, school, "spell_power")) {
            MagicRealms.LOGGER.debug("Entity {} has reached maximum spell power bonus for {} school",
                    entity.getEntityName(), school.getId());
            return;
        }

        double bonusPercentage = calculateSpellPowerBonus(entityClass, starLevel);
        Holder<Attribute> powerAttribute = getPowerAttributeForSchool(school);

        if (powerAttribute == null) {
            MagicRealms.LOGGER.warn("Could not find power attribute for school: {}", school.getId());
            return;
        }

        applyAttributeBonus(entity, powerAttribute, school, bonusPercentage, "spell_power");

        MagicRealms.LOGGER.info("Applied {}% spell power bonus for {} school to entity {} ({}★ {})",
                bonusPercentage, school.getId(), entity.getEntityName(), starLevel, entityClass.getName());
    }

    private static void applySpellResistanceBonus(RandomHumanEntity entity, SchoolType school) {
        EntityClass entityClass = entity.getEntityClass();
        int starLevel = entity.getStarLevel();

        if (!canGainSpellResistance(entityClass)) {
            MagicRealms.LOGGER.debug("Entity {} (class: {}) cannot gain spell resistance bonuses",
                    entity.getEntityName(), entityClass.getName());
            return;
        }

        if (hasReachedAttributeLimit(entity, school, "spell_resistance")) {
            MagicRealms.LOGGER.debug("Entity {} has reached maximum spell resistance bonus for {} school",
                    entity.getEntityName(), school.getId());
            return;
        }

        double bonusPercentage = calculateSpellResistanceBonus(entityClass, starLevel);
        Holder<Attribute> resistAttribute = getResistanceAttributeForSchool(school);

        if (resistAttribute == null) {
            MagicRealms.LOGGER.warn("Could not find resistance attribute for school: {}", school.getId());
            return;
        }

        applyAttributeBonus(entity, resistAttribute, school, bonusPercentage, "spell_resistance");

        MagicRealms.LOGGER.info("Applied {}% spell resistance bonus for {} school to entity {} ({}★ {})",
                bonusPercentage, school.getId(), entity.getEntityName(), starLevel, entityClass.getName());
    }

    private static void applyAttributeBonus(RandomHumanEntity entity, Holder<Attribute> attributeHolder,
                                            SchoolType school, double bonusPercentage, String type) {
        AttributeInstance instance = entity.getAttribute(attributeHolder);
        if (instance == null) {
            MagicRealms.LOGGER.warn("Could not find attribute instance for {} on entity {}",
                    attributeHolder.toString(), entity.getEntityName());
            return;
        }

        long timestamp = System.currentTimeMillis();
        int randomSuffix = RANDOM.nextInt(10000);

        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                MagicRealms.MODID,
                type + "_" + school.getId().getPath() + "_" + timestamp + "_" + randomSuffix
        );

        double bonusValue = bonusPercentage / 100.0;
        AttributeModifier modifier = new AttributeModifier(
                modifierId,
                bonusValue,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        instance.addPermanentModifier(modifier);

        MagicRealms.LOGGER.debug("Added attribute modifier {} with value {} to entity {}",
                modifierId, bonusValue, entity.getEntityName());
    }

    private static boolean hasReachedAttributeLimit(RandomHumanEntity entity, SchoolType school, String type) {
        // Calcular el límite máximo: 50% + (10% * estrellas)
        int starLevel = entity.getStarLevel();
        double maxBonusPercentage = 50.0 + (10.0 * starLevel);

        Holder<Attribute> attributeHolder = type.equals("spell_power") ?
                getPowerAttributeForSchool(school) : getResistanceAttributeForSchool(school);

        if (attributeHolder == null) {
            return false;
        }

        AttributeInstance instance = entity.getAttribute(attributeHolder);
        if (instance == null) {
            return false;
        }

        // Calcular el bonus total actual sumando todos los modificadores de Magic Realms para esta escuela
        // Formato: type_school_timestamp_random (ej: spell_power_fire_1642534823_7439)
        String expectedPrefix = type + "_" + school.getId().getPath() + "_";

        double currentTotalBonus = instance.getModifiers().stream()
                .filter(modifier -> modifier.id().getNamespace().equals(MagicRealms.MODID))
                .filter(modifier -> modifier.id().getPath().startsWith(expectedPrefix))
                .mapToDouble(AttributeModifier::amount)
                .sum();

        double currentTotalBonusPercentage = currentTotalBonus * 100.0;

        boolean hasReachedLimit = currentTotalBonusPercentage >= maxBonusPercentage;

        if (hasReachedLimit) {
            MagicRealms.LOGGER.debug("Entity {} has reached {}% bonus limit for {} {} (current: {}%)",
                    entity.getEntityName(), maxBonusPercentage, school.getId().getPath(), type, currentTotalBonusPercentage);
        }

        return hasReachedLimit;
    }

    private static double calculateSpellPowerBonus(EntityClass entityClass, int starLevel) {
        return switch (entityClass) {
            case MAGE -> switch (starLevel) {
                case 1 -> RANDOM.nextBoolean() ? 2.0 : 3.0;
                case 2 -> RANDOM.nextBoolean() ? 3.0 : 4.0;
                case 3 -> RANDOM.nextBoolean() ? 4.0 : 5.0;
                default -> 2.0;
            };
            case ROGUE -> switch (starLevel) {
                case 1 -> RANDOM.nextBoolean() ? 2.0 : 3.0;
                case 2 -> RANDOM.nextBoolean() ? 3.0 : 4.0;
                case 3 -> RANDOM.nextBoolean() ? 4.0 : 5.0;
                default -> 2.0;
            };
            default -> 0.0; // Warriors no ganan spell power
        };
    }

    private static double calculateSpellResistanceBonus(EntityClass entityClass, int starLevel) {
        return switch (entityClass) {
            case MAGE -> switch (starLevel) {
                case 1 -> RANDOM.nextBoolean() ? 2.0 : 3.0;
                case 2 -> RANDOM.nextBoolean() ? 3.0 : 4.0;
                case 3 -> RANDOM.nextBoolean() ? 4.0 : 5.0;
                default -> 2.0;
            };
            case WARRIOR -> switch (starLevel) {
                case 1 -> RANDOM.nextBoolean() ? 2.0 : 3.0;
                case 2 -> RANDOM.nextBoolean() ? 3.0 : 4.0;
                case 3 -> RANDOM.nextBoolean() ? 4.0 : 5.0;
                default -> 2.0;
            };
            default -> 0.0; // Rogues no ganan spell resistance
        };
    }

    private static boolean canGainSpellPower(EntityClass entityClass) {
        return entityClass == EntityClass.MAGE || entityClass == EntityClass.ROGUE;
    }

    private static boolean canGainSpellResistance(EntityClass entityClass) {
        return entityClass == EntityClass.MAGE || entityClass == EntityClass.WARRIOR;
    }

    private static Holder<Attribute> getPowerAttributeForSchool(SchoolType school) {
        // Construir el ResourceLocation del atributo de poder
        // Formato: "modid:schoolname_spell_power"
        ResourceLocation powerAttributeId = ResourceLocation.fromNamespaceAndPath(
                school.getId().getNamespace(),
                school.getId().getPath() + "_spell_power"
        );

        var attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(powerAttributeId).orElse(null);

        if (attributeHolder == null) {
            MagicRealms.LOGGER.debug("Power attribute not found for school {}: {}",
                    school.getId(), powerAttributeId);
        } else {
            MagicRealms.LOGGER.debug("Found power attribute for school {}: {}",
                    school.getId(), powerAttributeId);
        }

        return attributeHolder;
    }

    private static Holder<Attribute> getResistanceAttributeForSchool(SchoolType school) {
        // Construir el ResourceLocation del atributo de resistencia
        // Formato: "modid:schoolname_magic_resist"
        ResourceLocation resistAttributeId = ResourceLocation.fromNamespaceAndPath(
                school.getId().getNamespace(),
                school.getId().getPath() + "_magic_resist"
        );

        var attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(resistAttributeId).orElse(null);

        if (attributeHolder == null) {
            MagicRealms.LOGGER.debug("Resistance attribute not found for school {}: {}",
                    school.getId(), resistAttributeId);
        } else {
            MagicRealms.LOGGER.debug("Found resistance attribute for school {}: {}",
                    school.getId(), resistAttributeId);
        }

        return attributeHolder;
    }

    private static SchoolType getSchoolFromDamageSource(DamageSource damageSource) {
        List<SchoolType> schools = SchoolRegistry.REGISTRY.stream().toList();

        for (SchoolType school : schools) {
            if (damageSource.is(school.getDamageType())) {
                return school;
            }
        }

        return null;
    }
}
