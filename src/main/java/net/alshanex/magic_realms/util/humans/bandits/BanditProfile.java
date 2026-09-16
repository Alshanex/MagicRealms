package net.alshanex.magic_realms.util.humans.bandits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.combat.CombatClass;
import net.alshanex.magic_realms.util.humans.combat.CombatClasses;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.*;

/**
 * Datapack-loaded bandit profile. Each file at {@code data/magic_realms/mercenaries/bandit_profiles/<name>.json} becomes one of these,
 * with its id taken from the file path (e.g. {@code "magic_realms:giant_warrior"}).
 *
 * <p>A profile is a "stamp" applied to a {@code HostileRandomHumanEntity} during initialization. Each
 * field is optional: if omitted, the entity falls back to the random roll used by an unprofiled bandit.
 *
 * <p>Difficulty is expressed through {@code star_level}, {@code attribute_boosts}, {@code equipment} and
 * {@code titles} rather than through a level range. The old {@code min_level_*} / {@code max_level_*} fields are
 * gone along with the leveling system - a profile that wants a tougher bandit should grant it titles whose rewards
 * do the work, which also means the buff is visible to the player rather than an invisible stat multiplier.
 *
 * <p>{@code titles} is a list of title ids (from {@code mercenaries/titles/}) granted outright at spawn, bypassing
 * their normal requirements. All of their rewards - attributes, passive effects, on-hit procs, damage scaling -
 * apply exactly as they would for an earned title. {@code displayed_title} optionally pins which one shows above
 * the bandit's name; if omitted, the highest-priority non-hidden granted title is used automatically.
 *
 * <p>For mages, three spell-selection modes are available, in priority order:
 * <ol>
 *     <li>{@code explicit_spells} — exact spell ids inscribed onto the entity.</li>
 *     <li>{@code spells_tag} (+ optional {@code spells_tag_pick_count}) — pull from a tag, optionally picking N at random.</li>
 *     <li>Fall through to the regular {@code SpellListGenerator}.</li>
 * </ol>
 *
 * <p>{@code attribute_boosts} are flat modifiers applied <em>after</em> all class attribute math runs, so they
 * stack cleanly on top of regular stats — useful for "boss" profiles that double health or buff damage.
 *
 * <h3>Combat class</h3>
 * <p>Bare names are namespaced into {@code magic_realms} automatically, so {@code "mage"}, {@code "MAGE"} and
 * {@code "magic_realms:mage"} all resolve to the same class and every pre-existing profile JSON keeps working
 * unchanged. Addon or datapack-added classes can be referenced with their full id, e.g. {@code "magic_realms:support_mage"} or
 * {@code "othermod:necromancer"}.
 */
public record BanditProfile(
        String id,
        int weight,

        // Class & build
        Optional<ResourceLocation> combatClassId,
        Optional<Gender> gender,
        Optional<Boolean> hasShield,
        Optional<Boolean> isArcher,
        Optional<Integer> starLevel,

        // Titles granted at spawn (replaces the old level configuration)
        List<ResourceLocation> titles,
        Optional<ResourceLocation> displayedTitle,

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

        // Override loot table (resolved against Registries.LOOT_TABLE)
        Optional<ResourceLocation> lootTable,

        // Flags
        boolean isMiniBoss,
        boolean immortal,
        Optional<String> fixedPersonalityId,
        boolean inRandomPool
) {

    public static final BanditProfile EMPTY = new BanditProfile(
            "", 1,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            List.of(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(),
            List.of(), Optional.empty(),
            List.of(), Optional.empty(), Optional.empty(),
            Map.of(),
            List.of(),
            Optional.empty(),
            false, false, Optional.empty(), true
    );

    /**
     * Attribute boost: a target attribute id plus an {@link AttributeModifier}. The attribute is stored as a {@link ResourceLocation} rather
     * than a {@code Holder<Attribute>} because vanilla attributes ({@code minecraft:max_health}, etc.) live in the datapack registry and aren't
     * resolvable through {@code BuiltInRegistries.ATTRIBUTE} at codec-parse time. The applier resolves the id against the
     * entity's registries when applying the boost.
     */
    public record AttributeBoost(ResourceLocation attribute, AttributeModifier modifier) {
        public static final Codec<AttributeBoost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("attribute").forGetter(AttributeBoost::attribute),
                AttributeModifier.MAP_CODEC.forGetter(AttributeBoost::modifier)
        ).apply(instance, AttributeBoost::new));
    }

    /**
     * Codec for a combat class id.
     */
    private static final Codec<ResourceLocation> COMBAT_CLASS_ID_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                String raw = s.trim().toLowerCase(Locale.ROOT);
                if (raw.isEmpty()) {
                    return DataResult.error(() -> "Empty entity_class");
                }
                ResourceLocation parsed = raw.indexOf(':') >= 0
                        ? ResourceLocation.tryParse(raw)
                        : ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, raw);
                return parsed == null
                        ? DataResult.error(() -> "Malformed entity_class: " + s)
                        : DataResult.success(parsed);
            },
            ResourceLocation::toString
    );

    // Codec for Gender
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

    /** First half: identity, build, titles, visuals. */
    private record Half1(
            int weight,
            Optional<ResourceLocation> combatClassId,
            Optional<Gender> gender,
            Optional<Boolean> hasShield,
            Optional<Boolean> isArcher,
            Optional<Integer> starLevel,
            List<ResourceLocation> titles,
            Optional<ResourceLocation> displayedTitle,
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
            Optional<ResourceLocation> lootTable,
            boolean isMiniBoss,
            boolean immortal,
            Optional<String> fixedPersonalityId,
            boolean inRandomPool
    ) {}

    private static final MapCodec<Half1> HALF1_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("weight", 1).forGetter(Half1::weight),
            COMBAT_CLASS_ID_CODEC.optionalFieldOf("entity_class").forGetter(Half1::combatClassId),
            GENDER_CODEC.optionalFieldOf("gender").forGetter(Half1::gender),
            Codec.BOOL.optionalFieldOf("has_shield").forGetter(Half1::hasShield),
            Codec.BOOL.optionalFieldOf("is_archer").forGetter(Half1::isArcher),
            Codec.intRange(1, 3).optionalFieldOf("star_level").forGetter(Half1::starLevel),
            ResourceLocation.CODEC.listOf().optionalFieldOf("titles", List.of()).forGetter(Half1::titles),
            ResourceLocation.CODEC.optionalFieldOf("displayed_title").forGetter(Half1::displayedTitle),
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
            ResourceLocation.CODEC.optionalFieldOf("loot_table").forGetter(Half2::lootTable),
            Codec.BOOL.optionalFieldOf("is_mini_boss", false).forGetter(Half2::isMiniBoss),
            Codec.BOOL.optionalFieldOf("immortal", false).forGetter(Half2::immortal),
            Codec.STRING.optionalFieldOf("fixed_personality_id").forGetter(Half2::fixedPersonalityId),
            Codec.BOOL.optionalFieldOf("in_random_pool", true).forGetter(Half2::inRandomPool)
    ).apply(instance, Half2::new));

    /**
     * Combine the two MapCodec halves into a single MapCodec that reads from the same flat JSON object, then promote it to a regular Codec via .codec().
     * Both halves see the full field set; each picks up the keys it knows about.
     */
    public static final Codec<BanditProfile> BODY_CODEC = RecordCodecBuilder
            .<BanditProfile>create(instance -> instance.group(
                    HALF1_CODEC.forGetter(BanditProfile::splitHalf1),
                    HALF2_CODEC.forGetter(BanditProfile::splitHalf2)
            ).apply(instance, BanditProfile::assemble));

    private static BanditProfile assemble(Half1 h1, Half2 h2) {
        return new BanditProfile("",
                Math.max(1, h1.weight()),
                h1.combatClassId(), h1.gender(), h1.hasShield(), h1.isArcher(), h1.starLevel(),
                h1.titles(), h1.displayedTitle(),
                h1.entityScale(), h1.overrideName(), h1.skinPreset(),
                h2.magicSchools(), h2.magicSchoolsTagId(),
                h2.explicitSpells(), h2.spellsTagId(), h2.spellsTagPickCount(),
                h2.equipment(),
                h2.attributeBoosts(),
                h2.lootTable(),
                h2.isMiniBoss(), h2.immortal(), h2.fixedPersonalityId(), h2.inRandomPool());
    }

    private Half1 splitHalf1() {
        return new Half1(weight,
                combatClassId, gender, hasShield, isArcher, starLevel,
                titles, displayedTitle,
                entityScale, overrideName, skinPreset);
    }

    private Half2 splitHalf2() {
        return new Half2(magicSchools, magicSchoolsTagId,
                explicitSpells, spellsTagId, spellsTagPickCount,
                equipment,
                attributeBoosts,
                lootTable,
                isMiniBoss, immortal, fixedPersonalityId, inRandomPool);
    }

    /** Returns a copy of this profile with the id filled in (used by the reload listener). */
    public BanditProfile withId(String newId) {
        return new BanditProfile(newId, weight, combatClassId, gender, hasShield, isArcher, starLevel,
                titles, displayedTitle,
                entityScale, overrideName, skinPreset,
                magicSchools, magicSchoolsTagId,
                explicitSpells, spellsTagId, spellsTagPickCount,
                equipment,
                attributeBoosts,
                lootTable,
                isMiniBoss, immortal, fixedPersonalityId, inRandomPool);
    }

    // Combat class resolution

    /**
     * Resolve this profile's combat class against the {@link CombatClasses} registry.
     */
    public Optional<CombatClass> combatClass() {
        if (combatClassId.isEmpty()) return Optional.empty();

        ResourceLocation classId = combatClassId.get();
        CombatClass resolved = CombatClasses.get(classId);
        if (resolved == null) {
            MagicRealms.LOGGER.warn(
                    "Bandit profile '{}' references unknown combat class '{}'; falling back to a random class",
                    id, classId);
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    /** True if this profile pins a combat class, regardless of whether that class is currently registered. */
    public boolean hasCombatClass() {
        return combatClassId.isPresent();
    }

    /** True if this profile grants any titles at spawn. */
    public boolean hasTitles() {
        return !titles.isEmpty();
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
     * Resolve the equipment map into actual {@link EquipmentSlot}/{@link ItemStack} pairs, dropping any malformed slot names with a warning.
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

        writeOptString(buf, p.combatClassId.map(ResourceLocation::toString));
        writeOptString(buf, p.gender.map(Gender::getName));
        writeOptBool(buf, p.hasShield);
        writeOptBool(buf, p.isArcher);
        writeOptInt(buf, p.starLevel);

        buf.writeCollection(p.titles, FriendlyByteBuf::writeResourceLocation);
        writeOptResLoc(buf, p.displayedTitle);

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

        writeOptResLoc(buf, p.lootTable);

        buf.writeBoolean(p.isMiniBoss);
        buf.writeBoolean(p.immortal);
        writeOptString(buf, p.fixedPersonalityId);
        buf.writeBoolean(p.inRandomPool);
    }

    public static BanditProfile readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int weight = buf.readVarInt();

        // Written as a full "namespace:path" string by writeToBuf, so a plain tryParse is correct here.
        // flatMap drops a malformed value instead of propagating null into the Optional.
        Optional<ResourceLocation> classId = readOptString(buf)
                .flatMap(s -> Optional.ofNullable(ResourceLocation.tryParse(s)));
        Optional<Gender> g = readOptString(buf).map(s -> Gender.valueOf(s.toUpperCase(Locale.ROOT)));
        Optional<Boolean> hs = readOptBool(buf);
        Optional<Boolean> ia = readOptBool(buf);
        Optional<Integer> sl = readOptInt(buf);

        List<ResourceLocation> titles = new ArrayList<>(buf.readList(FriendlyByteBuf::readResourceLocation));
        Optional<ResourceLocation> displayedTitle = readOptResLoc(buf);

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

        Optional<ResourceLocation> lootTable = readOptResLoc(buf);

        boolean miniBoss = buf.readBoolean();
        boolean immortal = buf.readBoolean();
        Optional<String> fpId = readOptString(buf);
        boolean inPool = buf.readBoolean();

        return new BanditProfile(id, weight, classId, g, hs, ia, sl,
                titles, displayedTitle,
                scale, name, skinPreset,
                schools, schoolsTag,
                spells, spellsTag, spellsCount,
                equip,
                boosts,
                lootTable,
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

    public Optional<ResourceKey<LootTable>> lootTableKey() {
        return lootTable.map(loc -> ResourceKey.create(Registries.LOOT_TABLE, loc));
    }
}