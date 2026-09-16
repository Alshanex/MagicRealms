package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
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
        if (!(event.getEntity() instanceof AbstractMercenaryEntity humanEntity) || humanEntity.level().isClientSide) {
            return;
        }

        DamageSource damageSource = event.getSource();

        SchoolType school = getSchoolFromDamageSource(damageSource);
        if (school != null) {
            handleSpellDamageReceived(humanEntity, school);
        }
    }

    public static void handleSpellCast(AbstractMercenaryEntity entity, SchoolType school) {
        if (school == null) return;

        if (RANDOM.nextFloat() < 0.05f) {
            applySpellPowerBonus(entity, school);
        }
    }

    private static void handleSpellDamageReceived(AbstractMercenaryEntity entity, SchoolType school) {
        if (RANDOM.nextFloat() < 0.05f) {
            applySpellResistanceBonus(entity, school);
        }
    }

    private static void applySpellPowerBonus(AbstractMercenaryEntity entity, SchoolType school) {
        int starLevel = entity.getStarLevel();

        if (!canGainSpellPower(entity)) {
            return;
        }

        if (hasReachedAttributeLimit(entity, school, "spell_power")) {
            return;
        }

        double bonusPercentage = calculateSpellPowerBonus(entity, starLevel);
        Holder<Attribute> powerAttribute = getPowerAttributeForSchool(school);

        if (powerAttribute == null) {
            MagicRealms.LOGGER.warn("Could not find power attribute for school: {}", school.getId());
            return;
        }

        applyAttributeBonus(entity, powerAttribute, school, bonusPercentage, "spell_power");
    }

    private static void applySpellResistanceBonus(AbstractMercenaryEntity entity, SchoolType school) {
        int starLevel = entity.getStarLevel();

        if (!canGainSpellResistance(entity)) {
            return;
        }

        if (hasReachedAttributeLimit(entity, school, "spell_resistance")) {
            return;
        }

        double bonusPercentage = calculateSpellResistanceBonus(entity, starLevel);
        Holder<Attribute> resistAttribute = getResistanceAttributeForSchool(school);

        if (resistAttribute == null) {
            MagicRealms.LOGGER.warn("Could not find resistance attribute for school: {}", school.getId());
            return;
        }

        applyAttributeBonus(entity, resistAttribute, school, bonusPercentage, "spell_resistance");
    }

    private static void applyAttributeBonus(AbstractMercenaryEntity entity, Holder<Attribute> attributeHolder,
                                            SchoolType school, double bonusPercentage, String type) {
        AttributeInstance instance = entity.getAttribute(attributeHolder);
        if (instance == null) {
            MagicRealms.LOGGER.warn("Could not find attribute instance for {} on entity {}",
                    attributeHolder.toString(), entity.getEntityName());
            return;
        }

        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                MagicRealms.MODID,
                type + "_" + school.getId().getPath()  // no timestamp, no random
        );

        double bonusValue = bonusPercentage / 100.0;

        AttributeModifier existing = instance.getModifier(modifierId);
        double newValue = (existing != null ? existing.amount() : 0.0) + bonusValue;

        if (existing != null) instance.removeModifier(existing);
        instance.addPermanentModifier(new AttributeModifier(modifierId, newValue, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static boolean hasReachedAttributeLimit(AbstractMercenaryEntity entity, SchoolType school, String type) {
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

        // Single modifier per (type, school) — look it up directly instead of summing a prefix scan.
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                MagicRealms.MODID,
                type + "_" + school.getId().getPath()
        );

        AttributeModifier mod = instance.getModifier(modifierId);
        double currentTotalBonusPercentage = (mod != null ? mod.amount() : 0.0) * 100.0;

        return currentTotalBonusPercentage >= maxBonusPercentage;
    }

    private static double calculateSpellPowerBonus(AbstractMercenaryEntity entity, int starLevel) {
        return entity.getCombatClass().spellPowerBonus(starLevel, entity.level().getRandom());
    }

    private static double calculateSpellResistanceBonus(AbstractMercenaryEntity entity, int starLevel) {
        return entity.getCombatClass().spellResistanceBonus(starLevel, entity.level().getRandom());
    }

    private static boolean canGainSpellPower(AbstractMercenaryEntity entity) {
        return entity.getCombatClass().canGainSpellPower();
    }

    private static boolean canGainSpellResistance(AbstractMercenaryEntity entityClass) {
        return entityClass.getCombatClass().canGainSpellResist();
    }

    private static Holder<Attribute> getPowerAttributeForSchool(SchoolType school) {
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
