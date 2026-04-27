package net.alshanex.magic_realms.util.humans.bandits;

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

public class BanditProfileCatalogReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final String FOLDER = "mercenaries/bandit_profiles";

    public BanditProfileCatalogReloadListener() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<BanditProfile> loaded = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            ResourceLocation key = entry.getKey();
            BanditProfile.BODY_CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> MagicRealms.LOGGER.error(
                            "Failed to parse bandit profile {}: {}", key, err))
                    .ifPresent(def -> loaded.add(def.withId(key.toString())));
        }

        BanditProfileCatalog catalog = new BanditProfileCatalog(loaded);
        BanditProfileCatalogHolder.setServer(catalog);

        if (catalog.isEmpty()) {
            MagicRealms.LOGGER.debug("Bandit profile catalog loaded empty - no bandit_profiles JSONs found");
        } else {
            MagicRealms.LOGGER.debug("Bandit profile catalog rebuilt: {} profiles", catalog.size());
        }
    }
}
