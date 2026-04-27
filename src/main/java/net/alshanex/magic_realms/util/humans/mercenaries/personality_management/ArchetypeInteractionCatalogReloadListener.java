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

public class ArchetypeInteractionCatalogReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final String FOLDER = "mercenaries/personality/archetype_interactions";

    public ArchetypeInteractionCatalogReloadListener() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<ArchetypeInteraction> loaded = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            ResourceLocation key = entry.getKey();
            ArchetypeInteraction.BODY_CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> MagicRealms.LOGGER.error(
                            "Failed to parse archetype interaction {}: {}", key, err))
                    .ifPresent(def -> loaded.add(def.withId(key.toString())));
        }

        ArchetypeInteractionCatalog catalog = new ArchetypeInteractionCatalog(loaded);
        ArchetypeInteractionCatalogHolder.setServer(catalog);

        if (catalog.isEmpty()) {
            MagicRealms.LOGGER.debug("Archetype interaction catalog loaded empty - no archetype_interactions JSONs found");
        } else {
            MagicRealms.LOGGER.debug("Archetype interaction catalog rebuilt: {} entries", catalog.size());
        }
    }
}
