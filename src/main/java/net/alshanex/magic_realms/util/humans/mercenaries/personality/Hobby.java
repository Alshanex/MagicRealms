package net.alshanex.magic_realms.util.humans.mercenaries.personality;

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
        Map<String, List<String>> responses,
        boolean inRandomPool
) {
    public static final String DEFAULT_POOL_KEY = "default";

    public static final Codec<Hobby> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Hobby::id),
            Codec.STRING.fieldOf("display_key").forGetter(Hobby::displayKey),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).fieldOf("responses").forGetter(Hobby::responses),
            Codec.BOOL.optionalFieldOf("in_random_pool", true).forGetter(Hobby::inRandomPool)
    ).apply(instance, Hobby::new));

    /**
     * Get the response translation-key pool for a specific archetype, falling back to the default pool if the archetype isn't explicitly defined.
     * Returns an empty list if neither the archetype nor the default pool exists (malformed hobby, but we don't throw - reaction code handles empty gracefully).
     */
    public List<String> getResponsePool(String archetypeId) {
        if (archetypeId != null && responses.containsKey(archetypeId)) {
            List<String> pool = responses.get(archetypeId);
            if (pool != null && !pool.isEmpty()) return pool;
        }
        List<String> def = responses.get(DEFAULT_POOL_KEY);
        return def != null ? def : List.of();
    }

    // Network serialization

    public static void writeToBuf(FriendlyByteBuf buf, Hobby h) {
        buf.writeUtf(h.id);
        buf.writeUtf(h.displayKey);
        buf.writeMap(h.responses,
                FriendlyByteBuf::writeUtf,
                (b, list) -> b.writeCollection(list, FriendlyByteBuf::writeUtf));
        buf.writeBoolean(h.inRandomPool);
    }

    public static Hobby readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String displayKey = buf.readUtf();
        Map<String, List<String>> responses = buf.readMap(
                FriendlyByteBuf::readUtf,
                b -> b.readList(FriendlyByteBuf::readUtf)
        );
        boolean inRandomPool = buf.readBoolean();
        return new Hobby(id, displayKey, responses, inRandomPool);
    }
}
