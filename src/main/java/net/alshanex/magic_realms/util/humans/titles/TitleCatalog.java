package net.alshanex.magic_realms.util.humans.titles;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Immutable in-memory catalog of titles loaded from datapacks.
 * Built by {@link TitleCatalogReloadListener} on the server, synced to clients via {@code SyncTitleCatalogPacket}.
 */
public final class TitleCatalog {

    public static final TitleCatalog EMPTY = new TitleCatalog(List.of());

    private final List<Title> all;
    private final Map<ResourceLocation, Title> byId;

    public TitleCatalog(List<Title> titles) {
        // Sorted highest priority first so "pick the best title" is just "take the first match".
        List<Title> sorted = new ArrayList<>(titles);
        sorted.sort(Comparator.comparingInt(Title::priority).reversed()
                .thenComparing(t -> t.id().toString()));
        this.all = List.copyOf(sorted);

        Map<ResourceLocation, Title> m = new LinkedHashMap<>(sorted.size());
        for (Title t : sorted) {
            if (t.id() != null) m.put(t.id(), t);
        }
        this.byId = Collections.unmodifiableMap(m);
    }

    public List<Title> all() { return all; }

    public boolean isEmpty() { return all.isEmpty(); }

    public int size() { return all.size(); }

    /** Direct lookup by title id (the data file's resource location, e.g. {@code "magic_realms:dragonslayer"}). */
    public Title byId(ResourceLocation id) {
        return id == null ? null : byId.get(id);
    }

    public boolean contains(ResourceLocation id) {
        return id != null && byId.containsKey(id);
    }

    /** All known ids, in priority order. */
    public Set<ResourceLocation> ids() {
        return byId.keySet();
    }

    /**
     * Resolves a set of earned ids into title objects, skipping ids the current datapack no longer defines.
     * Result is in catalog (priority) order, not insertion order.
     */
    public List<Title> resolve(Collection<ResourceLocation> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Title> out = new ArrayList<>(ids.size());
        for (Title t : all) {
            if (ids.contains(t.id())) out.add(t);
        }
        return out;
    }
}
