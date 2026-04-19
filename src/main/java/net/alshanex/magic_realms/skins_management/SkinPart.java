package net.alshanex.magic_realms.skins_management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SkinPart(
        ResourceLocation texture,
        SkinCategory category,
        GenderFilter gender,
        ClassFilter entityClass,
        int weight
) {
    public static final Codec<SkinPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(SkinPart::texture),
            SkinCategory.CODEC.fieldOf("category").forGetter(SkinPart::category),
            GenderFilter.CODEC.optionalFieldOf("gender", GenderFilter.ANY).forGetter(SkinPart::gender),
            ClassFilter.CODEC.optionalFieldOf("entity_class", ClassFilter.ANY).forGetter(SkinPart::entityClass),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(SkinPart::weight)
    ).apply(instance, SkinPart::new));

    public static void writeToBuf(FriendlyByteBuf buf, SkinPart part) {
        buf.writeResourceLocation(part.texture);
        buf.writeEnum(part.category);
        buf.writeEnum(part.gender);
        buf.writeEnum(part.entityClass);
        buf.writeVarInt(part.weight);
    }

    public static SkinPart readFromBuf(FriendlyByteBuf buf) {
        return new SkinPart(
                buf.readResourceLocation(),
                buf.readEnum(SkinCategory.class),
                buf.readEnum(GenderFilter.class),
                buf.readEnum(ClassFilter.class),
                buf.readVarInt()
        );
    }
}
