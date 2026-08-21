package net.alshanex.magic_realms.util.humans.titles;

import net.minecraft.resources.ResourceLocation;

/**
 * Canonical counter keys used by {@link net.alshanex.magic_realms.data.TitleProgressData}.
 *
 * <p>Every "feat" a mercenary can accumulate is stored as a plain {@code String -> long} counter. Title requirements
 * either reference one of these keys through a dedicated requirement type (nicer JSON) or reference an arbitrary key
 * directly through the {@code counter} requirement type. That means a datapack can create a title gated on any counter
 * the mod (or an addon) ever writes, without a single line of new Java.
 */
public final class TitleKeys {

    private TitleKeys() {}

    /** Every hostile kill that counted, regardless of type. */
    public static final String KILLS_TOTAL = "kills.total";

    /** Kills of entities in the mod's boss tag. */
    public static final String KILLS_BOSS = "kills.boss";

    /** Cumulative minutes spent under an active contract. */
    public static final String CONTRACT_MINUTES = "contract.minutes";

    /** Cumulative damage dealt, rounded down to whole hearts-of-damage (i.e. raw damage points). */
    public static final String DAMAGE_DEALT = "damage.dealt";

    /** Cumulative damage taken. */
    public static final String DAMAGE_TAKEN = "damage.taken";

    /** Number of distinct times the mercenary has been brought to near-death and survived. */
    public static final String CLOSE_CALLS = "close_calls";

    /** Prefix for per-entity-type kill counters: {@code kills.entity.minecraft:zombie}. */
    public static final String KILL_ENTITY_PREFIX = "kills.entity.";

    /** Prefix for per-structure visit counters: {@code structures.minecraft:fortress}. */
    public static final String STRUCTURE_PREFIX = "structures.";

    public static String killEntity(ResourceLocation entityTypeId) {
        return KILL_ENTITY_PREFIX + entityTypeId;
    }

    public static String killEntity(String entityTypeId) {
        return KILL_ENTITY_PREFIX + entityTypeId;
    }

    public static String structure(ResourceLocation structureId) {
        return STRUCTURE_PREFIX + structureId;
    }

    public static String structure(String structureId) {
        return STRUCTURE_PREFIX + structureId;
    }
}
