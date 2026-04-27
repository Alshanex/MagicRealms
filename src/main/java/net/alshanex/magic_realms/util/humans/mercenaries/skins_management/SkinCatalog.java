package net.alshanex.magic_realms.util.humans.mercenaries.skins_management;

import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.*;

/**
 * Immutable in-memory catalog of skin parts and presets.
 * Built by the reload listener on the server, synced to clients.
 */
public final class SkinCatalog {

    public static final SkinCatalog EMPTY = new SkinCatalog(List.of(), List.of());

    private final List<SkinPart> allParts;
    private final List<SkinPreset> allPresets;
    private final Map<ResourceLocation, SkinPreset> presetsById;

    public SkinCatalog(List<SkinPart> parts, List<SkinPreset> presets) {
        this.allParts = List.copyOf(parts);
        this.allPresets = List.copyOf(presets);
        Map<ResourceLocation, SkinPreset> m = new HashMap<>(presets.size());
        for (SkinPreset p : presets) {
            if (p.id() != null) m.put(p.id(), p);
        }
        this.presetsById = Collections.unmodifiableMap(m);
    }

    public List<SkinPart> allParts() { return allParts; }
    public List<SkinPreset> allPresets() { return allPresets; }

    /** Direct lookup by preset id (the data file's resource location, e.g. {@code "magic_realms:vex"}). */
    public SkinPreset presetById(ResourceLocation id) {
        return id == null ? null : presetsById.get(id);
    }

    /** Parts matching category + gender + class, honoring ANY filters. */
    public List<SkinPart> partsFor(SkinCategory category, Gender gender, EntityClass entityClass) {
        List<SkinPart> out = new ArrayList<>();
        for (SkinPart p : allParts) {
            if (p.category() != category) continue;
            if (!p.gender().matches(gender)) continue;
            if (!p.entityClass().matches(entityClass)) continue;
            out.add(p);
        }
        return out;
    }

    public List<SkinPreset> presetsFor(Gender gender) {
        List<SkinPreset> out = new ArrayList<>();
        for (SkinPreset p : allPresets) {
            if (!p.addedToPool()) continue;
            if (p.gender().matches(gender)) out.add(p);
        }
        return out;
    }

    public boolean hasPresets(Gender gender) {
        return !presetsFor(gender).isEmpty();
    }

    // ---- weighted pickers ----

    public SkinPart pickPart(SkinCategory category, Gender gender, EntityClass entityClass, RandomSource rng) {
        List<SkinPart> pool = partsFor(category, gender, entityClass);
        return pickWeighted(pool, SkinPart::weight, rng);
    }

    public SkinPreset pickPreset(Gender gender, RandomSource rng) {
        return pickWeighted(presetsFor(gender), SkinPreset::weight, rng);
    }

    private static <T> T pickWeighted(List<T> pool, java.util.function.ToIntFunction<T> weightFn, RandomSource rng) {
        if (pool.isEmpty()) return null;
        int total = 0;
        for (T t : pool) total += Math.max(1, weightFn.applyAsInt(t));
        int roll = rng.nextInt(total);
        int cursor = 0;
        for (T t : pool) {
            cursor += Math.max(1, weightFn.applyAsInt(t));
            if (roll < cursor) return t;
        }
        return pool.get(pool.size() - 1); // safety
    }
}
