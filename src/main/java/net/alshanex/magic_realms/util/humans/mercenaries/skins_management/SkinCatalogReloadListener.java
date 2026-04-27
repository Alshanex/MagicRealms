package net.alshanex.magic_realms.util.humans.mercenaries.skins_management;

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
 * Loads skin parts and presets from datapacks
 * Because SimpleJsonResourceReloadListener takes a single folder, we register two listeners — one for parts, one for presets — via a small composite below.
 */
public class SkinCatalogReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final String PARTS_FOLDER   = "mercenaries/skin_parts";
    private static final String PRESETS_FOLDER = "mercenaries/skin_presets";

    private final boolean presets; // false = parts, true = presets

    private SkinCatalogReloadListener(boolean presets) {
        super(GSON, presets ? PRESETS_FOLDER : PARTS_FOLDER);
        this.presets = presets;
    }

    public static SkinCatalogReloadListener parts()   { return new SkinCatalogReloadListener(false); }
    public static SkinCatalogReloadListener presets() { return new SkinCatalogReloadListener(true); }

    // staging area shared between the two listeners during a reload
    private static final List<SkinPart> STAGING_PARTS = new ArrayList<>();
    private static final List<SkinPreset> STAGING_PRESETS = new ArrayList<>();

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (!presets) {
            STAGING_PARTS.clear();
            for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
                SkinPart.CODEC.parse(JsonOps.INSTANCE, e.getValue())
                        .resultOrPartial(err -> MagicRealms.LOGGER.error(
                                "Failed to parse skin part {}: {}", e.getKey(), err))
                        .ifPresent(STAGING_PARTS::add);
            }
            MagicRealms.LOGGER.debug("Loaded {} skin parts", STAGING_PARTS.size());
        } else {
            STAGING_PRESETS.clear();
            for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
                ResourceLocation key = e.getKey();
                SkinPreset.CODEC.parse(JsonOps.INSTANCE, e.getValue())
                        .resultOrPartial(err -> MagicRealms.LOGGER.error(
                                "Failed to parse skin preset {}: {}", key, err))
                        .map(parsed -> parsed.withId(key))
                        .ifPresent(STAGING_PRESETS::add);
            }
            MagicRealms.LOGGER.debug("Loaded {} skin presets", STAGING_PRESETS.size());

            // presets listener runs after parts in registration order
            SkinCatalog built = new SkinCatalog(STAGING_PARTS, STAGING_PRESETS);
            SkinCatalogHolder.setServer(built);
            MagicRealms.LOGGER.debug("Skin catalog rebuilt: {} parts, {} presets",
                    built.allParts().size(), built.allPresets().size());
        }
    }
}
