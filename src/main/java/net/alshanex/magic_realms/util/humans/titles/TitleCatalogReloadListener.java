package net.alshanex.magic_realms.util.humans.titles;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads every {@code data/<namespace>/mercenaries/titles/<name>.json} into the {@link TitleCatalog}.
 * The file's resource location becomes the title id, so {@code data/magic_realms/mercenaries/titles/dragonslayer.json} is referenced elsewhere as {@code magic_realms:dragonslayer}.
 */
public class TitleCatalogReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final String FOLDER = "mercenaries/titles";

    public TitleCatalogReloadListener() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Title> loaded = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            ResourceLocation key = entry.getKey();
            Title.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> MagicRealms.LOGGER.error(
                            "Failed to parse title {}: {}", key, err))
                    .map(parsed -> parsed.withId(key))
                    .ifPresent(loaded::add);
        }

        TitleCatalog catalog = new TitleCatalog(loaded);
        TitleCatalogHolder.setServer(catalog);

        if (catalog.isEmpty()) {
            MagicRealms.LOGGER.warn("Title catalog loaded empty - no title JSONs found");
        } else {
            MagicRealms.LOGGER.debug("Title catalog rebuilt: {} titles", catalog.size());
        }
    }
}
