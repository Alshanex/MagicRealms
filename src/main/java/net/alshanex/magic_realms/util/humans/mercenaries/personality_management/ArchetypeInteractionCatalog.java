package net.alshanex.magic_realms.util.humans.mercenaries.personality_management;

import java.util.*;

/**
 * Immutable in-memory catalog of archetype interactions loaded from datapacks.
 * Indexed by unordered archetype pair for fast tick-time lookup.
 */
public final class ArchetypeInteractionCatalog {

    public static final ArchetypeInteractionCatalog EMPTY = new ArchetypeInteractionCatalog(List.of());

    private final List<ArchetypeInteraction> all;
    private final Map<String, ArchetypeInteraction> byId;
    /** Pair key -> all interactions defined for that unordered pair. Multiple interactions per pair are allowed. */
    private final Map<String, List<ArchetypeInteraction>> byPair;

    public ArchetypeInteractionCatalog(List<ArchetypeInteraction> interactions) {
        this.all = List.copyOf(interactions);
        Map<String, ArchetypeInteraction> idMap = new HashMap<>();
        Map<String, List<ArchetypeInteraction>> pairMap = new HashMap<>();
        for (ArchetypeInteraction i : interactions) {
            if (i.id() == null || i.id().isEmpty()) continue;
            idMap.put(i.id(), i);
            String key = pairKey(i.archetypeA(), i.archetypeB());
            pairMap.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }
        // Freeze the lists.
        Map<String, List<ArchetypeInteraction>> frozen = new HashMap<>(pairMap.size());
        for (Map.Entry<String, List<ArchetypeInteraction>> e : pairMap.entrySet()) {
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.byId = Collections.unmodifiableMap(idMap);
        this.byPair = Collections.unmodifiableMap(frozen);
    }

    public List<ArchetypeInteraction> all() { return all; }

    public boolean isEmpty() { return all.isEmpty(); }

    public int size() { return all.size(); }

    public ArchetypeInteraction byId(String id) {
        return id == null ? null : byId.get(id);
    }

    /**
     * All interactions registered for the given unordered pair of archetype ids. Empty list if none.
     */
    public List<ArchetypeInteraction> forPair(String archetypeIdA, String archetypeIdB) {
        if (archetypeIdA == null || archetypeIdB == null) return List.of();
        List<ArchetypeInteraction> list = byPair.get(pairKey(archetypeIdA, archetypeIdB));
        return list != null ? list : List.of();
    }

    /** Order-insensitive key. Sort lexicographically so {@code (a,b)} and {@code (b,a)} map to the same string. */
    private static String pairKey(String a, String b) {
        String la = a.toLowerCase(Locale.ROOT);
        String lb = b.toLowerCase(Locale.ROOT);
        return la.compareTo(lb) <= 0 ? la + "|" + lb : lb + "|" + la;
    }
}
