package net.alshanex.magic_realms.util.humans.mercenaries.personality;

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

public class ArchetypeCatalogReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final String FOLDER = "personality_archetypes";

    public ArchetypeCatalogReloadListener() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Archetype> loaded = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            Archetype.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> MagicRealms.LOGGER.error(
                            "Failed to parse archetype {}: {}", entry.getKey(), err))
                    .ifPresent(loaded::add);
        }

        ArchetypeCatalog catalog = new ArchetypeCatalog(loaded);
        ArchetypeCatalogHolder.setServer(catalog);

        if (catalog.isEmpty()) {
            MagicRealms.LOGGER.warn("Archetype catalog loaded empty - no personality_archetypes JSONs found");
        } else {
            MagicRealms.LOGGER.debug("Archetype catalog rebuilt: {} archetypes", catalog.size());
        }
    }
}
