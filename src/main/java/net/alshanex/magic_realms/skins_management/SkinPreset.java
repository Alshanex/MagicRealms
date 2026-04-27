package net.alshanex.magic_realms.skins_management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SkinPreset(
        ResourceLocation texture,
        Optional<String> displayName,
        GenderFilter gender,
        int weight,
        Optional<String> fixedPersonalityId,
        boolean addedToPool
) {
    public static final Codec<SkinPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(SkinPreset::texture),
            Codec.STRING.optionalFieldOf("display_name").forGetter(SkinPreset::displayName),
            GenderFilter.CODEC.optionalFieldOf("gender", GenderFilter.ANY).forGetter(SkinPreset::gender),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(SkinPreset::weight),
            Codec.STRING.optionalFieldOf("fixed_personality_id").forGetter(SkinPreset::fixedPersonalityId),
            Codec.BOOL.optionalFieldOf("added_to_pool", true).forGetter(SkinPreset::addedToPool)
    ).apply(instance, SkinPreset::new));

    public static void writeToBuf(FriendlyByteBuf buf, SkinPreset preset) {
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
        ResourceLocation tex = buf.readResourceLocation();
        Optional<String> name = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
        GenderFilter gender = buf.readEnum(GenderFilter.class);
        int weight = buf.readVarInt();
        Optional<String> fixedPersonalityId = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
        boolean addedToPool = buf.readBoolean();
        return new SkinPreset(tex, name, gender, weight, fixedPersonalityId, addedToPool);
    }
}
