package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.item.HellPass;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.util.ContractTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MRItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MagicRealms.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_NOVICE =
            ITEMS.register("contract_novice", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .fireResistant(),
                    ContractTier.NOVICE
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_APPRENTICE =
            ITEMS.register("contract_apprentice", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .fireResistant(),
                    ContractTier.APPRENTICE
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_JOURNEYMAN =
            ITEMS.register("contract_journeyman", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .fireResistant(),
                    ContractTier.JOURNEYMAN
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_EXPERT =
            ITEMS.register("contract_expert", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .fireResistant(),
                    ContractTier.EXPERT
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_MASTER =
            ITEMS.register("contract_master", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .fireResistant(),
                    ContractTier.MASTER
            ));

    public static final DeferredHolder<Item, PermanentContractItem> CONTRACT_PERMANENT =
            ITEMS.register("contract_permanent", () -> new PermanentContractItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .fireResistant()
                            .rarity(net.minecraft.world.item.Rarity.EPIC)
            ));

    public static final DeferredHolder<Item, HellPass> HELL_PASS =
            ITEMS.register("hell_pass", HellPass::new);
}
