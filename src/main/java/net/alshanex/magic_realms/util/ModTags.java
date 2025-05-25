package net.alshanex.magic_realms.util;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class ModTags {
    public static TagKey<AbstractSpell> COMMON_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "common_warrior_spells"));
    public static TagKey<AbstractSpell> RARE_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "rare_warrior_spells"));
    public static TagKey<AbstractSpell> LEGENDARY_WARRIOR_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "legendary_warrior_spells"));

    public static TagKey<AbstractSpell> COMMON_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "common_assassin_spells"));
    public static TagKey<AbstractSpell> RARE_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "rare_assassin_spells"));
    public static TagKey<AbstractSpell> LEGENDARY_ASSASSIN_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "legendary_assassin_spells"));

    public static TagKey<AbstractSpell> COMMON_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "common_archer_spells"));
    public static TagKey<AbstractSpell> RARE_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "rare_archer_spells"));
    public static TagKey<AbstractSpell> LEGENDARY_ARCHER_SPELLS = create(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "common_archer_spells"));

    public static TagKey<AbstractSpell> create(ResourceLocation name) {
        return new TagKey<AbstractSpell>(SpellRegistry.SPELL_REGISTRY_KEY, name);
    }
}
