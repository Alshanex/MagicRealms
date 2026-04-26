package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;
import java.util.Map;

/**
 * Datapack-loaded personality archetype. Each file at {@code data/<namespace>/personality_archetypes/<name>.json}
 * becomes one of these, with its id taken from the file path (e.g. {@code "magic_realms:stoic"} but the catalog
 * indexes it by the short name {@code "stoic"} so JSON references stay terse).
 *
 * <p>An archetype's {@code class_weights} map carries per-{@link EntityClass} bonuses added on top of {@link #baseWeight}
 * during weighted rolls — exactly mirroring the old hardcoded enum logic. The map is keyed by lower-case class name
 * ({@code "mage"}, {@code "warrior"}, {@code "rogue"}); missing keys mean no bonus for that class.
 *
 * <p>{@code in_random_pool} lets datapacks add archetypes that exist only as references for fixed personalities or
 * named characters, without ever being rolled by a regular mercenary spawn.
 */
public record Archetype(
        String id,
        String displayKey,
        Map<String, Integer> classWeights,
        int baseWeight,
        boolean inRandomPool
) {
    public static final Codec<Archetype> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Archetype::id),
            Codec.STRING.fieldOf("display_key").forGetter(Archetype::displayKey),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("class_weights", Map.of()).forGetter(Archetype::classWeights),
            Codec.INT.optionalFieldOf("base_weight", 10).forGetter(Archetype::baseWeight),
            Codec.BOOL.optionalFieldOf("in_random_pool", true).forGetter(Archetype::inRandomPool)
    ).apply(instance, Archetype::new));

    /** Effective weight for a roll biased by entity class. Returns at least 0; callers should treat 0 as "ineligible". */
    public int effectiveWeightFor(EntityClass entityClass) {
        int total = Math.max(0, baseWeight);
        if (entityClass != null) {
            Integer bonus = classWeights.get(entityClass.getName().toLowerCase(Locale.ROOT));
            if (bonus != null) total += bonus;
        }
        return Math.max(0, total);
    }

    // Network serialization

    public static void writeToBuf(FriendlyByteBuf buf, Archetype a) {
        buf.writeUtf(a.id);
        buf.writeUtf(a.displayKey);
        buf.writeMap(a.classWeights, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
        buf.writeVarInt(a.baseWeight);
        buf.writeBoolean(a.inRandomPool);
    }

    public static Archetype readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String displayKey = buf.readUtf();
        Map<String, Integer> classWeights = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt);
        int baseWeight = buf.readVarInt();
        boolean inRandomPool = buf.readBoolean();
        return new Archetype(id, displayKey, classWeights, baseWeight, inRandomPool);
    }
}
