package net.alshanex.magic_realms.util.humans.bandits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Datapack-loaded bandit profile. Each file at {@code data/magic_realms/mercenaries/bandit_profiles/<name>.json} becomes one of these,
 * with its id taken from the file path (e.g. {@code "magic_realms:giant_warrior"}).
 *
 * <p>A profile is a "stamp" applied to a {@code HostileRandomHumanEntity} during initialization. Each
 * field is optional: if omitted, the entity falls back to the random roll used by an unprofiled bandit.
 *
 * <p>Level can be specified as either an absolute range or a percentage of {@link net.alshanex.magic_realms.Config#maxLevel}.
 * Absolute ranges take precedence when both are set. If neither is set, the vanilla random spawn-level roll is used.
 *
 * <p>For mages, three spell-selection modes are available, in priority order:
 * <ol>
 *     <li>{@code explicit_spells} — exact spell ids inscribed onto the entity.</li>
 *     <li>{@code spells_tag} (+ optional {@code spells_tag_pick_count}) — pull from a tag, optionally picking N at random.</li>
 *     <li>Fall through to the regular {@code SpellListGenerator}.</li>
 * </ol>
 *
 * <p>{@code attribute_boosts} are flat modifiers applied <em>after</em> all class/level attribute math runs, so they
 * stack cleanly on top of regular stats — useful for "boss" profiles that double health or buff damage.
 */
public record BanditProfile(
        String id,
        int weight,

        // Class & build
        Optional<EntityClass> entityClass,
        Optional<Gender> gender,
        Optional<Boolean> hasShield,
        Optional<Boolean> isArcher,
        Optional<Integer> starLevel,

        // Level configuration
        Optional<Float> minLevelPercent,
        Optional<Float> maxLevelPercent,
        Optional<Integer> minLevelAbsolute,
        Optional<Integer> maxLevelAbsolute,

        // Visuals
        Optional<Float> entityScale,
        Optional<String> overrideName,
        Optional<ResourceLocation> skinPreset,

        // Magic schools (mage profiles)
        List<ResourceLocation> magicSchools,
        Optional<ResourceLocation> magicSchoolsTagId,

        // Spell selection
        List<ResourceLocation> explicitSpells,
        Optional<ResourceLocation> spellsTagId,
        Optional<Integer> spellsTagPickCount,

        // Equipment slots: keyed by slot name ("mainhand", "offhand", "head", "chest", "legs", "feet")
        Map<String, ResourceLocation> equipment,

        // Flat attribute modifiers applied at the end of init
        List<AttributeBoost> attributeBoosts,

        // Flags
        boolean isMiniBoss,
        boolean immortal,
        Optional<String> fixedPersonalityId,
        boolean inRandomPool
) {

    public static final BanditProfile EMPTY = new BanditProfile(
            "", 1,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(),
            List.of(), Optional.empty(),
            List.of(), Optional.empty(), Optional.empty(),
            Map.of(),
            List.of(),
            false, false, Optional.empty(), true
    );

    /**
     * Attribute boost: a target attribute id plus an {@link AttributeModifier}. The attribute is stored as a
     * {@link ResourceLocation} rather than a {@code Holder<Attribute>} because vanilla attributes
     * ({@code minecraft:max_health}, etc.) live in the datapack registry and aren't resolvable through
     * {@code BuiltInRegistries.ATTRIBUTE} at codec-parse time. The applier resolves the id against the
     * entity's registries when applying the boost.
     */
    public record AttributeBoost(ResourceLocation attribute, AttributeModifier modifier) {
        public static final Codec<AttributeBoost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("attribute").forGetter(AttributeBoost::attribute),
                AttributeModifier.MAP_CODEC.forGetter(AttributeBoost::modifier)
        ).apply(instance, AttributeBoost::new));
    }

    // Codec for EntityClass — string ↔ enum, with safe parsing.
    private static final Codec<EntityClass> ENTITY_CLASS_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(EntityClass.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown entity_class: " + s);
                }
            },
            EntityClass::getName
    );

    // Codec for Gender — string ↔ enum, with safe parsing.
    private static final Codec<Gender> GENDER_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(Gender.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown gender: " + s);
                }
            },
            Gender::getName
    );

    // Codec for the JSON body. Mojang's RecordCodecBuilder.group has a hard arity limit (~16), so we split the codec into two halves as MapCodecs and combine them
    // with MapCodec.pair so both halves read from the same flat JSON object. The id is filled in by the reload listener after parsing.

    /** First half: identity, build, level, visuals. */
    private record Half1(
            int weight,
            Optional<EntityClass> entityClass,
            Optional<Gender> gender,
            Optional<Boolean> hasShield,
            Optional<Boolean> isArcher,
            Optional<Integer> starLevel,
            Optional<Float> minLevelPercent,
            Optional<Float> maxLevelPercent,
            Optional<Integer> minLevelAbsolute,
            Optional<Integer> maxLevelAbsolute,
            Optional<Float> entityScale,
            Optional<String> overrideName,
            Optional<ResourceLocation> skinPreset
    ) {}

    /** Second half: schools, spells, equipment, attribute boosts, flags. */
    private record Half2(
            List<ResourceLocation> magicSchools,
            Optional<ResourceLocation> magicSchoolsTagId,
            List<ResourceLocation> explicitSpells,
            Optional<ResourceLocation> spellsTagId,
            Optional<Integer> spellsTagPickCount,
            Map<String, ResourceLocation> equipment,
            List<AttributeBoost> attributeBoosts,
            boolean isMiniBoss,
            boolean immortal,
            Optional<String> fixedPersonalityId,
            boolean inRandomPool
    ) {}

    private static final MapCodec<Half1> HALF1_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("weight", 1).forGetter(Half1::weight),
            ENTITY_CLASS_CODEC.optionalFieldOf("entity_class").forGetter(Half1::entityClass),
            GENDER_CODEC.optionalFieldOf("gender").forGetter(Half1::gender),
            Codec.BOOL.optionalFieldOf("has_shield").forGetter(Half1::hasShield),
            Codec.BOOL.optionalFieldOf("is_archer").forGetter(Half1::isArcher),
            Codec.intRange(1, 3).optionalFieldOf("star_level").forGetter(Half1::starLevel),
            Codec.FLOAT.optionalFieldOf("min_level_percent").forGetter(Half1::minLevelPercent),
            Codec.FLOAT.optionalFieldOf("max_level_percent").forGetter(Half1::maxLevelPercent),
            Codec.INT.optionalFieldOf("min_level_absolute").forGetter(Half1::minLevelAbsolute),
            Codec.INT.optionalFieldOf("max_level_absolute").forGetter(Half1::maxLevelAbsolute),
            Codec.FLOAT.optionalFieldOf("entity_scale").forGetter(Half1::entityScale),
            Codec.STRING.optionalFieldOf("override_name").forGetter(Half1::overrideName),
            ResourceLocation.CODEC.optionalFieldOf("skin_preset").forGetter(Half1::skinPreset)
    ).apply(instance, Half1::new));

    private static final MapCodec<Half2> HALF2_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("magic_schools", List.of()).forGetter(Half2::magicSchools),
            ResourceLocation.CODEC.optionalFieldOf("magic_schools_tag").forGetter(Half2::magicSchoolsTagId),
            ResourceLocation.CODEC.listOf().optionalFieldOf("explicit_spells", List.of()).forGetter(Half2::explicitSpells),
            ResourceLocation.CODEC.optionalFieldOf("spells_tag").forGetter(Half2::spellsTagId),
            Codec.INT.optionalFieldOf("spells_tag_pick_count").forGetter(Half2::spellsTagPickCount),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .optionalFieldOf("equipment", Map.of()).forGetter(Half2::equipment),
            AttributeBoost.CODEC.listOf().optionalFieldOf("attribute_boosts", List.of()).forGetter(Half2::attributeBoosts),
            Codec.BOOL.optionalFieldOf("is_mini_boss", false).forGetter(Half2::isMiniBoss),
            Codec.BOOL.optionalFieldOf("immortal", false).forGetter(Half2::immortal),
            Codec.STRING.optionalFieldOf("fixed_personality_id").forGetter(Half2::fixedPersonalityId),
            Codec.BOOL.optionalFieldOf("in_random_pool", true).forGetter(Half2::inRandomPool)
    ).apply(instance, Half2::new));

    /**
     * Combine the two MapCodec halves into a single MapCodec that reads from the same flat JSON object,
     * then promote it to a regular Codec via .codec(). Both halves see the full field set; each picks up the keys it knows about.
     */
    public static final Codec<BanditProfile> BODY_CODEC = RecordCodecBuilder
            .<BanditProfile>create(instance -> instance.group(
                    HALF1_CODEC.forGetter(BanditProfile::splitHalf1),
                    HALF2_CODEC.forGetter(BanditProfile::splitHalf2)
            ).apply(instance, BanditProfile::assemble));

    private static BanditProfile assemble(Half1 h1, Half2 h2) {
        return new BanditProfile("",
                Math.max(1, h1.weight()),
                h1.entityClass(), h1.gender(), h1.hasShield(), h1.isArcher(), h1.starLevel(),
                h1.minLevelPercent(), h1.maxLevelPercent(), h1.minLevelAbsolute(), h1.maxLevelAbsolute(),
                h1.entityScale(), h1.overrideName(), h1.skinPreset(),
                h2.magicSchools(), h2.magicSchoolsTagId(),
                h2.explicitSpells(), h2.spellsTagId(), h2.spellsTagPickCount(),
                h2.equipment(),
                h2.attributeBoosts(),
                h2.isMiniBoss(), h2.immortal(), h2.fixedPersonalityId(), h2.inRandomPool());
    }

    private Half1 splitHalf1() {
        return new Half1(weight,
                entityClass, gender, hasShield, isArcher, starLevel,
                minLevelPercent, maxLevelPercent, minLevelAbsolute, maxLevelAbsolute,
                entityScale, overrideName, skinPreset);
    }

    private Half2 splitHalf2() {
        return new Half2(magicSchools, magicSchoolsTagId,
                explicitSpells, spellsTagId, spellsTagPickCount,
                equipment,
                attributeBoosts,
                isMiniBoss, immortal, fixedPersonalityId, inRandomPool);
    }

    /** Returns a copy of this profile with the id filled in (used by the reload listener). */
    public BanditProfile withId(String newId) {
        return new BanditProfile(newId, weight, entityClass, gender, hasShield, isArcher, starLevel,
                minLevelPercent, maxLevelPercent, minLevelAbsolute, maxLevelAbsolute,
                entityScale, overrideName, skinPreset,
                magicSchools, magicSchoolsTagId,
                explicitSpells, spellsTagId, spellsTagPickCount,
                equipment,
                attributeBoosts,
                isMiniBoss, immortal, fixedPersonalityId, inRandomPool);
    }

    // Helper: resolve the magic schools tag key.
    public Optional<TagKey<SchoolType>> magicSchoolsTag() {
        return magicSchoolsTagId.map(loc -> TagKey.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, loc));
    }

    // Helper: resolve the spells tag key.
    public Optional<TagKey<AbstractSpell>> spellsTag() {
        return spellsTagId.map(loc -> TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, loc));
    }

    /**
     * Resolve the equipment map into actual {@link EquipmentSlot}/{@link ItemStack} pairs, dropping any malformed slot
     * names with a warning.
     */
    public Map<EquipmentSlot, ItemStack> resolveEquipment() {
        Map<EquipmentSlot, ItemStack> resolved = new EnumMap<>(EquipmentSlot.class);
        for (Map.Entry<String, ResourceLocation> e : equipment.entrySet()) {
            EquipmentSlot slot = parseSlot(e.getKey());
            if (slot == null) continue;
            Item item = BuiltInRegistries.ITEM.getOptional(e.getValue()).orElse(null);
            if (item == null || item == Items.AIR) continue;
            resolved.put(slot, new ItemStack(item));
        }
        return resolved;
    }

    private static EquipmentSlot parseSlot(String key) {
        if (key == null) return null;
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main_hand" -> EquipmentSlot.MAINHAND;
            case "offhand", "off_hand" -> EquipmentSlot.OFFHAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    // Network serialization

    public static void writeToBuf(FriendlyByteBuf buf, BanditProfile p) {
        buf.writeUtf(p.id);
        buf.writeVarInt(p.weight);

        writeOptString(buf, p.entityClass.map(EntityClass::getName));
        writeOptString(buf, p.gender.map(Gender::getName));
        writeOptBool(buf, p.hasShield);
        writeOptBool(buf, p.isArcher);
        writeOptInt(buf, p.starLevel);

        writeOptFloat(buf, p.minLevelPercent);
        writeOptFloat(buf, p.maxLevelPercent);
        writeOptInt(buf, p.minLevelAbsolute);
        writeOptInt(buf, p.maxLevelAbsolute);

        writeOptFloat(buf, p.entityScale);
        writeOptString(buf, p.overrideName);
        writeOptResLoc(buf, p.skinPreset);

        buf.writeCollection(p.magicSchools, FriendlyByteBuf::writeResourceLocation);
        writeOptResLoc(buf, p.magicSchoolsTagId);

        buf.writeCollection(p.explicitSpells, FriendlyByteBuf::writeResourceLocation);
        writeOptResLoc(buf, p.spellsTagId);
        writeOptInt(buf, p.spellsTagPickCount);

        buf.writeMap(p.equipment, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeResourceLocation);

        buf.writeVarInt(p.attributeBoosts.size());
        for (AttributeBoost ab : p.attributeBoosts) {
            buf.writeResourceLocation(ab.attribute());
            buf.writeResourceLocation(ab.modifier().id());
            buf.writeDouble(ab.modifier().amount());
            buf.writeEnum(ab.modifier().operation());
        }

        buf.writeBoolean(p.isMiniBoss);
        buf.writeBoolean(p.immortal);
        writeOptString(buf, p.fixedPersonalityId);
        buf.writeBoolean(p.inRandomPool);
    }

    public static BanditProfile readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int weight = buf.readVarInt();

        Optional<EntityClass> ec = readOptString(buf).map(s -> EntityClass.valueOf(s.toUpperCase(Locale.ROOT)));
        Optional<Gender> g = readOptString(buf).map(s -> Gender.valueOf(s.toUpperCase(Locale.ROOT)));
        Optional<Boolean> hs = readOptBool(buf);
        Optional<Boolean> ia = readOptBool(buf);
        Optional<Integer> sl = readOptInt(buf);

        Optional<Float> minP = readOptFloat(buf);
        Optional<Float> maxP = readOptFloat(buf);
        Optional<Integer> minA = readOptInt(buf);
        Optional<Integer> maxA = readOptInt(buf);

        Optional<Float> scale = readOptFloat(buf);
        Optional<String> name = readOptString(buf);
        Optional<ResourceLocation> skinPreset = readOptResLoc(buf);

        List<ResourceLocation> schools = new ArrayList<>(buf.readList(FriendlyByteBuf::readResourceLocation));
        Optional<ResourceLocation> schoolsTag = readOptResLoc(buf);

        List<ResourceLocation> spells = new ArrayList<>(buf.readList(FriendlyByteBuf::readResourceLocation));
        Optional<ResourceLocation> spellsTag = readOptResLoc(buf);
        Optional<Integer> spellsCount = readOptInt(buf);

        Map<String, ResourceLocation> equip = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readResourceLocation);

        int boostCount = buf.readVarInt();
        List<AttributeBoost> boosts = new ArrayList<>(boostCount);
        for (int i = 0; i < boostCount; i++) {
            ResourceLocation attrId = buf.readResourceLocation();
            ResourceLocation modId = buf.readResourceLocation();
            double amount = buf.readDouble();
            AttributeModifier.Operation op = buf.readEnum(AttributeModifier.Operation.class);
            boosts.add(new AttributeBoost(attrId, new AttributeModifier(modId, amount, op)));
        }

        boolean miniBoss = buf.readBoolean();
        boolean immortal = buf.readBoolean();
        Optional<String> fpId = readOptString(buf);
        boolean inPool = buf.readBoolean();

        return new BanditProfile(id, weight, ec, g, hs, ia, sl,
                minP, maxP, minA, maxA,
                scale, name, skinPreset,
                schools, schoolsTag,
                spells, spellsTag, spellsCount,
                equip,
                boosts,
                miniBoss, immortal, fpId, inPool);
    }

    // Optional<T> network helpers

    private static void writeOptString(FriendlyByteBuf buf, Optional<String> opt) {
        buf.writeBoolean(opt.isPresent());
        opt.ifPresent(buf::writeUtf);
    }

    private static Optional<String> readOptString(FriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
    }

    private static void writeOptInt(FriendlyByteBuf buf, Optional<Integer> opt) {
        buf.writeBoolean(opt.isPresent());
        opt.ifPresent(buf::writeVarInt);
    }

    private static Optional<Integer> readOptInt(FriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(buf.readVarInt()) : Optional.empty();
    }

    private static void writeOptFloat(FriendlyByteBuf buf, Optional<Float> opt) {
        buf.writeBoolean(opt.isPresent());
        opt.ifPresent(buf::writeFloat);
    }

    private static Optional<Float> readOptFloat(FriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(buf.readFloat()) : Optional.empty();
    }

    private static void writeOptBool(FriendlyByteBuf buf, Optional<Boolean> opt) {
        buf.writeBoolean(opt.isPresent());
        opt.ifPresent(buf::writeBoolean);
    }

    private static Optional<Boolean> readOptBool(FriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(buf.readBoolean()) : Optional.empty();
    }

    private static void writeOptResLoc(FriendlyByteBuf buf, Optional<ResourceLocation> opt) {
        buf.writeBoolean(opt.isPresent());
        opt.ifPresent(buf::writeResourceLocation);
    }

    private static Optional<ResourceLocation> readOptResLoc(FriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(buf.readResourceLocation()) : Optional.empty();
    }
}
