package net.alshanex.magic_realms.util.humans.titles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a {@link Title} grants once earned. All of it is optional - a purely cosmetic title just omits every field.
 *
 * <p>Example JSON body:
 * <pre>{@code
 * "rewards": {
 *   "attribute_modifiers": [
 *     { "attribute": "minecraft:generic.attack_damage", "id": "magic_realms:dragonslayer", "amount": 3.0, "operation": "add_value" },
 *     { "attribute": "minecraft:generic.max_health",    "id": "magic_realms:dragonslayer_hp", "amount": 0.15, "operation": "add_multiplied_base" }
 *   ],
 *   "passive_effects": [ { "effect": "minecraft:fire_resistance" } ],
 *   "on_hit_effects":  [ { "effect": "minecraft:wither", "duration": 60, "amplifier": 1, "chance": 0.25 } ],
 *   "bonus_damage": 1.5,
 *   "damage_bonus_vs": [ { "entity_tag": "minecraft:undead", "multiplier": 1.5 } ],
 *   "weapon_bonus":    [ { "item_tag": "c:axes", "multiplier": 1.3, "bonus": 1.0 } ],
 *   "incoming_damage_multiplier": 0.9,
 *   "natural_regen": true
 * }
 * }</pre>
 */
public record TitleRewards(
        List<AttributeEntry> attributeModifiers,
        List<EffectSpec> passiveEffects,
        List<OnHitEffect> onHitEffects,
        double bonusDamage,
        List<DamageBonus> damageBonusVs,
        List<WeaponBonus> weaponBonuses,
        double incomingDamageMultiplier,
        boolean naturalRegen
) {

    public static final TitleRewards NONE =
            new TitleRewards(List.of(), List.of(), List.of(), 0.0, List.of(), List.of(), 1.0, false);

    public static final Codec<TitleRewards> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AttributeEntry.CODEC.listOf().optionalFieldOf("attribute_modifiers", List.of()).forGetter(TitleRewards::attributeModifiers),
            EffectSpec.CODEC.listOf().optionalFieldOf("passive_effects", List.of()).forGetter(TitleRewards::passiveEffects),
            OnHitEffect.CODEC.listOf().optionalFieldOf("on_hit_effects", List.of()).forGetter(TitleRewards::onHitEffects),
            Codec.DOUBLE.optionalFieldOf("bonus_damage", 0.0).forGetter(TitleRewards::bonusDamage),
            DamageBonus.CODEC.listOf().optionalFieldOf("damage_bonus_vs", List.of()).forGetter(TitleRewards::damageBonusVs),
            WeaponBonus.CODEC.listOf().optionalFieldOf("weapon_bonus", List.of()).forGetter(TitleRewards::weaponBonuses),
            Codec.DOUBLE.optionalFieldOf("incoming_damage_multiplier", 1.0).forGetter(TitleRewards::incomingDamageMultiplier),
            Codec.BOOL.optionalFieldOf("natural_regen", false).forGetter(TitleRewards::naturalRegen)
    ).apply(instance, TitleRewards::new));

    public boolean isEmpty() {
        return attributeModifiers.isEmpty()
                && passiveEffects.isEmpty()
                && onHitEffects.isEmpty()
                && bonusDamage == 0.0
                && damageBonusVs.isEmpty()
                && weaponBonuses.isEmpty()
                && incomingDamageMultiplier == 1.0
                && !naturalRegen;
    }

    /**
     * Attribute + modifier pair. Mirrors {@code ArchetypeInteraction.Entry} - Mojang's {@link AttributeModifier#MAP_CODEC}
     * only carries id/amount/operation, so the target attribute is wrapped alongside it.
     *
     * <p>The {@code id} in the JSON is only an authoring label; {@code TitleEffectTickHandler} derives a stable
     * per-title modifier id so modifiers can be cleanly diffed and removed.
     */
    public record AttributeEntry(Holder<Attribute> attribute, AttributeModifier modifier) {
        public static final Codec<AttributeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(AttributeEntry::attribute),
                AttributeModifier.MAP_CODEC.forGetter(AttributeEntry::modifier)
        ).apply(instance, AttributeEntry::new));
    }

    /** A mob effect kept permanently refreshed on the mercenary while the title is held. */
    public record EffectSpec(Holder<MobEffect> effect, int amplifier, boolean showParticles) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(EffectSpec::effect),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectSpec::amplifier),
                Codec.BOOL.optionalFieldOf("show_particles", false).forGetter(EffectSpec::showParticles)
        ).apply(instance, EffectSpec::new));
    }

    /** A mob effect rolled onto the victim whenever the mercenary lands a hit. */
    public record OnHitEffect(Holder<MobEffect> effect, int duration, int amplifier, float chance) {
        public static final Codec<OnHitEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(OnHitEffect::effect),
                Codec.INT.optionalFieldOf("duration", 60).forGetter(OnHitEffect::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(OnHitEffect::amplifier),
                Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(OnHitEffect::chance)
        ).apply(instance, OnHitEffect::new));
    }

    /** Bane-of-arthropods style scaling against a family of mobs. */
    public record DamageBonus(String entityTag, double multiplier) {
        public static final Codec<DamageBonus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("entity_tag").forGetter(DamageBonus::entityTag),
                Codec.DOUBLE.optionalFieldOf("multiplier", 1.0).forGetter(DamageBonus::multiplier)
        ).apply(instance, DamageBonus::new));

        /** Tag id without any leading '#'. */
        public String normalizedTag() {
            return entityTag != null && entityTag.startsWith("#") ? entityTag.substring(1) : entityTag;
        }
    }

    /**
     * Damage scaling tied to what the mercenary is <em>holding</em>, rather than to what they're hitting.
     * Lets a title reward a fighting style - an "Axemaster" hits harder with axes, a "Bowyer" with bows.
     *
     * <p>Set {@code item} for a single item, {@code item_tag} for items in a tag, or both (matching either is enough).
     * A leading {@code '#'} on the tag is accepted and ignored, for consistency with {@code damage_bonus_vs}.
     * If neither field is set the bonus never applies, which is the safe failure mode for a typo.
     *
     * <p>{@code bonus} is added flat and {@code multiplier} scales the total, combining with {@code bonus_damage}
     * and {@code damage_bonus_vs} in one formula. Matching is against the mainhand stack, so this covers ranged
     * attacks too: an arrow's damage is attributed to the shooter, who is still holding the bow.
     */
    public record WeaponBonus(String item, String itemTag, double multiplier, double bonus) {
        public static final Codec<WeaponBonus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("item", "").forGetter(WeaponBonus::item),
                Codec.STRING.optionalFieldOf("item_tag", "").forGetter(WeaponBonus::itemTag),
                Codec.DOUBLE.optionalFieldOf("multiplier", 1.0).forGetter(WeaponBonus::multiplier),
                Codec.DOUBLE.optionalFieldOf("bonus", 0.0).forGetter(WeaponBonus::bonus)
        ).apply(instance, WeaponBonus::new));

        /** True when the given stack satisfies either the item id or the item tag. */
        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;

            if (item != null && !item.isEmpty()) {
                ResourceLocation id = ResourceLocation.tryParse(item);
                if (id != null) {
                    Item held = stack.getItem();
                    if (id.equals(BuiltInRegistries.ITEM.getKey(held))) return true;
                }
            }

            if (itemTag != null && !itemTag.isEmpty()) {
                String raw = itemTag.startsWith("#") ? itemTag.substring(1) : itemTag;
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id != null && stack.is(TagKey.create(Registries.ITEM, id))) return true;
            }

            return false;
        }
    }

    // Network serialization

    public static void writeToBuf(FriendlyByteBuf buf, TitleRewards r) {
        buf.writeVarInt(r.attributeModifiers.size());
        for (AttributeEntry e : r.attributeModifiers) {
            ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(e.attribute().value());
            buf.writeResourceLocation(attrId);
            buf.writeResourceLocation(e.modifier().id());
            buf.writeDouble(e.modifier().amount());
            buf.writeEnum(e.modifier().operation());
        }

        buf.writeVarInt(r.passiveEffects.size());
        for (EffectSpec e : r.passiveEffects) {
            buf.writeResourceLocation(BuiltInRegistries.MOB_EFFECT.getKey(e.effect().value()));
            buf.writeVarInt(e.amplifier());
            buf.writeBoolean(e.showParticles());
        }

        buf.writeVarInt(r.onHitEffects.size());
        for (OnHitEffect e : r.onHitEffects) {
            buf.writeResourceLocation(BuiltInRegistries.MOB_EFFECT.getKey(e.effect().value()));
            buf.writeVarInt(e.duration());
            buf.writeVarInt(e.amplifier());
            buf.writeFloat(e.chance());
        }

        buf.writeDouble(r.bonusDamage);

        buf.writeVarInt(r.damageBonusVs.size());
        for (DamageBonus d : r.damageBonusVs) {
            buf.writeUtf(d.entityTag() == null ? "" : d.entityTag());
            buf.writeDouble(d.multiplier());
        }

        buf.writeVarInt(r.weaponBonuses.size());
        for (WeaponBonus w : r.weaponBonuses) {
            buf.writeUtf(w.item() == null ? "" : w.item());
            buf.writeUtf(w.itemTag() == null ? "" : w.itemTag());
            buf.writeDouble(w.multiplier());
            buf.writeDouble(w.bonus());
        }

        buf.writeDouble(r.incomingDamageMultiplier);
        buf.writeBoolean(r.naturalRegen);
    }

    public static TitleRewards readFromBuf(FriendlyByteBuf buf) {
        int attrCount = buf.readVarInt();
        List<AttributeEntry> attrs = new ArrayList<>(attrCount);
        for (int i = 0; i < attrCount; i++) {
            ResourceLocation attrId = buf.readResourceLocation();
            ResourceLocation modId = buf.readResourceLocation();
            double amount = buf.readDouble();
            AttributeModifier.Operation op = buf.readEnum(AttributeModifier.Operation.class);
            Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
            // Silently drop attributes this side doesn't know about rather than failing the whole packet.
            if (holder != null) attrs.add(new AttributeEntry(holder, new AttributeModifier(modId, amount, op)));
        }

        int passiveCount = buf.readVarInt();
        List<EffectSpec> passives = new ArrayList<>(passiveCount);
        for (int i = 0; i < passiveCount; i++) {
            ResourceLocation effId = buf.readResourceLocation();
            int amplifier = buf.readVarInt();
            boolean particles = buf.readBoolean();
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.getHolder(effId).orElse(null);
            if (holder != null) passives.add(new EffectSpec(holder, amplifier, particles));
        }

        int onHitCount = buf.readVarInt();
        List<OnHitEffect> onHits = new ArrayList<>(onHitCount);
        for (int i = 0; i < onHitCount; i++) {
            ResourceLocation effId = buf.readResourceLocation();
            int duration = buf.readVarInt();
            int amplifier = buf.readVarInt();
            float chance = buf.readFloat();
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.getHolder(effId).orElse(null);
            if (holder != null) onHits.add(new OnHitEffect(holder, duration, amplifier, chance));
        }

        double bonusDamage = buf.readDouble();

        int bonusCount = buf.readVarInt();
        List<DamageBonus> bonuses = new ArrayList<>(bonusCount);
        for (int i = 0; i < bonusCount; i++) {
            bonuses.add(new DamageBonus(buf.readUtf(), buf.readDouble()));
        }

        int weaponCount = buf.readVarInt();
        List<WeaponBonus> weapons = new ArrayList<>(weaponCount);
        for (int i = 0; i < weaponCount; i++) {
            String item = buf.readUtf();
            String itemTag = buf.readUtf();
            double multiplier = buf.readDouble();
            double flat = buf.readDouble();
            weapons.add(new WeaponBonus(item, itemTag, multiplier, flat));
        }

        double incoming = buf.readDouble();
        boolean regen = buf.readBoolean();

        return new TitleRewards(attrs, passives, onHits, bonusDamage, bonuses, weapons, incoming, regen);
    }
}