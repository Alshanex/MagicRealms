package net.alshanex.magic_realms.util.humans.combat;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatClasses {

    private static final Map<ResourceLocation, CombatClass> BY_ID = new LinkedHashMap<>();
    public static final ResourceLocation FALLBACK = MRCombatClasses.WARRIOR;

    private CombatClasses() {}

    public static void register(CombatClass cls) {
        if (BY_ID.put(cls.id(), cls) != null) {
            MagicRealms.LOGGER.warn("Combat class {} registered twice", cls.id());
        }
    }

    @Nullable
    public static CombatClass get(@Nullable ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
    }

    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();

    public static CombatClass getOrFallback(@Nullable ResourceLocation id) {
        CombatClass c = get(id);
        if (c != null) return c;
        if (id != null && WARNED.add(id)) {
            MagicRealms.LOGGER.warn("Unknown combat class {}, using fallback (further warnings suppressed)", id);
        }
        return BY_ID.get(FALLBACK);
    }

    /**
     * Resolves a datapack string. Accepts bare names ("mage", "MAGE") and full ids ("magic_realms:support_mage").
     */
    @Nullable
    public static CombatClass resolve(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String s = raw.trim();
        ResourceLocation id = s.indexOf(':') >= 0
                ? ResourceLocation.tryParse(s.toLowerCase(Locale.ROOT))
                : ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, s.toLowerCase(Locale.ROOT));
        return get(id);
    }

    public static Collection<CombatClass> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static CombatClass pickRandom(RandomSource rng) {
        List<CombatClass> pool = new ArrayList<>();
        for (CombatClass c : BY_ID.values()) if (c.inRandomPool()) pool.add(c);
        if (pool.isEmpty()) return BY_ID.get(FALLBACK);

        int total = 0;
        for (CombatClass c : pool) total += Math.max(0, c.spawnWeight());
        if (total <= 0) return pool.get(rng.nextInt(pool.size()));

        int roll = rng.nextInt(total);
        for (CombatClass c : pool) {
            roll -= Math.max(0, c.spawnWeight());
            if (roll < 0) return c;
        }
        return pool.get(pool.size() - 1);
    }
}
