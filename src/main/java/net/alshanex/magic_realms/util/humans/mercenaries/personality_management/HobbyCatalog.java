package net.alshanex.magic_realms.util.humans.mercenaries.personality_management;

import net.minecraft.util.RandomSource;

import java.util.*;

public final class HobbyCatalog {

    public static final HobbyCatalog EMPTY = new HobbyCatalog(List.of());

    private final List<Hobby> allHobbies;
    /** Subset of {@link #allHobbies} eligible for the random roll, computed once at construction. */
    private final List<Hobby> rollablePool;
    private final Map<String, Hobby> byId;

    public HobbyCatalog(List<Hobby> hobbies) {
        this.allHobbies = List.copyOf(hobbies);

        List<Hobby> rollable = new ArrayList<>();
        for (Hobby h : hobbies) {
            if (h.inRandomPool()) rollable.add(h);
        }
        this.rollablePool = List.copyOf(rollable);

        Map<String, Hobby> map = new HashMap<>();
        for (Hobby h : hobbies) {
            map.put(h.id(), h);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public List<Hobby> all() {
        return allHobbies;
    }

    public Hobby byId(String id) {
        if (id == null) return null;
        return byId.get(id);
    }

    public boolean isEmpty() {
        return allHobbies.isEmpty();
    }

    public int size() {
        return allHobbies.size();
    }

    /**
     * Pick a random hobby from the rollable pool only (i.e. hobbies whose JSON sets {@code in_random_pool: true} or omits the field, which defaults to true).
     * Returns null if no rollable hobbies exist - callers should treat this as "no hobby".
     */
    public Hobby pickRandom(RandomSource random) {
        if (rollablePool.isEmpty()) return null;
        return rollablePool.get(random.nextInt(rollablePool.size()));
    }
}
