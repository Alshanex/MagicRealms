package net.alshanex.magic_realms.util.humans.bandits;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Centralized helpers that apply a {@link BanditProfile} to a {@link net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity}.
 */
public final class BanditProfileApplier {

    /** Modifier id namespace prefix for profile-applied attribute boosts. */
    private static final String BOOST_ID_PREFIX = "bandit_profile";

    private BanditProfileApplier() {}

    // Resolution

    /** Resolve a profile id from either the server or client catalog. Returns null if not found. */
    public static BanditProfile resolve(String profileId, boolean isClientSide) {
        if (profileId == null || profileId.isEmpty()) return null;
        return BanditProfileCatalogHolder.get(isClientSide).byId(profileId);
    }

    // Per-stage appliers — call in this order during finalizeSpawn

    /**
     * Apply the visual/identity portion: gender, name, preset texture metadata. Should run during {@code initializeAppearance}.
     */
    public static void applyAppearance(HostileRandomHumanEntity entity, BanditProfile profile) {
        if (profile == null) return;

        profile.gender().ifPresent(entity::setGender);
        profile.entityClass().ifPresent(entity::setEntityClass);
        profile.overrideName().ifPresent(name -> {
            if (!name.isEmpty()) entity.setEntityName(name);
        });
    }

    /**
     * Apply class-specific build flags: shield/archer toggles and explicit magic schools. Should run during {@code initializeClassSpecifics}, AFTER the entity class is settled.
     */
    public static void applyClassSpecifics(HostileRandomHumanEntity entity, BanditProfile profile, RandomSource random) {
        if (profile == null) return;

        profile.hasShield().ifPresent(entity::setHasShield);
        profile.isArcher().ifPresent(entity::setIsArcher);

        // Resolve magic schools: explicit list takes precedence; otherwise tag.
        List<SchoolType> schools = resolveMagicSchools(profile);
        if (!schools.isEmpty()) {
            entity.setMagicSchools(schools);
        }
    }

    /**
     * Apply equipment overrides on top of (or replacing) the default equipment for unspecified slots.
     * Should run AFTER {@code initializeDefaultEquipment} so profile-specified slots overwrite defaults but unset slots keep their defaults (e.g. warrior wooden sword fallback).
     */
    public static void applyEquipment(HostileRandomHumanEntity entity, BanditProfile profile) {
        if (profile == null) return;

        Map<EquipmentSlot, ItemStack> equipment = profile.resolveEquipment();
        for (Map.Entry<EquipmentSlot, ItemStack> e : equipment.entrySet()) {
            entity.setItemSlot(e.getKey(), e.getValue().copy());
        }
    }

    /**
     * Override the level roll using the profile's range (absolute over percent).
     * Returns true if a profile-driven level was set, false if the caller should fall back to vanilla rolling. Should run inside {@code initializeHumanLevel}.
     */
    public static boolean applyLevelRoll(HostileRandomHumanEntity entity, BanditProfile profile,
                                         KillTrackerData killData, RandomSource random) {
        if (profile == null) return false;
        if (killData.isInitialized()) return true; // already set by a previous run

        int maxLevel = Config.maxLevel;

        Integer min = profile.minLevelAbsolute().orElse(null);
        Integer max = profile.maxLevelAbsolute().orElse(null);

        if (min == null || max == null) {
            // Try percent-based range
            Float minPct = profile.minLevelPercent().orElse(null);
            Float maxPct = profile.maxLevelPercent().orElse(null);
            if (minPct != null || maxPct != null) {
                float lo = minPct != null ? minPct : 0.0f;
                float hi = maxPct != null ? maxPct : 1.0f;
                if (hi < lo) hi = lo;
                min = Math.max(1, Math.round(lo * maxLevel));
                max = Math.max(min, Math.round(hi * maxLevel));
            }
        }

        if (min == null || max == null) {
            return false; // no level configuration — caller falls back to default roll
        }

        min = clamp(min, 1, maxLevel);
        max = clamp(max, min, maxLevel);

        int rolled = (max == min) ? min : (random.nextInt(max - min + 1) + min);
        killData.setLevel(rolled);
        return true;
    }

    /**
     * Generate the spell list according to the profile's preferences.
     * Returns null if the profile doesn't override spell selection (caller falls back to default generator).
     */
    public static List<AbstractSpell> resolveSpells(HostileRandomHumanEntity entity, BanditProfile profile,
                                                    RandomSource random) {
        if (profile == null) return null;

        // Mode 1: explicit spell ids
        if (!profile.explicitSpells().isEmpty()) {
            List<AbstractSpell> spells = new ArrayList<>();
            for (ResourceLocation id : profile.explicitSpells()) {
                AbstractSpell s = SpellRegistry.getSpell(id);
                if (s != null) spells.add(s);
                else MagicRealms.LOGGER.warn("Bandit profile {} references unknown spell {}", profile.id(), id);
            }
            return spells;
        }

        // Mode 2: tag-based
        if (profile.spellsTag().isPresent()) {
            TagKey<AbstractSpell> tag = profile.spellsTag().get();
            List<AbstractSpell> tagSpells = SpellListGenerator.getSpellsFromTag(tag);
            if (tagSpells.isEmpty()) return new ArrayList<>();

            int pickCount = profile.spellsTagPickCount().orElse(tagSpells.size());
            if (pickCount >= tagSpells.size()) {
                return new ArrayList<>(tagSpells);
            }
            // Random subset
            List<AbstractSpell> shuffled = new ArrayList<>(tagSpells);
            Collections.shuffle(shuffled, new Random(random.nextLong()));
            return new ArrayList<>(shuffled.subList(0, pickCount));
        }

        // No override — caller falls back.
        return null;
    }

    /**
     * Apply post-init effects: scale, attribute boosts, immortal flag.
     * Should run at the END of {@code handlePostSpawnInitialization}, AFTER class/level attribute math has set base values, so boosts stack cleanly on top.
     */
    public static void applyPostInit(HostileRandomHumanEntity entity, BanditProfile profile) {
        if (profile == null) return;

        profile.entityScale().ifPresent(scale -> {
            AttributeInstance scaleAttr = entity.getAttribute(Attributes.SCALE);
            if (scaleAttr != null) {
                ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID,
                        BOOST_ID_PREFIX + "/scale/" + sanitizeId(profile.id()));
                AttributeModifier existing = scaleAttr.getModifier(modId);
                if (existing != null) scaleAttr.removeModifier(existing);
                scaleAttr.addPermanentModifier(new AttributeModifier(
                        modId, scale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        });

        // Attribute boosts
        for (int i = 0; i < profile.attributeBoosts().size(); i++) {
            BanditProfile.AttributeBoost boost = profile.attributeBoosts().get(i);
            applyBoost(entity, profile, i, boost);
        }

        // Immortal flag
        if (profile.immortal()) {
            entity.setImmortal(true);
        }

        // Heal to full so all the new max-health stacks visibly.
        entity.heal(entity.getMaxHealth());
    }

    // Helpers

    private static List<SchoolType> resolveMagicSchools(BanditProfile profile) {
        // Explicit list wins
        if (!profile.magicSchools().isEmpty()) {
            List<SchoolType> result = new ArrayList<>();
            for (ResourceLocation id : profile.magicSchools()) {
                SchoolType school = SchoolRegistry.REGISTRY.get(id);
                if (school != null) result.add(school);
                else MagicRealms.LOGGER.warn("Bandit profile {} references unknown school {}", profile.id(), id);
            }
            return result;
        }

        // Tag-based
        if (profile.magicSchoolsTag().isPresent()) {
            TagKey<SchoolType> tag = profile.magicSchoolsTag().get();
            return SchoolRegistry.REGISTRY.stream()
                    .filter(s -> ModTags.isSchoolInTag(s, tag))
                    .toList();
        }

        return List.of();
    }

    private static void applyBoost(HostileRandomHumanEntity entity, BanditProfile profile, int index,
                                   BanditProfile.AttributeBoost boost) {
        AttributeInstance instance = entity.getAttribute(boost.attribute());
        if (instance == null) return;

        // Derive a stable, idempotent modifier id from the profile id and index so re-application (e.g. on world load) replaces rather than stacks.
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID,
                BOOST_ID_PREFIX + "/" + sanitizeId(profile.id()) + "/" + index);

        AttributeModifier existing = instance.getModifier(modifierId);
        if (existing != null) instance.removeModifier(existing);

        instance.addPermanentModifier(new AttributeModifier(
                modifierId, boost.modifier().amount(), boost.modifier().operation()));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Strip namespace separators so we can safely embed an id in a ResourceLocation path. */
    private static String sanitizeId(String id) {
        if (id == null || id.isEmpty()) return "unknown";
        return id.toLowerCase(Locale.ROOT).replace(':', '_').replace('/', '_');
    }
}
