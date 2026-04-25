package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Small binary traits that stack on top of an archetype. A mercenary rolls 0-3 of these at spawn. They're intentionally low-stakes and flavor-driven - each
 * one drives a specific reactive hook (one mood tick condition, one dialogue pool, one gameplay nudge).
 */
public enum Quirk {
    AFRAID_OF_THE_DARK("afraid_of_the_dark"),
    HATES_RAIN("hates_rain"),
    CANT_SWIM("cant_swim"),
    CLAUSTROPHOBIC("claustrophobic"),
    NIGHT_OWL("night_owl"),
    EARLY_RISER("early_riser"),
    HEAT_INTOLERANT("heat_intolerant"),
    COLD_INTOLERANT("cold_intolerant"),
    HEIGHT_SCARED("height_scared"),
    ANIMAL_FRIEND("animal_friend"),
    BOOKWORM("bookworm"),
    GLUTTON("glutton");

    private final String id;

    Quirk(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static Quirk fromId(String id) {
        if (id == null) return null;
        for (Quirk q : values()) {
            if (q.id.equalsIgnoreCase(id)) return q;
        }
        return null;
    }

    /**
     * Roll 0-3 non-contradictory quirks. We filter conflicts (e.g. NIGHT_OWL and EARLY_RISER shouldn't coexist) so the results make sense.
     */
    public static Set<Quirk> rollSet(RandomSource random) {
        // Number of quirks: most entities get 1 or 2, some get 0 or 3.
        int count;
        int r = random.nextInt(100);
        if (r < 15) count = 0;
        else if (r < 55) count = 1;
        else if (r < 90) count = 2;
        else count = 3;

        Set<Quirk> result = EnumSet.noneOf(Quirk.class);
        List<Quirk> pool = new ArrayList<>(List.of(values()));

        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            Quirk pick = pool.remove(random.nextInt(pool.size()));
            if (isConflicting(pick, result)) {
                i--;
                continue;
            }
            result.add(pick);
        }
        return result;
    }

    private static boolean isConflicting(Quirk candidate, Set<Quirk> chosen) {
        if (candidate == NIGHT_OWL && chosen.contains(EARLY_RISER)) return true;
        if (candidate == EARLY_RISER && chosen.contains(NIGHT_OWL)) return true;
        if (candidate == HEAT_INTOLERANT && chosen.contains(COLD_INTOLERANT)) return true;
        if (candidate == COLD_INTOLERANT && chosen.contains(HEAT_INTOLERANT)) return true;
        return false;
    }
}
