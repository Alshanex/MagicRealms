package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

public class ModLootTables {
    public static final ResourceKey<LootTable> MAGIC_SLIME_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "entities/magic_slime")
    );

    public static final ResourceKey<LootTable> MAGIC_CREEPER_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "entities/magic_creeper")
    );

    public static final ResourceKey<LootTable> ENDERMAGE_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "entities/endermage")
    );

    public static final ResourceKey<LootTable> TIM_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "entities/tim")
    );
}
