package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.minecraft.util.RandomSource;

import java.util.*;

/**
 * Immutable in-memory catalog of fixed personalities loaded from datapacks.
 * Built by HobbyCatalogReloadListener's equivalent for fixed personalities on the server, synced to clients as a full snapshot on datapack sync.
 */
public final class FixedPersonalityCatalog {

    public static final FixedPersonalityCatalog EMPTY = new FixedPersonalityCatalog(List.of());

    private final List<FixedPersonalityDef> all;
    private final Map<String, FixedPersonalityDef> byId;

    public FixedPersonalityCatalog(List<FixedPersonalityDef> defs) {
        this.all = List.copyOf(defs);
        Map<String, FixedPersonalityDef> m = new HashMap<>(defs.size());
        for (FixedPersonalityDef d : defs) {
            if (d.id() == null || d.id().isEmpty()) continue;
            m.put(d.id(), d);
        }
        this.byId = Collections.unmodifiableMap(m);
    }

    public List<FixedPersonalityDef> all() { return all; }

    public boolean isEmpty() { return all.isEmpty(); }

    /** Direct lookup by id (e.g. "magic_realms:alice"). Null if not found. */
    public FixedPersonalityDef byId(String id) {
        return id == null ? null : byId.get(id);
    }

    /**
     * Weighted pick from the random pool, honoring the claimed-ids filter. Returns null if no eligible entry exists.
     */
    public FixedPersonalityDef pickFromRandomPool(RandomSource random, Set<String> claimedIds) {
        if (all.isEmpty()) return null;

        List<FixedPersonalityDef> eligible = new ArrayList<>();
        int total = 0;
        for (FixedPersonalityDef d : all) {
            if (!d.inRandomPool()) continue;
            if (d.unique() && claimedIds != null && claimedIds.contains(d.id())) continue;
            eligible.add(d);
            total += Math.max(1, d.weight());
        }
        if (eligible.isEmpty()) return null;

        int roll = random.nextInt(total);
        int acc = 0;
        for (FixedPersonalityDef d : eligible) {
            acc += Math.max(1, d.weight());
            if (roll < acc) return d;
        }
        return eligible.get(eligible.size() - 1); // safety
    }
}
