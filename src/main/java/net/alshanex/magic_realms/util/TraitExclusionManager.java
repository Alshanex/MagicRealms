package net.alshanex.magic_realms.util;

import dev.xkmc.l2hostility.init.entries.TraitEntry;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.registry.TraitRegistry;

import java.util.*;

public class TraitExclusionManager {
    private static final Map<TraitEntry<?>, Set<TraitEntry<?>>> EXCLUSIONS = new HashMap<>();

    static {
        initializeExclusions();
    }

    private static void initializeExclusions() {
        addMutualExclusion(TraitRegistry.BLOOD_SCHOOL_IMMUNITY, TraitRegistry.VAMPIRIC);

        addMutualExclusion(TraitRegistry.BLOOD_SCHOOL_IMMUNITY, TraitRegistry.ANTI_MAGIC);

        addMutualExclusion(TraitRegistry.VAMPIRIC, TraitRegistry.ANTI_MAGIC);

        addMutualExclusion(LHTraits.REFLECT, LHTraits.DISPELL);

        addMutualExclusion(LHTraits.DEMENTOR, LHTraits.DISPELL);

        addMutualExclusion(LHTraits.ENDER, LHTraits.DISPELL);

        addMutualExclusion(TraitRegistry.ELDRITCH_SCHOOL_IMMUNITY, TraitRegistry.DEPHT_RULER);

        MagicRealms.LOGGER.info("Initialized trait exclusions: {} exclusion rules", EXCLUSIONS.size());
    }

    private static void addMutualExclusion(TraitEntry<?> trait1, TraitEntry<?> trait2) {
        EXCLUSIONS.computeIfAbsent(trait1, k -> new HashSet<>()).add(trait2);
        EXCLUSIONS.computeIfAbsent(trait2, k -> new HashSet<>()).add(trait1);
    }

    public static boolean isCompatible(TraitEntry<?> newTrait, List<TraitEntry<?>> existingTraits) {
        Set<TraitEntry<?>> exclusions = EXCLUSIONS.get(newTrait);
        if (exclusions == null || exclusions.isEmpty()) {
            return true;
        }

        for (TraitEntry<?> existingTrait : existingTraits) {
            if (exclusions.contains(existingTrait)) {
                MagicRealms.LOGGER.debug("Trait {} is incompatible with existing trait {}",
                        newTrait.getId(), existingTrait.getId());
                return false;
            }
        }

        return true;
    }

    public static List<TraitEntry<?>> filterCompatibleTraits(List<TraitEntry<?>> candidates,
                                                             List<TraitEntry<?>> existingTraits) {
        return candidates.stream()
                .filter(candidate -> isCompatible(candidate, existingTraits))
                .toList();
    }

    public static List<TraitEntry<?>> resolveConflicts(List<TraitEntry<?>> traits) {
        List<TraitEntry<?>> resolved = new ArrayList<>();

        for (TraitEntry<?> trait : traits) {
            if (isCompatible(trait, resolved)) {
                resolved.add(trait);
            } else {
                MagicRealms.LOGGER.warn("Removing conflicting trait {} during conflict resolution",
                        trait.getId());
            }
        }

        return resolved;
    }

    public static Set<TraitEntry<?>> getExclusions(TraitEntry<?> trait) {
        return EXCLUSIONS.getOrDefault(trait, new HashSet<>());
    }

    public static boolean areExclusive(TraitEntry<?> trait1, TraitEntry<?> trait2) {
        Set<TraitEntry<?>> exclusions1 = EXCLUSIONS.get(trait1);
        return exclusions1 != null && exclusions1.contains(trait2);
    }
}
