package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Identifies which boss story and which chapter a Lost Page belongs to.
 * Stored as a DataComponent on Lost Page items.
 */
public record PageIdentifier(ResourceLocation storyId, int chapter) {

    public static final Codec<PageIdentifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("story_id").forGetter(PageIdentifier::storyId),
                    Codec.INT.fieldOf("chapter").forGetter(PageIdentifier::chapter)
            ).apply(instance, PageIdentifier::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PageIdentifier> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, PageIdentifier::storyId,
                    ByteBufCodecs.VAR_INT, PageIdentifier::chapter,
                    PageIdentifier::new
            );
}
