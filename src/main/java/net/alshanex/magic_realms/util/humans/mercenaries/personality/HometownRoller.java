package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rolls a hometown name from a fantasy-flavored list. Used for backstory ("I was born in Westhollow"), letter-from-home items, dialogue flavor, and
 * title suffixes ("of Westhollow"). Purely cosmetic.
 */
public final class HometownRoller {

    private HometownRoller() {}

    // Emergency fallback if the config failed to load for some reason.
    private static final List<String> FALLBACK = Arrays.asList(
            "Westhollow", "Ironwatch", "Greyhaven", "Silverbrook", "Stormkeep"
    );

    public static String roll(RandomSource random) {
        List<String> pool = getPool();
        if (pool.isEmpty()) {
            MagicRealms.LOGGER.warn("Hometown pool empty - falling back to built-in list");
            pool = FALLBACK;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private static List<String> getPool() {
        try {
            if (Config.hometowns != null && !Config.hometowns.isEmpty()) {
                return new ArrayList<>(Config.hometowns);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error accessing Config.hometowns", e);
        }
        return FALLBACK;
    }
}
