package net.alshanex.magic_realms.util.humans.mercenaries.personality_management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Map;

/**
 * A hobby / interest that drives keyword-based chat reactions.
 *
 * Each hobby bundles a list of chat keywords and an archetype affinity note.
 * When a player types a message nearby a contracted mercenary and the message contains one of the keywords, the mercenary may respond from a hobby- and
 * archetype-specific pool.
 */
public record Hobby(
        String id,
        String displayKey,
        boolean inRandomPool
) {
    public static final String DEFAULT_POOL_KEY = "default";

    public static final Codec<Hobby> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Hobby::id),
            Codec.STRING.fieldOf("display_key").forGetter(Hobby::displayKey),
            Codec.BOOL.optionalFieldOf("in_random_pool", true).forGetter(Hobby::inRandomPool)
    ).apply(instance, Hobby::new));

    // Network serialization

    public static void writeToBuf(FriendlyByteBuf buf, Hobby h) {
        buf.writeUtf(h.id);
        buf.writeUtf(h.displayKey);
        buf.writeBoolean(h.inRandomPool);
    }

    public static Hobby readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String displayKey = buf.readUtf();
        boolean inRandomPool = buf.readBoolean();
        return new Hobby(id, displayKey, inRandomPool);
    }
}
