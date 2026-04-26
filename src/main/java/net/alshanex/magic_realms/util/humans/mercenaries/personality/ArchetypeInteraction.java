package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Datapack-loaded archetype interaction. Each file at {@code data/<namespace>/archetype_interactions/<n>.json}
 * defines what happens when two contracted mercenaries with the given pair of archetypes are within range of each
 * other while both contracted by the same player.
 *
 * <p>The pair is unordered: an interaction with {@code archetype_a = "stoic"} and {@code archetype_b = "jovial"}
 * matches both a stoic-near-jovial pair and a jovial-near-stoic pair. The {@link #matches(String, String)} helper
 * encodes this.
 *
 * <p>{@link #modifiers} is a list of attribute modifiers that get applied to <em>both</em> mercenaries while the
 * interaction is active. Each modifier is paired with the attribute it targets via {@link Entry}. The modifier id
 * inside each entry is purely an authoring label - the runtime tick handler derives stable per-mercenary modifier
 * ResourceLocations from the interaction id and entry index so they can be cleanly replaced/removed each cycle.
 *
 * <p>{@code kind} is an optional tag (e.g. {@code "rival"} or {@code "friend"}) that legacy banter code can read
 * to pick a chat line tone. It's purely informational - the modifier list is the source of truth for stat effects.
 */
public record ArchetypeInteraction(
        String id,
        String archetypeA,
        String archetypeB,
        List<Entry> modifiers,
        double radius,
        String kind
) {
    /** Attribute + modifier pair. Mojang's {@link AttributeModifier#MAP_CODEC} only stores id/amount/operation, so we wrap it with the target attribute id. */
    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(Entry::attribute),
                AttributeModifier.MAP_CODEC.forGetter(Entry::modifier)
        ).apply(instance, Entry::new));
    }

    /** Default search radius (in blocks) for "two contracted mercs near each other" if the JSON omits the field. */
    public static final double DEFAULT_RADIUS = 12.0;

    /** Codec for the JSON body. The {@code id} field is filled in by the reload listener from the file path. */
    public static final Codec<ArchetypeInteraction> BODY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("archetype_a").forGetter(ArchetypeInteraction::archetypeA),
            Codec.STRING.fieldOf("archetype_b").forGetter(ArchetypeInteraction::archetypeB),
            Entry.CODEC.listOf().optionalFieldOf("attribute_modifiers", List.of()).forGetter(ArchetypeInteraction::modifiers),
            Codec.DOUBLE.optionalFieldOf("radius", DEFAULT_RADIUS).forGetter(ArchetypeInteraction::radius),
            Codec.STRING.optionalFieldOf("kind", "").forGetter(ArchetypeInteraction::kind)
    ).apply(instance, (a, b, mods, r, k) -> new ArchetypeInteraction("", a, b, mods, r, k)));

    /** Returns a copy of this interaction with the id filled in (used by the reload listener). */
    public ArchetypeInteraction withId(String newId) {
        return new ArchetypeInteraction(newId, archetypeA, archetypeB, modifiers, radius, kind);
    }

    /**
     * Returns true if this interaction applies to the given pair of archetype ids in either order.
     * Self-pairs (archetype_a == archetype_b) match only when both inputs equal that id.
     */
    public boolean matches(String idA, String idB) {
        if (idA == null || idB == null) return false;
        return (archetypeA.equalsIgnoreCase(idA) && archetypeB.equalsIgnoreCase(idB))
                || (archetypeA.equalsIgnoreCase(idB) && archetypeB.equalsIgnoreCase(idA));
    }

    public boolean isRival() { return "rival".equalsIgnoreCase(kind); }
    public boolean isFriendly() { return "friend".equalsIgnoreCase(kind); }

    // Network serialization

    public static void writeToBuf(FriendlyByteBuf buf, ArchetypeInteraction i) {
        buf.writeUtf(i.id);
        buf.writeUtf(i.archetypeA);
        buf.writeUtf(i.archetypeB);
        buf.writeVarInt(i.modifiers.size());
        for (Entry e : i.modifiers) {
            ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(e.attribute().value());
            buf.writeResourceLocation(attrId);
            buf.writeResourceLocation(e.modifier().id());
            buf.writeDouble(e.modifier().amount());
            buf.writeEnum(e.modifier().operation());
        }
        buf.writeDouble(i.radius);
        buf.writeUtf(i.kind);
    }

    public static ArchetypeInteraction readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String a = buf.readUtf();
        String b = buf.readUtf();
        int count = buf.readVarInt();
        List<Entry> mods = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation attrId = buf.readResourceLocation();
            Holder<Attribute> attrHolder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
            ResourceLocation modId = buf.readResourceLocation();
            double amount = buf.readDouble();
            AttributeModifier.Operation op = buf.readEnum(AttributeModifier.Operation.class);
            if (attrHolder == null) continue; // silently drop unknown attributes after a server-side reload mismatch
            mods.add(new Entry(attrHolder, new AttributeModifier(modId, amount, op)));
        }
        double radius = buf.readDouble();
        String kind = buf.readUtf();
        return new ArchetypeInteraction(id, a, b, mods, radius, kind);
    }
}
