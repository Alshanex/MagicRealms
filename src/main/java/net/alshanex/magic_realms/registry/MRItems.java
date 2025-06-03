package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.item.HumanInfoItem;
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

    public static final DeferredHolder<Item, HumanInfoItem> HUMAN_INFO_ITEM =
            ITEMS.register("human_info_item", () -> new HumanInfoItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .fireResistant()
            ));
}
