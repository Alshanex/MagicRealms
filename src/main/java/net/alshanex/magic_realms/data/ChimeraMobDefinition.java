package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A mob's chimera part definition, loaded from JSON.
 * <p>
 * Model parts are resolved from Minecraft's EntityModelSet automatically.
 * <p>
 * Each slot entry maps to model part paths and includes an optional scale
 * factor to normalize the part's size to the chimera's standard skeleton.
 * <p>
 * JSON structure:
 * <pre>
 * {
 *   "entity": "minecraft:zombie",
 *   "parts": {
 *     "head":      { "bones": ["head", "hat"], "scale": 1.0 },
 *     "torso":     { "bones": ["body"], "scale": 1.0 },
 *     "left_arm":  { "bones": ["left_arm"], "scale": 1.0 },
 *     "right_arm": { "bones": ["right_arm"], "scale": 1.0 },
 *     "left_leg":  { "bones": ["left_leg"], "scale": 1.0 },
 *     "right_leg": { "bones": ["right_leg"], "scale": 1.0 }
 *   },
 *   "texture": "minecraft:textures/entity/zombie/zombie.png"
 * }
 * </pre>
 * <p>
 * For mobs with different proportions (like the warden), use scale to
 * normalize. The scale value represents how much to shrink/grow the part
 * so it fits the chimera's standard humanoid-sized skeleton:
 * <pre>
 * {
 *   "entity": "minecraft:warden",
 *   "parts": {
 *     "head":  { "bones": ["bone.body.head"], "scale": 0.65 },
 *     "torso": { "bones": ["bone.body"], "scale": 0.65 },
 *     ...
 *   },
 *   "texture": "minecraft:textures/entity/warden/warden.png"
 * }
 * </pre>
 */
public record ChimeraMobDefinition(
        ResourceLocation entity,
        Optional<ResourceLocation> modelLayer,
        Optional<ResourceLocation> geoModel,
        Map<ChimeraSlot, SlotEntry> parts,
        ResourceLocation texture
) {
    public record SlotEntry(
            List<String> bones,
            Optional<Float> scale,
            Optional<List<Float>> offset,
            Optional<Float> legHeight
    ) {
        public static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.listOf().fieldOf("bones").forGetter(SlotEntry::bones),
                        Codec.FLOAT.optionalFieldOf("scale").forGetter(SlotEntry::scale),
                        Codec.FLOAT.listOf().optionalFieldOf("offset").forGetter(SlotEntry::offset),
                        Codec.FLOAT.optionalFieldOf("leg_height").forGetter(SlotEntry::legHeight)
                ).apply(instance, SlotEntry::new)
        );

        public float getEffectiveScale() {
            return scale.orElse(1.0f);
        }

        public float getLegHeightPixels() {
            return legHeight.orElse(12.0f);
        }

        public float getRenderedLegHeight() {
            return getLegHeightPixels() * getEffectiveScale();
        }

        public float getLegHeightRatio() {
            return getRenderedLegHeight() / 12.0f;
        }

        public float getOffsetX() { return offset.map(l -> l.size() > 0 ? l.get(0) : 0f).orElse(0f); }
        public float getOffsetY() { return offset.map(l -> l.size() > 1 ? l.get(1) : 0f).orElse(0f); }
        public float getOffsetZ() { return offset.map(l -> l.size() > 2 ? l.get(2) : 0f).orElse(0f); }
    }

    private static final Codec<Map<ChimeraSlot, SlotEntry>> PARTS_CODEC =
            Codec.unboundedMap(ChimeraSlot.CODEC, SlotEntry.CODEC);

    public static final Codec<ChimeraMobDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("entity").forGetter(ChimeraMobDefinition::entity),
                    ResourceLocation.CODEC.optionalFieldOf("model_layer").forGetter(ChimeraMobDefinition::modelLayer),
                    ResourceLocation.CODEC.optionalFieldOf("geo_model").forGetter(ChimeraMobDefinition::geoModel),
                    PARTS_CODEC.fieldOf("parts").forGetter(ChimeraMobDefinition::parts),
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(ChimeraMobDefinition::texture)
            ).apply(instance, ChimeraMobDefinition::new)
    );

    public boolean isGeckoLib() {
        return geoModel.isPresent();
    }

    public ResourceLocation getEffectiveModelLayer() {
        return modelLayer.orElse(entity);
    }

    public Optional<SlotEntry> getSlotEntry(ChimeraSlot slot) {
        return Optional.ofNullable(parts.get(slot));
    }

    public boolean hasSlot(ChimeraSlot slot) {
        return parts.containsKey(slot);
    }

    public Set<ChimeraSlot> getAvailableSlots() {
        return parts.keySet();
    }
}
