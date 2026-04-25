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
 * Datapack-loaded fixed personality. Each file at data/<namespace>/fixed_personalities/<n>.json becomes one of these,
 * with its id taken from the file path (e.g. "magic_realms:alice").
 *
 * A fixed personality is a pre-rolled collection of personality traits that can be stamped onto a mercenary instead of rolling a fresh random one.
 * There are three ways it gets applied:
 *   1. An exclusive mercenary's Java override can reference it by id.
 *   2. A skin preset JSON can specify it, locking personality to the preset.
 *   3. A regular mercenary rolls it from the random pool (if in_random_pool).
 *
 * overrideEntityName lets the personality rename the mercenary when assigned
 *
 * unique controls whether the id can be assigned to multiple mercenaries in the same world. When true (the default), once assigned the id is
 * claimed for the life of the world - even after death - preserving the narrative convention that named characters are one-of-a-kind.
 *
 * in_random_pool controls whether random mercenaries can roll this personality. Set to false for preset-locked character personalities
 * that should only appear on their matching skin preset.
 */
public record FixedPersonalityDef(
        String id,
        PersonalityArchetype archetype,
        String favoriteFoodTagId,
        String dislikedFoodTagId,
        String hobbyId,
        String hometown,
        int birthdayDayOfYear,
        Set<Quirk> quirks,
        Optional<String> overrideEntityName,
        boolean inRandomPool,
        int weight,
        boolean unique
) {

    private static final Codec<PersonalityArchetype> ARCHETYPE_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                PersonalityArchetype a = PersonalityArchetype.fromId(s);
                return a != null
                        ? DataResult.success(a)
                        : DataResult.error(() -> "Unknown personality archetype: " + s);
            },
            PersonalityArchetype::getId
    );

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
            ARCHETYPE_CODEC.fieldOf("archetype").forGetter(FixedPersonalityDef::archetype),
            Codec.STRING.optionalFieldOf("favorite_food_tag", "").forGetter(d -> d.favoriteFoodTagId == null ? "" : d.favoriteFoodTagId),
            Codec.STRING.optionalFieldOf("disliked_food_tag", "").forGetter(d -> d.dislikedFoodTagId == null ? "" : d.dislikedFoodTagId),
            Codec.STRING.optionalFieldOf("hobby", "").forGetter(d -> d.hobbyId == null ? "" : d.hobbyId),
            Codec.STRING.optionalFieldOf("hometown", "").forGetter(d -> d.hometown == null ? "" : d.hometown),
            Codec.INT.optionalFieldOf("birthday_day_of_year", 0).forGetter(FixedPersonalityDef::birthdayDayOfYear),
            QUIRK_CODEC.listOf().optionalFieldOf("quirks", List.of()).forGetter(d -> new ArrayList<>(d.quirks)),
            Codec.STRING.optionalFieldOf("override_entity_name").forGetter(FixedPersonalityDef::overrideEntityName),
            Codec.BOOL.optionalFieldOf("in_random_pool", true).forGetter(FixedPersonalityDef::inRandomPool),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(FixedPersonalityDef::weight),
            Codec.BOOL.optionalFieldOf("unique", true).forGetter(FixedPersonalityDef::unique)
    ).apply(instance, (arch, favStr, disStr, hob, home, bday, quirksList, overrideName, inPool, w, uniq) -> new FixedPersonalityDef(
            "", // id is filled in by the reload listener from the filename
            arch,
            favStr.isEmpty() ? null : favStr,
            disStr.isEmpty() ? null : disStr,
            hob.isEmpty() ? null : hob,
            home.isEmpty() ? null : home,
            bday,
            quirksList.isEmpty() ? EnumSet.noneOf(Quirk.class) : EnumSet.copyOf(quirksList),
            overrideName,
            inPool,
            Math.max(1, w),
            uniq
    )));

    /** Return a copy of this def with the id filled in (reload listener uses this). */
    public FixedPersonalityDef withId(String newId) {
        return new FixedPersonalityDef(
                newId, archetype, favoriteFoodTagId, dislikedFoodTagId, hobbyId,
                hometown, birthdayDayOfYear, quirks, overrideEntityName,
                inRandomPool, weight, unique
        );
    }

    /** Convert to the runtime FixedPersonality record used by PersonalityData.initialize. */
    public PersonalityInitializer.FixedPersonality toRuntime() {
        return new PersonalityInitializer.FixedPersonality(
                archetype, favoriteFoodTagId, dislikedFoodTagId, hobbyId,
                hometown, birthdayDayOfYear, quirks
        );
    }

    // Network codec

    public static void writeToBuf(FriendlyByteBuf buf, FixedPersonalityDef def) {
        buf.writeUtf(def.id);
        buf.writeUtf(def.archetype.getId());
        buf.writeUtf(def.favoriteFoodTagId == null ? "" : def.favoriteFoodTagId);
        buf.writeUtf(def.dislikedFoodTagId == null ? "" : def.dislikedFoodTagId);
        buf.writeUtf(def.hobbyId == null ? "" : def.hobbyId);
        buf.writeUtf(def.hometown == null ? "" : def.hometown);
        buf.writeVarInt(def.birthdayDayOfYear);

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
        PersonalityArchetype arch = PersonalityArchetype.fromId(buf.readUtf());
        String fav = buf.readUtf(); if (fav.isEmpty()) fav = null;
        String dis = buf.readUtf(); if (dis.isEmpty()) dis = null;
        String hob = buf.readUtf(); if (hob.isEmpty()) hob = null;
        String home = buf.readUtf(); if (home.isEmpty()) home = null;
        int bday = buf.readVarInt();

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

        return new FixedPersonalityDef(id, arch, fav, dis, hob, home, bday,
                quirks, overrideName, inPool, weight, unique);
    }
}
