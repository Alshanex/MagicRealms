package net.alshanex.magic_realms.util.humans;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.entries.TraitEntry;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.TraitRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.*;

public class LevelingStatsManager {
    private static final List<TraitEntry<?>> commonTraits = List.of(LHTraits.FIERY, LHTraits.PROTECTION, LHTraits.REGEN, LHTraits.SPEEDY, LHTraits.TANK, LHTraits.MOONWALK,
            LHTraits.GRAVITY, TraitRegistry.SNEAKY);

    private static final List<TraitEntry<?>> warriorTraits = List.of(LHTraits.STRIKE);

    private static final List<TraitEntry<?>> archerTraits = List.of(LHTraits.GRENADE);

    private static final List<TraitEntry<?>> assassinTraits = List.of(LHTraits.INVISIBLE);

    private static final List<TraitEntry<?>> epicTraits = List.of(LHTraits.DRAIN, LHTraits.CORROSION, LHTraits.EROSION, TraitRegistry.BLOOD_SCHOOL_IMMUNITY, TraitRegistry.ELDRITCH_SCHOOL_IMMUNITY,
            TraitRegistry.ENDER_SCHOOL_IMMUNITY, TraitRegistry.EVOCATION_SCHOOL_IMMUNITY, TraitRegistry.FIRE_SCHOOL_IMMUNITY, TraitRegistry.HOLY_SCHOOL_IMMUNITY,
            TraitRegistry.ICE_SCHOOL_IMMUNITY, TraitRegistry.LIGHTNING_SCHOOL_IMMUNITY, TraitRegistry.NATURE_SCHOOL_IMMUNITY, TraitRegistry.VAMPIRIC, TraitRegistry.ANTI_MAGIC,
            TraitRegistry.ANTI_MAGIC);

    private static final List<TraitEntry<?>> legendaryTraits = List.of(LHTraits.ADAPTIVE, LHTraits.KILLER_AURA, TraitRegistry.MANA_SIPHON);

    private static final List<TraitEntry<?>> specialConditionTraits = List.of(LHTraits.RAGNAROK, LHTraits.UNDYING, LHTraits.ENDER, LHTraits.DISPELL, LHTraits.DEMENTOR);

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

        applyLevelBasedTraits(entity, level);
    }

    private static void applyLevelBasedTraits(RandomHumanEntity entity, int level) {
        MobTraitCap cap = LHMiscs.MOB.type().getOrCreate(entity);
        if (cap == null) {
            MagicRealms.LOGGER.warn("Could not obtain MobTraitCap for entity {}", entity.getEntityName());
            return;
        }

        Set<TraitEntry<?>> currentTraits = new HashSet<>();
        cap.traitEvent((trait, traitLevel) -> {
            TraitEntry<?> traitEntry = findTraitEntry(trait);
            if (traitEntry != null) {
                currentTraits.add(traitEntry);
            }
        });

        List<TraitEntry<?>> expectedTraits = calculateExpectedTraits(entity, level);

        for (TraitEntry<?> traitEntry : expectedTraits) {
            if (!currentTraits.contains(traitEntry)) {
                try {
                    cap.setTrait(traitEntry.get(), 1);
                    MagicRealms.LOGGER.info("Added trait {} to entity {} at level {}",
                            traitEntry.getId(), entity.getEntityName(), level);
                } catch (Exception e) {
                    MagicRealms.LOGGER.error("Failed to add trait {} to entity {}: {}",
                            traitEntry.getId(), entity.getEntityName(), e.getMessage());
                }
            }
        }
    }

    private static List<TraitEntry<?>> calculateExpectedTraits(RandomHumanEntity entity, int level) {
        List<TraitEntry<?>> expectedTraits = new ArrayList<>();
        EntityClass entityClass = entity.getEntityClass();
        RandomSource random = entity.getRandom();

        // Niveles 0-200: cada 25 niveles
        int traitsFromEarlyLevels = Math.min(level, 200) / 25;

        // Niveles 200-300: cada 50 niveles
        int traitsFromLateLevels = Math.max(0, level - 200) / 50;

        int totalTraits = traitsFromEarlyLevels + traitsFromLateLevels;

        MagicRealms.LOGGER.debug("Entity {} at level {} should have {} traits ({} early + {} late)",
                entity.getEntityName(), level, totalTraits, traitsFromEarlyLevels, traitsFromLateLevels);

        Random consistentRandom = new Random(entity.getUUID().hashCode());

        // Generar traits para niveles 0-200
        for (int i = 0; i < traitsFromEarlyLevels; i++) {
            TraitEntry<?> trait = selectTraitForEarlyLevel(entityClass, expectedTraits, consistentRandom);
            if (trait != null) {
                expectedTraits.add(trait);
            }
        }

        // Generar traits para niveles 200-300
        for (int i = 0; i < traitsFromLateLevels; i++) {
            TraitEntry<?> trait = selectTraitForLateLevel(entityClass, expectedTraits, consistentRandom);
            if (trait != null) {
                expectedTraits.add(trait);
            }
        }

        return expectedTraits;
    }

    private static TraitEntry<?> selectTraitForEarlyLevel(EntityClass entityClass, List<TraitEntry<?>> alreadySelected, Random random) {
        // Probabilidades para niveles 0-200: 75% common, 24% epic, 1% legendary
        int roll = random.nextInt(100);

        List<TraitEntry<?>> availableTraits = new ArrayList<>();

        if (roll < 75) {
            // Common traits
            availableTraits.addAll(commonTraits);
        } else if (roll < 99) {
            // Epic traits + class-specific traits
            availableTraits.addAll(epicTraits);
            availableTraits.addAll(getClassSpecificTraits(entityClass));
        } else {
            // Legendary traits
            availableTraits.addAll(legendaryTraits);
        }

        // Filtrar traits ya seleccionados
        availableTraits.removeAll(alreadySelected);

        if (availableTraits.isEmpty()) {
            MagicRealms.LOGGER.warn("No available traits for early level selection");
            return null;
        }

        return availableTraits.get(random.nextInt(availableTraits.size()));
    }

    private static TraitEntry<?> selectTraitForLateLevel(EntityClass entityClass, List<TraitEntry<?>> alreadySelected, Random random) {
        // Probabilidades para niveles 200-300: 24% common, 70% epic, 5% legendary, 1% special
        int roll = random.nextInt(100);

        List<TraitEntry<?>> availableTraits = new ArrayList<>();

        if (roll < 24) {
            // Common traits
            availableTraits.addAll(commonTraits);
        } else if (roll < 94) {
            // Epic traits + class-specific traits
            availableTraits.addAll(epicTraits);
            availableTraits.addAll(getClassSpecificTraits(entityClass));
        } else if (roll < 99) {
            // Legendary traits
            availableTraits.addAll(legendaryTraits);
        } else {
            // Special condition traits
            availableTraits.addAll(specialConditionTraits);
        }

        // Filtrar traits ya seleccionados
        availableTraits.removeAll(alreadySelected);

        if (availableTraits.isEmpty()) {
            MagicRealms.LOGGER.warn("No available traits for late level selection");
            return null;
        }

        return availableTraits.get(random.nextInt(availableTraits.size()));
    }

    private static List<TraitEntry<?>> getClassSpecificTraits(EntityClass entityClass) {
        return switch (entityClass) {
            case WARRIOR -> new ArrayList<>(warriorTraits);
            case ROGUE -> {
                List<TraitEntry<?>> rogueTraits = new ArrayList<>();
                rogueTraits.addAll(archerTraits);
                rogueTraits.addAll(assassinTraits);
                yield rogueTraits;
            }
            case MAGE -> new ArrayList<>();
        };
    }

    private static TraitEntry<?> findTraitEntry(dev.xkmc.l2hostility.content.traits.base.MobTrait trait) {
        List<List<TraitEntry<?>>> allTraitLists = List.of(
                commonTraits, warriorTraits, archerTraits, assassinTraits,
                epicTraits, legendaryTraits, specialConditionTraits
        );

        for (List<TraitEntry<?>> traitList : allTraitLists) {
            for (TraitEntry<?> traitEntry : traitList) {
                if (traitEntry.get().getClass().equals(trait.getClass())) {
                    return traitEntry;
                }
            }
        }

        return null;
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
