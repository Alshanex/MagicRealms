package net.alshanex.magic_realms.util;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

public class ModTags {
    public static final TagKey<EntityType<?>> BOSSES_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "bosses"));

    public static final TagKey<Item> TAVERNKEEP_SELLS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tavernkeep_sells"));

    public static TagKey<AbstractSpell> COMMON_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "common_warrior_spells"));
    public static TagKey<AbstractSpell> RARE_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "rare_warrior_spells"));
    public static TagKey<AbstractSpell> LEGENDARY_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "legendary_warrior_spells"));

    public static TagKey<AbstractSpell> COMMON_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "common_assassin_spells"));
    public static TagKey<AbstractSpell> RARE_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "rare_assassin_spells"));
    public static TagKey<AbstractSpell> LEGENDARY_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "legendary_assassin_spells"));

    public static TagKey<AbstractSpell> COMMON_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "common_archer_spells"));
    public static TagKey<AbstractSpell> RARE_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "rare_archer_spells"));
    public static TagKey<AbstractSpell> LEGENDARY_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "common_archer_spells"));

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

    public static TagKey<AbstractSpell> ATTACK_BACK_DEFENSE = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "defense/attack_back"));
    public static TagKey<AbstractSpell> SELF_BUFF_DEFENSE = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "defense/self_buff"));

    public static TagKey<AbstractSpell> CLOSE_DISTANCE_MOVEMENT = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "movement/close_distance"));
    public static TagKey<AbstractSpell> ESCAPE_MOVEMENT = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "movement/escape"));

    public static TagKey<AbstractSpell> DEBUFF_BUFFING = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "buffing/debuff"));
    public static TagKey<AbstractSpell> SAFE_BUFF_BUFFING = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "buffing/safe_buff"));
    public static TagKey<AbstractSpell> UNSAFE_BUFF_BUFFING = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "buffing/unsafe_buff"));

    public static TagKey<AbstractSpell> SPELL_BLACKLIST = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "blacklisted"));

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
                if (a.is(ModTags.ATTACK_BACK_DEFENSE) || a.is(ModTags.SELF_BUFF_DEFENSE)) {
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
                if (a.is(ModTags.CLOSE_DISTANCE_MOVEMENT) || a.is(ModTags.ESCAPE_MOVEMENT)) {
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
                if (a.is(ModTags.UNSAFE_BUFF_BUFFING) || a.is(ModTags.SAFE_BUFF_BUFFING) || a.is(ModTags.DEBUFF_BUFFING)) {
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
}
