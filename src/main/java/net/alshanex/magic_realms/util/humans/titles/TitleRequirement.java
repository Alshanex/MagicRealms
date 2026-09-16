package net.alshanex.magic_realms.util.humans.titles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.data.TitleProgressData;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.combat.CombatClass;
import net.alshanex.magic_realms.util.humans.combat.CombatClasses;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Locale;

/**
 * One condition a mercenary must meet to earn a {@link Title}.
 *
 * <p>Deliberately modelled as a single flat record rather than a dispatch codec, so the JSON stays trivially readable
 * and so adding a new condition only means adding an enum constant plus one branch in {@link #currentValue}:
 *
 * <pre>{@code
 * { "type": "kill_entity", "target": "minecraft:zombie", "amount": 250 }
 * { "type": "kill_entity_tag", "target": "minecraft:skeletons", "amount": 100 }
 * { "type": "visit_structure", "target": "minecraft:fortress" }
 * { "type": "contract_minutes", "amount": 600 }
 * { "type": "star_level", "amount": 3 }
 * { "type": "has_title", "target": "magic_realms:veteran" }
 * { "type": "counter", "target": "myaddon.dragons_ridden", "amount": 3 }
 * }</pre>
 *
 * <p>The {@code counter} type is the escape hatch: any counter written into
 * {@link TitleProgressData} by this mod or an addon can gate a title with no code changes at all.
 */
public record TitleRequirement(Type type, String target, long amount) {

    public enum Type {
        /** Raw counter lookup. {@code target} is the counter key. */
        COUNTER,
        /** Kills of one entity type. {@code target} is an entity type id. */
        KILL_ENTITY,
        /** Kills summed across every entity type in a tag. {@code target} is an entity type tag id (no leading '#'). */
        KILL_ENTITY_TAG,
        /** Total kills of any kind. */
        TOTAL_KILLS,
        /** Kills of entities in the mod's boss tag. */
        BOSS_KILLS,
        /** Times the mercenary has stood inside a given structure. {@code target} is a structure id. */
        VISIT_STRUCTURE,
        /** Visits summed across every structure in a tag. {@code target} is a structure tag id. */
        VISIT_STRUCTURE_TAG,
        /** Cumulative minutes under contract. */
        CONTRACT_MINUTES,
        /** Cumulative damage dealt. */
        DAMAGE_DEALT,
        /** Cumulative damage taken. */
        DAMAGE_TAKEN,
        /** Minimum star level. {@code amount} is the minimum. */
        STAR_LEVEL,
        /** Class gate. {@code target} is "mage", "warrior" or "rogue" (case-insensitive). */
        ENTITY_CLASS,
        /** Requires another title first, enabling title chains. {@code target} is a title id. */
        HAS_TITLE;

        public static final Codec<Type> CODEC = Codec.STRING.comapFlatMap(
                s -> {
                    try {
                        return DataResult.success(Type.valueOf(s.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException e) {
                        return DataResult.error(() -> "Unknown title requirement type: " + s);
                    }
                },
                t -> t.name().toLowerCase(Locale.ROOT)
        );
    }

    public static final Codec<TitleRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Type.CODEC.fieldOf("type").forGetter(TitleRequirement::type),
            Codec.STRING.optionalFieldOf("target", "").forGetter(TitleRequirement::target),
            Codec.LONG.optionalFieldOf("amount", 1L).forGetter(TitleRequirement::amount)
    ).apply(instance, TitleRequirement::new));

    /**
     * The mercenary's current progress towards this requirement. Comparable against {@link #amount}.
     * Boolean-ish requirements return 1 when satisfied and 0 otherwise so progress bars still work.
     */
    public long currentValue(AbstractMercenaryEntity entity, TitleProgressData data) {
        return switch (type) {
            case COUNTER -> data.getCounter(target);
            case KILL_ENTITY -> data.getCounter(TitleKeys.killEntity(normalizedTarget()));
            case KILL_ENTITY_TAG -> sumEntityTag(data);
            case TOTAL_KILLS -> data.getCounter(TitleKeys.KILLS_TOTAL);
            case BOSS_KILLS -> data.getCounter(TitleKeys.KILLS_BOSS);
            case VISIT_STRUCTURE -> data.getCounter(TitleKeys.structure(normalizedTarget()));
            case VISIT_STRUCTURE_TAG -> sumStructureTag(entity, data);
            case CONTRACT_MINUTES -> data.getCounter(TitleKeys.CONTRACT_MINUTES);
            case DAMAGE_DEALT -> data.getCounter(TitleKeys.DAMAGE_DEALT);
            case DAMAGE_TAKEN -> data.getCounter(TitleKeys.DAMAGE_TAKEN);
            case STAR_LEVEL -> entity.getStarLevel();
            case ENTITY_CLASS -> {
                CombatClass required = CombatClasses.resolve(normalizedTarget());
                yield required != null && required.id().equals(entity.getCombatClassId()) ? 1 : 0;
            }
            case HAS_TITLE -> {
                ResourceLocation id = tryParse(normalizedTarget());
                yield id != null && data.hasTitle(id) ? 1L : 0L;
            }
        };
    }

    public boolean isSatisfied(AbstractMercenaryEntity entity, TitleProgressData data) {
        return currentValue(entity, data) >= Math.max(1L, amount);
    }

    /** 0.0 - 1.0, for progress display in the contract screen. */
    public float progressFraction(AbstractMercenaryEntity entity, TitleProgressData data) {
        long goal = Math.max(1L, amount);
        long current = Math.min(currentValue(entity, data), goal);
        return (float) current / (float) goal;
    }

    /** Strips a leading '#' so authors can write either {@code "minecraft:skeletons"} or {@code "#minecraft:skeletons"}. */
    private String normalizedTarget() {
        if (target == null) return "";
        return target.startsWith("#") ? target.substring(1) : target;
    }

    private long sumEntityTag(TitleProgressData data) {
        ResourceLocation tagId = tryParse(normalizedTarget());
        if (tagId == null) return 0L;
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);

        long total = 0L;
        var named = BuiltInRegistries.ENTITY_TYPE.getTag(tag);
        if (named.isEmpty()) return 0L;
        for (Holder<EntityType<?>> holder : named.get()) {
            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(holder.value());
            if (typeId != null) total += data.getCounter(TitleKeys.killEntity(typeId));
        }
        return total;
    }

    private long sumStructureTag(AbstractMercenaryEntity entity, TitleProgressData data) {
        ResourceLocation tagId = tryParse(normalizedTarget());
        if (tagId == null) return 0L;
        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagId);

        Registry<Structure> registry = entity.level().registryAccess().registryOrThrow(Registries.STRUCTURE);
        long total = 0L;
        var named = registry.getTag(tag);
        if (named.isEmpty()) return 0L;
        for (Holder<Structure> holder : named.get()) {
            ResourceLocation structureId = registry.getKey(holder.value());
            if (structureId != null) total += data.getCounter(TitleKeys.structure(structureId));
        }
        return total;
    }

    private static ResourceLocation tryParse(String s) {
        if (s == null || s.isEmpty()) return null;
        return ResourceLocation.tryParse(s);
    }

    // Network serialization

    public static void writeToBuf(FriendlyByteBuf buf, TitleRequirement req) {
        buf.writeEnum(req.type);
        buf.writeUtf(req.target == null ? "" : req.target);
        buf.writeVarLong(req.amount);
    }

    public static TitleRequirement readFromBuf(FriendlyByteBuf buf) {
        Type type = buf.readEnum(Type.class);
        String target = buf.readUtf();
        long amount = buf.readVarLong();
        return new TitleRequirement(type, target, amount);
    }
}
