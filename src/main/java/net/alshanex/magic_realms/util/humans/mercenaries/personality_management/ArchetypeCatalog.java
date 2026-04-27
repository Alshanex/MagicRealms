package net.alshanex.magic_realms.util.humans.mercenaries.personality_management;

import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.util.RandomSource;

import java.util.*;

/**
 * Immutable in-memory catalog of personality archetypes loaded from datapacks.
 * Built by {@link ArchetypeCatalogReloadListener} on the server, synced to clients via {@code SyncArchetypeCatalogPacket}.
 */
public final class ArchetypeCatalog {

    public static final ArchetypeCatalog EMPTY = new ArchetypeCatalog(List.of());

    private final List<Archetype> all;
    private final Map<String, Archetype> byId;

    public ArchetypeCatalog(List<Archetype> archetypes) {
        this.all = List.copyOf(archetypes);
        Map<String, Archetype> m = new HashMap<>();
        for (Archetype a : archetypes) {
            if (a.id() == null || a.id().isEmpty()) continue;
            m.put(a.id(), a);
        }
        this.byId = Collections.unmodifiableMap(m);
    }

    public List<Archetype> all() { return all; }

    public boolean isEmpty() { return all.isEmpty(); }

    public int size() { return all.size(); }

    /** Direct lookup by short id (e.g. {@code "stoic"}). Null if not found. */
    public Archetype byId(String id) {
        return id == null ? null : byId.get(id);
    }

    /** Convenience: true if the catalog has an archetype with this id. */
    public boolean contains(String id) {
        return id != null && byId.containsKey(id);
    }

    /**
     * Weighted random pick biased by the given entity class. Mirrors the old enum-based {@code roll(...)} method:
     * each archetype contributes its effective weight (base + class bonus). Returns null only if the catalog is empty
     * or no archetype has positive weight.
     */
    public Archetype pickWeighted(EntityClass entityClass, RandomSource random) {
        if (all.isEmpty()) return null;

        int total = 0;
        for (Archetype a : all) {
            if (!a.inRandomPool()) continue;
            total += a.effectiveWeightFor(entityClass);
        }
        if (total <= 0) return null;

        int roll = random.nextInt(total);
        int acc = 0;
        for (Archetype a : all) {
            if (!a.inRandomPool()) continue;
            acc += a.effectiveWeightFor(entityClass);
            if (roll < acc) return a;
        }
        // Fallback: first rollable archetype.
        for (Archetype a : all) {
            if (a.inRandomPool()) return a;
        }
        return null;
    }
}
