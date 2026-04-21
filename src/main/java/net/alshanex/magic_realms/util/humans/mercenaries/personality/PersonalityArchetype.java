package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.util.RandomSource;

/**
 * Broad personality archetypes. Each mercenary rolls one at spawn (or has a fixed one for exclusives).
 */
public enum PersonalityArchetype {
    STOIC("stoic"),
    CHEERFUL("cheerful"),
    GREEDY("greedy"),
    LOYAL("loyal"),
    ARROGANT("arrogant"),
    COWARDLY("cowardly"),
    SCHOLARLY("scholarly"),
    HOTHEADED("hotheaded"),
    SUPERSTITIOUS("superstitious"),
    PIOUS("pious"),
    GRIM("grim"),
    JOVIAL("jovial");

    private final String id;

    PersonalityArchetype(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Parse from serialized string form. Returns null if unknown.
     */
    public static PersonalityArchetype fromId(String id) {
        if (id == null) return null;
        for (PersonalityArchetype a : values()) {
            if (a.id.equalsIgnoreCase(id)) return a;
        }
        return null;
    }

    /**
     * Roll an archetype with a mild bias from entity class.
     */
    public static PersonalityArchetype roll(EntityClass entityClass, RandomSource random) {
        // Weighted table: start every archetype at weight 10, add class bonuses.
        int[] weights = new int[values().length];
        for (int i = 0; i < weights.length; i++) weights[i] = 10;

        if (entityClass != null) {
            switch (entityClass) {
                case MAGE -> {
                    weights[SCHOLARLY.ordinal()] += 8;
                    weights[SUPERSTITIOUS.ordinal()] += 5;
                    weights[ARROGANT.ordinal()] += 3;
                    weights[GRIM.ordinal()] += 2;
                }
                case WARRIOR -> {
                    weights[STOIC.ordinal()] += 7;
                    weights[HOTHEADED.ordinal()] += 6;
                    weights[LOYAL.ordinal()] += 4;
                    weights[JOVIAL.ordinal()] += 2;
                }
                case ROGUE -> {
                    weights[GREEDY.ordinal()] += 7;
                    weights[ARROGANT.ordinal()] += 5;
                    weights[COWARDLY.ordinal()] += 3;
                    weights[CHEERFUL.ordinal()] += 2;
                }
            }
        }

        int total = 0;
        for (int w : weights) total += w;
        int roll = random.nextInt(total);
        int acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (roll < acc) return values()[i];
        }
        return STOIC; // unreachable
    }

    /**
     * Some archetypes dislike each other when placed together. Used for inter-mercenary banter and for a small affinity decay tick when two rivals are in the same contracted group.
     */
    public boolean rivalsWith(PersonalityArchetype other) {
        if (other == null) return false;
        return switch (this) {
            case ARROGANT -> other == ARROGANT || other == HOTHEADED;
            case STOIC -> other == HOTHEADED || other == JOVIAL;
            case PIOUS -> other == GREEDY || other == SUPERSTITIOUS;
            case SCHOLARLY -> other == HOTHEADED;
            case GRIM -> other == CHEERFUL || other == JOVIAL;
            case COWARDLY -> other == ARROGANT;
            default -> false;
        };
    }

    /**
     * Archetypes that get along well. Triggers occasional friendly banter and a small affinity boost when grouped.
     */
    public boolean friendsWith(PersonalityArchetype other) {
        if (other == null) return false;
        return switch (this) {
            case CHEERFUL -> other == CHEERFUL || other == JOVIAL || other == LOYAL;
            case JOVIAL -> other == CHEERFUL || other == JOVIAL;
            case SCHOLARLY -> other == SCHOLARLY || other == PIOUS;
            case PIOUS -> other == PIOUS || other == LOYAL;
            case LOYAL -> other == LOYAL || other == STOIC;
            case STOIC -> other == STOIC || other == LOYAL;
            case GRIM -> other == GRIM;
            default -> false;
        };
    }
}
