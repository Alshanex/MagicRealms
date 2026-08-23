package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.SetPageLootFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MRLootRegistry {
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, MagicRealms.MODID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<SetPageLootFunction>> SET_PAGE =
            LOOT_FUNCTIONS.register("set_page", () -> {
                var type = new LootItemFunctionType<>(SetPageLootFunction.CODEC);
                SetPageLootFunction.TYPE = type;
                return type;
            });

    public static void register(IEventBus bus) {
        LOOT_FUNCTIONS.register(bus);
    }
}
