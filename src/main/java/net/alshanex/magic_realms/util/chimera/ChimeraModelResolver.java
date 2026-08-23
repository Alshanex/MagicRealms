package net.alshanex.magic_realms.util.chimera;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ChimeraMobDefinition;
import net.alshanex.magic_realms.data.ChimeraSlot;
import net.alshanex.magic_realms.registry.ChimeraPartRegistry;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.*;

import java.util.*;

/**
 * Resolves model parts from Minecraft's {@link EntityModelSet}, which contains ALL baked entity models (vanilla and modded). Fully data-driven.
 */
public final class ChimeraModelResolver {

    // Vanilla cache
    private static final Map<ResourceLocation, ModelPart> VANILLA_ROOT_CACHE = new HashMap<>();
    private static final Map<String, Float> GEO_LEG_HEIGHT_CACHE = new HashMap<>();
    private static EntityModelSet entityModelSet;

    // GeckoLib cache
    private static final Map<ResourceLocation, BakedGeoModel> GEO_MODEL_CACHE = new HashMap<>();

    private ChimeraModelResolver() {}

    public static void setEntityModelSet(EntityModelSet modelSet) {
        entityModelSet = modelSet;
        VANILLA_ROOT_CACHE.clear();
    }

    /**
     * Build a {@link ChimeraPartSource} for the given entity and slot.
     * Automatically chooses vanilla or GeckoLib path based on the definition.
     */
    public static Optional<ChimeraPartSource> buildPartSource(ResourceLocation entityId, ChimeraSlot slot) {
        ChimeraPartRegistry registry = ChimeraPartRegistry.getInstance();
        if (registry == null) return Optional.empty();

        Optional<ChimeraMobDefinition> defOpt = registry.getDefinition(entityId);
        if (defOpt.isEmpty()) return Optional.empty();

        ChimeraMobDefinition def = defOpt.get();
        Optional<ChimeraMobDefinition.SlotEntry> entryOpt = def.getSlotEntry(slot);
        if (entryOpt.isEmpty()) return Optional.empty();

        ChimeraMobDefinition.SlotEntry entry = entryOpt.get();

        if (def.isGeckoLib()) {
            return buildGeckoLibPartSource(def, entry, slot, entityId);
        } else {
            return buildVanillaPartSource(def, entry, slot, entityId);
        }
    }

    //  Vanilla path

    private static Optional<ChimeraPartSource> buildVanillaPartSource(
            ChimeraMobDefinition def, ChimeraMobDefinition.SlotEntry entry,
            ChimeraSlot slot, ResourceLocation entityId) {

        ResourceLocation modelLayerId = def.getEffectiveModelLayer();
        Optional<ModelPart> rootOpt = getBakedRoot(modelLayerId);
        if (rootOpt.isEmpty()) return Optional.empty();

        ModelPart root = rootOpt.get();
        List<ModelPart> resolvedParts = new ArrayList<>();

        for (String path : entry.bones()) {
            findVanillaPart(root, path).ifPresentOrElse(
                    resolvedParts::add,
                    () -> MagicRealms.LOGGER.warn(
                            "Could not resolve vanilla bone '{}' for entity {} slot {}",
                            path, entityId, slot.getSerializedName())
            );
        }

        if (resolvedParts.isEmpty()) return Optional.empty();

        return Optional.of(ChimeraPartSource.vanilla(
                resolvedParts, def.texture(), entry.getEffectiveScale(),
                entry.getOffsetX(), entry.getOffsetY(), entry.getOffsetZ(),
                entry.getLegHeightPixels()
        ));
    }

    public static Optional<ModelPart> getBakedRoot(ResourceLocation modelLayerId) {
        if (entityModelSet == null) return Optional.empty();

        ModelPart cached = VANILLA_ROOT_CACHE.get(modelLayerId);
        if (cached != null) return Optional.of(cached);

        try {
            ModelLayerLocation layerLoc = new ModelLayerLocation(modelLayerId, "main");
            ModelPart root = entityModelSet.bakeLayer(layerLoc);
            VANILLA_ROOT_CACHE.put(modelLayerId, root);
            return Optional.of(root);
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Could not bake vanilla model layer '{}': {}", modelLayerId, e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<ModelPart> findVanillaPart(ModelPart root, String path) {
        String[] segments = path.split("\\.");
        ModelPart current = root;
        for (String segment : segments) {
            try {
                current = current.getChild(segment);
            } catch (Exception e) {
                MagicRealms.LOGGER.debug("Could not find '{}' in vanilla path '{}'", segment, path);
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    //  GeckoLib path

    private static Optional<ChimeraPartSource> buildGeckoLibPartSource(
            ChimeraMobDefinition def, ChimeraMobDefinition.SlotEntry entry,
            ChimeraSlot slot, ResourceLocation entityId) {

        ResourceLocation geoModelId = def.geoModel().orElseThrow();
        Optional<BakedGeoModel> modelOpt = getBakedGeoModel(geoModelId);
        if (modelOpt.isEmpty()) return Optional.empty();

        BakedGeoModel model = modelOpt.get();
        List<GeoBone> resolvedBones = new ArrayList<>();

        for (String boneName : entry.bones()) {
            findGeoBone(model, boneName).ifPresentOrElse(
                    resolvedBones::add,
                    () -> MagicRealms.LOGGER.warn(
                            "Could not resolve GeckoLib bone '{}' for entity {} slot {}",
                            boneName, entityId, slot.getSerializedName())
            );
        }

        if (resolvedBones.isEmpty()) return Optional.empty();

        // Auto-measure leg height for leg slots from the resolved GeckoLib bones
        float legHeight = entry.getLegHeightPixels();
        if (slot.isLeg() && entry.legHeight().isEmpty() && !resolvedBones.isEmpty()) {
            // No explicit leg_height in JSON -> auto-measure from geometry
            legHeight = measureGeoBoneHeight(resolvedBones.get(0));
            MagicRealms.LOGGER.debug(
                    "Auto-measured leg height for {} slot {}: {}px",
                    entityId, slot.getSerializedName(), legHeight);
        }

        return Optional.of(ChimeraPartSource.geckoLib(
                resolvedBones, def.texture(), entry.getEffectiveScale(),
                entry.getOffsetX(), entry.getOffsetY(), entry.getOffsetZ(),
                geoModelId, legHeight
        ));
    }

    /**
     * Get a baked GeckoLib model from the cache.
     */
    private static Optional<BakedGeoModel> getBakedGeoModel(ResourceLocation geoModelId) {
        // Check our local cache first
        BakedGeoModel cached = GEO_MODEL_CACHE.get(geoModelId);
        if (cached != null) return Optional.of(cached);

        try {
            BakedGeoModel model = GeckoLibCache.getBakedModels().get(geoModelId);
            if (model != null) {
                GEO_MODEL_CACHE.put(geoModelId, model);
                return Optional.of(model);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Could not load GeckoLib model '{}': {}", geoModelId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Find a bone by name in a baked GeckoLib model.
     * Supports dot-notation paths (e.g., "body.torso.head") for nested bones.
     */
    private static Optional<GeoBone> findGeoBone(BakedGeoModel model, String bonePath) {
        String[] segments = bonePath.split("\\.");

        // Start from the first segment - search all top-level bones
        GeoBone current = findBoneInList(model.topLevelBones(), segments[0]);
        if (current == null) {
            // Deep search the entire tree for the first segment
            current = deepSearchBone(model.topLevelBones(), segments[0]);
        }
        if (current == null) return Optional.empty();

        // Walk the remaining path segments
        for (int i = 1; i < segments.length; i++) {
            GeoBone child = findBoneInList(current.getChildBones(), segments[i]);
            if (child == null) return Optional.empty();
            current = child;
        }

        return Optional.of(current);
    }

    private static GeoBone findBoneInList(List<GeoBone> bones, String name) {
        for (GeoBone bone : bones) {
            if (bone.getName().equals(name)) {
                return bone;
            }
        }
        return null;
    }

    /**
     * Recursively search the entire bone tree for a bone with the given name.
     */
    private static GeoBone deepSearchBone(List<GeoBone> bones, String name) {
        for (GeoBone bone : bones) {
            if (bone.getName().equals(name)) {
                return bone;
            }
            GeoBone found = deepSearchBone(bone.getChildBones(), name);
            if (found != null) return found;
        }
        return null;
    }

    public static float measureGeoBoneHeight(GeoBone bone) {
        String cacheKey = bone.getName() + "@" + System.identityHashCode(bone);
        Float cached = GEO_LEG_HEIGHT_CACHE.get(cacheKey);
        if (cached != null) return cached;

        float[] minMax = { Float.MAX_VALUE, Float.MIN_VALUE }; // [minY, maxY]
        collectYExtent(bone, minMax);

        float height;
        if (minMax[0] <= minMax[1]) {
            // GeckoLib bakes vertex positions in block-space,so multiply back to get model pixels.
            height = (minMax[1] - minMax[0]) * 16.0f;
        } else {
            height = 12.0f; // fallback if bone has no geometry
        }

        GEO_LEG_HEIGHT_CACHE.put(cacheKey, height);
        return height;
    }

    private static void collectYExtent(GeoBone bone, float[] minMax) {
        for (GeoCube cube : bone.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;
                for (GeoVertex vertex : quad.vertices()) {
                    float y = vertex.position().y();
                    if (y < minMax[0]) minMax[0] = y;
                    if (y > minMax[1]) minMax[1] = y;
                }
            }
        }
        for (GeoBone child : bone.getChildBones()) {
            collectYExtent(child, minMax);
        }
    }

    //  Cache management

    public static void clearCache() {
        VANILLA_ROOT_CACHE.clear();
        GEO_MODEL_CACHE.clear();
        GEO_LEG_HEIGHT_CACHE.clear();  // NEW
    }
}
