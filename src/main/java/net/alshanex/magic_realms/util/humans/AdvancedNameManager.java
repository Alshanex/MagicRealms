package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;

public class AdvancedNameManager {
    private static final Random RANDOM = new Random();

    // Fallback names in case config fails to load
    private static final List<String> FALLBACK_MALE_NAMES = Arrays.asList(
            "Alshanex", "Mekka", "DragonSlayer"
    );

    private static final List<String> FALLBACK_FEMALE_NAMES = Arrays.asList(
            "Elle", "Moth", "M"
    );

    public static String getRandomName(Gender gender) {
        List<String> names = getNamesByGender(gender);

        if (names == null || names.isEmpty()) {
            MagicRealms.LOGGER.warn("No names available for gender: {}, using fallback names", gender.getName());
            names = getFallbackNames(gender);
        }

        if (names.isEmpty()) {
            MagicRealms.LOGGER.error("Even fallback names are empty for gender: {}", gender.getName());
            return "Unknown";
        }

        String selectedName = names.get(RANDOM.nextInt(names.size()));
        MagicRealms.LOGGER.debug("Selected name '{}' for gender '{}'", selectedName, gender.getName());
        return selectedName;
    }

    private static List<String> getNamesByGender(Gender gender) {
        try {
            switch (gender) {
                case MALE:
                    return Config.maleNames != null ? new ArrayList<>(Config.maleNames) : null;
                case FEMALE:
                    return Config.femaleNames != null ? new ArrayList<>(Config.femaleNames) : null;
                default:
                    MagicRealms.LOGGER.warn("Unknown gender: {}", gender.getName());
                    return null;
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error accessing config names for gender: {}", gender.getName(), e);
            return null;
        }
    }

    private static List<String> getFallbackNames(Gender gender) {
        switch (gender) {
            case MALE:
                return FALLBACK_MALE_NAMES;
            case FEMALE:
                return FALLBACK_FEMALE_NAMES;
            default:
                return FALLBACK_MALE_NAMES;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void reloadNames() {
        // This method can be called when config is reloaded
        // The actual names are now loaded from config, so we just log the reload
        MagicRealms.LOGGER.info("Name configuration reloaded from config file");

        // Log current counts for debugging
        int maleCount = Config.maleNames != null ? Config.maleNames.size() : 0;
        int femaleCount = Config.femaleNames != null ? Config.femaleNames.size() : 0;
        MagicRealms.LOGGER.debug("Reloaded names - Male: {}, Female: {}", maleCount, femaleCount);
    }

    // Utility method to get the count of available names for a gender
    public static int getNameCount(Gender gender) {
        List<String> names = getNamesByGender(gender);
        return names != null ? names.size() : 0;
    }

    // Utility method to check if custom names are available
    public static boolean hasCustomNames(Gender gender) {
        List<String> names = getNamesByGender(gender);
        List<String> fallback = getFallbackNames(gender);
        return names != null && !names.isEmpty() && !names.equals(fallback);
    }
}