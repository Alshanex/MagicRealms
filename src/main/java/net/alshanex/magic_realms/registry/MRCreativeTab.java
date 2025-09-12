package net.alshanex.magic_realms.registry;

import io.redspace.ironsspellbooks.registries.CreativeTabRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MRCreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicRealms.MODID);

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("magic_realms_main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MagicRealms.MODID + ".main_tab"))
            .icon(() -> new ItemStack(MRItems.CONTRACT_MASTER))
            .displayItems((enabledFeatures, entries) -> {
                entries.accept(MRItems.CONTRACT_PERMANENT.get());
                entries.accept(MRItems.CONTRACT_NOVICE.get());
                entries.accept(MRItems.CONTRACT_EXPERT.get());
                entries.accept(MRItems.CONTRACT_MASTER.get());
                entries.accept(MRItems.CONTRACT_APPRENTICE.get());
                entries.accept(MRItems.CONTRACT_JOURNEYMAN.get());
                entries.accept(MRItems.HELL_PASS.get());
                entries.accept(MRItems.TIME_ESSENCE.get());
            })
            .withTabsBefore(CreativeTabRegistry.EQUIPMENT_TAB.getId())
            .build());
}
