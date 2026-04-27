package net.alshanex.magic_realms.util.humans.mercenaries.personality_management;

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

public class HobbyCatalogReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final String FOLDER = "mercenaries/personality/hobbies";

    public HobbyCatalogReloadListener() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Hobby> loaded = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            Hobby.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> MagicRealms.LOGGER.error(
                            "Failed to parse hobby {}: {}", entry.getKey(), err))
                    .ifPresent(loaded::add);
        }

        HobbyCatalog catalog = new HobbyCatalog(loaded);
        HobbyCatalogHolder.setServer(catalog);

        if (catalog.isEmpty()) {
            MagicRealms.LOGGER.warn("Hobby catalog loaded empty - no hobbies JSONs found");
        } else {
            MagicRealms.LOGGER.debug("Hobby catalog rebuilt: {} hobbies", catalog.size());
        }
    }
}
