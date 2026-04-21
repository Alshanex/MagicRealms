package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.FoodTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FoodTagDiscovery {

    private FoodTagDiscovery() {}

    private static volatile List<TagKey<Item>> favorites = List.of();
    private static volatile List<TagKey<Item>> dislikes = List.of();

    /** Immutable snapshot of all discovered favorite-food tags. */
    public static List<TagKey<Item>> favorites() {
        return favorites;
    }

    /** Immutable snapshot of all discovered disliked-food tags. */
    public static List<TagKey<Item>> dislikes() {
        return dislikes;
    }

    /**
     * Rescan the item registry. Call from ServerStartedEvent and from any datapack reload hook.
     */
    public static void rebuild() {
        List<TagKey<Item>> favs = new ArrayList<>();
        List<TagKey<Item>> dls = new ArrayList<>();

        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            TagKey<Item> tag = pair.getFirst();
            String path = tag.location().getPath();

            if (pair.getSecond().size() == 0) return;

            if (path.startsWith(FoodTags.FAVORITE_PREFIX)) {
                favs.add(tag);
            } else if (path.startsWith(FoodTags.DISLIKED_PREFIX)) {
                dls.add(tag);
            }
        });

        favorites = Collections.unmodifiableList(favs);
        dislikes = Collections.unmodifiableList(dls);

        MagicRealms.LOGGER.debug(
                "FoodTagDiscovery: {} favorite tags, {} disliked tags",
                favs.size(), dls.size());

        if (favs.isEmpty()) {
            MagicRealms.LOGGER.warn(
                    "No favorite-food tags discovered. Check that " +
                            "data/.../tags/item/favorite_foods/*.json files exist.");
        }
    }
}
