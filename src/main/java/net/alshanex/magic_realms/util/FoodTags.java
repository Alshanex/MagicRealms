package net.alshanex.magic_realms.util;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

public final class FoodTags {

    private FoodTags() {}

    public static final String FAVORITE_PREFIX = "favorite_foods/";
    public static final String DISLIKED_PREFIX = "disliked_foods/";

    /**
     * Short name of a food-preference tag (e.g. "sweet" from
     * "magic_realms:favorite_foods/sweet"). Used for class-bias lookups.
     * Returns null if the tag isn't a food-preference tag.
     */
    public static String extractShortName(TagKey<Item> tag) {
        if (tag == null) return null;
        return extractShortName(tag.location().getPath());
    }

    public static String extractShortName(String path) {
        if (path == null) return null;
        if (path.startsWith(FAVORITE_PREFIX)) return path.substring(FAVORITE_PREFIX.length());
        if (path.startsWith(DISLIKED_PREFIX)) return path.substring(DISLIKED_PREFIX.length());
        return null;
    }

    public static boolean isFavoriteFoodTag(TagKey<Item> tag) {
        return tag != null && tag.location().getPath().startsWith(FAVORITE_PREFIX);
    }

    public static boolean isDislikedFoodTag(TagKey<Item> tag) {
        return tag != null && tag.location().getPath().startsWith(DISLIKED_PREFIX);
    }

    /**
     * Rehydrate a tag key from a stored id string. Returns null on
     * malformed input. Does NOT validate that the tag actually exists -
     * callers should handle empty-tag cases gracefully.
     */
    public static TagKey<Item> parse(String tagIdString) {
        if (tagIdString == null || tagIdString.isEmpty()) return null;
        try {
            ResourceLocation id = ResourceLocation.parse(tagIdString);
            return TagKey.create(Registries.ITEM, id);
        } catch (Exception e) {
            return null;
        }
    }
}
