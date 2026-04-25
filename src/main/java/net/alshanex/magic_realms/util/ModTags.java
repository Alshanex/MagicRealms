package net.alshanex.magic_realms.util;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

public class ModTags {
    public static final TagKey<EntityType<?>> BOSSES_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "bosses"));
    public static final TagKey<EntityType<?>> EXCLUSIVE_MERCENARIES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "exclusive_mercenaries"));

    // Fears
    public static final TagKey<EntityType<?>> ALSHANEX_FEARS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "alshanex_fears"));
    public static final TagKey<EntityType<?>> ALIANA_FEARS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "aliana_fears"));
    public static final TagKey<EntityType<?>> CATAS_FEARS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "catas_fears"));
    public static final TagKey<EntityType<?>> AMADEUS_FEARS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "amadeus_fears"));

    public static final TagKey<Item> TAVERNKEEP_SELLS_FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tavernkeep_foods"));
    public static final TagKey<Item> TAVERNKEEP_SELLS_DRINKS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tavernkeep_drinks"));

    public static final TagKey<Item> GEM_ARMOR = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "gem_armor"));

    public static final TagKey<Item> BOWS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "bows"));

    public static TagKey<AbstractSpell> COMMON_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/warrior/common"));
    public static TagKey<AbstractSpell> RARE_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/warrior/rare"));
    public static TagKey<AbstractSpell> LEGENDARY_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/warrior/legendary"));

    public static TagKey<AbstractSpell> COMMON_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/assassin/common"));
    public static TagKey<AbstractSpell> RARE_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/assassin/rare"));
    public static TagKey<AbstractSpell> LEGENDARY_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/assassin/legendary"));

    public static TagKey<AbstractSpell> COMMON_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/archer/common"));
    public static TagKey<AbstractSpell> RARE_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/archer/rare"));
    public static TagKey<AbstractSpell> LEGENDARY_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/archer/legendary"));

    public static TagKey<AbstractSpell> MAGE_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "classes/mage/spells"));

    public static TagKey<AbstractSpell> CATAS_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "catas_spells"));

    public static TagKey<AbstractSpell> CREEPER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "fizzle_casts"));

    public static final TagKey<Biome> HOT_BIOMES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "hot_biomes"));
    public static final TagKey<Biome> COLD_BIOMES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "cold_biomes"));
    public static final TagKey<Item> BOOKS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "books"));

    // Structures
    public static TagKey<Structure> FURLED_MAP_STRUCTURES = create("furled_map_structures");

    private static TagKey<Structure> create(String pName) {
        return TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, pName));
    }

    //Spells
    public static TagKey<AbstractSpell> AOE_ATTACKS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "attack/aoe"));
    public static TagKey<AbstractSpell> SINGLE_TARGET_ATTACKS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "attack/single_target"));
    public static TagKey<AbstractSpell> CLOSE_RANGE_ATTACKS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "attack/close_range"));
    public static TagKey<AbstractSpell> MID_RANGE_ATTACKS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "attack/mid_range"));
    public static TagKey<AbstractSpell> LONG_RANGE_ATTACKS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "attack/long_range"));

    public static TagKey<AbstractSpell> COUNTERATTACK_DEFENSE = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "attack/counterattack"));
    public static TagKey<AbstractSpell> SELF_BUFF_DEFENSE = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "buffing/restoration"));

    public static TagKey<AbstractSpell> APPROACH_MOVEMENT = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "movement/approach"));
    public static TagKey<AbstractSpell> RETREAT_MOVEMENT = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "movement/retreat"));

    public static TagKey<AbstractSpell> DEBUFF_BUFFING = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "buffing/debuff"));
    public static TagKey<AbstractSpell> UNTHREATENED_BUFF_BUFFING = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "buffing/unthreatened"));
    public static TagKey<AbstractSpell> THREATENED_BUFF_BUFFING = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "buffing/threatened"));
    public static TagKey<AbstractSpell> RESTORATION = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "buffing/restoration"));

    public static TagKey<AbstractSpell> BUFF_ALLIES = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "support_mage/buff_allies"));
    public static TagKey<AbstractSpell> HEAL_ALLIES = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "support_mage/heal_allies"));

    public static TagKey<AbstractSpell> SPELL_BLACKLIST = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "blacklisted"));
    public static TagKey<SchoolType> SCHOOL_WHITELIST = createSchoolTag(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "school_whitelist"));

    public static TagKey<SchoolType> createSchoolTag(ResourceLocation name) {
        return new TagKey<SchoolType>(SchoolRegistry.SCHOOL_REGISTRY_KEY, name);
    }

    public static TagKey<AbstractSpell> create(ResourceLocation name) {
        return new TagKey<AbstractSpell>(SpellRegistry.SPELL_REGISTRY_KEY, name);
    }

    public static List<AbstractSpell> filterSpellsByTag(List<AbstractSpell> spells, TagKey<AbstractSpell> tag) {
        var list = new ArrayList<AbstractSpell>();

        for (var spell : spells) {
            SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                if (a.is(tag)) {
                    list.add(spell);
                }
            });
        }

        return list;
    }

    public static List<AbstractSpell> filterAttackSpells(List<AbstractSpell> spells) {
        var list = new ArrayList<AbstractSpell>();

        for (var spell : spells) {
            SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                if (a.is(ModTags.AOE_ATTACKS) || a.is(ModTags.SINGLE_TARGET_ATTACKS)) {
                    list.add(spell);
                }
            });
        }

        return list;
    }

    public static List<AbstractSpell> filterDefenseSpells(List<AbstractSpell> spells) {
        var list = new ArrayList<AbstractSpell>();

        for (var spell : spells) {
            SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                if (a.is(ModTags.COUNTERATTACK_DEFENSE) || a.is(ModTags.SELF_BUFF_DEFENSE)) {
                    list.add(spell);
                }
            });
        }

        return list;
    }

    public static List<AbstractSpell> filterMovementSpells(List<AbstractSpell> spells) {
        var list = new ArrayList<AbstractSpell>();

        for (var spell : spells) {
            SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                if (a.is(ModTags.APPROACH_MOVEMENT) || a.is(ModTags.RETREAT_MOVEMENT)) {
                    list.add(spell);
                }
            });
        }

        return list;
    }

    public static List<AbstractSpell> filterSupportSpells(List<AbstractSpell> spells) {
        var list = new ArrayList<AbstractSpell>();

        for (var spell : spells) {
            SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                if (a.is(ModTags.THREATENED_BUFF_BUFFING) || a.is(ModTags.UNTHREATENED_BUFF_BUFFING) || a.is(ModTags.DEBUFF_BUFFING)) {
                    list.add(spell);
                }
            });
        }

        return list;
    }

    public static boolean isSpellInTag(AbstractSpell spell, TagKey<AbstractSpell> tag) {
        var list = new ArrayList<AbstractSpell>();

        SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
            if (a.is(tag)) {
                list.add(spell);
            }
        });

        return !list.isEmpty();
    }

    public static boolean isSchoolInTag(SchoolType schoolType, TagKey<SchoolType> tag) {
        var list = new ArrayList<SchoolType>();

        SchoolRegistry.REGISTRY.getHolder(schoolType.getId()).ifPresent(a -> {
            if (a.is(tag)) {
                list.add(schoolType);
            }
        });

        return !list.isEmpty();
    }
}
