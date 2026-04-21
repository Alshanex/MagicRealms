package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.FoodTags;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rolls favorite and disliked foods.
 *
 * Class bias:
 *   - Mages lean toward sweet / mystical foods (cake, sweet_berries, chorus)
 *   - Warriors lean toward hearty / meat foods (steak, porkchop, rabbit_stew)
 *   - Rogues lean toward luxury / exotic foods (golden_apple, glow_berries, fish)
 *
 * Every mercenary gets ONE favorite and ONE disliked food, and we guarantee they're different items.
 */
public final class FoodPreferenceRoller {

    private FoodPreferenceRoller() {}

    private static final int BASE_WEIGHT = 10;
    private static final int BIAS_BUMP = 8;
    private static final int BIAS_MINOR = 4;

    /**
     * Short-name bonuses by class. A class with an entry here gives extra weight to any discovered favorite tag whose short name matches.
     * Names not in the bias table roll at BASE_WEIGHT regardless of class.
     */
    private static final Map<EntityClass, Map<String, Integer>> FAVORITE_CLASS_BIAS = Map.of(
            EntityClass.MAGE, Map.of(
                    "sweet", BIAS_BUMP,
                    "mystical", BIAS_BUMP,
                    "fruit", BIAS_MINOR
            ),
            EntityClass.WARRIOR, Map.of(
                    "hearty", BIAS_BUMP,
                    "meat", BIAS_BUMP,
                    "stew", BIAS_MINOR
            ),
            EntityClass.ROGUE, Map.of(
                    "luxury", BIAS_BUMP,
                    "seafood", BIAS_MINOR,
                    "bread", BIAS_MINOR
            )
    );

    /**
     * Roll a favorite food tag. Returns the tag's id as a string (the form we store on the entity). Returns null if the discovery cache is
     * empty (no tags loaded - likely a missing datapack).
     */
    public static String rollFavoriteTagId(EntityClass entityClass, RandomSource random) {
        List<TagKey<Item>> pool = FoodTagDiscovery.favorites();
        if (pool.isEmpty()) {
            MagicRealms.LOGGER.warn("No favorite-food tags discovered; personality rolled without favorite food");
            return null;
        }

        Map<String, Integer> bias = entityClass != null
                ? FAVORITE_CLASS_BIAS.getOrDefault(entityClass, Map.of())
                : Map.of();

        int[] weights = new int[pool.size()];
        int total = 0;
        for (int i = 0; i < pool.size(); i++) {
            String shortName = FoodTags.extractShortName(pool.get(i));
            int bonus = shortName != null ? bias.getOrDefault(shortName, 0) : 0;
            weights[i] = BASE_WEIGHT + bonus;
            total += weights[i];
        }

        int roll = random.nextInt(total);
        int acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (roll < acc) return pool.get(i).location().toString();
        }
        return pool.get(0).location().toString(); // unreachable
    }

    /**
     * Roll a disliked food tag, guaranteed different from the favorite. Uniform roll - no class bias on dislikes.
     */
    public static String rollDislikedTagId(String favoriteTagId, RandomSource random) {
        List<TagKey<Item>> pool = FoodTagDiscovery.dislikes();
        if (pool.isEmpty()) {
            MagicRealms.LOGGER.warn("No disliked-food tags discovered; personality rolled without disliked food");
            return null;
        }

        List<TagKey<Item>> filtered = new ArrayList<>(pool);
        if (favoriteTagId != null) {
            filtered.removeIf(t -> t.location().toString().equals(favoriteTagId));
        }
        if (filtered.isEmpty()) filtered = pool;

        return filtered.get(random.nextInt(filtered.size())).location().toString();
    }
}
