package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Datapack-loaded fixed personality. Each file at {@code data/<namespace>/mercenaries/personality/fixed_personalities/<n>.json} becomes one of
 * these, with its id taken from the file path (e.g. {@code "magic_realms:alice"}).
 *
 * <p>A fixed personality is a pre-rolled collection of personality traits that can be stamped onto a mercenary instead
 * of rolling a fresh random one. There are three ways it gets applied:
 * <ol>
 *   <li>An exclusive mercenary's Java override can reference it by id.</li>
 *   <li>A skin preset JSON can specify it, locking personality to the preset.</li>
 *   <li>A regular mercenary rolls it from the random pool (if {@code in_random_pool}).</li>
 * </ol>
 *
 * <p>{@code overrideEntityName} lets the personality rename the mercenary when assigned.
 *
 * <p>{@code unique} controls whether the id can be assigned to multiple mercenaries in the same world. When true (the
 * default), once assigned the id is claimed for the life of the world - even after death - preserving the narrative
 * convention that named characters are one-of-a-kind.
 *
 * <p>{@code in_random_pool} controls whether random mercenaries can roll this personality. Set to false for
 * preset-locked character personalities that should only appear on their matching skin preset.
 *
 * <p><b>Archetype is now data-driven</b>: stored as a string id referencing an entry in the {@link ArchetypeCatalog}.
 */
public record FixedPersonalityDef(
        String id,
        String archetypeId,
        String hobbyId,
        String hometown,
        Set<Quirk> quirks,
        Optional<String> overrideEntityName,
        boolean inRandomPool,
        int weight,
        boolean unique
) {

    private static final Codec<Quirk> QUIRK_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                Quirk q = Quirk.fromId(s);
                return q != null
                        ? DataResult.success(q)
                        : DataResult.error(() -> "Unknown quirk: " + s);
            },
            Quirk::getId
    );

    // Codec for the JSON body

    public static final Codec<FixedPersonalityDef> BODY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("archetype").forGetter(FixedPersonalityDef::archetypeId),
            Codec.STRING.optionalFieldOf("hobby", "").forGetter(d -> d.hobbyId == null ? "" : d.hobbyId),
            Codec.STRING.optionalFieldOf("hometown", "").forGetter(d -> d.hometown == null ? "" : d.hometown),
            QUIRK_CODEC.listOf().optionalFieldOf("quirks", List.of()).forGetter(d -> new ArrayList<>(d.quirks)),
            Codec.STRING.optionalFieldOf("override_entity_name").forGetter(FixedPersonalityDef::overrideEntityName),
            Codec.BOOL.optionalFieldOf("in_random_pool", true).forGetter(FixedPersonalityDef::inRandomPool),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(FixedPersonalityDef::weight),
            Codec.BOOL.optionalFieldOf("unique", true).forGetter(FixedPersonalityDef::unique)
    ).apply(instance, (arch, hob, home, quirksList, overrideName, inPool, w, uniq) -> new FixedPersonalityDef(
            "", // id is filled in by the reload listener from the filename
            arch,
            hob.isEmpty() ? null : hob,
            home.isEmpty() ? null : home,
            quirksList.isEmpty() ? EnumSet.noneOf(Quirk.class) : EnumSet.copyOf(quirksList),
            overrideName,
            inPool,
            Math.max(1, w),
            uniq
    )));

    /** Return a copy of this def with the id filled in (reload listener uses this). */
    public FixedPersonalityDef withId(String newId) {
        return new FixedPersonalityDef(
                newId, archetypeId, hobbyId,
                hometown, quirks, overrideEntityName,
                inRandomPool, weight, unique
        );
    }

    /** Convert to the runtime FixedPersonality record used by PersonalityData.initialize. */
    public PersonalityInitializer.FixedPersonality toRuntime() {
        return new PersonalityInitializer.FixedPersonality(
                archetypeId, hobbyId,
                hometown, quirks
        );
    }

    // Network codec

    public static void writeToBuf(FriendlyByteBuf buf, FixedPersonalityDef def) {
        buf.writeUtf(def.id);
        buf.writeUtf(def.archetypeId == null ? "" : def.archetypeId);
        buf.writeUtf(def.hobbyId == null ? "" : def.hobbyId);
        buf.writeUtf(def.hometown == null ? "" : def.hometown);

        buf.writeVarInt(def.quirks.size());
        for (Quirk q : def.quirks) buf.writeUtf(q.getId());

        buf.writeBoolean(def.overrideEntityName.isPresent());
        def.overrideEntityName.ifPresent(buf::writeUtf);

        buf.writeBoolean(def.inRandomPool);
        buf.writeVarInt(def.weight);
        buf.writeBoolean(def.unique);
    }

    public static FixedPersonalityDef readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String arch = buf.readUtf();
        if (arch.isEmpty()) arch = null;
        String hob = buf.readUtf(); if (hob.isEmpty()) hob = null;
        String home = buf.readUtf(); if (home.isEmpty()) home = null;

        int quirkCount = buf.readVarInt();
        Set<Quirk> quirks = EnumSet.noneOf(Quirk.class);
        for (int i = 0; i < quirkCount; i++) {
            Quirk q = Quirk.fromId(buf.readUtf());
            if (q != null) quirks.add(q);
        }

        Optional<String> overrideName = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
        boolean inPool = buf.readBoolean();
        int weight = buf.readVarInt();
        boolean unique = buf.readBoolean();

        return new FixedPersonalityDef(id, arch, hob, home,
                quirks, overrideName, inPool, weight, unique);
    }
}
