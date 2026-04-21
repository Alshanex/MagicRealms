package net.alshanex.magic_realms.util.humans.mercenaries.personality;

/**
 * Events the mercenary "remembers" having happened with a specific player.
 * Stored in a bounded FIFO list per player (~10 entries) so the memory footprint stays flat even over long playthroughs.
 *
 * Used for:
 *   - Occasional callback barks ("Remember when you almost died in the swamp?")
 *   - Unlocking rare dialogue options
 *   - Affinity thresholds that require a specific event to have occurred
 */
public enum MemoryEvent {
    SAVED_FROM_DEATH,        // player healed them while they were < 20% HP
    NEAR_DEATH_SURVIVED,     // they were saved by Hell's Pass while with this player
    KILLED_FEARED_FOE,       // player killed an entity of their feared type/tag
    BOSS_DEFEATED_TOGETHER,
    GIFTED_FAVORITE_FOOD,
    GIFTED_DISLIKED_FOOD,
    CONTRACT_BROKE_BADLY,    // player killed them, abandoned them in a dangerous place
    FRIENDLY_FIRE,
    LEVELED_UP_UNDER_CONTRACT,
    TRAVELED_TOGETHER_FAR,   // player has traveled a long cumulative distance with them
    BIRTHDAY_CELEBRATED;

    public static MemoryEvent fromName(String name) {
        if (name == null) return null;
        try {
            return MemoryEvent.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
