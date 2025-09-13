package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.item.HellPass;
import net.alshanex.magic_realms.item.PermanentBloodPact;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.util.ContractTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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
                            .stacksTo(64),
                    ContractTier.NOVICE
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_APPRENTICE =
            ITEMS.register("contract_apprentice", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
                    ContractTier.APPRENTICE
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_JOURNEYMAN =
            ITEMS.register("contract_journeyman", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
                    ContractTier.JOURNEYMAN
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_EXPERT =
            ITEMS.register("contract_expert", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
                    ContractTier.EXPERT
            ));

    public static final DeferredHolder<Item, TieredContractItem> CONTRACT_MASTER =
            ITEMS.register("contract_master", () -> new TieredContractItem(
                    new Item.Properties()
                            .stacksTo(64),
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

    public static final DeferredHolder<Item, Item> BLOOD_PACT =
            ITEMS.register("blood_pact", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, PermanentBloodPact> PERMANENT_BLOOD_PACT =
            ITEMS.register("permanent_blood_pact", PermanentBloodPact::new);
}
