package net.alshanex.magic_realms.util.humans.bandits;

import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.function.Predicate;

/**
 * Immutable in-memory catalog of bandit profiles loaded from datapacks.
 */
public final class BanditProfileCatalog {

    public static final BanditProfileCatalog EMPTY = new BanditProfileCatalog(List.of());

    private final List<BanditProfile> all;
    private final Map<String, BanditProfile> byId;

    public BanditProfileCatalog(List<BanditProfile> profiles) {
        this.all = List.copyOf(profiles);
        Map<String, BanditProfile> m = new HashMap<>();
        for (BanditProfile p : profiles) {
            if (p.id() == null || p.id().isEmpty()) continue;
            m.put(p.id(), p);
        }
        this.byId = Collections.unmodifiableMap(m);
    }

    public List<BanditProfile> all() { return all; }
    public boolean isEmpty() { return all.isEmpty(); }
    public int size() { return all.size(); }

    /** Direct lookup by full id (e.g. {@code "magic_realms:giant_warrior"}). Null if not found. */
    public BanditProfile byId(String id) {
        return id == null ? null : byId.get(id);
    }

    public boolean contains(String id) {
        return id != null && byId.containsKey(id);
    }

    /**
     * Weighted random pick from the random pool. Returns null only if no profile is in the pool.
     */
    public BanditProfile pickRandom(RandomSource random) {
        return pickRandomFiltered(random, p -> true);
    }

    /**
     * Weighted random pick from the random pool, with an arbitrary additional filter. Useful for callers
     * that want to constrain by class, level range, miniboss flag, or any other field on the profile.
     */
    public BanditProfile pickRandomFiltered(RandomSource random, Predicate<BanditProfile> filter) {
        if (all.isEmpty()) return null;

        int total = 0;
        for (BanditProfile p : all) {
            if (!p.inRandomPool()) continue;
            if (!filter.test(p)) continue;
            total += Math.max(1, p.weight());
        }
        if (total <= 0) return null;

        int roll = random.nextInt(total);
        int acc = 0;
        for (BanditProfile p : all) {
            if (!p.inRandomPool()) continue;
            if (!filter.test(p)) continue;
            acc += Math.max(1, p.weight());
            if (roll < acc) return p;
        }
        // Fallback safety
        for (BanditProfile p : all) {
            if (p.inRandomPool() && filter.test(p)) return p;
        }
        return null;
    }
}
