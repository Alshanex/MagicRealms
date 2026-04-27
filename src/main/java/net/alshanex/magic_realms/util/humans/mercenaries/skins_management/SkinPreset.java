package net.alshanex.magic_realms.util.humans.mercenaries.skins_management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Datapack-loaded skin preset.
 *
 * <p>Each file at {@code data/magic_realms/mercenaries/skin_presets/<n>.json} becomes one of these,
 * with its {@link #id} taken from the file path (e.g. a file at {@code data/magic_realms/mercenaries/skin_presets/vex.json} produces an id of {@code "magic_realms:vex"}).
 *
 * <p>The {@link #id} is the stable handle other systems use to reference a specific preset — for example, a bandit profile's {@code skin_preset} field uses this id.
 * The {@link #texture} is a separate {@link ResourceLocation} pointing at the actual {@code .png} asset under {@code assets/}.
 *
 * <p>The id is <em>not</em> in the JSON file. The reload listener fills it in from the file path via {@link #withId(ResourceLocation)} after parsing the body.
 */
public record SkinPreset(
        ResourceLocation id,
        ResourceLocation texture,
        Optional<String> displayName,
        GenderFilter gender,
        int weight,
        Optional<String> fixedPersonalityId,
        boolean addedToPool
) {
    /**
     * Placeholder id used immediately after JSON parsing, before the reload listener calls {@link #withId(ResourceLocation)} with the real value derived from the file path.
     */
    private static final ResourceLocation UNIDENTIFIED =
            ResourceLocation.fromNamespaceAndPath("magic_realms", "unidentified");

    public static final Codec<SkinPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(SkinPreset::texture),
            Codec.STRING.optionalFieldOf("display_name").forGetter(SkinPreset::displayName),
            GenderFilter.CODEC.optionalFieldOf("gender", GenderFilter.ANY).forGetter(SkinPreset::gender),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(SkinPreset::weight),
            Codec.STRING.optionalFieldOf("fixed_personality_id").forGetter(SkinPreset::fixedPersonalityId),
            Codec.BOOL.optionalFieldOf("added_to_pool", true).forGetter(SkinPreset::addedToPool)
    ).apply(instance, (texture, displayName, gender, weight, fpId, addedToPool) ->
            new SkinPreset(UNIDENTIFIED, texture, displayName, gender, weight, fpId, addedToPool)));

    /** Returns a copy of this preset with the id field populated. Called by the reload listener. */
    public SkinPreset withId(ResourceLocation newId) {
        return new SkinPreset(newId, texture, displayName, gender, weight, fixedPersonalityId, addedToPool);
    }

    public static void writeToBuf(FriendlyByteBuf buf, SkinPreset preset) {
        buf.writeResourceLocation(preset.id);
        buf.writeResourceLocation(preset.texture);
        buf.writeBoolean(preset.displayName.isPresent());
        preset.displayName.ifPresent(buf::writeUtf);
        buf.writeEnum(preset.gender);
        buf.writeVarInt(preset.weight);
        buf.writeBoolean(preset.fixedPersonalityId.isPresent());
        preset.fixedPersonalityId.ifPresent(buf::writeUtf);
        buf.writeBoolean(preset.addedToPool);
    }

    public static SkinPreset readFromBuf(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        ResourceLocation tex = buf.readResourceLocation();
        Optional<String> name = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
        GenderFilter gender = buf.readEnum(GenderFilter.class);
        int weight = buf.readVarInt();
        Optional<String> fixedPersonalityId = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
        boolean addedToPool = buf.readBoolean();
        return new SkinPreset(id, tex, name, gender, weight, fixedPersonalityId, addedToPool);
    }
}