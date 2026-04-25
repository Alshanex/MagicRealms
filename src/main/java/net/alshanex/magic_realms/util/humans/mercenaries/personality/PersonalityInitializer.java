package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.AssignedFixedPersonalitiesData;
import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Entry point for stamping personality onto a mercenary.
 *
 * For exclusive mercenaries with a fixed archetype (Catas, Amadeus, etc.) the entity's {@link AbstractMercenaryEntity#getFixedPersonality} hook returns
 * non-null and we use that preset instead of rolling.
 */
public final class PersonalityInitializer {

    private PersonalityInitializer() {}

    /** Odds that a random-rolling mercenary gets a fixed personality from the catalog instead of a pure random roll. */
    public static final double FIXED_POOL_ROLL_CHANCE = 0.1;

    public static void initializeFor(AbstractMercenaryEntity entity, RandomSource random) {
        PersonalityData data = entity.getData(MRDataAttachments.PERSONALITY);
        if (data.isInitialized()) return;

        // Priority 1: Java override (exclusive mercenaries).
        FixedPersonality fixed = entity.getFixedPersonality();
        if (fixed != null) {
            applyRuntime(data, fixed);
            return;
        }

        // Priority 2: preset-locked fixed personality id from texture metadata.
        String presetLockedId = extractPresetLockedFixedPersonalityId(entity);
        if (presetLockedId != null && !presetLockedId.isEmpty()) {
            FixedPersonalityDef def = FixedPersonalityCatalogHolder.server().byId(presetLockedId);
            if (def != null) {
                applyFromCatalog(entity, data, def);
                return;
            } else {
                MagicRealms.LOGGER.warn(
                        "Mercenary {} has preset-locked fixed_personality_id '{}' but no such entry in catalog; falling through to random roll",
                        entity.getUUID(), presetLockedId);
            }
        }

        // Priority 3a: roll against the fixed pool.
        if (random.nextDouble() < FIXED_POOL_ROLL_CHANCE) {
            FixedPersonalityCatalog catalog = FixedPersonalityCatalogHolder.server();
            if (!catalog.isEmpty()) {
                Set<String> claimed = getClaimedIds(entity);
                FixedPersonalityDef pick = catalog.pickFromRandomPool(random, claimed);
                if (pick != null) {
                    applyFromCatalog(entity, data, pick);
                    return;
                }
                // pickFromRandomPool returned null - no eligible entries. Fall through.
            }
        }

        // Priority 3b: pure random roll.
        applyRandomRoll(entity, data, random);
    }

    // =============== Application helpers ===============

    /**
     * Apply a catalog-sourced fixed personality. Handles the "claim unique" bookkeeping and the optional name override.
     */
    private static void applyFromCatalog(AbstractMercenaryEntity entity, PersonalityData data, FixedPersonalityDef def) {
        applyRuntime(data, def.toRuntime());

        // Apply override name if present.
        def.overrideEntityName().ifPresent(name -> {
            if (!name.isEmpty()) {
                entity.setEntityName(name);
                // Refresh the visible nameplate with the current level.
                entity.updateCustomNameWithStars();
            }
        });

        // Claim the id if unique so no future mercenary rolls the same character.
        if (def.unique()) {
            claimId(entity, def.id());
        }
    }

    /** Apply a runtime FixedPersonality record (from Java override or catalog). */
    private static void applyRuntime(PersonalityData data, FixedPersonality fixed) {
        data.initialize(
                fixed.archetype(),
                fixed.hobbyId(),
                fixed.hometown(),
                fixed.quirks()
        );
    }

    private static void applyRandomRoll(AbstractMercenaryEntity entity, PersonalityData data, RandomSource random) {
        PersonalityArchetype archetype = PersonalityArchetype.roll(entity.getEntityClass(), random);
        String hobbyId = rollHobbyId(random);
        String hometown = HometownRoller.roll(random);
        Set<Quirk> quirks = Quirk.rollSet(random);

        data.initialize(archetype, hobbyId, hometown, quirks);
    }

    private static String rollHobbyId(RandomSource random) {
        HobbyCatalog catalog = HobbyCatalogHolder.server();
        if (catalog.isEmpty()) {
            MagicRealms.LOGGER.warn("Hobby catalog empty during personality init - mercenary will have no hobby");
            return null;
        }
        Hobby pick = catalog.pickRandom(random);
        return pick != null ? pick.id() : null;
    }

    // Preset-locked id extraction

    /**
     * Read the optional "presetFixedPersonalityId" string from the mercenary's texture metadata if it's a RandomHumanEntity whose
     * preset specified one. Returns null for non-RandomHumanEntity or entities without a preset or without a preset-locked personality.
     */
    @Nullable
    private static String extractPresetLockedFixedPersonalityId(AbstractMercenaryEntity entity) {
        if (!(entity instanceof net.alshanex.magic_realms.entity.random.RandomHumanEntity random)) return null;

        net.minecraft.nbt.CompoundTag metadata = random.getTextureMetadata();
        if (metadata == null || metadata.isEmpty()) return null;
        if (!metadata.getBoolean("usePreset")) return null;
        if (!metadata.contains("presetFixedPersonalityId")) return null;

        String id = metadata.getString("presetFixedPersonalityId");
        return id.isEmpty() ? null : id;
    }

    // Unique-id claim bookkeeping

    /**
     * Returns the set of unique fixed personality ids already claimed in this world's overworld-level attachment. Used to filter the random-pool roll.
     */
    private static Set<String> getClaimedIds(AbstractMercenaryEntity entity) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel sl)) return Set.of();
        MinecraftServer server = sl.getServer();
        if (server == null) return Set.of();
        ServerLevel overworld = server.overworld();
        AssignedFixedPersonalitiesData data = overworld.getData(MRDataAttachments.ASSIGNED_FIXED_PERSONALITIES);
        return data.snapshot();
    }

    /**
     * Record that `id` has been assigned to a mercenary in this world. Writes to the overworld's AssignedFixedPersonalitiesData attachment.
     */
    private static void claimId(AbstractMercenaryEntity entity, String id) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel sl)) return;
        MinecraftServer server = sl.getServer();
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        AssignedFixedPersonalitiesData data = overworld.getData(MRDataAttachments.ASSIGNED_FIXED_PERSONALITIES);
        data.claim(id);
    }

    // Preset-locked record

    /**
     * Preset for exclusive mercenaries (Java-side). Data-driven personalities go through FixedPersonalityDef.toRuntime().
     */
    public record FixedPersonality(
            PersonalityArchetype archetype,
            String hobbyId,
            String hometown,
            Set<Quirk> quirks
    ) {
        /**
         * Factory helper for Java exclusives that want to reference a data-driven fixed personality by id. Returns null if the catalog has no such entry
         * (caller falls back to their literal inline FixedPersonality).
         */
        @Nullable
        public static FixedPersonality fromCatalog(String fixedPersonalityId) {
            if (fixedPersonalityId == null || fixedPersonalityId.isEmpty()) return null;
            FixedPersonalityDef def = FixedPersonalityCatalogHolder.server().byId(fixedPersonalityId);
            return def != null ? def.toRuntime() : null;
        }

        public static FixedPersonality fromCatalogOrElse(String fixedPersonalityId, java.util.function.Supplier<FixedPersonality> fallback) {
            FixedPersonality fromData = fromCatalog(fixedPersonalityId);
            return fromData != null ? fromData : fallback.get();
        }
    }
}
