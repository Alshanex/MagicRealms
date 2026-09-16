package net.alshanex.magic_realms.util.humans.bandits;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.HostileRandomHumanEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.alshanex.magic_realms.util.humans.titles.TitleCatalogHolder;
import net.alshanex.magic_realms.util.humans.titles.TitleManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Centralized helpers that apply a {@link BanditProfile} to a {@link HostileRandomHumanEntity}.
 */
public final class BanditProfileApplier {

    private static final String BOOST_ID_PREFIX = "bandit_profile";

    private BanditProfileApplier() {}

    public static BanditProfile resolve(String profileId, boolean isClientSide) {
        if (profileId == null || profileId.isEmpty()) return null;
        return BanditProfileCatalogHolder.get(isClientSide).byId(profileId);
    }

    // Per-stage appliers — call in this order during finalizeSpawn

    /**
     * Apply the visual/identity portion: gender, name, entity class. Should run during {@code initializeAppearance},
     * but in practice the new flow uses the {@code chooseGender}/{@code chooseEntityClass}/{@code chooseName} hooks
     * in {@link HostileRandomHumanEntity} to seed the values directly so the random texture roll uses the right
     * gender. This method remains for any post-hoc adjustments and for compatibility.
     */
    public static void applyAppearance(HostileRandomHumanEntity entity, BanditProfile profile) {
        if (profile == null) return;

        profile.gender().ifPresent(entity::setGender);
        profile.combatClass().ifPresent(entity::setCombatClass);
        profile.overrideName().ifPresent(name -> {
            if (!name.isEmpty()) entity.setEntityName(name);
        });
    }

    public static void applyClassSpecifics(HostileRandomHumanEntity entity, BanditProfile profile, RandomSource random) {
        if (profile == null) return;

        profile.hasShield().ifPresent(entity::setHasShield);
        profile.isArcher().ifPresent(entity::setIsArcher);

        List<SchoolType> schools = resolveMagicSchools(profile);
        if (!schools.isEmpty()) {
            entity.setMagicSchools(schools);
        }
    }

    public static void applyEquipment(HostileRandomHumanEntity entity, BanditProfile profile) {
        if (profile == null) return;

        Map<EquipmentSlot, ItemStack> equipment = profile.resolveEquipment();
        for (Map.Entry<EquipmentSlot, ItemStack> e : equipment.entrySet()) {
            entity.setItemSlot(e.getKey(), e.getValue().copy());
        }
    }

    public static List<AbstractSpell> resolveSpells(HostileRandomHumanEntity entity, BanditProfile profile,
                                                    RandomSource random) {
        if (profile == null) return null;

        if (!profile.explicitSpells().isEmpty()) {
            List<AbstractSpell> spells = new ArrayList<>();
            for (ResourceLocation id : profile.explicitSpells()) {
                AbstractSpell s = SpellRegistry.getSpell(id);
                if (s != null) spells.add(s);
                else MagicRealms.LOGGER.warn("Bandit profile {} references unknown spell {}", profile.id(), id);
            }
            return spells;
        }

        if (profile.spellsTag().isPresent()) {
            TagKey<AbstractSpell> tag = profile.spellsTag().get();
            List<AbstractSpell> tagSpells = SpellListGenerator.getSpellsFromTag(tag);
            if (tagSpells.isEmpty()) return new ArrayList<>();

            int pickCount = profile.spellsTagPickCount().orElse(tagSpells.size());
            if (pickCount >= tagSpells.size()) {
                return new ArrayList<>(tagSpells);
            }
            List<AbstractSpell> shuffled = new ArrayList<>(tagSpells);
            Collections.shuffle(shuffled, new Random(random.nextLong()));
            return new ArrayList<>(shuffled.subList(0, pickCount));
        }

        return null;
    }

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

        for (int i = 0; i < profile.attributeBoosts().size(); i++) {
            BanditProfile.AttributeBoost boost = profile.attributeBoosts().get(i);
            applyBoost(entity, profile, i, boost);
        }

        // Titles come after the attribute boosts so their own modifiers layer on top. The actual attribute
        // application is handled by TitleEffectTickHandler on the next tick, not here.
        applyTitles(entity, profile);

        if (profile.immortal()) {
            entity.setImmortal(true);
        }

        entity.heal(entity.getMaxHealth());
    }

    /**
     * Grant the profile's titles outright, bypassing their normal requirements, and pin the displayed one.
     *
     * <p>Uses {@link TitleManager#grantSilently} rather than the usual grant path: a bandit spawning shouldn't
     * fire the level-up chime and particle burst that a mercenary earning a title through deeds does.
     *
     * <p>Unknown ids are dropped with a warning rather than granted, so a typo in a datapack doesn't leave the
     * entity holding a title id nothing can resolve.
     */
    public static void applyTitles(HostileRandomHumanEntity entity, BanditProfile profile) {
        if (profile == null || !profile.hasTitles()) return;
        if (entity.level().isClientSide()) return;

        var catalog = TitleCatalogHolder.server();
        List<ResourceLocation> valid = new ArrayList<>(profile.titles().size());

        for (ResourceLocation titleId : profile.titles()) {
            if (catalog.contains(titleId)) {
                valid.add(titleId);
            } else {
                MagicRealms.LOGGER.warn("Bandit profile {} references unknown title {}", profile.id(), titleId);
            }
        }

        if (valid.isEmpty()) return;

        TitleManager.grantSilently(entity, valid);

        // Only pin a displayed title that was actually granted; setDisplayedTitle rejects unearned ids anyway,
        // but warning here makes the datapack mistake visible instead of silently falling back.
        profile.displayedTitle().ifPresent(displayId -> {
            if (valid.contains(displayId)) {
                TitleManager.setDisplayedTitle(entity, displayId);
            } else {
                MagicRealms.LOGGER.warn("Bandit profile {} sets displayed_title {} which is not in its titles list",
                        profile.id(), displayId);
            }
        });
    }

    // Helpers

    private static List<SchoolType> resolveMagicSchools(BanditProfile profile) {
        if (!profile.magicSchools().isEmpty()) {
            List<SchoolType> result = new ArrayList<>();
            for (ResourceLocation id : profile.magicSchools()) {
                SchoolType school = SchoolRegistry.REGISTRY.get(id);
                if (school != null) result.add(school);
                else MagicRealms.LOGGER.warn("Bandit profile {} references unknown school {}", profile.id(), id);
            }
            return result;
        }

        if (profile.magicSchoolsTag().isPresent()) {
            TagKey<SchoolType> tag = profile.magicSchoolsTag().get();
            return SchoolRegistry.REGISTRY.stream()
                    .filter(s -> ModTags.isSchoolInTag(s, tag))
                    .toList();
        }

        return List.of();
    }

    /**
     * Apply one attribute boost.
     */
    private static void applyBoost(HostileRandomHumanEntity entity, BanditProfile profile, int index,
                                   BanditProfile.AttributeBoost boost) {
        Holder<Attribute> attrHolder = BuiltInRegistries.ATTRIBUTE.getHolder(boost.attribute()).orElse(null);
        if (attrHolder == null) {
            MagicRealms.LOGGER.warn("Bandit profile {} references unknown attribute {} (boost #{} skipped)",
                    profile.id(), boost.attribute(), index);
            return;
        }

        AttributeInstance instance = entity.getAttribute(attrHolder);
        if (instance == null) {
            MagicRealms.LOGGER.debug("Bandit profile {} boost #{} targets attribute {} not present on entity",
                    profile.id(), index, boost.attribute());
            return;
        }

        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID,
                BOOST_ID_PREFIX + "/" + sanitizeId(profile.id()) + "/" + index);

        AttributeModifier existing = instance.getModifier(modifierId);
        if (existing != null) instance.removeModifier(existing);

        instance.addPermanentModifier(new AttributeModifier(
                modifierId, boost.modifier().amount(), boost.modifier().operation()));
    }

    private static String sanitizeId(String id) {
        if (id == null || id.isEmpty()) return "unknown";
        return id.toLowerCase(Locale.ROOT).replace(':', '_').replace('/', '_');
    }
}