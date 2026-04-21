package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HobbyCatalog {

    public static final HobbyCatalog EMPTY = new HobbyCatalog(List.of());

    private final List<Hobby> allHobbies;
    private final Map<String, Hobby> byId;

    public HobbyCatalog(List<Hobby> hobbies) {
        this.allHobbies = List.copyOf(hobbies);
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
     * Pick a random hobby.
     */
    public Hobby pickRandom(RandomSource random) {
        if (allHobbies.isEmpty()) return null;
        return allHobbies.get(random.nextInt(allHobbies.size()));
    }
}
